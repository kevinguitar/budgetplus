# Store listing text

Listing text is version-controlled and uploaded only by the manual
**update_store_assets** workflow. Its selected branch/ref supplies the
text and assets. App release workflows are unchanged and do not read these
sources. The initial text comes from the November 2026 store audit, with
**Traditional Chinese as the semantic reference**.

## Edit the source files

| Store | Source directory | Listing fields per locale |
|---|---|---|
| App Store | `store-assets/listings/ios/<locale>/` | `name.txt`, `subtitle.txt`, `description.txt` |
| Google Play | `store-assets/listings/android/<locale>/` | `title.txt`, `short_description.txt`, `full_description.txt` |

| Language | App Store locale | Play locale |
|---|---|---|
| Traditional Chinese (reference) | `zh-Hant` | `zh-TW` |
| Simplified Chinese | `zh-Hans` | `zh-CN` |
| English | `en-US` | `en-US` |
| Japanese | `ja` | `ja-JP` |
| Korean | `ko` | `ko-KR` |

Both stores' description wording matches within each language, with different
heading styles: App Store uses plain-text `【Heading】`, while Google Play full
descriptions use `<b>Heading</b>`. Keep a blank line between sections. App Store
descriptions additionally include the Apple standard EULA link; Play descriptions
do not. Edit both files when changing a shared claim. These are independent,
editable store fields, not machine translations generated during upload.

Do not use HTML on App Store or Markdown `**bold**` on either store. Limit Play
HTML to the full-description headings; names, titles, subtitles, and short
descriptions remain unformatted. No custom colors, fonts, or font sizes are used.
Preview Play descriptions on both web and mobile because rendering can differ.

Titles and App Store subtitles lead with benefits, without paid-tier labels.
Keep Premium feature disclosures in descriptions so pricing scope stays clear.

Release notes remain exclusively in the app release workflows' existing locations:

- iOS: `fastlane/metadata/<locale>/release_notes.txt`.
- Android: `distribution/whatsnew/whatsnew-<locale>`, uploaded by the existing
  binary-upload action. The manual workflow skips release notes on both stores.

Do not move listing sources into `fastlane/metadata`: the unchanged iOS release
workflow already uploads that directory. Keeping listing copy separate prevents
accidental listing changes during an app release.

These are plain UTF-8 files using real LF line breaks. Names, titles, subtitles,
and short descriptions must occupy a single line. One terminal newline from an
editor is accepted; the staging step removes it before upload. This matters for
Google Play because Fastlane Supply reads text verbatim, and the English title
is already exactly 30 characters. Leading/trailing whitespace, extra trailing
blank lines, BOMs, invalid UTF-8, control characters, and literal `\n` escapes
are rejected.

## Validate and preview locally (no credentials or network)

From the repository root:

```bash
python3 store-assets/listings.py
python3 -m unittest discover -s store-assets -p 'test_*.py' -v
```

The tests use Python's standard library and Ruby (to record lane options), but
do not load Fastlane or call any store API.

To inspect exactly what the manual workflow will upload, choose a **new, non-existing** directory:

```bash
python3 store-assets/listings.py --stage-dir build/store-listing-preview
```

This writes `ios/<locale>/*.txt` and `android/<locale>/*.txt` without changing
source files. Add `--target ios` or `--target android` to stage and validate only
one store. It refuses to overwrite an existing directory. Pick a fresh output
path for the next preview. Staging is
allowlisted: screenshots, release notes, keywords, separate URL fields, pricing,
review credentials, and other metadata files are not copied. Adding a new field or
locale requires explicitly updating `store-assets/listings.py` and its tests.

Validation checks all five locales and required fields against these limits:

| Field | Limit |
|---|---|
| App Store name | 2–30 characters |
| App Store subtitle | 1–30 characters |
| App Store description | 1–4,000 characters |
| Play title | 1–30 characters |
| Play short description | 1–80 characters |
| Play full description | 1–4,000 characters |

Counts use Unicode code points, including internal line breaks and Play HTML
tags (not just the rendered text). Store Console
validation and review remain authoritative. Local validation does not establish
policy approval, translation quality, live feature availability, or permissions.

## Manual upload workflow

Open `.github/workflows/update_store_assets.yml` (**update_store_assets**) in
GitHub Actions and use **Run workflow**. It supports listing text, screenshots,
and creative assets. Every dispatched upload is real; use the local preview commands
above to validate without making store changes.

Inputs, in UI order:

