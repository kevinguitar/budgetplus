#!/usr/bin/env python3
"""Upload in-app product and subscription names/descriptions from store-assets/in-app.

usage: upload_in_app_listings.py --store ios|android [--dry-run]

Env  ios:     ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_PATH (App Store Connect API key, .p8)
     android: PLAY_KEY_PATH (Play service account JSON); PLAY_REGIONS_VERSION optional

fastlane does not manage in-app products, so this calls the store APIs directly (standard library
+ the openssl CLI, no pip installs). Only products, locales, and fields in the repo are touched;
other locales stay as they are. Products are never created, priced, or deleted.

App Store: In-App Purchase, subscription, and subscription group metadata is versioned. If the
text differs from the latest version, the latest version is edited when it is still a draft;
after approval a new draft version is created (Apple copies the current text into it). Drafts are
NOT submitted: submit them for review in App Store Connect, alone or with an app version. Products
whose latest version is waiting for or in review fail without changes.

Google Play: one-time products and subscriptions are patched with updateMask=listings and go live
after Play's usual processing; benefits.txt is optional and only valid for subscriptions.

--dry-run reads the stores and prints what would change without writing anything.
"""
import argparse, base64, json, os, subprocess, sys, tempfile, time, urllib.error, urllib.parse, urllib.request
from collections import defaultdict
from dataclasses import dataclass

from in_app_listings import load_in_app_listings
from upload_creative_assets import ASC, BUNDLE_ID, errors_text, q

PACKAGE = "com.kevlina.budgetplus"
PLAY_API = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE}"
PLAY_SCOPE = "https://www.googleapis.com/auth/androidpublisher"
# Google documents 2022/02 as the regions version; one-time products report their own.
DEFAULT_REGIONS_VERSION = "2022/02"

EDITABLE = {"PREPARE_FOR_SUBMISSION", "READY_FOR_REVIEW", "REJECTED", "DEVELOPER_REJECTED"}
IN_REVIEW = {"WAITING_FOR_REVIEW", "IN_REVIEW"}
ATTRIBUTES = {"name": "name", "description": "description", "custom_app_name": "customAppName"}


class SyncError(Exception):
    pass


@dataclass(frozen=True)
class AscKind:
    label: str
    versions: str              # versions of the product/group
    version_type: str
    parent_relationship: str
    parent_type: str
    localizations: str         # localizations of one version
    localization_type: str
    localization_path: str     # v2 create/update endpoint, scoped to a version


ASC_KINDS = {
    "iap": AscKind("In-App Purchase", "/v2/inAppPurchases/{id}/versions", "inAppPurchaseVersions",
                   "inAppPurchase", "inAppPurchases", "/v1/inAppPurchaseVersions/{id}/localizations",
                   "inAppPurchaseLocalizations", "/v2/inAppPurchaseLocalizations"),
    "subscription": AscKind("subscription", "/v1/subscriptions/{id}/versions", "subscriptionVersions",
                            "subscription", "subscriptions", "/v1/subscriptionVersions/{id}/localizations",
                            "subscriptionLocalizations", "/v2/subscriptionLocalizations"),
    "group": AscKind("subscription group", "/v1/subscriptionGroups/{id}/versions", "subscriptionGroupVersions",
                     "subscriptionGroup", "subscriptionGroups", "/v1/subscriptionGroupVersions/{id}/localizations",
                     "subscriptionGroupLocalizations", "/v2/subscriptionGroupLocalizations"),
}


# ---------- App Store Connect ----------
def asc_request(api, method, path, body=None):
    status, payload = api.call(method, path, body)
    if status not in (200, 201, 204):
        raise SyncError(f"{method} {path.split('?')[0]} -> {status}: {errors_text(payload)}")
    return payload


def asc_all(api, path):
    data = []
    while path:
        payload = asc_request(api, "GET", path)
        data.extend(payload["data"])
        path = payload.get("links", {}).get("next")
    return data


