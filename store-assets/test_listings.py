"""Offline validation and staging tests; no credentials or store API calls."""

from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import unittest

from listings import FIELDS, LOCALES, ROOT, SOURCES, load_listings, stage_listings


class ListingValidationTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name) / "repo"
        for source in SOURCES.values():
            shutil.copytree(ROOT / source, self.root / source)

    def replace(self, path, text):
        (self.root / path).write_bytes(text.encode("utf-8"))

    def test_all_required_locales_and_fields_are_present(self):
        files = load_listings(self.root)
        self.assertEqual(len(files), 30)
        for store in SOURCES:
            locales = LOCALES if store == "ios" else LOCALES.values()
            expected = {(store, locale, field) for locale in locales for field in FIELDS[store]}
            actual = {(item.store, item.locale, item.field) for item in files if item.store == store}
            self.assertEqual(actual, expected)

    def test_paid_tier_labels_stay_in_full_descriptions_only(self):
        paid_tiers = r"(?i)premium|進階版|高级版|プレミアム|프리미엄"
        for listing in load_listings(self.root):
            with self.subTest(store=listing.store, locale=listing.locale, field=listing.field):
                if listing.field in {"description", "full_description"}:
                    self.assertRegex(listing.text, paid_tiers)
                else:
                    self.assertNotRegex(listing.text, paid_tiers)

    def test_unicode_lengths_not_utf8_byte_lengths(self):
        path = SOURCES["ios"] / "zh-Hant/name.txt"
        self.replace(path, "記" * 30 + "\n")
        load_listings(self.root)
        self.replace(path, "記" * 31)
        with self.assertRaisesRegex(ValueError, r"31 characters; expected 2–30"):
            load_listings(self.root)

    def test_description_heading_styles_and_wording_match_across_stores(self):
        for apple_locale, play_locale in LOCALES.items():
            with self.subTest(locale=apple_locale):
                apple = (self.root / SOURCES["ios"] / apple_locale / "description.txt").read_text().rstrip("\n")
                play = (self.root / SOURCES["android"] / play_locale / "full_description.txt").read_text().rstrip("\n")
                apple_body, footer = apple.rsplit("\n\n", 1)
                apple_headings = re.findall(r"^【([^\n]+)】$", apple_body, re.M)
                play_headings = re.findall(r"^<b>([^<>\n]+)</b>$", play, re.M)
                self.assertEqual(len(apple_headings), 6)
                self.assertEqual(apple_headings, play_headings)
                self.assertNotIn("<", apple)
                self.assertNotIn("**", apple + play)
                plain_apple = re.sub(r"^【([^\n]+)】$", r"\1", apple_body, flags=re.M)
                plain_play = re.sub(r"^<b>([^<>\n]+)</b>$", r"\1", play, flags=re.M)
                self.assertNotIn("<", plain_play)
                self.assertEqual(plain_apple, plain_play)
                self.assertTrue(footer.endswith("https://www.apple.com/legal/internet-services/itunes/dev/stdeula/"))

    def test_play_length_limit_includes_html_tags(self):
        path = SOURCES["android"] / "en-US/full_description.txt"
        text = "<b>Heading</b>\n" + "x" * (4000 - len("<b>Heading</b>\n"))
        self.replace(path, text)
        load_listings(self.root)
        self.replace(path, text + "x")
        with self.assertRaisesRegex(ValueError, "4001 characters; expected 1–4000"):
            load_listings(self.root)

    def test_each_field_length_limit(self):
        for store, source in SOURCES.items():
            for field, (minimum, maximum, _) in FIELDS[store].items():
                path = source / "en-US" / (field + ".txt")
                original = (self.root / path).read_bytes()
                for length in (minimum - 1, maximum + 1):
                    with self.subTest(store=store, field=field, length=length):
                        self.replace(path, "a" * length)
                        with self.assertRaisesRegex(ValueError, "characters; expected"):
                            load_listings(self.root)
                (self.root / path).write_bytes(original)

    def test_missing_field_fails(self):
        path = SOURCES["android"] / "ko-KR/short_description.txt"
        (self.root / path).unlink()
        with self.assertRaisesRegex(ValueError, "short_description.txt"):
            load_listings(self.root)

    def test_missing_locale_fails(self):
        shutil.rmtree(self.root / SOURCES["android"] / "zh-TW")
        with self.assertRaisesRegex(ValueError, "zh-TW/title.txt"):
            load_listings(self.root)

    def test_only_selected_store_is_required(self):
        shutil.rmtree(self.root / SOURCES["android"])
        self.assertEqual(len(load_listings(self.root, target="ios")), 15)
        with self.assertRaisesRegex(ValueError, "missing metadata directory"):
            load_listings(self.root)

    def test_wrong_store_locale_code_fails(self):
        source = self.root / SOURCES["android"]
        (source / "ja-JP").rename(source / "ja")
        with self.assertRaisesRegex(ValueError, "ja: unexpected locale"):
            load_listings(self.root)

    def test_invalid_utf8_fails(self):
        (self.root / SOURCES["ios"] / "en-US/description.txt").write_bytes(b"\xff")
        with self.assertRaisesRegex(ValueError, "utf-8"):
            load_listings(self.root)

    def test_multiline_short_fields_fail(self):
        for store, field in (("ios", "subtitle"), ("android", "title")):
            path = SOURCES[store] / "en-US" / (field + ".txt")
            with self.subTest(path=path):
                original = (self.root / path).read_bytes()
                self.replace(path, "Two\nlines\n")
                with self.assertRaisesRegex(ValueError, "must be a single line"):
                    load_listings(self.root)
                (self.root / path).write_bytes(original)

    def test_real_description_line_breaks_pass_but_literal_escape_fails(self):
        path = SOURCES["android"] / "ja-JP/full_description.txt"
        self.replace(path, "Heading\n\nBody\n")
        load_listings(self.root)
        self.replace(path, r"Heading\nBody")
        with self.assertRaisesRegex(ValueError, "literal"):
            load_listings(self.root)

    def test_whitespace_and_invisible_characters_fail(self):
        for text in (" Leading", "Trailing ", "Heading \nBody", "Body\n\n", "Body\r\n", "Body\t", "Body\x00", "\ufeffBody"):
            with self.subTest(text=repr(text)):
                self.replace(SOURCES["ios"] / "en-US/description.txt", text)
                with self.assertRaises(ValueError):
                    load_listings(self.root)

    def test_staging_normalizes_one_final_lf_and_preserves_sources(self):
        path = self.root / SOURCES["android"] / "en-US/title.txt"
        original = b"Budget+ Shared Expense Tracker\n"
        path.write_bytes(original)
        files = load_listings(self.root)
        destination = Path(self.temporary.name) / "staged"
        stage_listings(files, destination)
        self.assertEqual(len(list(destination.rglob("*.txt"))), 30)
        self.assertEqual((destination / "android/en-US/title.txt").read_bytes(), original[:-1])
        self.assertEqual(path.read_bytes(), original)
        for item in files:
            self.assertEqual((destination / item.destination).read_bytes(), item.text.encode("utf-8"))

    def test_staging_excludes_release_notes_and_other_metadata_or_images(self):
        ios = self.root / SOURCES["ios"] / "en-US"
        for field in ("release_notes", "keywords", "support_url"):
            (ios / (field + ".txt")).write_text("not part of this listing upload")
        (ios / "screenshots").mkdir()
        (ios / "screenshots/image.png").write_bytes(b"not an image")
        play = self.root / SOURCES["android"] / "en-US"
        (play / "images").mkdir()
        (play / "changelogs").mkdir()
        (play / "changelogs/default.txt").write_text("owned by app release")
        destination = Path(self.temporary.name) / "staged"
        stage_listings(load_listings(self.root), destination)
        self.assertEqual(len(list(destination.rglob("*.txt"))), 30)
        self.assertFalse(any(path.name in {"release_notes.txt", "keywords.txt", "support_url.txt",
                                          "images", "screenshots", "changelogs"}
                             for path in destination.rglob("*")))

    def test_staging_refuses_to_overwrite_existing_directory(self):
        destination = Path(self.temporary.name) / "staged"
        destination.mkdir()
        marker = destination / "keep.txt"
        marker.write_text("existing data")
        with self.assertRaises(FileExistsError):
            stage_listings(load_listings(self.root), destination)
        self.assertEqual(marker.read_text(), "existing data")

    def test_cli_stages_only_selected_store_without_credentials(self):
        destination = Path(self.temporary.name) / "cli-staged"
        result = subprocess.run(
            [sys.executable, str(ROOT / "store-assets/listings.py"), "--target", "ios",
             "--stage-dir", str(destination)],
            capture_output=True, text=True, check=True,
        )
        self.assertIn("No network calls made", result.stdout)
        self.assertTrue((destination / "ios/zh-Hant/name.txt").is_file())
        self.assertFalse((destination / "android").exists())
        self.assertFalse(list(destination.rglob("release_notes.txt")))


if __name__ == "__main__":
    unittest.main()
