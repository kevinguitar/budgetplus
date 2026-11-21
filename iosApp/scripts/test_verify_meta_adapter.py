from pathlib import Path
import plistlib
import tempfile
import unittest

from verify_meta_adapter import ADAPTER_CLASS, class_definitions, resolve_executable


def metadata(flags, name=ADAPTER_CLASS):
    return f"""        flags          {flags}
        instanceStart  8
        instanceSize   56
        reserved       0x0
        ivarLayout     0x1040e296e
        layout map     0x06
        name           0x1040e6094 {name}
"""


class ClassDefinitionsTests(unittest.TestCase):
    def test_counts_real_class_but_not_metaclass(self):
        output = "Contents of (__DATA_CONST,__objc_classlist) section\n"
        output += metadata("0x184") + metadata("0x185 RO_META")
        self.assertEqual(class_definitions(output.splitlines())[ADAPTER_CLASS], 1)

    def test_supports_data_section_class_list(self):
        output = "Contents of (__DATA,__objc_classlist) section\n" + metadata("0x184")
        self.assertEqual(class_definitions(output.splitlines())[ADAPTER_CLASS], 1)

    def test_metaclass_alone_is_not_a_definition(self):
        output = "Contents of (__DATA_CONST,__objc_classlist) section\n" + metadata("0x185 RO_META")
        self.assertEqual(class_definitions(output.splitlines())[ADAPTER_CLASS], 0)

    def test_rejects_class_references_and_unrelated_sections(self):
        output = "Contents of (__DATA_CONST,__objc_classlist) section\n"
        output += metadata("0x184", "AnotherAdapter")
        output += "Contents of (__DATA,__objc_classrefs) section\n" + metadata("0x184")
        self.assertEqual(class_definitions(output.splitlines())[ADAPTER_CLASS], 0)

    def test_class_name_string_alone_is_not_proof(self):
        self.assertEqual(class_definitions([ADAPTER_CLASS])[ADAPTER_CLASS], 0)

    def test_duplicate_definitions_are_detected(self):
        output = "Contents of (__DATA_CONST,__objc_classlist) section\n" + metadata("0x184") * 2
        self.assertEqual(class_definitions(output.splitlines())[ADAPTER_CLASS], 2)


def make_app(parent, name="BudgetPlus"):
    app = Path(parent) / f"{name}.app"
    app.mkdir(parents=True)
    with (app / "Info.plist").open("wb") as info:
        plistlib.dump({"CFBundleExecutable": name}, info)
    (app / name).write_bytes(b"")
    return app


class ResolveExecutableTests(unittest.TestCase):
    def test_archive_resolves_to_its_app_executable(self):
        with tempfile.TemporaryDirectory() as tmp:
            archive = Path(tmp) / "iosApp.xcarchive"
            app = make_app(archive / "Products/Applications")
            self.assertEqual(resolve_executable(archive), app / "BudgetPlus")

    def test_archive_must_hold_exactly_one_app(self):
        with tempfile.TemporaryDirectory() as tmp:
            archive = Path(tmp) / "iosApp.xcarchive"
            (archive / "Products/Applications").mkdir(parents=True)
            with self.assertRaises(ValueError):
                resolve_executable(archive)

    def test_app_bundle_resolves_through_info_plist(self):
        with tempfile.TemporaryDirectory() as tmp:
            app = make_app(tmp)
            self.assertEqual(resolve_executable(app), app / "BudgetPlus")

    def test_executable_path_is_used_as_is(self):
        with tempfile.TemporaryDirectory() as tmp:
            executable = make_app(tmp) / "BudgetPlus"
            self.assertEqual(resolve_executable(executable), executable)

    def test_missing_executable_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(ValueError):
                resolve_executable(Path(tmp) / "BudgetPlus")


if __name__ == "__main__":
    unittest.main()
