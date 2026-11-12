"""Record real lane options and inspect workflow wiring, without loading Fastlane."""

import itertools
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

from listings import ROOT, SOURCES


RUBY_RECORDER = r"""
@lanes = {}
@calls = []
def desc(*); end
def lane(name, &block); @lanes[name.to_s] = block; end
[:app_store_connect_api_key, :upload_to_app_store, :upload_to_play_store,
 :google_play_track_version_codes].each do |action|
  define_singleton_method(action) do |options|
    @calls << { action: action, options: options }
    JSON.parse(ENV.fetch("TEST_VERSION_CODES", "[99,100]")) if action == :google_play_track_version_codes
  end
end
load ARGV.fetch(0)
@lanes.fetch(ARGV.fetch(1)).call
puts JSON.generate(@calls)
"""


class ManualLaneTests(unittest.TestCase):
    def run_lane(self, lane, overrides=None, missing=None):
        env = os.environ.copy()
        env.update({
            "ASC_KEY_ID": "test-key-id", "ASC_ISSUER_ID": "test-issuer", "ASC_KEY_PATH": "/unused/key.p8",
            "IOS_APP_VERSION": "1.7.4", "STORE_LISTINGS_DIR": "/validated/listings",
            "STAGE_DIR": "/validated/screenshots", "PLAY_KEY_PATH": "/unused/play.json",
            "UPLOAD_STORE_LISTING": "true", "UPLOAD_SCREENSHOTS": "false",
        })
        env.update(overrides or {})
        if missing is not None:
            env.pop(missing, None)
        return subprocess.run(
            ["ruby", "-rjson", "-e", RUBY_RECORDER, str(ROOT / "store-assets/Fastfile"), lane],
            env=env, capture_output=True, text=True,
        )

    def options_for(self, lane, action, **kwargs):
        result = self.run_lane(lane, **kwargs)
        self.assertEqual(result.returncode, 0, result.stderr)
        calls = json.loads(result.stdout)
        uploads = [call for call in calls if call["action"] == action]
        self.assertEqual(len(uploads), 1)
        return uploads[0]["options"]

    def test_ios_listing_only_does_not_require_screenshot_staging(self):
        options = self.options_for("ios_assets", "upload_to_app_store", missing="STAGE_DIR")
        self.assertEqual(options["app_version"], "1.7.4")
        self.assertTrue(options["skip_app_version_update"])
        self.assertEqual(options["metadata_path"], "/validated/listings/ios")
        self.assertFalse(options["skip_metadata"])
        self.assertTrue(options["skip_screenshots"])
        self.assertFalse(options["overwrite_screenshots"])
        self.assertNotIn("screenshots_path", options)
        self.assertTrue(options["skip_binary_upload"])
        self.assertFalse(options["submit_for_review"])
        self.assertFalse(options["run_precheck_before_submit"])

    def test_ios_screenshots_only_do_not_require_listing_sources(self):
        options = self.options_for("ios_assets", "upload_to_app_store", missing="STORE_LISTINGS_DIR",
                                   overrides={"UPLOAD_STORE_LISTING": "false", "UPLOAD_SCREENSHOTS": "true"})
        self.assertTrue(options["skip_metadata"])
        self.assertNotIn("metadata_path", options)
        self.assertFalse(options["skip_screenshots"])
        self.assertTrue(options["overwrite_screenshots"])
        self.assertEqual(options["screenshots_path"], "/validated/screenshots/ios")

    def test_ios_both_options_share_one_upload(self):
        options = self.options_for("ios_assets", "upload_to_app_store",
                                   overrides={"UPLOAD_SCREENSHOTS": "true"})
        self.assertFalse(options["skip_metadata"])
        self.assertFalse(options["skip_screenshots"])

    def test_ios_no_options_fails_closed(self):
        result = self.run_lane("ios_assets", overrides={"UPLOAD_STORE_LISTING": "false"})
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Select listing text or screenshots", result.stderr)

    def test_android_independently_toggles_listing_screenshots_and_creative(self):
        for listing, screenshots, creative in itertools.product(("true", "false"), repeat=3):
            if "true" not in (listing, screenshots, creative):
                continue
            with self.subTest(listing=listing, screenshots=screenshots, creative=creative):
                options = self.options_for("android_assets", "upload_to_play_store",
                                           overrides={"UPLOAD_STORE_LISTING": listing,
                                                      "UPLOAD_SCREENSHOTS": screenshots,
                                                      "UPLOAD_CREATIVE_ASSETS": creative,
                                                      "TEST_VERSION_CODES": '["99","100"]'})
                self.assertEqual(options["package_name"], "com.kevlina.budgetplus")
                self.assertEqual(options["metadata_path"], "/validated/screenshots/android")
                self.assertEqual(options["track"], "production")
                self.assertEqual(options["version_code"], 100)
                self.assertEqual(options["skip_upload_metadata"], listing != "true")
                self.assertEqual(options["skip_upload_screenshots"], screenshots != "true")
                self.assertEqual(options["skip_upload_images"], creative != "true")
                for field in ("apk", "aab", "changelogs"):
                    self.assertTrue(options["skip_upload_" + field])
                for field in ("apk", "aab", "rollout", "track_promote_to", "version_codes_to_retain", "validate_only"):
                    self.assertNotIn(field, options)

    def test_android_no_options_fails_closed(self):
        result = self.run_lane("android_assets", overrides={"UPLOAD_STORE_LISTING": "false"})
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Select listing text, screenshots, or creative assets", result.stderr)

    def test_android_creative_defaults_off_when_unset(self):
        options = self.options_for("android_assets", "upload_to_play_store", missing="UPLOAD_CREATIVE_ASSETS")
        self.assertTrue(options["skip_upload_images"])

    def test_android_missing_production_release_is_an_actionable_failure(self):
        result = self.run_lane("android_assets", overrides={"TEST_VERSION_CODES": "[]"})
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("No production Play release", result.stderr)

    def test_required_environment_fails_closed(self):
        for lane, variable in (("ios_assets", "IOS_APP_VERSION"), ("ios_assets", "ASC_KEY_PATH"),
                               ("ios_assets", "STORE_LISTINGS_DIR"), ("android_assets", "PLAY_KEY_PATH")):
            with self.subTest(lane=lane, variable=variable):
                result = self.run_lane(lane, missing=variable)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(variable, result.stderr)


class ManualWorkflowTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        result = subprocess.run(
            ["ruby", "-ryaml", "-rjson", "-e", "puts JSON.generate(YAML.load_file(ARGV.fetch(0)))",
             str(ROOT / ".github/workflows/update_store_assets.yml")],
            capture_output=True, text=True, check=True,
        )
        cls.workflow = json.loads(result.stdout)
        cls.job = cls.workflow["jobs"]["upload"]
        cls.steps = {step.get("name"): step for step in cls.job["steps"]}

    @staticmethod
    def enabled(node, target="both", listing=False, screenshots=False, creative=False):
        condition = node.get("if")
        if condition is None:
            return True
        values = {"target": target, "store_listing": listing, "screenshots": screenshots,
                  "creative_assets": creative}
        condition = condition.removeprefix("${{").removesuffix("}}")
        condition = re.sub(r"inputs\.(\w+)", lambda match: repr(values[match[1]]), condition)
        return eval(condition.replace("&&", " and ").replace("||", " or "), {"__builtins__": {}})

    def test_manual_options_order_and_defaults(self):
        self.assertEqual(self.workflow["name"], "update_store_assets")
        triggers = self.workflow.get("on", self.workflow.get("true"))
        self.assertEqual(list(triggers), ["workflow_dispatch"])
        inputs = triggers["workflow_dispatch"]["inputs"]
        self.assertEqual(list(inputs), ["target", "store_listing", "screenshots", "creative_assets"])
        self.assertEqual(inputs["target"]["options"], ["both", "ios", "android"])
        self.assertEqual(inputs["target"]["default"], "both")
        for key in ("store_listing", "screenshots", "creative_assets"):
            self.assertIs(inputs[key]["default"], False)
        text = (ROOT / ".github/workflows/update_store_assets.yml").read_text()
        self.assertNotIn("dry_run", text)
        self.assertNotIn("inputs.ios_app_version", text)
        self.assertNotIn("ios_screenshots", text)

    def test_target_and_option_truth_table(self):
        for target in ("both", "ios", "android"):
            for listing, screenshots, creative in itertools.product((False, True), repeat=3):
                options = dict(target=target, listing=listing, screenshots=screenshots, creative=creative)
                with self.subTest(**options):
                    ios = target != "android"
                    android = target != "ios"
                    self.assertEqual(self.enabled(self.job, **options), listing or screenshots or creative)
                    self.assertEqual(self.enabled(self.steps["Upload to Google Play"], **options),
                                     android and (listing or screenshots or creative))
                    self.assertEqual(self.enabled(self.steps["Stage and validate selected store listing text"], **options), listing)
                    self.assertEqual(self.enabled(self.steps["Stage and validate screenshots"], **options),
                                     screenshots or creative)
                    for name in ("Setup Ruby", "Install Fastlane", "Use manual store upload lanes"):
                        self.assertEqual(self.enabled(self.steps[name], **options),
                                         listing or screenshots or (android and creative))
                    for name in ("Write App Store Connect key", "Resolve or create editable iOS version"):
                        self.assertEqual(self.enabled(self.steps[name], **options), ios and (listing or screenshots or creative))
                    self.assertEqual(self.enabled(self.steps["Upload listing text and/or screenshots to App Store Connect"], **options),
                                     ios and (listing or screenshots))
                    self.assertEqual(self.enabled(self.steps["Upload header and search results to App Store Connect"], **options),
                                     ios and creative)

    def test_listing_only_does_not_validate_images_on_either_store(self):
        for target in ("both", "ios", "android"):
            with self.subTest(target=target):
                self.assertFalse(self.enabled(self.steps["Stage and validate screenshots"], target=target, listing=True))

    def test_android_listing_only_staging_needs_no_image_directory(self):
        for target in ("android", "both"):
            with self.subTest(target=target), tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                stage = root / "assets"
                env = os.environ.copy()
                env.update({"TARGET": target, "STAGE_DIR": str(stage), "STORE_LISTINGS_DIR": str(root / "listings")})
                result = subprocess.run(
                    ["bash", "-eu", "-c", self.steps["Stage and validate selected store listing text"]["run"]],
                    cwd=ROOT, env=env, capture_output=True, text=True,
                )
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual(len(list(stage.rglob("*.txt"))), 15)
                self.assertFalse(list(stage.rglob("*.png")))
                self.assertFalse(list(stage.rglob("images")))

    def test_ios_creative_only_does_not_install_fastlane(self):
        self.assertFalse(self.enabled(self.steps["Install Fastlane"], target="ios", creative=True))
        # The Play feature graphic goes up through supply.
        self.assertTrue(self.enabled(self.steps["Install Fastlane"], target="android", creative=True))

    def test_all_ios_uploads_use_same_resolved_version(self):
        self.assertEqual(self.steps["Resolve or create editable iOS version"]["id"], "ios_version")
        for name in ("Upload listing text and/or screenshots to App Store Connect",
                     "Upload header and search results to App Store Connect"):
            self.assertEqual(self.steps[name]["env"]["IOS_APP_VERSION"], "${{ steps.ios_version.outputs.version }}")

    def test_shared_staging_paths_are_exported_after_runner_allocation(self):
        configure = self.steps["Configure staging paths"]["run"]
        self.assertIn('STAGE_DIR=$RUNNER_TEMP/store', configure)
        self.assertIn('STORE_LISTINGS_DIR=$RUNNER_TEMP/store-listings', configure)
        self.assertIn('>> "$GITHUB_ENV"', configure)

    def test_app_releases_do_not_use_manual_sources_or_lanes(self):
        for platform in ("ios", "android"):
            release = (ROOT / f".github/workflows/publish-{platform}-app.yml").read_text()
            for token in ("store-assets/", "ios_assets", "android_assets", "ensure_ios_version"):
                self.assertNotIn(token, release)
        self.assertTrue(all(str(source).startswith("store-assets/listings/") for source in SOURCES.values()))
        for field in ("name", "subtitle", "description"):
            self.assertFalse(list((ROOT / "fastlane/metadata").glob(f"*/{field}.txt")))
        self.assertFalse((ROOT / "fastlane/Fastfile").exists())

    def test_manual_uploads_are_serialized_and_keys_always_cleaned(self):
        self.assertIs(self.workflow["concurrency"]["cancel-in-progress"], False)
        self.assertEqual(self.steps["Clean up keys"]["if"], "${{ always() }}")


if __name__ == "__main__":
    unittest.main()
