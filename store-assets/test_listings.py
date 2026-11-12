"""Offline validation and staging tests; no credentials or store API calls."""

from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import unittest

from listings import FIELDS, LOCALES, ROOT, SOURCES, load_listings, stage_listings


# Only these sections may differ between stores. All other localized copy stays
# identical apart from heading markup and the iOS EULA footer.
PLATFORM_SECTIONS = {
    "zh-Hant": (
        "小工具配色，跟著桌布走",
        ("不同手機，也能一起記", "語音輸入，少打一點字", "常記的項目，放在桌面"),
    ),
    "zh-Hans": (
        "小组件配色，跟着壁纸走",
        ("不同手机，也能一起记", "语音输入，少打一点字", "常记的项目，放在桌面"),
    ),
    "en-US": (
        "Widget colors that fit your wallpaper",
        ("Different phones, one book", "Speak instead of typing", "Keep frequent items on your Home Screen"),
    ),
    "ja": (
        "壁紙になじむウィジェット",
        ("違うスマホでも、一緒に記帳", "音声入力で、文字入力を少なく", "よく使う項目をホーム画面に"),
    ),
    "ko": (
        "배경화면에 어울리는 위젯 색상",
        ("휴대폰이 달라도 함께 기록", "음성 입력으로 타이핑을 줄이세요", "자주 쓰는 항목을 홈 화면에"),
    ),
}
EULA_URL = "https://www.apple.com/legal/internet-services/itunes/dev/stdeula/"


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

    def description_sections(self, text, heading_pattern):
        parts = re.split(heading_pattern, text, flags=re.M)
        pairs = list(zip(parts[1::2], parts[2::2]))
        self.assertTrue(pairs)
        self.assertEqual(len(pairs), len({heading for heading, _ in pairs}))
        for heading, body in pairs:
            self.assertTrue(heading.strip())
            self.assertTrue(body.strip().startswith("• "))
        return parts[0].strip(), {heading: body.strip() for heading, body in pairs}

    def assert_store_descriptions_match(self, apple_locale, play_locale):
        apple = (self.root / SOURCES["ios"] / apple_locale / "description.txt").read_text().rstrip("\n")
        play = (self.root / SOURCES["android"] / play_locale / "full_description.txt").read_text().rstrip("\n")
        apple_body, footer = apple.rsplit("\n\n", 1)
        apple_intro, apple_sections = self.description_sections(apple_body, r"^【([^\n]+)】$")
        play_intro, play_sections = self.description_sections(play, r"^<b>([^<>\n]+)</b>$")
        android_only, adapted = PLATFORM_SECTIONS[apple_locale]

        self.assertEqual(apple_intro, play_intro)
        self.assertIn(android_only, play_sections)
        self.assertEqual(list(apple_sections), [heading for heading in play_sections if heading != android_only])
        # Match the user's approved reference, not an obsolete fixed section count.
        reference = (self.root / SOURCES["android"] / "zh-TW/full_description.txt").read_text()
        self.assertEqual(len(play_sections), len(re.findall(r"^<b>([^<>\n]+)</b>$", reference, re.M)))
        for heading, body in apple_sections.items():
            if heading in adapted:
                self.assertNotEqual(body, play_sections[heading])
            else:
                self.assertEqual(body, play_sections[heading])

        self.assertNotIn("<", apple)
        self.assertNotIn("**", apple + play)
        self.assertNotIn("<", re.sub(r"^<b>([^<>\n]+)</b>$", r"\1", play, flags=re.M))
        self.assertNotRegex(apple, r"Android|Material You")
        self.assertIn("Material You", play_sections[android_only])
        self.assertIn("12", play_sections[android_only])
        widget = apple_sections[adapted[2]]
        for detail in ("iOS 17", "30", "3", "5"):
            self.assertIn(detail, widget)
        self.assertTrue(footer.endswith(EULA_URL))
        self.assertEqual(apple.count(EULA_URL), 1)
        self.assertNotIn(EULA_URL, play)

    def test_description_heading_styles_and_shared_wording_match_across_stores(self):
        for apple_locale, play_locale in LOCALES.items():
            with self.subTest(locale=apple_locale):
                self.assert_store_descriptions_match(apple_locale, play_locale)

    def test_shared_description_wording_drift_fails(self):
        path = SOURCES["android"] / "en-US/full_description.txt"
        self.replace(path, (self.root / path).read_text().replace("Invite family", "Invite anyone"))
        with self.assertRaises(AssertionError):
            self.assert_store_descriptions_match("en-US", "en-US")

    def test_android_only_widget_section_in_ios_fails(self):
        path = SOURCES["ios"] / "en-US/description.txt"
        body, footer = (self.root / path).read_text().rstrip("\n").rsplit("\n\n", 1)
        section = "【Widget colors that fit your wallpaper】\n• Material You on Android 12+."
        self.replace(path, f"{body}\n\n{section}\n\n{footer}\n")
        with self.assertRaises(AssertionError):
            self.assert_store_descriptions_match("en-US", "en-US")

    def test_ios_widget_minimum_version_is_required(self):
        path = SOURCES["ios"] / "en-US/description.txt"
        self.replace(path, (self.root / path).read_text().replace("iOS 17+", "iOS 16+"))
        with self.assertRaises(AssertionError):
            self.assert_store_descriptions_match("en-US", "en-US")

    def test_ios_eula_is_required(self):
        path = SOURCES["ios"] / "en-US/description.txt"
        self.replace(path, (self.root / path).read_text().replace(EULA_URL, "https://example.com/"))
        with self.assertRaises(AssertionError):
            self.assert_store_descriptions_match("en-US", "en-US")

    def test_ios_length_limit_includes_eula_footer(self):
        path = SOURCES["ios"] / "en-US/description.txt"
        footer = f"\n\nTerms of Use (EULA): {EULA_URL}"
        text = "x" * (4000 - len(footer)) + footer
        self.replace(path, text)
        load_listings(self.root)
        self.replace(path, "x" + text)
        with self.assertRaisesRegex(ValueError, "4001 characters; expected 1–4000"):
            load_listings(self.root)

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
