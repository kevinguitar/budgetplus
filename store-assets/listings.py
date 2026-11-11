#!/usr/bin/env python3
"""Validate store text offline and optionally stage a text-only Fastlane upload.

Usage: python3 store-assets/listings.py [--target both|ios|android] [--stage-dir NEW_DIRECTORY]
Only the allowlisted text fields below are staged; no screenshots, pricing,
review credentials, or Play changelogs are included. Sources are never modified.
"""

import argparse
from dataclasses import dataclass
from pathlib import Path
import sys
import unicodedata


ROOT = Path(__file__).resolve().parents[1]
LOCALES = {  # App Store Connect -> Google Play; Traditional Chinese is canonical.
    "zh-Hant": "zh-TW",
    "zh-Hans": "zh-CN",
    "en-US": "en-US",
    "ja": "ja-JP",
    "ko": "ko-KR",
}
SOURCES = {store: Path("store-assets/listings") / store for store in ("ios", "android")}
# (minimum, maximum, single line). Counts are Unicode code points, not bytes.
FIELDS = {
    "ios": {
        "name": (2, 30, True),
        "subtitle": (1, 30, True),
        "description": (1, 4000, False),
    },
    "android": {
        "title": (1, 30, True),
        "short_description": (1, 80, True),
        "full_description": (1, 4000, False),
    },
}


@dataclass(frozen=True)
class ListingFile:
    store: str
    locale: str
    field: str
    text: str

    @property
    def destination(self):
        return Path(self.store) / self.locale / (self.field + ".txt")


def load_listings(root=ROOT, target="both"):
    """Return validated text or raise one error listing every invalid field."""
    files, errors = [], []
    for store, source in SOURCES.items():
        if target != "both" and store != target:
            continue
        directory = root / source
        locales = set(LOCALES if store == "ios" else LOCALES.values())
        if not directory.is_dir():
            errors.append(f"{source}: missing metadata directory")
        else:
            actual = {p.name for p in directory.iterdir() if p.is_dir() and not p.name.startswith(".")}
            for locale in sorted(actual - locales):
                errors.append(f"{source / locale}: unexpected locale directory")

        for locale in LOCALES if store == "ios" else LOCALES.values():
            for field, (minimum, maximum, single_line) in FIELDS[store].items():
                path = source / locale / (field + ".txt")
                try:
                    # Decode bytes directly so CRLF/BOM are not silently normalized.
                    raw = (root / path).read_bytes().decode("utf-8")
                except (OSError, UnicodeError) as error:
                    errors.append(f"{path}: {error}")
                    continue

                # Editors may add one terminal LF. Do not upload it, especially
                # for a Play title already at the 30-character limit (Supply
                # reads files verbatim, unlike Deliver's whitespace stripping).
                text = raw[:-1] if raw.endswith("\n") else raw
                if not minimum <= len(text) <= maximum:
                    errors.append(f"{path}: {len(text)} characters; expected {minimum}–{maximum}")
                if text != text.strip() or any(line != line.rstrip() for line in text.split("\n")):
                    errors.append(f"{path}: leading/trailing whitespace or trailing blank lines")
                if single_line and "\n" in text:
                    errors.append(f"{path}: must be a single line")
                if "\\n" in text:
                    errors.append(f"{path}: literal \\n; use real line breaks in descriptions")
                if any(unicodedata.category(char) in {"Cc", "Cs"} and char != "\n" for char in text):
                    errors.append(f"{path}: control characters are not allowed (use UTF-8 with LF)")
                if "\ufeff" in text:
                    errors.append(f"{path}: UTF-8 BOM is not allowed")
                files.append(ListingFile(store, locale, field, text))

    if errors:
        raise ValueError("\n".join(errors))
    return files


def stage_listings(files, destination):
    """Write only validated fields to a NEW directory; never delete existing data."""
    destination.mkdir(parents=True, exist_ok=False)
    for listing in files:
        path = destination / listing.destination
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(listing.text.encode("utf-8"))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", choices=("both", "ios", "android"), default="both")
    parser.add_argument("--stage-dir", type=Path, help="New output directory for ios/ and android/ metadata")
    args = parser.parse_args()
    try:
        files = load_listings(target=args.target)
        if args.stage_dir is not None:
            stage_listings(files, args.stage_dir)
    except (ValueError, OSError) as error:
        print(f"Store listing validation failed:\n{error}", file=sys.stderr)
        return 1

    for listing in files:
        maximum = FIELDS[listing.store][listing.field][1]
        print(f"{str(listing.destination):55} {len(listing.text):4} / {maximum}")
    print(f"Validated {len(files)} text fields across five locales (target: {args.target}). No network calls made.")
    if args.stage_dir is not None:
        print(f"Staged text-only metadata at {args.stage_dir}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
