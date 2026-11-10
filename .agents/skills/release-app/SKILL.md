---
name: release-app
description: Use when the user wants to release, ship, or publish the Android and/or iOS app, or bump the app version for a release. Asks which platforms to release and the bump type (major, minor, or patch), updates the version, commits and pushes to origin/main, then creates a GitHub release (`<version>-android` / `<version>-ios`). Creating the release starts the store publish workflows.
---

# Release App

A release has four parts: **pick platforms → bump version → commit and push →
create GitHub release(s)**. Creating a GitHub release also creates its tag, and
the tag push starts the publish workflow automatically:
- `<version>-android` → `.github/workflows/publish-android-app.yml`
- `<version>-ios` → `.github/workflows/publish-ios-app.yml`

Android and iOS have **separate version numbers** (for example, Android `3.9.1`
and iOS `1.7.1`). Only bump the platforms you're releasing.

## Where versions live

| Platform | File | Key |
|---|---|---|
| Android | `gradle.properties` | `appVersion=X.Y.Z` |
| iOS | `iosApp/Configuration/Config.xcconfig` | `MARKETING_VERSION=X.Y.Z` |

- The Android `versionCode` comes from `appVersion` in
  `build-logic/src/main/kotlin/BudgetPlusAppConventionPlugin.kt`
  (`major*10^6 + minor*10^3 + patch`). Minor and patch must stay **≤ 999**.
- `project.pbxproj` reads `$(MARKETING_VERSION)`, so don't edit the pbxproj.
- **Don't touch `CURRENT_PROJECT_VERSION`.** The iOS publish workflow sets it
  to the GitHub run number at build time.

## Steps

### 1. Check that the repo is ready

```sh
git status --porcelain              # must be empty
git branch --show-current           # must be main
git fetch origin && git pull --ff-only origin main
```

Stop and tell the user if the working tree has changes, the branch isn't
`main`, or the fast-forward fails. Don't stash, reset, or switch branches
without asking.

### 2. Gather the current state for each platform

```sh
grep '^appVersion=' gradle.properties
grep '^MARKETING_VERSION=' iosApp/Configuration/Config.xcconfig
git describe --tags --abbrev=0 --match '*-android'   # last Android release
git describe --tags --abbrev=0 --match '*-ios'       # last iOS release
git log --oneline <last-android-tag>..HEAD
git log --oneline <last-ios-tag>..HEAD
```

### 3. Ask which platforms to release

Use the question tool, unless the user already said which. Options: **Both**,
**Android only**, **iOS only**. For each platform, add the commit count since
its last release to the option description.

### 4. Ask the bump type for each selected platform

Use the question tool, with one question per platform. Options: **Patch**,
**Minor**, **Major**. Show the resulting version in each label, for example
`Patch (1.7.2)`.

Put **(Recommended)** on one option and list it first. Base the recommendation
on the commits since that platform's last release:
- **Major:** a big redesign or rework, a breaking data or sync change, or a new
  product direction. This is rare.
- **Minor:** a new feature or screen, a clearly visible UX change, or a notable
  behavior change.
- **Patch:** fixes, polish, copy or translation updates, dependency updates,
  performance work, or refactors with no visible change.

Ignore commits that don't ship anything for that platform when you recommend a
bump type. That includes CI, `ui-tests/`, `store-assets/`, docs, and
`androidApp/` or `iosApp/`-only changes when you're judging the other platform.

Reset the lower parts: minor → `X.Y+1.0`, major → `X+1.0.0`.

Edge case: if the current version is already newer than the last release tag,
the version was bumped but never released. Ask whether to release the current
version as is (no bump) or bump again.

### 5. Bump, commit, and push

Edit only the version line(s) for the selected platform(s). Then:

```sh
git add gradle.properties iosApp/Configuration/Config.xcconfig
git commit -m "Version bump"
git push origin main
```

Use one `Version bump` commit for both platforms. If the push is rejected, stop
and report it. Don't force-push.

### 6. Create the GitHub release(s)

Create the releases at the pushed commit, with notes generated from the **last
release of the same platform**:

```sh
SHA=$(git rev-parse HEAD)

gh release create <android-version>-android \
  --target "$SHA" \
  --title <android-version>-android \
  --generate-notes \
  --notes-start-tag <last-android-tag>

gh release create <ios-version>-ios \
  --target "$SHA" \
  --title <ios-version>-ios \
  --generate-notes \
  --notes-start-tag <last-ios-tag>
```

For example, `1.7.2-ios` uses `--notes-start-tag 1.7.1-ios`, and
`3.9.2-android` uses `--notes-start-tag 3.9.1-android`. Always pass
`--notes-start-tag`. Without it, GitHub compares against the latest release of
either platform.

### 7. Check that the publish jobs started

```sh
gh run list --workflow publish-android-app.yml --limit 1
gh run list --workflow publish-ios-app.yml --limit 1
```

A run can take a few seconds to show up after the tag is created. Report to the
user:
- Each platform's version change (old → new)
- The release URL(s)
- The publish run URL(s)