| Input | Default | Behavior |
|---|---|---|
| `target` | `both` | Select `android`, `ios`, or `both`. |
| `store_listing` | Off | Upload names/titles, subtitles/short descriptions, and full descriptions to the selected store(s). |
| `screenshots` | Off | Replace screenshots in each language/device class on the selected store(s). |
| `ios_creative_assets` | Off | Replace App Store product-page header and search-results art. |

Listing text and screenshots are independently opt-in on both stores. Checking
only `store_listing` does not stage, validate, or replace screenshots. When both
options are selected, Play uploads text and screenshots in the same edit.
Leaving every checkbox off skips the job for any target and does not create an
iOS version. Selecting only `ios_creative_assets` with an Android-only target
is also a no-op. Disabled asset types and unselected stores are not validated
or uploaded.

The manual lanes live in `store-assets/Fastfile`; the workflow copies them into
its isolated checkout's `fastlane/Fastfile`. App release jobs still generate
their own release lanes, without using this file. The existing Play service
account and App Store Connect key secrets are reused. Temporary credentials are
removed with `always()` cleanup. Manual workflow runs are serialized; avoid
running an app release or making Console edits at the same time.

### Automatic iOS version selection

When any selected iOS upload needs a version, `ensure_ios_version.py` resolves it
once and passes the same version to listing, screenshot, and creative uploads:

1. Reuse an editable iOS version, choosing the highest numeric version if needed.
2. If none exists, derive the next patch from the latest released version:
   **`1.7.3` → `1.7.4`**. Both current `READY_FOR_DISTRIBUTION` and legacy
   `READY_FOR_SALE` states are supported, and all version pages are inspected.
3. Create that version as a **manual-release draft** in App Store Connect.

If the patch already exists but is locked/in review, fail with its state instead
of canceling review or inventing a further version. If a concurrent Console user
creates the same editable patch first, reuse it. If there is no released or
editable version, fail rather than guess a starting version.

Creating a store draft does **not** bump app source versions, create a Git tag,
upload a binary, select a build, submit for review, or release the app. A later
submission may need an appropriate build/version and manual review in App Store
Connect. Name/subtitle changes also depend on editable App Information. Existing
noneditable review states and permission errors are reported, not bypassed.

### Google Play behavior

**Google Play listing text and screenshots are app-wide**, subject to Play review
and publishing settings. Only advertise features available to that public
audience. The manual lane looks up a production version code to disambiguate
Fastlane's metadata edit, but does not upload APK/AAB files, change changelogs,
modify rollout, or promote/update a track. A production release must already
exist. Other Play image types (icons/graphics) are not uploaded.

### Validation and tests

Store listing validation and tooling tests are not part of PR CI. The manual
workflow validates the selected text and assets before uploading. The local
commands above validate all five locales and run offline regression tests for
text, selective asset staging, version resolution, manual lane boundaries, and
all target/checkbox combinations. Local validation and tests do not authenticate
or upload anything.

## Before the first manual listing upload

- Review the names/subtitles and obtain native Japanese/Korean review.
- Confirm the release build and active Premium offering match the copy on both
  platforms. For Play, also check the publicly available version, not just beta.
- Confirm that the Apple standard EULA link matches the configured agreement.
- Preserve the currency distinctions: Premium entry/display conversion uses
  available rates; it is not guaranteed realtime or transaction-day FX, and no
  manual rate override is promised. Changing a book's currency does not convert
  existing amounts. Settings' preferred-currency preference itself is not paid.
- Keep the combined created/joined ten-book Premium limit and the one-book,
  ad-supported free limit accurate.
- Verify the existing store API credentials have listing-edit permissions. The
  upload paths have not been tested against live APIs as part of implementation.
- Treat this repo as the source of truth after rollout: manual Console edits to
  these fields will be overwritten by the next manual listing upload unless
  mirrored here. To roll back text, restore the previous source values and upload
  through the appropriate store's normal review/release process; reverting a commit
  alone cannot undo a completed store upload.

## References

- [Apple app information](https://developer.apple.com/help/app-store-connect/reference/app-information/app-information/)
- [Apple version information](https://developer.apple.com/help/app-store-connect/reference/app-information/platform-version-information/)
- [Google Play listing guidance](https://support.google.com/googleplay/android-developer/answer/13393723?hl=en)
- [Fastlane Deliver](https://docs.fastlane.tools/actions/upload_to_app_store/)
- [Fastlane Supply](https://docs.fastlane.tools/actions/upload_to_play_store/)
