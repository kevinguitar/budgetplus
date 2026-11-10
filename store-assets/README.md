# Store assets

Store screenshots for the App Store and Google Play, in five languages.

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

Run the **Upload store screenshots** workflow (Actions tab, manual trigger). By
default it uploads the screenshots for every device class (iPhone Duo included),
replacing the ones already in each language. Tick `ios_creative_assets` to also
upload the header and search results art, untick `ios_screenshots` to skip the
App Store screenshots, or tick `dry_run` to only stage and validate everything
(Google Play also runs a validate-only edit).

`stage_for_fastlane.py OUT_DIR` lays the files out for fastlane and checks sizes,
alpha and per-store limits, including the header and search results art; it runs
locally too.

fastlane doesn't upload the header and search results art, so
`upload_creative_assets.py [--version X]` does it through the App Store Connect API
(needs `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_PATH`). It places each language's
pair on the editable version's matching localization, replacing what is there, and
skips images that are already placed. The art goes to App Review with the version.
