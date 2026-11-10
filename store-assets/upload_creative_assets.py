#!/usr/bin/env python3
"""Upload the product page header and search results art to App Store Connect.

usage: upload_creative_assets.py [--version 1.2.3] [--locale zh-Hant,ja] [--wait-minutes 30]

Reads   store-assets/final/ios/creative/<repo-locale>/header.png   3840x1646
        store-assets/final/ios/creative/<repo-locale>/search.png   3840x2560 (any 3:2 from 1920x1280)
Env     ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_PATH (App Store Connect API key, .p8)

fastlane does not handle these placements, so this talks to the App Store Connect API directly
(standard library + the openssl CLI, no pip installs). For each localization of the target version:
  1. upload every image that isn't placed yet: reserve it in the app's asset library (category
     CREATIVE_ASSETS), PUT the parts, commit. An earlier upload of the same bytes is reused instead
     (found by referenceName, which carries the MD5), so a re-run picks up where the last one stopped.
  2. wait for Apple to process all of them together, placing each one as soon as it is ready:
     replace the localization's placement of that type (placements can't be edited)
An image whose bytes already sit in that placement is skipped. Apple's processing time varies; an
image still processing when --wait-minutes runs out is left in the library for the next run.
The placed art goes to App Review with the version, like screenshots.
"""
import argparse, base64, hashlib, json, os, struct, subprocess, sys, time, urllib.error, urllib.parse, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
CREATIVE = os.path.join(HERE, "final", "ios", "creative")
BUNDLE_ID = "com.kevlina.budgetplus"
API = "https://api.appstoreconnect.apple.com"
LOCALES = {"en-US": "en-US", "zh-Hant": "zh-Hant", "zh-Hans": "zh-Hans", "ja": "ja", "ko": "ko"}  # repo -> ASC
KINDS = {"header": "PRODUCT_PAGE_HEADER_ASSET", "search": "APP_STORE_SEARCH_RESULTS_ASSET"}
EDITABLE = {"PREPARE_FOR_SUBMISSION", "DEVELOPER_REJECTED", "REJECTED", "METADATA_REJECTED", "INVALID_BINARY"}
PROCESSING = {"AWAITING_UPLOAD", "UPLOAD_COMPLETE"}          # still with Apple
BROKEN = {"FAILED", "REJECTED", "ARCHIVED"}                     # can't be placed


# ---------- validation (shared with stage_for_fastlane.py) ----------
def png_info(path):
    with open(path, "rb") as f:
        head = f.read(26)
    if head[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"{path}: not a PNG")
    w, h = struct.unpack(">II", head[16:24])
    return w, h, head[25]

def check_creative(locales=LOCALES):
    """Return (errors, summary) for store-assets/final/ios/creative."""
    errors, summary = [], []
    for loc in locales:
        for kind in KINDS:
            p = os.path.join(CREATIVE, loc, kind + ".png")
            if not os.path.isfile(p): errors.append(f"missing ios/creative/{loc}/{kind}.png"); continue
            w, h, ct = png_info(p)
            if ct != 2: errors.append(f"ios/creative/{loc}/{kind}.png: color type {ct}, expected RGB without alpha")
            if kind == "header" and (w, h) != (3840, 1646):
                errors.append(f"ios/creative/{loc}/header.png: {w}x{h}, expected 3840x1646")
            if kind == "search" and not (w * 2 == h * 3 and 1920 <= w <= 3840):
                errors.append(f"ios/creative/{loc}/search.png: {w}x{h}, expected 3:2 from 1920x1280 to 3840x2560")
        summary.append(f"ios      creative           {loc:8} -> {LOCALES[loc]:8} header + search")
    return errors, summary


# ---------- App Store Connect API ----------
def b64url(b):
    return base64.urlsafe_b64encode(b).rstrip(b"=").decode()

def der_to_raw(der):
    """ECDSA DER signature (openssl) -> 64-byte r||s (JWS ES256)."""
    def read_int(b, i):
        assert b[i] == 0x02; n = b[i + 1]; return int.from_bytes(b[i + 2:i + 2 + n], "big"), i + 2 + n
    i = 2 if der[1] < 0x80 else 3
    r, i = read_int(der, i); s, _ = read_int(der, i)
    return r.to_bytes(32, "big") + s.to_bytes(32, "big")

