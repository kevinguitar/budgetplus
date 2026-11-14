"""Offline tests for in-app listing validation and the upload logic, against fake store APIs."""

import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import unittest
import unittest.mock
import urllib.parse

from in_app_listings import SOURCE, load_in_app_listings
from listings import ROOT
import upload_in_app_listings as upload
from upload_in_app_listings import ASC_KINDS, SyncError, merged_listings, sync_asc_item, sync_play_product


class InAppValidationTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        # Android sources may hold empty placeholders; these tests write their own Android fixtures.
        shutil.copytree(ROOT / SOURCE / "ios", self.root / SOURCE / "ios")

    def write(self, relative, text):
        path = self.root / SOURCE / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(text.encode("utf-8"))

    def android_product(self, product="budgetplus.premium", locale="zh-TW", **extra):
        self.write(f"android/{product}/{locale}/title.txt", "極簡記帳－進階版\n")
        self.write(f"android/{product}/{locale}/description.txt", "不受任何廣告打擾\n")
        for name, text in extra.items():
            self.write(f"android/{product}/{locale}/{name}.txt", text)

    def assert_invalid(self, pattern):
        with self.assertRaisesRegex(ValueError, pattern):
            load_in_app_listings(self.root)

    def test_repo_sources_are_valid_and_every_product_has_traditional_chinese(self):
        listings = load_in_app_listings()
        self.assertTrue(listings)
        for key in {item.key for item in listings}:
            locales = {item.locale for item in listings if item.key == key}
            self.assertTrue({"zh-Hant", "zh-TW"} & locales, key)

    def test_missing_store_directory_means_nothing_to_upload(self):
        shutil.rmtree(self.root / SOURCE)
        self.assertEqual(load_in_app_listings(self.root), [])

    def test_android_product_with_optional_benefits(self):
        self.android_product(benefits="No ads\nUp to 10 books\n")
        listing = next(i for i in load_in_app_listings(self.root, "android") if i.key == "budgetplus.premium")
        self.assertEqual(listing.fields, {"title": "極簡記帳－進階版", "description": "不受任何廣告打擾",
                                          "benefits": "No ads\nUp to 10 books"})

    def test_target_filters_stores(self):
        self.android_product()
        self.assertEqual({i.store for i in load_in_app_listings(self.root, "ios")}, {"ios"})
        self.assertEqual({i.store for i in load_in_app_listings(self.root, "android")}, {"android"})

    def test_traditional_chinese_is_required(self):
        self.android_product(locale="en-US")
        self.assert_invalid(r"budgetplus\.premium/zh-TW: missing")

    def test_store_specific_locale_codes(self):
        self.android_product()
        self.android_product(locale="ja")
        self.assert_invalid(r"ja: unexpected locale")

    def test_limits(self):
        cases = (
            ("ios/products/budgetplus.premium.monthly/en-US/name.txt", "n" * 36, "36 characters; expected 2–35"),
            ("ios/products/budgetplus.premium.monthly/en-US/name.txt", "n", "1 characters; expected 2–35"),
            ("ios/products/budgetplus.premium.monthly/en-US/description.txt", "d" * 56, "expected 1–55"),
            ("android/budgetplus.premium/zh-TW/title.txt", "t" * 56, "expected 1–55"),
            ("android/budgetplus.premium/zh-TW/description.txt", "d" * 201, "expected 1–200"),
        )
        for relative, text, message in cases:
            with self.subTest(relative=relative, length=len(text)):
                self.setUp()
                self.android_product()
                self.write(relative, text)
                self.assert_invalid(message)

    def test_at_limit_passes(self):
        self.android_product()
        self.write("ios/products/budgetplus.premium.monthly/en-US/name.txt", "n" * 35 + "\n")
        self.write("ios/products/budgetplus.premium.monthly/en-US/description.txt", "d" * 55)
        self.write("android/budgetplus.premium/zh-TW/title.txt", "t" * 55)
        self.write("android/budgetplus.premium/zh-TW/description.txt", "d" * 200)
        load_in_app_listings(self.root)

    def test_benefits_rules(self):
        for text, message in (("a\nb\nc\nd\ne", "5 benefits; expected at most 4"),
                              ("b" * 41, "line 1 has 41 characters"),
                              ("a\n\nb", "line 2 is empty")):
            with self.subTest(text=text):
                self.setUp()
                self.android_product(benefits=text)
                self.assert_invalid(message)

    def test_fields_are_single_line_and_clean(self):
        for text in ("Two\nlines", " Leading", "Trailing ", "Tab\there", "﻿BOM"):
            with self.subTest(text=repr(text)):
                self.setUp()
                self.write("ios/products/budgetplus.premium.monthly/en-US/name.txt", text)
                with self.assertRaises(ValueError):
                    load_in_app_listings(self.root)

    def test_play_description_allows_line_breaks_but_title_does_not(self):
        self.android_product()
        self.write("android/budgetplus.premium/zh-TW/description.txt", "移除廣告\n自動匯率換算\n")
        load_in_app_listings(self.root)
        self.write("android/budgetplus.premium/zh-TW/title.txt", "極簡記帳\n進階版")
        self.assert_invalid("title.txt: must be a single line")

    def test_missing_required_field(self):
        (self.root / SOURCE / "ios/products/budgetplus.premium.monthly/ja/description.txt").unlink()
        self.assert_invalid(r"monthly/ja/description\.txt")

    def test_unexpected_entries(self):
        cases = (
            ("ios/products/budgetplus.premium.monthly/en-US/title.txt", "Play field on iOS", "title.txt: unexpected entry"),
            ("ios/products/budgetplus.premium.monthly/en-US/benefits.txt", "a", "benefits.txt: unexpected entry"),
            ("ios/subscriptions/x/zh-Hant/name.txt", "x", "subscriptions: unexpected directory"),
            ("ios/products/notes.txt", "x", "notes.txt: unexpected file"),
        )
        for relative, text, message in cases:
            with self.subTest(relative=relative):
                self.setUp()
                self.write(relative, text)
                self.assert_invalid(message)

    def test_product_ids(self):
        self.android_product(product="Budgetplus.Premium")
        self.assert_invalid("not a valid android product ID")

    def test_subscription_group_fields(self):
        self.write("ios/subscription-groups/Budget+ Premium/zh-Hant/name.txt", "進階版")
        self.write("ios/subscription-groups/Budget+ Premium/zh-Hant/custom_app_name.txt", "極簡記帳")
        group = next(i for i in load_in_app_listings(self.root, "ios")
                     if i.kind == "subscription-groups" and i.locale == "zh-Hant")
        self.assertEqual(group.key, "Budget+ Premium")
        self.assertEqual(group.fields, {"name": "進階版", "custom_app_name": "極簡記帳"})


