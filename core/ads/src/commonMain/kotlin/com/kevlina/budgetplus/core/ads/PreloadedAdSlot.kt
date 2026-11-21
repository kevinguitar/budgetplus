package com.kevlina.budgetplus.core.ads

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Holds at most one preloaded ad, so the ad can be shown immediately at the chosen moment
 * instead of popping up seconds later while the user already moved on to something else.
 *
 * Not thread safe, it's meant to be used from the main thread, where the Mobile Ads SDK
 * invokes its callbacks.
 *
 * @param load starts loading an ad, then calls either onLoaded or onFailed.
 */
class PreloadedAdSlot<T : Any>(
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val maxAge: Duration = MAX_AGE,
    private val load: (onLoaded: (T) -> Unit, onFailed: () -> Unit) -> Unit,
) {

    private var ad: T? = null
    private var loadedAt: TimeMark? = null
    private var isLoading = false

    /** Starts loading an ad, unless one is already loading or loaded. */
    fun preload() {
        if (isLoading || ad != null) return
        isLoading = true
        load(
            { loadedAd ->
                isLoading = false
                ad = loadedAd
                loadedAt = timeSource.markNow()
            },
            {
                isLoading = false
            }
        )
    }

    /**
     * Takes the preloaded ad out of the slot, or returns null when it isn't ready or has expired.
     * Call [preload] afterward to get the next one ready.
     */
    fun take(): T? {
        val currentAd = ad ?: return null
        val isExpired = loadedAt?.elapsedNow()?.let { it > maxAge } ?: true
        ad = null
        loadedAt = null
        return currentAd.takeUnless { isExpired }
    }

    private companion object {
        /** AdMob ads expire an hour after loading, keep some margin. */
        val MAX_AGE = 55.minutes
    }
}