class ASC:
    def __init__(self, key_id, issuer, key_path):
        self.key_id, self.issuer, self.key_path = key_id, issuer, key_path
        self._token, self._issued = None, 0

    def token(self):
        if self._token and time.time() - self._issued < 900: return self._token
        now = int(time.time())
        head = b64url(json.dumps({"alg": "ES256", "kid": self.key_id, "typ": "JWT"}).encode())
        body = b64url(json.dumps({"iss": self.issuer, "iat": now, "exp": now + 1200, "aud": "appstoreconnect-v1"}).encode())
        msg = f"{head}.{body}".encode()
        der = subprocess.run(["openssl", "dgst", "-sha256", "-sign", self.key_path], input=msg,
                             capture_output=True, check=True).stdout
        self._token, self._issued = f"{head}.{body}.{b64url(der_to_raw(der))}", now
        return self._token

    def call(self, method, path, body=None):
        url = path if path.startswith("http") else API + path
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(url, data=data, method=method, headers={
            "Authorization": f"Bearer {self.token()}", "Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                raw = r.read(); return r.status, (json.loads(raw) if raw else {})
        except urllib.error.HTTPError as e:
            raw = e.read()
            try: return e.code, json.loads(raw)
            except ValueError: return e.code, {"raw": raw.decode(errors="replace")}

    def must(self, method, path, body=None):
        status, payload = self.call(method, path, body)
        if status not in (200, 201, 204):
            sys.exit(f"{method} {path} -> {status}\n{json.dumps(payload, indent=2)[:2000]}")
        return payload

def q(params):
    return "?" + urllib.parse.urlencode(params)

def errors_text(payload):
    return " | ".join(f"{e.get('code')}: {e.get('detail')}" for e in payload.get("errors", [])) or json.dumps(payload)[:500]


def find_version(api, app_id, wanted):
    versions = api.must("GET", f"/v1/apps/{app_id}/appStoreVersions" + q({"filter[platform]": "IOS", "limit": 50}))["data"]
    def state(v): return v["attributes"].get("appVersionState") or v["attributes"].get("appStoreState")
    if wanted:
        for v in versions:
            if v["attributes"]["versionString"] == wanted:
                if state(v) not in EDITABLE:
                    print(f"warning: version {wanted} is {state(v)}; Apple may refuse new placements on it")
                return v
        sys.exit(f"no iOS App Store version {wanted}")
    for v in versions:
        if state(v) in EDITABLE: return v
    sys.exit("no editable iOS App Store version (Prepare for Submission); create one or pass --version")

def find_upload(api, library_id, ref):
    """An earlier upload of the same bytes that can still be placed, or None."""
    imgs = api.must("GET", f"/v1/appAssetLibraries/{library_id}/images" + q({
        "filter[referenceName]": ref, "filter[category]": "CREATIVE_ASSETS", "sort": "-createdDate", "limit": 20}))["data"]
    usable = [i for i in imgs if i["attributes"].get("state") not in BROKEN | {"AWAITING_UPLOAD"}]
    return usable[0] if usable else None

def upload_image(api, library_id, path, ref):
    data = open(path, "rb").read()
    res = api.must("POST", "/v1/appAssetLibraryImages", {"data": {
        "type": "appAssetLibraryImages",
        "attributes": {"category": "CREATIVE_ASSETS", "fileName": os.path.basename(path),
                       "fileSize": len(data), "referenceName": ref},
        "relationships": {"assetLibrary": {"data": {"type": "appAssetLibraries", "id": library_id}}}}})["data"]
    for op in res["attributes"].get("uploadOperations") or []:
        part = data[op["offset"]:op["offset"] + op["length"]]
        headers = {h["name"]: h["value"] for h in op.get("requestHeaders") or []}
        req = urllib.request.Request(op["url"], data=part, method=op["method"], headers=headers)  # no bearer token
        with urllib.request.urlopen(req, timeout=300) as r:
            if r.status >= 300: sys.exit(f"upload part failed: {r.status}")
    api.must("PATCH", f"/v1/appAssetLibraryImages/{res['id']}",
             {"data": {"type": "appAssetLibraryImages", "id": res["id"], "attributes": {"uploaded": True}}})
    return res["id"]

def placements(api, loc_id):
    path = f"/v1/appStoreVersionLocalizations/{loc_id}/placements"
    status, payload = api.call("GET", path + q({"include": "image", "limit": 50}))
    if status != 200: payload = api.must("GET", path + q({"limit": 50}))
    refs = {i["id"]: i["attributes"].get("referenceName") for i in payload.get("included", [])
            if i["type"] == "appAssetLibraryImages"}
    out = []
    for p in payload.get("data", []):
        img = ((p.get("relationships") or {}).get("image") or {}).get("data") or {}
        out.append({"id": p["id"], "type": p["attributes"].get("placementType"),
                    "group": p["attributes"].get("placementGroup"), "ref": refs.get(img.get("id"))})
    return out

def place(api, loc_id, image_id, ptype, group):
    """Create the placement. Returns None on success, else an error message."""
    attrs = {"placementType": ptype}
    if group: attrs["placementGroup"] = group
    body = {"data": {"type": "appAssetLibraryPlacements", "attributes": attrs, "relationships": {
        "appStoreVersionLocalization": {"data": {"type": "appStoreVersionLocalizations", "id": loc_id}},
        "image": {"data": {"type": "appAssetLibraryImages", "id": image_id}}}}}
    for attempt in range(8):                              # Apple may still be post-processing the image
        status, payload = api.call("POST", "/v1/appAssetLibraryPlacements", body)
        if status in (200, 201): return None
        if status == 409 and "POST_PROCESSING" in json.dumps(payload) and attempt < 7:
            time.sleep(15); continue
        hint = ""
        if "placementGroup" in json.dumps(payload):
            hint = " (this placement needs a placementGroup; see GET /v1/appAssetLibraryRefData)"
        return f"placing failed ({status}): {errors_text(payload)}{hint}"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--version", default="", help="App Store version string; default: the editable version")
    ap.add_argument("--locale", default="", help="comma-separated repo locales; default: all")
    ap.add_argument("--wait-minutes", type=int, default=30, help="how long to wait for Apple to process uploads")
    a = ap.parse_args()
    locales = {k: v for k, v in LOCALES.items() if not a.locale or k in a.locale.split(",")}
    errors, _ = check_creative(locales)
    if errors: sys.exit("ERRORS:\n" + "\n".join(errors))

    api = ASC(os.environ["ASC_KEY_ID"], os.environ["ASC_ISSUER_ID"], os.environ["ASC_KEY_PATH"])
    apps = api.must("GET", "/v1/apps" + q({"filter[bundleId]": BUNDLE_ID}))["data"]
    if not apps: sys.exit(f"no app with bundle id {BUNDLE_ID}")
    app_id = apps[0]["id"]
    version = find_version(api, app_id, a.version)
    print(f"version {version['attributes']['versionString']} ({version['id']})")
    library_id = api.must("GET", f"/v1/apps/{app_id}/assetLibrary")["data"]["id"]
    locs = {l["attributes"]["locale"]: l["id"] for l in api.must(
        "GET", f"/v1/appStoreVersions/{version['id']}/appStoreVersionLocalizations" + q({"limit": 50}))["data"]}

    # 1. work out what has to change, and upload (or reuse) those images
    jobs = []
    for repo_loc, asc_loc in locales.items():
        if asc_loc not in locs:
            print(f"{asc_loc:8} skipped: the version has no {asc_loc} localization"); continue
        current = placements(api, locs[asc_loc])
        for kind, ptype in KINDS.items():
            path = os.path.join(CREATIVE, repo_loc, kind + ".png")
            ref = f"{asc_loc}-{kind}-{hashlib.md5(open(path, 'rb').read()).hexdigest()}"
            old = [p for p in current if p["type"] == ptype]
            label = f"{asc_loc:8} {kind:6}"
            if any(p["ref"] == ref for p in old):
                print(f"{label} unchanged"); continue
            found = find_upload(api, library_id, ref)
            if found:
                image_id = found["id"]; print(f"{label} reusing earlier upload ({found['attributes'].get('state')})")
            else:
                image_id = upload_image(api, library_id, path, ref); print(f"{label} uploaded")
            jobs.append({"label": label, "loc": locs[asc_loc], "ptype": ptype, "old": old, "image": image_id, "state": None})

    # 2. wait for Apple to process them together; place each one as soon as it is ready
    deadline, failed = time.time() + a.wait_minutes * 60, []
    while jobs:
        for j in list(jobs):
            attrs = api.must("GET", f"/v1/appAssetLibraryImages/{j['image']}")["data"]["attributes"]
            st = attrs.get("state")
            if st != j["state"]:
                details = attrs.get("stateDetails")
                print(f"{j['label']} {st}" + (f"  {json.dumps(details)}" if details else "")); j["state"] = st
            if st in PROCESSING: continue
            jobs.remove(j)
            if st in BROKEN:
                failed.append(f"{j['label']} {st}"); continue
            for p in j["old"]:                             # placements can't be edited: replace
                api.must("DELETE", f"/v1/appAssetLibraryPlacements/{p['id']}")
            err = place(api, j["loc"], j["image"], j["ptype"], next((p["group"] for p in j["old"] if p["group"]), None))
            if err: failed.append(f"{j['label']} {err}")
            else: print(f"{j['label']} placed")
        if jobs and time.time() > deadline:
            failed += [f"{j['label']} still processing ({j['state']}) after {a.wait_minutes} min" for j in jobs]; break
        if jobs: time.sleep(15)

    if failed:
        sys.exit("\nNot placed:\n  " + "\n  ".join(failed) +
                 "\nRe-run to finish: finished uploads are reused, placed ones are skipped.")
    print("\nOK. Check each language with Preview in App Store Connect before submitting.")

if __name__ == "__main__":
    main()
