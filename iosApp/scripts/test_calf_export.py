"""Fail CI before a Calf component that needs export(libs.calf.ui) reaches a Release build.

See calf_export_check.py for why calf-ui is not exported and how to re-audit after an upgrade.
Run with: python3 -m unittest discover -s iosApp/scripts -v
"""

import unittest

from calf_export_check import (
    AUDITED_CALF_VERSION,
    CALLBACK_PACKAGES,
    ROOT,
    calf_version,
    callback_packages_in_sources,
    callback_packages_used,
    exports_calf,
    violations,
)


class RepositoryTests(unittest.TestCase):
    def test_no_calf_callback_component_without_export(self):
        found = violations()
        details = "\n".join(f"  {file}: {', '.join(packages)}" for file, packages in found)
        self.assertEqual(
            found,
            [],
            "These files use Calf components that receive UIKit callbacks:\n"
            f"{details}\n"
            "Without export(libs.calf.ui) in shared/build.gradle.kts they crash in Release with "
            "'unrecognized selector sent to instance' (Calf #189, #330). Re-add the export "
            "(about +6.7 MB of app size) or use a different component.",
        )

    def test_calf_upgrade_is_audited(self):
        version = calf_version((ROOT / "gradle/libs.versions.toml").read_text())
        self.assertEqual(
            version,
            AUDITED_CALF_VERSION,
            f"Calf changed from {AUDITED_CALF_VERSION} to {version}: its iOS components may now "
            "receive UIKit callbacks in new places. Run the `audit` command from "
            "iosApp/scripts/calf_export_check.py on the new sources jar, then update "
            "CALLBACK_PACKAGES and AUDITED_CALF_VERSION.",
        )


class ExportDetectionTests(unittest.TestCase):
    def test_detects_export(self):
        self.assertTrue(exports_calf("        export(libs.calf.ui)\n"))

    def test_detects_conditional_export(self):
        self.assertTrue(exports_calf("if (buildType == NativeBuildType.RELEASE) export(libs.calf.ui)\n"))

    def test_ignores_commented_export(self):
        self.assertFalse(exports_calf("// export(libs.calf.ui)\n/* export(libs.calf.ui) */\n"))

    def test_ignores_other_exports(self):
        self.assertFalse(exports_calf("export(libs.compottie)\napi(libs.calf.ui)\n"))


class UsageDetectionTests(unittest.TestCase):
    def test_flags_imports_from_callback_packages(self):
        kotlin = (
            "import com.mohamedrejeb.calf.ui.datepicker.AdaptiveDatePicker\n"
            "import com.mohamedrejeb.calf.ui.dialog.*\n"
        )
        self.assertEqual(callback_packages_used(kotlin), ["datepicker", "dialog"])

    def test_flags_fully_qualified_use(self):
        kotlin = "com.mohamedrejeb.calf.ui.sheet.AdaptiveBottomSheet(onDismissRequest = {}) {}"
        self.assertEqual(callback_packages_used(kotlin), ["sheet"])

    def test_allows_components_without_callbacks(self):
        kotlin = (
            "import com.mohamedrejeb.calf.ui.toggle.CupertinoSwitch\n"
            "import com.mohamedrejeb.calf.ui.dropdown.AdaptiveDropDown\n"
            "import com.mohamedrejeb.calf.ui.button.CupertinoButton\n"
        )
        self.assertEqual(callback_packages_used(kotlin), [])

    def test_ignores_comments(self):
        self.assertEqual(callback_packages_used("// com.mohamedrejeb.calf.ui.datepicker\n"), [])


class AuditTests(unittest.TestCase):
    def test_finds_objc_actions_and_delegate_protocols(self):
        files = [
            ("iosMain/com/mohamedrejeb/calf/ui/timepicker/A.kt", "object : NSObject() {\n    @ObjCAction fun x() {}\n}"),
            ("iosMain/com/mohamedrejeb/calf/ui/sheet/B.kt", "class D : NSObject(), UIAdaptivePresentationControllerDelegateProtocol"),
            ("iosMain/com/mohamedrejeb/calf/ui/dropdown/C.kt", "internal class DropDownMenuDelegate : NSObject() {}"),
            ("commonMain/com/mohamedrejeb/calf/ui/dialog/D.kt", "@ObjCAction"),
        ]
        self.assertEqual(callback_packages_in_sources(files), ["sheet", "timepicker"])

    def test_audited_packages_exclude_the_components_in_use(self):
        self.assertFalse({"toggle", "button", "dropdown", "progress", "gesture", "uikit"} & CALLBACK_PACKAGES)


if __name__ == "__main__":
    unittest.main()
