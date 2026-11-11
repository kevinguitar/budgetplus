# Store assets

Store screenshots and listing-text tooling for the App Store and Google Play,
in five languages. See [Store listing text](LISTINGS.md) for version-controlled
names, subtitles, descriptions, validation, and manual uploads.

- `originals/<platform>/<device>/<locale>/` — raw device screenshots, unedited.
  `anchors.json` next to them holds the pixel positions the editor uses (badge
  icons, currency crop box), so the edited set can be regenerated.
- `final/<platform>/<device-class>/<locale>/01-login.png … 08-share.png` — the
  edited, upload-ready screenshots, in store order: login, books, shared, currency,
  overview, tones, colors, share. The `NN-` prefix sets the display order on both
  stores (both sort by file name).
- `final/ios/creative/<locale>/header.png`, `search.png` — App Store product page
  header (3840×1646) and search results art (3840×2560), one pair per language.
  Composed from the iPhone 18 Pro originals by `build/screenshot-bridge/tools/creative.py`
  (headline and feature line in `creative.json` next to it).

| Device class | Size | Store slot | Source device |
|---|---|---|---|
| `android/phone` | 1440×2560 | Play phone | Pixel 7a |
| `android/tablet` | 2560×1440 | Play 10-inch tablet | Medium Tablet (landscape) |
| `ios/iphone-6.3` | 1206×2622 | iPhone 6.3" (Dynamic Island, medium) | iPhone 18 Pro |
| `ios/iphone-duo-outer` | 1398×2034 | iPhone Duo outer display | iPhone Duo (folded) |
| `ios/ipad-13` | 2048×2732 | iPad 13" | iPad Air 13" (portrait) |

| Repo locale | App Store Connect | Google Play |
|---|---|---|
| `en-US` | en-US | en-US |
| `zh-Hant` | zh-Hant | zh-TW |
| `zh-Hans` | zh-Hans | zh-CN |
| `ja` | ja | ja-JP |
| `ko` | ko | ko-KR |

## Uploading

Run the **update_store_assets** workflow (Actions tab, manual
trigger; filename `update_store_assets.yml`). Keep `target` set to
`both`, or select `android` / `ios`. Tick `store_listing` to upload names,
subtitles/short descriptions, and full descriptions from `listings/` (off by
default). Tick `screenshots` to replace screenshots on the selected store(s) for
every device class, iPhone Duo included (off by default), or `ios_creative_assets`
for the App Store header and search-results art (also off). Listing-only uploads
leave screenshots untouched on both stores. Every upload is real; use local
staging to preview without contacting stores.

For any selected iOS upload, the workflow reuses an editable version or creates
the latest released version's next patch draft (e.g. `1.7.3` → `1.7.4`). It does
not upload a build or submit/release that draft. All options off is a no-op for
any target. See [Store listing text](LISTINGS.md) for details and limitations.

`stage_for_fastlane.py OUT_DIR` lays the files out for fastlane and checks sizes,
alpha and per-store limits, including the header and search results art; it runs
locally too. Add `--target ios` / `--target android`, `--skip-screenshots`,
and/or `--skip-creative-assets` to validate only selected assets. Use a new output
directory; existing directories are never deleted or overwritten.

fastlane doesn't upload the header and search results art, so
`upload_creative_assets.py [--version X]` does it through the App Store Connect API
(needs `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_PATH`). It places each language's
pair on the editable version's matching localization, replacing what is there, and
skips images that are already placed. The art goes to App Review with the version.
