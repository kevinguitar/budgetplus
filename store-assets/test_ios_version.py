"""Test App Store version resolution with a fake API; never sign in or upload."""

from contextlib import redirect_stdout
import io
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from ensure_ios_version import ensure_editable_version, main, next_patch


def version(number, state, state_key="appVersionState"):
    return {"id": "id-" + number, "type": "appStoreVersions",
            "attributes": {"versionString": number, state_key: state}}


class FakeASC:
    def __init__(self, pages, post_status=201, post_payload=None):
        self.pages = list(pages)
        self.calls = []
        self.post_status = post_status
        self.post_payload = post_payload or {"data": version("1.7.4", "PREPARE_FOR_SUBMISSION")}

    def must(self, method, path):
        self.calls.append((method, path, None))
        assert method == "GET", method
        return self.pages.pop(0)

    def call(self, method, path, body):
        self.calls.append((method, path, body))
        assert method == "POST", method
        return self.post_status, self.post_payload


class IOSVersionTests(unittest.TestCase):
    def test_reuses_highest_numeric_editable_version_without_creation(self):
        api = FakeASC([{"data": [version("1.9.9", "PREPARE_FOR_SUBMISSION"),
                                 version("1.10.0", "DEVELOPER_REJECTED")]}])
        selected, created = ensure_editable_version(api, "app-id")
        self.assertEqual(selected["attributes"]["versionString"], "1.10.0")
        self.assertFalse(created)
        self.assertEqual(len(api.calls), 1)

    def test_legacy_state_field_is_supported(self):
        api = FakeASC([{"data": [version("1.7.4", "REJECTED", "appStoreState")]}])
        selected, created = ensure_editable_version(api, "app-id")
        self.assertEqual(selected["attributes"]["versionString"], "1.7.4")
        self.assertFalse(created)

    def test_creates_next_patch_draft_with_manual_release(self):
        api = FakeASC([{"data": [version("1.7.3", "READY_FOR_DISTRIBUTION")]}])
        selected, created = ensure_editable_version(api, "app-id")
        self.assertTrue(created)
        self.assertEqual(selected["attributes"]["versionString"], "1.7.4")
        method, path, body = api.calls[-1]
        self.assertEqual((method, path), ("POST", "/v1/appStoreVersions"))
        self.assertEqual(body["data"]["attributes"],
                         {"platform": "IOS", "versionString": "1.7.4", "releaseType": "MANUAL"})
        self.assertEqual(body["data"]["relationships"]["app"]["data"], {"type": "apps", "id": "app-id"})
        self.assertNotIn("build", body["data"]["relationships"])

    def test_next_patch_is_numeric_and_handles_two_component_versions(self):
        for current, expected in (("1.7.3", "1.7.4"), ("1.7.9", "1.7.10"), ("1.7", "1.7.1")):
            with self.subTest(current=current):
                self.assertEqual(next_patch(current), expected)
        for invalid in ("v1.7.3", "1.7.3-beta", "1.2.3.4", "1.7\n"):
            with self.subTest(invalid=invalid), self.assertRaises(ValueError):
                next_patch(invalid)

    def test_uses_latest_released_version_not_response_order(self):
        api = FakeASC([{"data": [version("1.9.9", "READY_FOR_SALE", "appStoreState"),
                                 version("1.10.3", "READY_FOR_DISTRIBUTION")]}])
        ensure_editable_version(api, "app-id")
        self.assertEqual(api.calls[-1][2]["data"]["attributes"]["versionString"], "1.10.4")

    def test_follows_pagination_before_creating_anything(self):
        api = FakeASC([
            {"data": [version("1.7.3", "READY_FOR_SALE")], "links": {"next": "/next-page"}},
            {"data": [version("1.8.0", "PREPARE_FOR_SUBMISSION")]},
        ])
        selected, created = ensure_editable_version(api, "app-id")
        self.assertEqual(selected["attributes"]["versionString"], "1.8.0")
        self.assertFalse(created)
        self.assertEqual(api.calls[-1][1], "/next-page")

    def test_no_live_or_editable_version_fails_without_creation(self):
        api = FakeASC([{"data": [version("1.7.3", "IN_REVIEW")]}])
        with self.assertRaisesRegex(ValueError, "cannot safely choose"):
            ensure_editable_version(api, "app-id")
        self.assertEqual(len(api.calls), 1)

    def test_noneditable_patch_collision_does_not_skip_or_cancel_review(self):
        api = FakeASC([{"data": [version("1.7.3", "READY_FOR_SALE"), version("1.7.4", "IN_REVIEW")]}])
        with self.assertRaisesRegex(ValueError, "1.7.4 already exists in IN_REVIEW"):
            ensure_editable_version(api, "app-id")
        self.assertEqual(len(api.calls), 1)

    def test_conflict_reuses_only_exact_editable_patch(self):
        api = FakeASC([
            {"data": [version("1.7.3", "READY_FOR_SALE")]},
            {"data": [version("1.7.4", "PREPARE_FOR_SUBMISSION")]},
        ], post_status=409)
        selected, created = ensure_editable_version(api, "app-id")
        self.assertEqual(selected["attributes"]["versionString"], "1.7.4")
        self.assertFalse(created)

    def test_unresolved_conflict_does_not_create_another_patch(self):
        api = FakeASC([
            {"data": [version("1.7.3", "READY_FOR_SALE")]},
            {"data": [version("1.7.4", "IN_REVIEW")]},
        ], post_status=409, post_payload={"errors": [{"code": "CONFLICT", "detail": "Version locked"}]})
        with self.assertRaisesRegex(ValueError, "Version locked"):
            ensure_editable_version(api, "app-id")
        self.assertEqual(sum(call[0] == "POST" for call in api.calls), 1)

    def test_api_creation_failure_is_not_silently_ignored(self):
        api = FakeASC([{"data": [version("1.7.3", "READY_FOR_SALE")]}], post_status=403,
                      post_payload={"errors": [{"code": "FORBIDDEN", "detail": "Missing permission"}]})
        with self.assertRaisesRegex(ValueError, "403.*Missing permission"):
            ensure_editable_version(api, "app-id")

    def test_main_exports_resolved_version_for_every_ios_upload_step(self):
        api = FakeASC([{"data": [{"id": "app-id"}]},
                       {"data": [version("1.8.0", "PREPARE_FOR_SUBMISSION")]}])
        with tempfile.TemporaryDirectory() as temporary:
            output = Path(temporary) / "outputs"
            env = {"ASC_KEY_ID": "test", "ASC_ISSUER_ID": "test", "ASC_KEY_PATH": "/unused/key",
                   "GITHUB_OUTPUT": str(output)}
            with patch.dict(os.environ, env, clear=True), patch("ensure_ios_version.ASC", return_value=api), \
                    redirect_stdout(io.StringIO()):
                main()
            self.assertEqual(output.read_text(), "version=1.8.0\n")


if __name__ == "__main__":
    unittest.main()
