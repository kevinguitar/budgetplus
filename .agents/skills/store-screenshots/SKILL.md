---
name: store-screenshots
description: Use when capturing or composing localized App Store / Google Play screenshots for Budget+ across the Android emulators and iOS simulators (store listing assets, "make store screenshots", new locale or device class). Looks up store size rules live; never hardcodes them.
---

# Store screenshots

Produces, for every device × locale × screen, a raw device screenshot plus an
edited store asset. Raw captures are never edited, so the edits can be redone.

## Requirements

1. Android and iOS. Every asset must meet the current store rules for its
   device class.
2. Locales: `en-US`, `zh-Hant`, `zh-Hans`, `ja`, `ko` (folder names follow
   `fastlane/metadata/`).
3. The screens follow the current store set (ask for the latest assets).
   Captions may change per locale. Keep assets realistic: real app UI,
   plausible data, little decoration.
4. All booted devices share one stage account. Prepare a locale's data once,
   on one device, then capture that locale on every device before switching.
5. Deliver one device × one locale first and get approval before batching.

## Look up store rules every run

Never hardcode sizes. Fetch and derive them each time:

- Apple: https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications
  (required display classes, accepted pixel sizes, max count, no alpha).
- Google Play: https://support.google.com/googleplay/android-developer/answer/9866151
  (min/max px, long side ≤ 2× short side, 9:16 or 16:9 for promotion, tablet
  minimum, max 8 per type, caption ≤ 20% of the image, no calls to action or
  ranking words such as "Best", "#1", "New", avoid device frames, clean status
  bar, alt text ≤ 140 chars).

Write the sizes you derived in the run notes, not in this skill.

## Tooling

Scripts are in `scripts/`. Working files go in `build/screenshot-bridge/`
(gitignored): `queue/`, `results/`, `flows/`, `fonts/`,
`files/originals/<platform>/<device>/<locale>/`, `files/final/<platform>/<class>/<locale>/`.

- `bridge.py`: the user runs it once in Terminal on the Mac. It runs queued
  jobs from an allowlist (`adb`, `xcrun simctl`, `maestro`, `sleep`) without a
  shell and writes only inside the working folder. Needed when Claude's shell
  can't reach adb/xcrun (cloud sessions, Linux VM) and computer use is off.
- `br.py` queues one job and waits. `ui.py <device>` prints the visible text
  with tap centers (Maestro hierarchy). `m.sh <device>` runs a Maestro flow
  from stdin. `cap.sh` saves a native screenshot into `originals/`. `rec.py`
  enters records in bulk; its coordinates are for the Pixel 7a.
- `compose.py` composes the store assets; captions are in `captions.json`.
  Run `fetch_fonts.py` once for Noto Sans CJK and Material Icons Round.
- Flows: `flows/create-book.yml`, `flows/open-delete-book.yml`.
- Drivers (one locale at a time, each call kept under ~3 minutes):
  `pixel_data.py LOCALE OLD_MAIN OLD_TRIP pref|books|currency|records|trip`
  (records/trip take `SKIP`/`LIMIT` env to enter 5 at a time),
  `android_capture.py pixel|tablet LOCALE record|overview|tones`,
  `ios_capture.py iphone|duo|ipad LOCALE currency|record|overview|modes|tones`
  (`REC=trip-only` retakes just the foreign-currency entry),
  `ios_login.sh UDID NAME [DISPLAY]`, `compose_all.sh LOCALE` (all device
  classes + a review sheet). Test data, UI strings and currency search names
  per locale live in `data/locales.json`; UI strings come from
  `core/common/src/commonMain/composeResources/values-*/strings.xml`.
- App id `com.kevlina.budgetplus.stage`; Android activity
  `com.kevlina.budgetplus.book.BookActivity`.

## Per-locale procedure

1. **Locale on every device.** Android:
   `adb shell cmd locale set-app-locales <pkg> --locales <en|zh-TW|zh-CN|ja-JP|ko-KR>`.
   iOS: relaunch with `-AppleLanguages (<tag>) -AppleLocale <tag>`.
2. **Clean status bar.** Android: demo mode broadcasts (clock 0941, battery
   100, wifi full, mobile hidden), then open the shade and tap "Clear all"
   (demo mode alone leaves notification icons). iOS:
   `xcrun simctl status_bar <udid> override --time 9:41 --batteryState charged --batteryLevel 100 --wifiBars 3 --cellularBars 4`.
3. **Data, on one device only.** Deleting books on stage is fine.
   - Create the books after switching locale, so the default categories are in
     that language. Delete the old books.
   - Main book in the local currency. Travel book in a foreign currency.
     Preferred currency set to the local one, so conversions show.
   - About 14 everyday records plus 1 income in the current month, and about
     11 trip records. Use local note text and amounts.
4. **Shots, on each device.** Raw files are named by content (`book-menu`,
   `record-entry`, …); the store order lives in `compose.py`: 01 login,
   02 books, 03 shared, 04 currency, 05 overview, 06 tones, 07 colors, 08 share.
   - 02: book menu open.
   - 03: record entry with category, note and amount filled in, not saved.
   - 04a/04b: two preset tones, e.g. 夕暮暖調 and 田野鄉村.
   - 05: custom palette set to the Barbie tone with the deep link
     `budgetplus://colors?hex=fff9fafb%3bfff9e7ef%3bffe59ebf%3bff849faf`
     (order light;lightBg;primary;dark). Wait for the snackbar to clear. Leave
     without saving so the app theme stays the default.
   - 06a: foreign-currency entry. 06b/06c: overview with the currency switch
     off and on.
   - 07a: overview list. 07b: overview summary in bar mode (the pie looks bad
     when rent dominates).
   - 08: color-wheel dialog opened from the primary swatch's edit icon.
