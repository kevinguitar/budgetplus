package com.kevlina.budgetplus.core.ads

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource

class PreloadedAdSlotTest {

    private val timeSource = TestTimeSource()
    private var loadCount = 0
    private var pendingLoad: Pair<(String) -> Unit, () -> Unit>? = null

    private val slot = PreloadedAdSlot<String>(timeSource = timeSource) { onLoaded, onFailed ->
        loadCount++
        pendingLoad = onLoaded to onFailed
    }

    private fun completeLoad(ad: String) = pendingLoad!!.first(ad)

    private fun failLoad() = pendingLoad!!.second()

    @Test
    fun `take returns null before the ad is loaded`() {
        slot.preload()
        assertNull(slot.take())
    }

    @Test
    fun `take returns the loaded ad only once`() {
        slot.preload()
        completeLoad("ad")

        assertEquals("ad", slot.take())
        assertNull(slot.take())
    }

    @Test
    fun `preload doesn't load again while loading or loaded`() {
        slot.preload()
        slot.preload()
        assertEquals(1, loadCount)

        completeLoad("ad")
        slot.preload()
        assertEquals(1, loadCount)
    }

    @Test
    fun `preload loads again after a failure`() {
        slot.preload()
        failLoad()
        slot.preload()

        assertEquals(2, loadCount)
    }

    @Test
    fun `an expired ad is discarded`() {
        slot.preload()
        completeLoad("ad")
        timeSource += 56.minutes

        assertNull(slot.take())
    }
}
