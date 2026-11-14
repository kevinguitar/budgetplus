# Store assets

Localized listing copy and screenshots for the App Store and Google Play.
See [LISTINGS.md](LISTINGS.md) for copy rules, validation, and upload details.

- `listings/<platform>/<locale>/` — titles, subtitles, and descriptions.
- `in-app/<platform>/…` — in-app product and subscription names and descriptions.
- `original-screenshots/<platform>/<device>/<locale>/` — raw screenshots and editor positions (`anchors.json`).
- `screenshots/<platform>/<device-class>/<locale>/` — upload-ready screenshots, ordered by `NN-` prefix.
- `screenshots/ios/creative/<locale>/` — `header.png` (3840×1646) and `search.png` (3840×2560).
- `screenshots/android/creative/<locale>/featureGraphic.png` — Google Play feature graphic (1024×500).

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

Run [update_store_assets](../.github/workflows/update_store_assets.yml) manually.
Choose `android`, `ios`, or `both`, then enable `store_listing`, `screenshots`,
`creative_assets` (App Store header and search results, Google Play
feature graphic), and/or `in_app_listing` (all off by default). Only selected assets are
uploaded. iOS reuses an editable version or creates a patch draft; no builds
are uploaded or submitted for review.

Preview screenshots locally using a new output directory:

```bash
python3 store-assets/stage_for_fastlane.py build/store-assets-preview
```

Use `--target ios` / `--target android`, `--skip-screenshots`, or
`--skip-creative-assets` to limit staging. This makes no store API calls.