class FakeASC:
    """In-memory versions/localizations for one product, speaking the ASC JSON:API shapes."""

    def __init__(self, versions):
        # versions: list of (state, {locale: {attribute: value}})
        self.versions, self.localizations, self.writes = [], {}, []
        for number, (state, locales) in enumerate(versions, 1):
            self.add_version(number, state, locales)

    def add_version(self, number, state, locales):
        version_id = f"v{number}"
        self.versions.append({"id": version_id, "attributes": {"version": number, "state": state}})
        for locale, attributes in locales.items():
            loc_id = f"{version_id}-{locale}"
            self.localizations[loc_id] = {"version": version_id, "attributes": {"locale": locale, **attributes}}
        return version_id

    def call(self, method, path, body=None):
        route = path.split("?")[0]
        if method != "GET":
            self.writes.append((method, route, body))
        if method == "GET" and re.fullmatch(r"/v[12]/\w+/p1/versions", route):
            return 200, {"data": self.versions}
        if method == "GET" and (match := re.fullmatch(r"/v1/\w+Versions/(\w+)/localizations", route)):
            return 200, {"data": [{"id": i, "attributes": l["attributes"]}
                                  for i, l in self.localizations.items() if l["version"] == match[1]]}
        if method == "POST" and route.startswith("/v1/") and route.endswith("Versions"):
            latest = self.versions[-1]
            copied = {l["attributes"]["locale"]: {k: v for k, v in l["attributes"].items() if k != "locale"}
                      for l in self.localizations.values() if l["version"] == latest["id"]}
            new_id = self.add_version(len(self.versions) + 1, "PREPARE_FOR_SUBMISSION", copied)
            return 201, {"data": self.versions[-1] | {"id": new_id}}
        if method == "PATCH" and route.startswith("/v2/"):
            self.localizations[body["data"]["id"]]["attributes"].update(body["data"]["attributes"])
            return 200, {"data": body["data"]}
        if method == "POST" and route.startswith("/v2/"):
            version_id = body["data"]["relationships"]["version"]["data"]["id"]
            attributes = body["data"]["attributes"]
            self.localizations[f"{version_id}-{attributes['locale']}"] = {"version": version_id, "attributes": attributes}
            return 201, {"data": {"id": "new"}}
        return 404, {"errors": [{"code": "NOT_FOUND", "detail": f"{method} {route}"}]}

    def text(self, version_id):
        return {l["attributes"]["locale"]: {k: v for k, v in l["attributes"].items() if k not in ("locale", "state")}
                for l in self.localizations.values() if l["version"] == version_id}