5. **Anchors.** Next to each device/locale's originals, write `anchors.json`:
   pixel centers (in the raw screenshot) of the icons badges enlarge — invite
   (`record-entry`), share (`tone-share`), mode button (`overview-summary`),
   primary swatch edit (`color-wheel`), conversion icon (`currency-record`,
   the currency cards are placed just below it) — plus `crop-currency-overview`, the
   pixel box [l, t, r, b] from the currency switch down to the totals row in
   the overview. Use `ui.py` for labelled icons (iOS reports points: ×3) and a
   zoomed crop to check the rest. Layouts differ per device, so never reuse
   another device's numbers.
6. **Login screen (01) last.** It needs a logged-out app. Log out once, close
   the Android one-tap sign-in sheet, then capture all 5 locales back to back.
   Never sign in or enter credentials; the user signs back in.
7. **Compose** at the sizes you derived, one class at a time. View a contact
   sheet before delivering.

## Editing rules (from review)

- Background: olive `#8A8B7D` with a cream `#FFF2E0` **true circle** (large
  radius) behind the phones. Never use a stretched ellipse; the arc must look
  smooth.
- A badge must be centered on the in-app icon it enlarges and cover it
  completely (taken from `anchors.json`). A badge sitting near an icon looks
  wrong. If a badge would hide key UI text (the 06 conversion icon sits right
  under the amounts), use a free-standing glyph beside the phone instead
  (`float`).
- The 07 mode badge covers the mode button; covering the download button too
  is fine.
- Palette shots use the Barbie tone. Their badges use Barbie primary `#E59EBF`.
  Other badges use brown `#957660`.
- Rounded screenshots with no device frame on both stores (Play guidance; the
  owner approved the look for iOS too). Captions stay within the top ~17%;
  caption and badge sizes come from a 9:16-equivalent width so wide canvases
  (iPad) don't get oversized text.

## Devices

- Pixel 7a: portrait 1080×2400; Play phone canvas 1440×2560 (9:16).
- Medium Tablet: landscape (`settings put system accelerometer_rotation 0`,
  `user_rotation 0` — its natural orientation is landscape) for the nav-rail
  layout; Play tablet canvas 2560×1440 (16:9). Captions become one line.
- iPhone 18 Pro: 1206×2622 is itself an accepted size, compose at that size.
- iPhone Duo: two displays. `SIM_DISPLAY=1` = outer 1398×2034 (active when
  folded), `SIM_DISPLAY=3` = inner, landscape 2853×2007 when unfolded (the user
  unfolds it in Simulator). The outer layout puts status/invite/settings/tabs
  in a side column. Maestro cannot drive the inner display (its coordinates
  are offset and taps don't land), so inner screenshots need manual taps.
- iPad Air 13": portrait 2048×2732 (accepted 13"); the owner wants iPad in
  portrait. The app must run full screen, not windowed: windowed mode shows
  the app name ("Budget+ Stage") in the status bar and a resize handle — ask
  the user to switch the simulator to full-screen apps. Swipe the tone pager
  inside the palette area or it won't move.

## Locale gotchas

- Search currencies by localized name (`cur_names`), not ISO code: CJK
  keyboards swallow Latin text.
- zh-CN date pickers start weeks on Monday (`FIRST` offset in pixel_data.py);
  verify record dates in the overview after entry.
- Longer translations reflow layouts: English/Korean categories wrap to an
  extra row, Korean period chips wrap on the Duo outer screen, Japanese
  puts the palette entry lower in Settings (scroll to it). Re-check per-locale
  crop boxes (`crop-currency-overview`) and conversion anchors.
- Exchange rates load a few seconds after launch; wait before capturing the
  conversion row.
- Never re-run the `books` step with duplicate names: it creates before it
  deletes and can remove the new books.
- First login on iOS shows the tracking prompt; answer "Ask App Not to Track".

## Things to check

- Review every capture: leftover note text (the record screen keeps the last
  note across books), status bar icons, snackbars, tooltips, half-finished
  animations.
- Plain Latin text typed with Gboard's Zhuyin, Japanese or Korean layouts goes
  into composition and is lost. Text that includes CJK types fine with Maestro
  `inputText`. The color picker's hex field won't take typed input; use the
  colors deep link.
- Android BACK, and also ESC, on the record screen navigates back or exits the
  app; dismiss menus/keyboards by tapping empty space instead.
- Gboard's floating toolbar on the tablet covers the nav rail after typing;
  tap empty space to clear focus.
- iOS: Maestro `eraseText` doesn't clear the note field. Relaunch the app
  (`simctl launch --terminate-running-process`) to reset an unsaved note.
  Tap the note's centre before `inputText`; after a relaunch wait for the
  first animation or the text is dropped.
- iOS: the currency switch only flips via `tapOn: {text: "¥, \\$"}`, not a point.
- iOS asks "Open in …?" the first time a deep link opens; tap Open.
- iPhone Duo outer: typing scrolls the record pane up; swipe down twice on the
  upper pane before capturing.
- First-run tooltips appear on the overview (mode, export, record hint) and the
  palette share icon; dismiss each before capturing.
- Changing an empty book's currency shows no confirmation; a book with records
  asks to confirm.
- A first-run tooltip appears the first time the overview pie chart opens.