def asc_targets(api, app_id):
    """{(source kind, key): (ASC kind, id)} for every product and subscription group of the app."""
    targets = {}
    for iap in asc_all(api, f"/v1/apps/{app_id}/inAppPurchasesV2" + q({"limit": 200})):
        targets["products", iap["attributes"]["productId"]] = ("iap", iap["id"])
    for group in asc_all(api, f"/v1/apps/{app_id}/subscriptionGroups" + q({"limit": 200})):
        targets["subscription-groups", group["attributes"]["referenceName"]] = ("group", group["id"])
        for sub in asc_all(api, f"/v1/subscriptionGroups/{group['id']}/subscriptions" + q({"limit": 200})):
            targets["products", sub["attributes"]["productId"]] = ("subscription", sub["id"])
    return targets


def asc_localizations(api, kind, version_id):
    """{locale: (localization id, attributes)} of one version."""
    path = kind.localizations.format(id=version_id) + q({"limit": 200})
    return {loc["attributes"]["locale"]: (loc["id"], loc["attributes"]) for loc in asc_all(api, path)}


def asc_changes(wanted, current):
    """{locale: attributes to send}: all attributes for new locales, only differing ones otherwise."""
    changes = {}
    for locale, attributes in sorted(wanted.items()):
        if locale not in current:
            changes[locale] = attributes
            continue
        changed = {k: v for k, v in attributes.items() if current[locale][1].get(k) != v}
        if changed:
            changes[locale] = changed
    return changes


def sync_asc_item(api, kind, parent_id, wanted, dry_run=False):
    """Bring one product/group's latest metadata version in line with wanted. Returns a summary."""
    versions = asc_all(api, kind.versions.format(id=parent_id) + q({"limit": 200}))
    latest = max(versions, key=lambda v: v["attributes"].get("version") or 0, default=None)
    state = latest["attributes"].get("state") if latest else None
    current = asc_localizations(api, kind, latest["id"]) if latest else {}
    changes = asc_changes(wanted, current)
    if not changes:
        return "unchanged"
    if state in IN_REVIEW:
        raise SyncError(f"version {latest['attributes'].get('version')} is {state}; "
                        "wait for App Review before changing its text")
    locales = ", ".join(changes)
    if dry_run:
        where = "the draft" if state in EDITABLE else "a new draft version"
        return f"would update {locales} on {where}"

    created = state not in EDITABLE
    if created:
        version = asc_request(api, "POST", f"/v1/{kind.version_type}", {"data": {
            "type": kind.version_type,
            "relationships": {kind.parent_relationship: {"data": {"type": kind.parent_type, "id": parent_id}}}}})["data"]
        # The new draft starts from the current text; only real differences remain.
        current = asc_localizations(api, kind, version["id"])
        changes = asc_changes(wanted, current)
    else:
        version = latest

    for locale, attributes in changes.items():
        if locale in current:
            loc_id = current[locale][0]
            asc_request(api, "PATCH", f"{kind.localization_path}/{loc_id}", {"data": {
                "type": kind.localization_type, "id": loc_id, "attributes": attributes}})
        else:
            asc_request(api, "POST", kind.localization_path, {"data": {
                "type": kind.localization_type,
                "attributes": {"locale": locale, **attributes},
                "relationships": {"version": {"data": {"type": kind.version_type, "id": version["id"]}}}}})
    number = version["attributes"].get("version")
    return f"updated {', '.join(changes) or 'nothing'} on {'new ' if created else ''}draft version {number}"