LIVE = {"zh-Hant": {"name": "極簡記帳－進階版", "description": "舊說明"}, "ja": {"name": "旧", "description": "旧"}}


class AppStoreSyncTests(unittest.TestCase):
    def test_unchanged_text_writes_nothing(self):
        for kind in ASC_KINDS.values():
            with self.subTest(kind=kind.label):
                api = FakeASC([("APPROVED", LIVE)])
                self.assertEqual(sync_asc_item(api, kind, "p1", {"zh-Hant": LIVE["zh-Hant"]}), "unchanged")
                self.assertEqual(api.writes, [])

    def test_approved_version_gets_a_new_draft_with_only_the_changes(self):
        api = FakeASC([("APPROVED", LIVE)])
        wanted = {"zh-Hant": {"name": "極簡記帳－進階版", "description": "新說明"}, "ko": {"name": "프리미엄", "description": "광고 없음"}}
        summary = sync_asc_item(api, ASC_KINDS["iap"], "p1", wanted)
        self.assertEqual(summary, "updated ko, zh-Hant on new draft version 2")
        create, *edits = api.writes
        self.assertEqual(create[1], "/v1/inAppPurchaseVersions")
        self.assertEqual(create[2]["data"]["relationships"], {"inAppPurchase": {"data": {"type": "inAppPurchases", "id": "p1"}}})
        patch = next(body for method, _, body in edits if method == "PATCH")
        self.assertEqual(patch["data"], {"type": "inAppPurchaseLocalizations", "id": "v2-zh-Hant",
                                         "attributes": {"description": "新說明"}})
        post = next(body for method, _, body in edits if method == "POST")
        self.assertEqual(post["data"]["relationships"]["version"]["data"], {"type": "inAppPurchaseVersions", "id": "v2"})
        self.assertEqual(api.text("v1"), LIVE)  # live version untouched
        self.assertEqual(api.text("v2"), {**LIVE, **wanted})  # unlisted ja kept

    def test_existing_draft_is_edited_in_place(self):
        api = FakeASC([("APPROVED", LIVE), ("PREPARE_FOR_SUBMISSION", LIVE)])
        summary = sync_asc_item(api, ASC_KINDS["subscription"], "p1", {"ja": {"name": "新", "description": "旧"}})
        self.assertEqual(summary, "updated ja on draft version 2")
        self.assertEqual([(m, r) for m, r, _ in api.writes], [("PATCH", "/v2/subscriptionLocalizations/v2-ja")])

    def test_subscription_group_uses_custom_app_name(self):
        api = FakeASC([("APPROVED", {"zh-Hant": {"name": "進階版"}})])
        sync_asc_item(api, ASC_KINDS["group"], "p1", {"zh-Hant": {"name": "進階版", "customAppName": "極簡記帳"}})
        self.assertEqual(api.writes[0][1], "/v1/subscriptionGroupVersions")
        self.assertEqual(api.writes[1][2]["data"]["attributes"], {"customAppName": "極簡記帳"})

    def test_version_in_review_fails_without_writes(self):
        for state in ("WAITING_FOR_REVIEW", "IN_REVIEW"):
            with self.subTest(state=state):
                api = FakeASC([("APPROVED", LIVE), (state, LIVE)])
                with self.assertRaisesRegex(SyncError, state):
                    sync_asc_item(api, ASC_KINDS["iap"], "p1", {"ja": {"name": "新", "description": "旧"}})
                self.assertEqual(api.writes, [])

    def test_dry_run_writes_nothing(self):
        api = FakeASC([("APPROVED", LIVE)])
        summary = sync_asc_item(api, ASC_KINDS["iap"], "p1", {"ja": {"name": "新", "description": "旧"}}, dry_run=True)
        self.assertEqual(summary, "would update ja on a new draft version")
        self.assertEqual(api.writes, [])

    def test_api_errors_are_reported(self):
        api = FakeASC([("APPROVED", LIVE)])
        api.call = lambda method, path, body=None: (403, {"errors": [{"code": "FORBIDDEN", "detail": "no access"}]})
        with self.assertRaisesRegex(SyncError, "403: FORBIDDEN: no access"):
            sync_asc_item(api, ASC_KINDS["iap"], "p1", {"ja": {"name": "新", "description": "旧"}})


