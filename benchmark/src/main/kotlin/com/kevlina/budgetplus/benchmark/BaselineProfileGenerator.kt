package com.kevlina.budgetplus.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() {
        // The app under test is the `nonMinifiedRelease` variant, which routes Firebase to the
        // local emulators and enables anonymous sign-in (is_ui_test=true). The first iteration
        // signs in and creates a book; subsequent iterations start already authenticated and land
        // directly on the Record screen.
        var needSignIn = true
        baselineProfileRule.collect(
            packageName = APP_PACKAGE,
            maxIterations = 10,
            includeInStartupProfile = true,
            profileBlock = {
                pressHome()
                startActivityAndWait()

                if (needSignIn) {
                    signInAndCreateBook()
                    dismissOnboardingBubbles()
                    needSignIn = false
                }
            }
        )
    }
}