def upload_ios(listings, dry_run):
    wanted = defaultdict(dict)
    for item in listings:
        wanted[item.kind, item.key][item.locale] = {ATTRIBUTES[k]: v for k, v in item.fields.items()}
    if not wanted:
        print("No App Store in-app text in store-assets/in-app/ios; nothing to upload.")
        return []

    api = ASC(os.environ["ASC_KEY_ID"], os.environ["ASC_ISSUER_ID"], os.environ["ASC_KEY_PATH"])
    apps = api.must("GET", "/v1/apps" + q({"filter[bundleId]": BUNDLE_ID}))["data"]
    if len(apps) != 1:
        return [f"expected one app with bundle id {BUNDLE_ID}, found {len(apps)}"]
    targets = asc_targets(api, apps[0]["id"])

    failures, drafts = [], False
    for (source_kind, key), locales in sorted(wanted.items()):
        target = targets.get((source_kind, key))
        label = f"{key:40}"
        if target is None:
            what = "product ID" if source_kind == "products" else "subscription group reference name"
            failures.append(f"{key}: no App Store {what} {key!r}; products are never created here")
            continue
        kind = ASC_KINDS[target[0]]
        try:
            summary = sync_asc_item(api, kind, target[1], locales, dry_run)
        except SyncError as error:
            failures.append(f"{key} ({kind.label}): {error}")
            continue
        drafts |= summary.startswith("updated")
        print(f"{label} {kind.label:18} {summary}")
    if drafts:
        print("\nDraft versions are not submitted. Submit them for review in App Store Connect "
              "(In-App Purchases / Subscriptions), on their own or with the next app version.")
    return failures


