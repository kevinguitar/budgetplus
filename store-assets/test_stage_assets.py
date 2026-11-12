"""Selective screenshot/creative staging tests using small synthetic PNG headers."""

from contextlib import redirect_stdout
import io
from pathlib import Path
import struct
import tempfile
import unittest
from unittest.mock import patch

import stage_for_fastlane as stage


class StageAssetsTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.final = self.root / "final"
        self.out = self.root / "out"

    def screenshots(self, platform):
        classes = stage.IOS if platform == "ios" else stage.ANDROID
        for device in classes:
            for locale in stage.LOCALES:
                path = self.final / platform / device / locale / "01-login.png"
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(b"\x89PNG\r\n\x1a\n" + b"\x00" * 8 + struct.pack(">II", 1200, 2000) + b"\x08\x02")

    def feature_graphics(self, size=(1024, 500), color_type=2):
        for locale in stage.LOCALES:
            path = self.final / "android" / "creative" / locale / "featureGraphic.png"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b"\x89PNG\r\n\x1a\n" + b"\x00" * 8 + struct.pack(">II", *size) + bytes([8, color_type]))

    def run_stage(self, *args):
        with patch.object(stage, "FINAL", str(self.final)), \
                patch("sys.argv", ["stage_for_fastlane.py", str(self.out), *args]), redirect_stdout(io.StringIO()):
            stage.main()

    def test_android_does_not_require_any_ios_images_or_creative(self):
        self.screenshots("android")
        self.feature_graphics()
        with patch.object(stage, "check_creative", side_effect=AssertionError("must not check iOS")):
            self.run_stage("--target", "android")
        self.assertEqual(len(list((self.out / "android").rglob("*.png"))), 15)
        self.assertEqual(len(list((self.out / "android").glob("*/images/featureGraphic.png"))), 5)
        self.assertFalse((self.out / "ios").exists())

    def test_android_creative_only_stages_feature_graphics_without_screenshots(self):
        self.feature_graphics()
        self.run_stage("--target", "android", "--skip-screenshots")
        staged = sorted(p.relative_to(self.out).as_posix() for p in self.out.rglob("*.png"))
        self.assertEqual(staged, sorted(f"android/{play}/images/featureGraphic.png"
                                        for _, play in stage.LOCALES.values()))

    def test_android_screenshots_with_creative_disabled_do_not_need_feature_graphics(self):
        self.screenshots("android")
        self.run_stage("--target", "android", "--skip-creative-assets")
        self.assertFalse(list(self.out.rglob("featureGraphic.png")))

    def test_invalid_or_missing_feature_graphics_fail(self):
        for size, color_type in (((1024, 512), 2), ((1024, 500), 6), (None, None)):
            with self.subTest(size=size, color_type=color_type):
                self.setUp()
                if size: self.feature_graphics(size, color_type)
                with self.assertRaises(SystemExit):
                    self.run_stage("--target", "android", "--skip-screenshots")

    def test_ios_screenshots_do_not_require_android_or_disabled_creative(self):
        self.screenshots("ios")
        with patch.object(stage, "check_creative", side_effect=AssertionError("creative disabled")):
            self.run_stage("--target", "ios", "--skip-creative-assets")
        self.assertEqual(len(list((self.out / "ios").rglob("*.png"))), 15)
        self.assertFalse((self.out / "android").exists())

    def test_creative_only_does_not_require_any_screenshots(self):
        with patch.object(stage, "check_creative", return_value=([], ["creative OK"])) as check:
            self.run_stage("--target", "ios", "--skip-screenshots")
        check.assert_called_once_with()
        self.assertFalse(list(self.out.rglob("*.png")))

    def test_disabled_screenshots_are_neither_read_nor_staged_on_either_store(self):
        with patch.object(stage, "check_creative", side_effect=AssertionError("creative disabled")):
            self.run_stage("--target", "both", "--skip-screenshots", "--skip-creative-assets")
        self.assertFalse((self.out / "ios").exists())
        self.assertFalse((self.out / "android").exists())

    def test_both_target_creative_only_does_not_require_screenshots(self):
        self.feature_graphics()
        with patch.object(stage, "check_creative", return_value=([], ["creative OK"])) as check:
            self.run_stage("--target", "both", "--skip-screenshots")
        check.assert_called_once_with()
        self.assertEqual(len(list(self.out.rglob("*.png"))), 5)
        self.assertFalse((self.out / "ios").exists())

    def test_original_all_assets_mode_still_works(self):
        self.screenshots("android")
        self.screenshots("ios")
        self.feature_graphics()
        with patch.object(stage, "check_creative", return_value=([], [])) as check:
            self.run_stage()
        check.assert_called_once_with()
        self.assertEqual(len(list(self.out.rglob("*.png"))), 30)

    def test_existing_destination_is_never_deleted(self):
        self.out.mkdir()
        marker = self.out / "keep.txt"
        marker.write_text("keep")
        with self.assertRaises(FileExistsError):
            self.run_stage("--target", "android")
        self.assertEqual(marker.read_text(), "keep")


if __name__ == "__main__":
    unittest.main()
