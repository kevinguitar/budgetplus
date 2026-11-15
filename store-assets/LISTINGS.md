# Store listing text

Listing text is uploaded through the manual
[update-store-assets](../.github/workflows/update-store-assets.yml) workflow,
not app releases. Validation runs locally or before manual uploads, not in PR CI.

## Source files

| Store | Directory | Files |
|---|---|---|
| App Store | `store-assets/listings/ios/<locale>/` | `name.txt`, `subtitle.txt`, `description.txt` |
| Google Play | `store-assets/listings/android/<locale>/` | `title.txt`, `short_description.txt`, `full_description.txt` |
| App Store in-app | `store-assets/in-app/ios/products/<product ID>/<locale>/` | `name.txt`, `description.txt` |
| App Store subscription groups | `store-assets/in-app/ios/subscription-groups/<reference name>/<locale>/` | `name.txt`, optional `custom_app_name.txt` |
| Google Play in-app | `store-assets/in-app/android/<product ID>/<locale>/` | `title.txt`, `description.txt`, optional `benefits.txt` (subscriptions) |

Use **Traditional Chinese as the reference** and update both platforms' wording
together. Translate the latest approved selection; do not restore removed points.
Keep shared feature claims consistent, but adapt platform-specific wording:
Material You widget colors are Android-only; iOS widgets require iOS 17+, saved
presets, and show up to 3 items at medium size or 5 at large size. iOS voice input
also needs speech recognition permission. See [locale mappings](README.md). Keep listing files outside
`fastlane/metadata`, which the iOS release workflow already uploads. Release
notes stay in their existing locations and are not part of manual uploads.

### Copy and formatting

- Use UTF-8 with LF line breaks; names, titles, subtitles, and short descriptions are single-line.
- App Store headings: `【Heading】`. Play full-description headings: `<b>Heading</b>`.
  Separate sections with blank lines; no HTML on App Store or Markdown formatting on either store.
- Keep paid-tier labels out of titles, subtitles, and short descriptions;
  disclose Premium features in full descriptions.
- Keep the Apple EULA link in iOS descriptions only; confirm it matches the configured agreement.
- Limits: Apple name/subtitle **30**, Play title **30**, Play short description **80**,
  full descriptions **4,000** characters. Counts include line breaks, bullet characters,
  HTML tags on Play, and the EULA footer on iOS. Shorten translations as needed without
  dropping paid-tier disclosures or important limitations; paragraph counts are not fixed.

### In-app products and subscriptions

- iOS `products/` covers In-App Purchases and auto-renewable subscriptions; the uploader
  finds each product ID's type on App Store Connect. Group directories use the
  subscription group's reference name.
- Every product needs Traditional Chinese (`zh-Hant` / `zh-TW`); other locales are optional.
  Only the products, locales, and fields present are uploaded. Other store locales are kept,
  and products are never created, priced, or deleted.
- Plain text. Names, titles, and App Store descriptions are single-line; Play descriptions
  may use one feature per line. Limits: App Store name **2–35**, description **55**;
  Play title **55**, description **200** (line breaks count), `benefits.txt` up to **4**
  lines of **40**. Apple's help page still lists 30/45, but approved products already
  exceed 45; keep App Store descriptions at **53** or less (the longest approved so far)
  until a longer one is accepted. Subscription group names and custom app names have no
  published limit; the API enforces it on upload.
- Copy follows each locale's "Free and Premium" summary in the full description and the app's
  own terms (進階版 / 高级版 / プレミアム版 / 프리미엄; 记账, not 记帐). Every Premium
  feature in that summary should appear in Play descriptions; App Store descriptions keep as
  many as fit, in this order: no ads, 10 books, batch entry, currency conversion, themes,
  longer periods. All products share the same text per locale.

## Check and preview

From the repo root (Python 3; Ruby also required for tests). These commands are offline:

```bash
python3 store-assets/listings.py
python3 -m unittest discover -s store-assets -p 'test_*.py' -v
python3 store-assets/listings.py --stage-dir build/store-listing-preview
python3 store-assets/in_app_listings.py
```

With store credentials, preview in-app changes without writing anything:
`python3 store-assets/upload_in_app_listings.py --store ios --dry-run` (or `--store android`;
see the script header for the environment variables).

Add `--target ios` or `--target android` to validate/stage one store. The staging
directory must not already exist. Source files are never modified.

## Upload

In GitHub Actions, run **update-store-assets** against the desired branch/ref:

| Input | Default | Action |
|---|---|---|
| `target` | `both` | Choose `android`, `ios`, or `both`. |
| `store_listing` | Off | Upload listing text. |
| `screenshots` | Off | Replace screenshots on the selected store(s). |
| `creative_assets` | Off | Replace App Store header/search-results art and the Play feature graphic. |
| `in_app_listing` | Off | Upload in-app product, subscription, and subscription group text. |

Text, screenshots, creative art, and in-app text are independently opt-in. All options off is a no-op.
Every upload is real; review the copy and confirm released features/entitlements first.

- **iOS:** Reuses an editable version or creates the latest release's next patch
  draft (`1.7.3` → `1.7.4`). A locked patch fails instead of bypassing review.
  This does not bump app source versions, upload a build, submit, or release the app.
- **Play:** Changes are app-wide, subject to Play review/publishing settings.
  An existing production release is required; binaries, tracks, and release notes are untouched.
- **In-app (iOS):** Product and group text is versioned separately from the app, so no app
  version is created. Unchanged products are skipped. A draft metadata version is edited
  in place; after approval a new draft is created. Drafts are **not** submitted: submit them
  in App Store Connect, alone or with an app version. Products in review fail untouched.
- **In-app (Play):** Patches only the listings of existing one-time products and subscriptions.
- Uploads overwrite the selected fields/assets, including manual Console edits.
  Avoid concurrent app releases or Console edits while this workflow runs.
