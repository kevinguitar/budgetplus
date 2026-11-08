# Store assets

Store screenshots for the App Store and Google Play, in five languages.

- `originals/<platform>/<device>/<locale>/` — raw device screenshots, unedited.
  `anchors.json` next to them holds the pixel positions the editor uses (badge
  icons, currency crop box), so the edited set can be regenerated.
- `final/<platform>/<device-class>/<locale>/01-login.png … 08-colors.png` — the
  edited, upload-ready screenshots.

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

Run the **Upload store screenshots** workflow (Actions tab, manual trigger). It
defaults to a dry run that stages and validates everything and runs a
validate-only edit on Google Play. Turn off `dry_run` to upload.

`stage_for_fastlane.py OUT_DIR [--with-duo]` lays the files out for fastlane
and checks sizes, alpha and per-store limits; it runs locally too.
