---
name: investigate-ui-test-failures
description: Use when investigating failing or flaky UI tests (Maestro iOS/Android flows) in this repo — a GitHub Actions "iOS UI tests" / "Android UI tests" run/job failed, a UI-tests PR check is red, or the user shares a workflow run/job URL and asks to check or fix a UI test. ALWAYS download the test report artifacts and inspect the logs and screenshots before proposing any fix.
---

# Investigate UI Test Failures

Maestro UI test flows live under `ui-tests/` and run in the `iOS UI tests` and
`Android UI tests` GitHub Actions workflows. When a flow fails, the log line
(e.g. `Assertion is false: "X" is visible`) is **not enough** to diagnose the
cause — it only says which assertion failed, not what the app was actually
showing. You MUST download the run's artifacts and inspect the screenshots and
per-flow `maestro.log` before making any change.

## Hard rule

**Never propose or apply a fix from the CI log text alone.** The assertion
message routinely points at a shared subflow (e.g. `setup-login.yml`) while the
real failure is somewhere else (sign-in bounced back, a dialog covered the
target, the ad SDK never loaded, a tap missed). Download the report, look at the
failing-step screenshot and the surrounding steps, and read `maestro.log`
first. Only then form a diagnosis.

## Workflow

### 1. Authenticate gh

This repo stores the token in `local.properties`. Follow the `github-cli`
skill:

```sh
export GH_TOKEN=$(grep '^GH_TOKEN=' local.properties | cut -d= -f2-)
```

Keep it exported in the same shell for all `gh` calls below.

### 2. Identify the failing jobs

From a run or job URL the user shares, confirm which jobs failed:

```sh
gh api repos/kevinguitar/budgetplus/actions/jobs/<JOB_ID> \
  --jq '{name,status,conclusion}'
```

Get the failing-flow summary from the job log (assertion + which flow):

```sh
gh api --allow-escape-sequences \
  repos/kevinguitar/budgetplus/actions/jobs/<JOB_ID>/logs > /tmp/job.log
```

Then search `/tmp/job.log` with Grep for `[Failed]`, `passed,.*failed`, and
`Assertion is false` to get the failing flow name(s) and assertion text. Note
the log needs `--allow-escape-sequences` (it contains terminal colors) and
`cat -v` / Read may be needed to make it legible.

Tip: the two runner variants are named `iOS UI tests (free)` and
`iOS UI tests (premium)` (and the Android equivalents). Match the failing flow
to its variant folder under `ui-tests/after-login/{free,premium}/`.

### 3. Download the test report artifacts

List artifacts for the run (a job URL contains the run id, or use
`gh run view <JOB_ID>` to find it):

```sh
gh api repos/kevinguitar/budgetplus/actions/runs/<RUN_ID>/artifacts \
  --jq '.artifacts[] | {name,id,size:.size_in_bytes}'
```

Artifacts are named `ios-ui-test-artifacts-{free,premium,login}` and
`android-ui-test-artifacts-{free,premium,login}`. Download and unzip the one
matching the failing variant into a temp dir (they can be large — 100MB+):

```sh
mkdir -p /tmp/uitest
gh api repos/kevinguitar/budgetplus/actions/artifacts/<ARTIFACT_ID>/zip \
  > /tmp/uitest/report.zip
unzip -q -o /tmp/uitest/report.zip -d /tmp/uitest/report
```

The layout is:

```
build/maestro/{ios,android}/<suite>/<timestamp>/<flow-name>/
  commands.json
  manifest.json
  maestro.log                 (device logs also under logs/)
  screenshots/step-NNN-<action>.png
  screen-hierarchy/step-NNN-<action>.json
```

### 4. Inspect the evidence (required before any fix)

For the failing flow directory:

- **Read the failing-step screenshot** with the Read tool
  (`screenshots/step-NNN-...png`, usually the highest-numbered / the
  `assertCondition` step). Confirm what screen the app was actually on.
- **Read the screenshots of the preceding steps** to see how it got there
  (e.g. did a sign-in tap land, did a dialog appear/cover the target).
- **Grep the per-flow `maestro.log`** for the tap/assert timeline: when each
  command started/finished, how long it waited, and where it failed. This
  distinguishes "slow load" (waited then appeared) from "genuinely stuck / wrong
  screen" (waited the full timeout, screen never changed).
- **Grep the `screen-hierarchy/*.json`** for the expected text/testTag to
  confirm whether the element was truly absent vs. present-but-unmatched.

### 5. Diagnose, then decide flaky vs. real

- Cross-check the PR: if it's an unrelated dependency bump (e.g. a Renovate PR)
  and the failure is in an unrelated flow, it is almost certainly flaky/env, not
  a regression — but still confirm with the screenshots.
- Common real causes seen here: sign-in via the Firebase Auth emulator bounces
  back to the Auth screen (needs a retry, not a longer timeout); a system
  permission dialog covered the target; an iOS tap didn't register; an ad/SDK
  state that never surfaces matchable text.

### 6. Fixing

Prefer robustness over just bumping timeouts (a longer timeout does nothing when
the app is on the wrong screen):

- Flaky taps / bounce-backs: wrap the gesture in a Maestro `retry` block, or
  guard with `when: visible/notVisible`.
- Untestable transient UI (loaders, ad states with no text): expose a stable
  identifier from app code via `semantics { contentDescription = ... }` gated
  behind `UiTestFlags.enabled` (the established pattern in this repo, e.g.
  `SnackbarHost`, `BannerAd`), and assert on that instead of volatile text.
- Match Maestro elements by `text:` — it matches accessibility
  `contentDescription`/identifiers, not just visible labels.

### 7. Clean up

Remove the downloaded artifacts when done: `rm -rf /tmp/uitest`.

## Reference

- Flow conventions and existing flakiness workarounds are documented in
  `ui-tests/TEST_COVERAGE.md`.
- Shared subflows live in `ui-tests/common/` (`setup-login.yml`,
  `dismiss-system-dialogs.yml`, `seed-premium.yml`, `assert-on-record.yml`).
- Workflows: `.github/workflows/ios-ui-tests.yml`,
  `.github/workflows/android-ui-tests.yml`.
- `UiTestFlags` lives in `core/common` and gates test-only affordances in app
  code.
