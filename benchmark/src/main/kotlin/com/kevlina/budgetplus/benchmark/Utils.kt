package com.kevlina.budgetplus.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

internal const val APP_PACKAGE = "com.kevlina.budgetplus"
private const val UI_TIMEOUT = 10_000L
private const val AUTH_TIMEOUT = 60_000L

/**
 * Drives the app through the critical startup journey used to capture the Baseline Profile:
 * anonymous sign-in -> create the first book -> land on the Record screen -> dismiss the
 * onboarding bubbles.
 *
 * The `nonMinifiedRelease` variant this runs against overrides `is_ui_test=true`, so the app
 * routes Firebase to the local emulators and swaps Google/Apple Sign-In for anonymous sign-in
 * (see BookNavGraph + UiTestEnvironment). That lets profile generation authenticate on CI
 * without Google Sign-In.
 */
internal fun MacrobenchmarkScope.signInAndCreateBook() {
    // Auth screen: with is_ui_test the "Continue with Google" button triggers anonymous sign-in.
    if (waitForText("Continue with Google", AUTH_TIMEOUT)) {
        clickText("Continue with Google")
    }

    // Welcome screen: create the first book so we reach the Record screen.
    if (waitForText("Book Name", AUTH_TIMEOUT)) {
        val bookNameField = device.findObject(By.text("My accounting book"))
        if (bookNameField != null) {
            bookNameField.click()
            bookNameField.text = "Benchmark Book"
        }
        clickText("Go")
    }

    // Record screen: wait for the numeric keypad / expense entry to render.
    waitForText("Expense", AUTH_TIMEOUT)
}

/**
 * Dismisses the onboarding coachmark bubbles that appear on the Record screen the first time.
 * Best-effort: each bubble is only tapped when present.
 */
internal fun MacrobenchmarkScope.dismissOnboardingBubbles() {
    clickTextIfPresent("Invite fellows to track expenses together!")
    clickTextIfPresent("Long tap to record your expenses by voice")
    clickTextIfPresent("Scroll to switch the record currency")
    waitForText("Expense")
}

private fun MacrobenchmarkScope.waitForText(text: String, timeout: Long = UI_TIMEOUT): Boolean {
    return device.wait(Until.hasObject(By.text(text)), timeout)
}

private fun MacrobenchmarkScope.clickText(text: String) {
    device.findObject(By.text(text))?.click()
}

private fun MacrobenchmarkScope.clickTextIfPresent(text: String) {
    if (waitForText(text, UI_TIMEOUT)) {
        clickText(text)
    }
}
