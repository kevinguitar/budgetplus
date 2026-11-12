# Store listing text

Listing text is uploaded through the manual
[update_store_assets](../.github/workflows/update_store_assets.yml) workflow,
not app releases. Validation runs locally or before manual uploads, not in PR CI.

## Source files

| Store | Directory | Files |
|---|---|---|
| App Store | `store-assets/listings/ios/<locale>/` | `name.txt`, `subtitle.txt`, `description.txt` |
| Google Play | `store-assets/listings/android/<locale>/` | `title.txt`, `short_description.txt`, `full_description.txt` |

Use **Traditional Chinese as the reference** and update both platforms' wording
together. See [locale mappings](README.md). Keep listing files outside
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
  full descriptions **4,000** characters. Counts include line breaks and HTML tags.

## Check and preview

From the repo root (Python 3; Ruby also required for tests). These commands are offline:

```bash
python3 store-assets/listings.py
python3 -m unittest discover -s store-assets -p 'test_*.py' -v
python3 store-assets/listings.py --stage-dir build/store-listing-preview
```

Add `--target ios` or `--target android` to validate/stage one store. The staging
directory must not already exist. Source files are never modified.

## Upload

In GitHub Actions, run **update_store_assets** against the desired branch/ref:

| Input | Default | Action |
|---|---|---|
| `target` | `both` | Choose `android`, `ios`, or `both`. |
| `store_listing` | Off | Upload listing text. |
| `screenshots` | Off | Replace screenshots on the selected store(s). |
| `creative_assets` | Off | Replace App Store header/search-results art and the Play feature graphic. |

Text, screenshots, and creative art are independently opt-in. All options off is a no-op.
Every upload is real; review the copy and confirm released features/entitlements first.

- **iOS:** Reuses an editable version or creates the latest release's next patch
  draft (`1.7.3` → `1.7.4`). A locked patch fails instead of bypassing review.
  This does not bump app source versions, upload a build, submit, or release the app.
- **Play:** Changes are app-wide, subject to Play review/publishing settings.
  An existing production release is required; binaries, tracks, and release notes are untouched.
- Uploads overwrite the selected fields/assets, including manual Console edits.
  Avoid concurrent app releases or Console edits while this workflow runs.
