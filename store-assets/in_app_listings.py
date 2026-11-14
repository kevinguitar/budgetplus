#!/usr/bin/env python3
"""Validate in-app product and subscription text offline.

Usage: python3 store-assets/in_app_listings.py [--target both|ios|android]

Sources live in store-assets/listings/in-app/ and use each store's locale codes
(see LOCALES in listings.py). One directory per product, keyed by the store's ID:

  ios/products/<product ID>/<locale>/                name.txt, description.txt
      (In-App Purchases and auto-renewable subscriptions alike)
  ios/subscription-groups/<reference name>/<locale>/ name.txt [, custom_app_name.txt]
  android/<product ID>/<locale>/                     title.txt, description.txt
      [, benefits.txt: subscriptions only, one benefit per line]

Only the products, locales, and fields present are uploaded; anything else on the
stores is left alone. Traditional Chinese is required for every product.
"""

import argparse
from dataclasses import dataclass, field
from pathlib import Path
import re
import sys

from listings import LOCALES, ROOT, read_text


SOURCE = Path("store-assets/listings/in-app")
CANONICAL = {"ios": "zh-Hant", "android": "zh-TW"}
# Directory kinds per store: kind -> directory under SOURCE/<store>.
KINDS = {
    "ios": {"products": "products", "subscription-groups": "subscription-groups"},
    "android": {"products": "."},
}
# field -> (minimum, maximum or None, single line, required). Counts are Unicode code points.
# Apple's help page still says 30/45 for In-App Purchases, but approved products already
# exceed 45, so these follow the current App Store Connect limits. Apple and Google publish
# no limits for the None fields; the store APIs enforce them on upload.
FIELDS = {
    ("ios", "products"): {
        "name": (2, 35, True, True),
        "description": (1, 55, True, True),
    },
    ("ios", "subscription-groups"): {
        "name": (1, None, True, True),
        "custom_app_name": (1, None, True, False),
    },
    ("android", "products"): {
        "title": (1, 55, True, True),
        "description": (1, 200, False, True),  # Play accepts line breaks; they count toward 200
        "benefits": (1, None, False, False),
    },
}
MAX_BENEFITS, MAX_BENEFIT_LENGTH = 4, 40
PRODUCT_ID = {
    "ios": re.compile(r"[A-Za-z0-9._]+"),
    "android": re.compile(r"[a-z0-9][a-z0-9._]{0,39}"),
}


@dataclass
class InAppListing:
    store: str
    kind: str      # "products" or "subscription-groups"
    key: str       # product ID, or the subscription group's reference name
    locale: str
    fields: dict = field(default_factory=dict)

    @property
    def path(self):
        return SOURCE / self.store / KINDS[self.store][self.kind] / self.key / self.locale


def visible_dirs(directory, errors, label):
    """Subdirectories, reporting stray files; hidden entries are ignored."""
    entries = sorted(p for p in directory.iterdir() if not p.name.startswith("."))
    for path in entries:
        if not path.is_dir():
            errors.append(f"{label / path.name}: unexpected file")
    return [p for p in entries if p.is_dir()]


def load_in_app_listings(root=ROOT, target="both"):
    """Return validated in-app listings, or raise one error listing every problem.

    A store without a source directory simply has nothing to upload.
    """
    listings, errors = [], []
    for store, kinds in KINDS.items():
        if target not in ("both", store):
            continue
        base = SOURCE / store
        if not (root / base).is_dir():
            continue
        if store == "ios":
            for path in visible_dirs(root / base, errors, base):
                if path.name not in kinds.values():
                    errors.append(f"{base / path.name}: unexpected directory; expected {', '.join(kinds.values())}")
        locales = set(LOCALES) if store == "ios" else set(LOCALES.values())

        for kind, directory in kinds.items():
            kind_dir = (base / directory) if directory != "." else base
            if not (root / kind_dir).is_dir():
                continue
            fields = FIELDS[store, kind]
            for item in visible_dirs(root / kind_dir, errors, kind_dir):
                key, item_dir = item.name, kind_dir / item.name
                if kind == "products" and not PRODUCT_ID[store].fullmatch(key):
                    errors.append(f"{item_dir}: not a valid {store} product ID")
                found = [p.name for p in visible_dirs(root / item_dir, errors, item_dir)]
                for locale in found:
                    if locale not in locales:
                        errors.append(f"{item_dir / locale}: unexpected locale directory")
                if CANONICAL[store] not in found:
                    errors.append(f"{item_dir / CANONICAL[store]}: missing (Traditional Chinese is required)")
                for locale in sorted(set(found) & locales):
                    listing = load_locale(root, store, kind, key, locale, fields, errors)
                    listings.append(listing)

    if errors:
        raise ValueError("\n".join(errors))
    return listings


def load_locale(root, store, kind, key, locale, fields, errors):
    listing = InAppListing(store, kind, key, locale)
    directory = listing.path
    for path in sorted((root / directory).iterdir()):
        if path.name.startswith("."):
            continue
        if path.suffix != ".txt" or path.stem not in fields or not path.is_file():
            errors.append(f"{directory / path.name}: unexpected entry; expected {', '.join(f + '.txt' for f in fields)}")
    for name, (minimum, maximum, single_line, required) in fields.items():
        path = directory / (name + ".txt")
        if not required and not (root / path).exists():
            continue
        text = read_text(root, path, minimum, maximum, single_line, errors)
        if text is None:
            continue
        if name == "benefits" and text:
            lines = text.split("\n")
            if len(lines) > MAX_BENEFITS:
                errors.append(f"{path}: {len(lines)} benefits; expected at most {MAX_BENEFITS}")
            for number, line in enumerate(lines, 1):
                if not line:
                    errors.append(f"{path}: line {number} is empty")
                elif len(line) > MAX_BENEFIT_LENGTH:
                    errors.append(f"{path}: line {number} has {len(line)} characters; expected at most {MAX_BENEFIT_LENGTH}")
        listing.fields[name] = text
    return listing


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--target", choices=("both", "ios", "android"), default="both")
    args = parser.parse_args()
    try:
        listings = load_in_app_listings(target=args.target)
    except (ValueError, OSError) as error:
        print(f"In-app listing validation failed:\n{error}", file=sys.stderr)
        return 1

    for listing in listings:
        for name, text in listing.fields.items():
            maximum = FIELDS[listing.store, listing.kind][name][1]
            limit = f"{len(text):4} / {maximum}" if maximum else f"{len(text):4}"
            print(f"{str(listing.path / (name + '.txt')):90} {limit}")
    products = {(item.store, item.kind, item.key) for item in listings}
    print(f"Validated {len(products)} in-app items, {len(listings)} localizations (target: {args.target}). "
          "No network calls made.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