# ---------- Google Play ----------
def b64url(data):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def rs256_sign(private_key_pem, message):
    """PKCS#1 v1.5 SHA-256 signature via the openssl CLI; the key only touches a private temp file."""
    with tempfile.TemporaryDirectory() as temporary:
        key_path = os.path.join(temporary, "key.pem")
        descriptor = os.open(key_path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(descriptor, "w") as key_file:
            key_file.write(private_key_pem)
        return subprocess.run(["openssl", "dgst", "-sha256", "-sign", key_path], input=message,
                              capture_output=True, check=True).stdout


class Play:
    def __init__(self, key_path):
        with open(key_path, encoding="utf-8") as key_file:
            info = json.load(key_file)
        self.email, self.private_key = info["client_email"], info["private_key"]
        self.token_uri = info.get("token_uri") or "https://oauth2.googleapis.com/token"
        self._token, self._issued = None, 0

    def token(self):
        if self._token and time.time() - self._issued < 1800:
            return self._token
        now = int(time.time())
        head = b64url(json.dumps({"alg": "RS256", "typ": "JWT"}).encode())
        claims = b64url(json.dumps({"iss": self.email, "scope": PLAY_SCOPE, "aud": self.token_uri,
                                    "iat": now, "exp": now + 3600}).encode())
        signature = rs256_sign(self.private_key, f"{head}.{claims}".encode())
        form = urllib.parse.urlencode({"grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
                                       "assertion": f"{head}.{claims}.{b64url(signature)}"}).encode()
        request = urllib.request.Request(self.token_uri, data=form, method="POST",
                                         headers={"Content-Type": "application/x-www-form-urlencoded"})
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                self._token = json.loads(response.read())["access_token"]
        except urllib.error.HTTPError as error:
            raise SyncError(f"Google OAuth token request -> {error.code}: {error.read().decode(errors='replace')[:500]}")
        self._issued = now
        return self._token

    def call(self, method, path, body=None):
        data = json.dumps(body).encode() if body is not None else None
        request = urllib.request.Request(PLAY_API + path, data=data, method=method, headers={
            "Authorization": f"Bearer {self.token()}", "Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                raw = response.read()
                return response.status, (json.loads(raw) if raw else {})
        except urllib.error.HTTPError as error:
            raw = error.read()
            try:
                return error.code, json.loads(raw)
            except ValueError:
                return error.code, {"raw": raw.decode(errors="replace")}


def play_request(play, method, path, body=None):
    status, payload = play.call(method, path, body)
    if status not in (200, 204):
        message = (payload.get("error") or {}).get("message") or json.dumps(payload)[:500]
        raise SyncError(f"{method} {path.split('?')[0]} -> {status}: {message}")
    return payload


def play_all(play, path, key):
    items, token = [], None
    while True:
        params = {"pageSize": 1000, **({"pageToken": token} if token else {})}
        payload = play_request(play, "GET", path + "?" + urllib.parse.urlencode(params))
        items.extend(payload.get(key, []))
        token = payload.get("nextPageToken")
        if not token:
            return items


def merged_listings(product, wanted, subscription):
    """(listings to send, changed locales). Locales not in the repo are kept unchanged."""
    current = {listing["languageCode"]: listing for listing in product.get("listings", [])}
    merged, changed = dict(current), []
    for locale, fields in sorted(wanted.items()):
        if "benefits" in fields and not subscription:
            raise SyncError(f"{locale}/benefits.txt: benefits are only supported for subscriptions")
        listing = dict(current.get(locale, {}), languageCode=locale,
                       title=fields["title"], description=fields["description"])
        if "benefits" in fields:
            listing["benefits"] = fields["benefits"].split("\n")
        if listing != current.get(locale):
            merged[locale] = listing
            changed.append(locale)
    return list(merged.values()), changed


def sync_play_product(play, product, subscription, wanted, dry_run=False):
    listings, changed = merged_listings(product, wanted, subscription)
    if not changed:
        return "unchanged"
    if dry_run:
        return f"would update {', '.join(changed)}"
    if subscription:
        path = f"/subscriptions/{product['productId']}"
        regions = os.environ.get("PLAY_REGIONS_VERSION") or DEFAULT_REGIONS_VERSION
    else:
        path = f"/onetimeproducts/{product['productId']}"
        regions = (product.get("regionsVersion") or {}).get("version") or DEFAULT_REGIONS_VERSION
    body = {k: v for k, v in product.items() if k != "regionsVersion"}  # output only
    body["listings"] = listings
    query = urllib.parse.urlencode({"updateMask": "listings", "regionsVersion.version": regions})
    play_request(play, "PATCH", f"{path}?{query}", body)
    return f"updated {', '.join(changed)}"


def upload_android(listings, dry_run):
    wanted = defaultdict(dict)
    for item in listings:
        wanted[item.key][item.locale] = item.fields
    if not wanted:
        print("No Google Play in-app text in store-assets/in-app/android; nothing to upload.")
        return []

    play = Play(os.environ["PLAY_KEY_PATH"])
    one_time = {p["productId"]: p for p in play_all(play, "/oneTimeProducts", "oneTimeProducts")}
    subscriptions = {p["productId"]: p for p in play_all(play, "/subscriptions", "subscriptions")}
    failures = []
    for product_id, locales in sorted(wanted.items()):
        subscription = product_id in subscriptions
        product = subscriptions.get(product_id) or one_time.get(product_id)
        if product is None:
            failures.append(f"{product_id}: no Play one-time product or subscription with this ID; "
                            "products are never created here")
            continue
        kind = "subscription" if subscription else "one-time product"
        try:
            print(f"{product_id:40} {kind:18} {sync_play_product(play, product, subscription, locales, dry_run)}")
        except SyncError as error:
            failures.append(f"{product_id} ({kind}): {error}")
    return failures


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--store", choices=("ios", "android"), required=True)
    parser.add_argument("--dry-run", action="store_true", help="read the store and print changes without writing")
    args = parser.parse_args()
    try:
        listings = load_in_app_listings(target=args.store)
    except ValueError as error:
        sys.exit(f"In-app listing validation failed:\n{error}")
    try:
        failures = (upload_ios if args.store == "ios" else upload_android)(listings, args.dry_run)
    except SyncError as error:
        failures = [str(error)]
    if failures:
        sys.exit("\nNot updated:\n  " + "\n  ".join(failures))
    print("\nOK." + (" Dry run: nothing was written." if args.dry_run else ""))


if __name__ == "__main__":
    main()
