"""Device-free localization checks, using Traditional Chinese as the reference.

Run with the existing CI checks: python3 -m unittest discover -s ui-tests/scripts -v
These validate resource structure and cross-platform consistency, not translation quality.
"""

from collections import Counter
import json
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
COMMON = ROOT / "core/common/src/commonMain/composeResources"
WIDGET = ROOT / "feature/record-widget/src/androidMain/res"
# Android app-name resources use a region qualifier for Simplified Chinese.
LOCALES = (
    ("values", "values", "en"),
    ("values-zh-rTW", "values-zh-rTW", "zh-Hant"),
    ("values-zh", "values-zh-rCN", "zh-Hans"),
    ("values-ja", "values-ja", "ja"),
    ("values-ko", "values-ko", "ko"),
)
PLACEHOLDERS = re.compile(r"%(?:\d+\$)?[@a-zA-Z]")


def placeholders(text):
    return Counter(PLACEHOLDERS.findall(text.replace("%%", "")))


class LocalizationTests(unittest.TestCase):
    def resources(self, path):
        elements = list(ET.parse(path).getroot())
        resources = {element.attrib["name"]: element for element in elements}
        self.assertEqual(len(elements), len(resources), f"Duplicate keys in {path}")
        return resources

    def test_xml_locales_match_traditional_chinese_structure(self):
        for directory in (COMMON, WIDGET):
            reference = self.resources(directory / "values-zh-rTW/strings.xml")
            for path in sorted(directory.glob("*/strings.xml")):
                with self.subTest(path=path.relative_to(ROOT)):
                    resources = self.resources(path)
                    self.assertEqual(reference.keys(), resources.keys())
                    for name, element in resources.items():
                        with self.subTest(key=name):
                            original = reference[name]
                            self.assertEqual(original.tag, element.tag)
                            if element.tag == "string":
                                pairs = [(original, element)]
                            elif element.tag == "string-array":
                                self.assertEqual(len(original), len(element))
                                pairs = zip(original, element)
                            elif element.tag == "plurals":
                                quantities = [item.attrib["quantity"] for item in element]
                                self.assertEqual(len(quantities), len(set(quantities)))
                                self.assertIn("other", quantities)
                                # These CJK locales only need 'other'; English also needs 'one'.
                                expected = {"one", "other"} if path.parent.name == "values" else {"other"}
                                self.assertEqual(expected, set(quantities))
                                other = original.find("item[@quantity='other']")
                                pairs = [(other, item) for item in element]
                            else:
                                self.fail(f"Unexpected resource type: {element.tag}")
                            for source, translation in pairs:
                                self.assertTrue((translation.text or "").strip(), name)
                                self.assertEqual(placeholders(source.text or ""),
                                                 placeholders(translation.text or ""), name)

    def test_shared_traditional_chinese_regional_copies_match(self):
        reference = (COMMON / "values-zh-rTW/strings.xml").read_bytes()
        for region in ("HK", "MO"):
            self.assertEqual(reference, (COMMON / f"values-zh-r{region}/strings.xml").read_bytes())

    def test_batch_units_have_correct_english_forms_for_zero_one_two(self):
        resources = self.resources(COMMON / "values/strings.xml")
        for unit in ("month", "week", "day"):
            resource = resources[f"batch_record_unit_{unit}"]
            self.assertEqual("plurals", resource.tag)
            for count, form, suffix in ((0, "other", "s"), (1, "one", ""), (2, "other", "s")):
                with self.subTest(unit=unit, count=count):
                    # English CLDR cardinal rules: only the integer 1 uses 'one'.
                    label = resource.find(f"item[@quantity='{form}']").text
                    self.assertEqual(f"{count} {unit.title()}{suffix}", f"{count} {label}")

    def test_record_count_messages_have_correct_english_forms_for_zero_one_two(self):
        resources = self.resources(COMMON / "values/strings.xml")
        expected = {
            "batch_record_created": (
                "0 records added to Food", "1 record added to Food", "2 records added to Food"),
            "batch_record_edited": (
                "0 records were edited", "1 record was edited", "2 records were edited"),
            "batch_record_deleted": (
                "0 records were deleted", "1 record was deleted", "2 records were deleted"),
            "overview_selected_delete_confirmation": (
                "Are you sure you want to delete the 0 selected records?",
                "Are you sure you want to delete the 1 selected record?",
                "Are you sure you want to delete the 2 selected records?"),
        }
        for key, messages in expected.items():
            resource = resources[key]
            self.assertEqual("plurals", resource.tag)
            for count, message in enumerate(messages):
                with self.subTest(key=key, count=count):
                    form = "one" if count == 1 else "other"
                    template = resource.find(f"item[@quantity='{form}']").text
                    actual = template.replace("%1$d", str(count)).replace("%2$s", "Food")
                    self.assertEqual(message, actual)

    def test_record_count_plurals_require_integer_counts_in_every_locale(self):
        for path in sorted(COMMON.glob("*/strings.xml")):
            resources = self.resources(path)
            for key in ("batch_record_created", "batch_record_edited", "batch_record_deleted",
                        "overview_selected_delete_confirmation"):
                with self.subTest(path=path.relative_to(ROOT), key=key):
                    resource = resources[key]
                    self.assertEqual("plurals", resource.tag)
                    expected = Counter({"%1$d": 1})
                    if key == "batch_record_created":
                        expected["%2$s"] = 1
                    for item in resource:
                        self.assertEqual(expected, placeholders(item.text))

    def test_ios_user_facing_strings_cover_all_locales(self):
        catalogs = (
            ("iosApp/InfoPlist.xcstrings", {"CFBundleName", "NSHumanReadableCopyright"}),
            ("iosApp/RecordWidgetExtension/Localizable.xcstrings", {"%@", "+%lld"}),
        )
        languages = {locale[2] for locale in LOCALES}
        for path, metadata in catalogs:
            catalog = json.loads((ROOT / path).read_text())
            for key, entry in catalog["strings"].items():
                if key in metadata:
                    continue
                with self.subTest(path=path, key=key):
                    translations = entry["localizations"]
                    # The source language can be represented by the key itself in xcstrings.
                    self.assertEqual(languages, set(translations) | {catalog["sourceLanguage"]})
                    source = translations["zh-Hant"]["stringUnit"]["value"]
                    for language in languages:
                        unit = translations.get(language, {}).get("stringUnit")
                        value = unit["value"] if unit else key
                        self.assertTrue(value.strip())
                        self.assertEqual(placeholders(source), placeholders(value))
                        if unit:
                            self.assertEqual("translated", unit["state"])

    def test_app_names_match_across_platforms(self):
        catalog = json.loads((ROOT / "iosApp/InfoPlist.xcstrings").read_text())["strings"]
        for common, android, language in LOCALES:
            with self.subTest(locale=language):
                name = self.resources(COMMON / common / "strings.xml")["app_name"].text
                for variant, suffix in (("main", ""), ("debug", " Stage")):
                    path = ROOT / "androidApp/src" / variant / "res" / android / "strings.xml"
                    self.assertEqual(name + suffix, self.resources(path)["app_name"].text)
                self.assertEqual(name, catalog["CFBundleDisplayName"]["localizations"][language]["stringUnit"]["value"])
                for key in ("NSMicrophoneUsageDescription", "NSSpeechRecognitionUsageDescription"):
                    self.assertIn(name, catalog[key]["localizations"][language]["stringUnit"]["value"])

    def test_widget_copy_matches_across_platforms(self):
        catalog = json.loads((ROOT / "iosApp/RecordWidgetExtension/Localizable.xcstrings").read_text())["strings"]
        description = self.resources(WIDGET / "values/strings.xml")["record_widget_description"].text
        for directory, _, language in LOCALES:
            with self.subTest(locale=language):
                shared = self.resources(COMMON / directory / "strings.xml")
                widget = self.resources(WIDGET / directory / "strings.xml")
                self.assertEqual(shared["record_widget_title"].text, widget["record_widget_label"].text)
                self.assertEqual(shared["record_widget_setup"].text, widget["record_widget_setup"].text)
                self.assertEqual(shared["record_income"].text, widget["record_widget_income"].text)
                for key, resource in (("Record Widget", "record_widget_label"),
                                      ("Income", "record_widget_income"),
                                      (description, "record_widget_description")):
                    entry = catalog[key].get("localizations", {}).get(language)
                    value = entry["stringUnit"]["value"] if entry else key
                    self.assertEqual(value, widget[resource].text)
        # The sample widget in the widget gallery shows the same items on both platforms.
        english = self.resources(WIDGET / "values/strings.xml")
        samples = [name for name in english if name.startswith("record_widget_sample_")]
        self.assertTrue(samples)
        for directory, _, language in LOCALES:
            widget = self.resources(WIDGET / directory / "strings.xml")
            for name in samples:
                with self.subTest(locale=language, sample=name):
                    entry = catalog[english[name].text].get("localizations", {}).get(language)
                    value = entry["stringUnit"]["value"] if entry else english[name].text
                    self.assertEqual(widget[name].text, value)
        for filename in ("RecordWidget.swift", "RecordWidgetIntent.swift"):
            self.assertIn(f'"{description}"', (ROOT / "iosApp/RecordWidgetExtension" / filename).read_text())


if __name__ == "__main__":
    unittest.main()