class FakePlay:
    def __init__(self):
        self.calls = []

    def call(self, method, path, body=None):
        self.calls.append((method, path, body))
        return 200, body or {}


PLAY_PRODUCT = {
    "packageName": "com.kevlina.budgetplus", "productId": "budgetplus.premium",
    "purchaseOptions": [{"purchaseOptionId": "lifetime"}], "regionsVersion": {"version": "2025/03"},
    "listings": [{"languageCode": "zh-TW", "title": "舊", "description": "舊"},
                 {"languageCode": "en-US", "title": "Old", "description": "Old"}],
}
PLAY_SUBSCRIPTION = {
    "packageName": "com.kevlina.budgetplus", "productId": "premium.sub", "basePlans": [{"basePlanId": "annual"}],
    "listings": [{"languageCode": "zh-TW", "title": "舊", "description": "舊", "benefits": ["無廣告"]}],
}


class PlaySyncTests(unittest.TestCase):
    def test_one_time_product_patch_keeps_other_locales_and_fields(self):
        play = FakePlay()
        summary = sync_play_product(play, PLAY_PRODUCT, False, {"zh-TW": {"title": "新", "description": "新"}})
        self.assertEqual(summary, "updated zh-TW")
        (method, path, body), = play.calls
        route, query = path.split("?")
        self.assertEqual((method, route), ("PATCH", "/onetimeproducts/budgetplus.premium"))
        self.assertEqual(urllib.parse.parse_qs(query), {"updateMask": ["listings"], "regionsVersion.version": ["2025/03"]})
        self.assertNotIn("regionsVersion", body)
        self.assertEqual(body["purchaseOptions"], PLAY_PRODUCT["purchaseOptions"])
        self.assertEqual(body["listings"], [{"languageCode": "zh-TW", "title": "新", "description": "新"},
                                            PLAY_PRODUCT["listings"][1]])

    def test_subscription_benefits_and_regions_version(self):
        play = FakePlay()
        wanted = {"zh-TW": {"title": "舊", "description": "舊", "benefits": "無廣告\n十本帳本"},
                  "ja-JP": {"title": "新", "description": "新"}}
        with unittest.mock.patch.dict(os.environ, {"PLAY_REGIONS_VERSION": ""}):
            self.assertEqual(sync_play_product(play, PLAY_SUBSCRIPTION, True, wanted), "updated ja-JP, zh-TW")
        (_, path, body), = play.calls
        self.assertTrue(path.startswith("/subscriptions/premium.sub?"))
        self.assertIn("regionsVersion.version=2022%2F02", path)
        self.assertEqual(body["listings"][0]["benefits"], ["無廣告", "十本帳本"])
        self.assertEqual(body["listings"][1], {"languageCode": "ja-JP", "title": "新", "description": "新"})

    def test_missing_benefits_file_keeps_store_benefits(self):
        listings, changed = merged_listings(PLAY_SUBSCRIPTION, {"zh-TW": {"title": "新", "description": "舊"}}, True)
        self.assertEqual(changed, ["zh-TW"])
        self.assertEqual(listings[0]["benefits"], ["無廣告"])

    def test_unchanged_and_dry_run_write_nothing(self):
        play = FakePlay()
        same = {"en-US": {"title": "Old", "description": "Old"}}
        self.assertEqual(sync_play_product(play, PLAY_PRODUCT, False, same), "unchanged")
        changed = {"en-US": {"title": "New", "description": "Old"}}
        self.assertEqual(sync_play_product(play, PLAY_PRODUCT, False, changed, dry_run=True), "would update en-US")
        self.assertEqual(play.calls, [])

    def test_benefits_rejected_for_one_time_products(self):
        with self.assertRaisesRegex(SyncError, "only supported for subscriptions"):
            merged_listings(PLAY_PRODUCT, {"zh-TW": {"title": "a", "description": "b", "benefits": "c"}}, False)

    def test_rs256_signature_verifies_with_openssl(self):
        with tempfile.TemporaryDirectory() as temporary:
            key, public = Path(temporary, "key.pem"), Path(temporary, "public.pem")
            subprocess.run(["openssl", "genpkey", "-algorithm", "RSA", "-pkeyopt", "rsa_keygen_bits:2048",
                            "-out", str(key)], check=True, capture_output=True)
            subprocess.run(["openssl", "pkey", "-in", str(key), "-pubout", "-out", str(public)], check=True, capture_output=True)
            signature = Path(temporary, "signature")
            signature.write_bytes(upload.rs256_sign(key.read_text(), b"header.claims"))
            verified = subprocess.run(["openssl", "dgst", "-sha256", "-verify", str(public), "-signature", str(signature)],
                                      input=b"header.claims", capture_output=True)
            self.assertEqual(verified.returncode, 0, verified.stderr)

    def test_play_token_claims(self):
        with tempfile.TemporaryDirectory() as temporary:
            key_file = Path(temporary, "play.json")
            key_file.write_text(json.dumps({"client_email": "uploader@example.iam.gserviceaccount.com",
                                            "private_key": "unused", "token_uri": "https://oauth2.example/token"}))
            play = upload.Play(str(key_file))
        captured = {}

        def fake_sign(_, message):
            captured["claims"] = json.loads(upload.base64.urlsafe_b64decode(message.split(b".")[1] + b"=="))
            return b"sig"

        class Response:
            def __enter__(self): return self
            def __exit__(self, *_): return False
            def read(self): return b'{"access_token": "token"}'

        with unittest.mock.patch.object(upload, "rs256_sign", fake_sign), \
                unittest.mock.patch.object(upload.urllib.request, "urlopen", return_value=Response()):
            self.assertEqual(play.token(), "token")
        self.assertEqual(captured["claims"]["aud"], "https://oauth2.example/token")
        self.assertEqual(captured["claims"]["scope"], "https://www.googleapis.com/auth/androidpublisher")


if __name__ == "__main__":
    unittest.main()
