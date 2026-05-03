package com.kevlina.budgetplus.core.unit.test

import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class BaseTest(
    val useUnconfinedDispatcher: Boolean = false,
    val observeComposeSnapshots: Boolean = false,
) {

    val testDispatcher by lazy {
        UnconfinedTestDispatcher()
    }

    private var observerHandle: ObserverHandle? = null

    @BeforeTest
    fun setUp() {
        if (useUnconfinedDispatcher) {
            Dispatchers.setMain(testDispatcher)
        }
        if (observeComposeSnapshots && observerHandle == null) {
            observerHandle = Snapshot.registerGlobalWriteObserver {
                Snapshot.sendApplyNotifications()
            }
        }
    }

    @AfterTest
    fun tearDown() {
        if (useUnconfinedDispatcher) {
            Dispatchers.resetMain()
        }
        if (observeComposeSnapshots) {
            observerHandle?.dispose()
            observerHandle = null
        }
    }
}