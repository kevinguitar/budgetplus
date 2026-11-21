package com.kevlina.budgetplus.book

import com.kevlina.budgetplus.core.ads.InterstitialPlacement
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.nav.BookDest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InterstitialPlacementForTest {

    private val records = BookDest.Records(type = RecordType.Expense, category = "Food", authorId = null)

    @Test
    fun `switching to History from the Add tab enters History`() {
        assertEquals(
            InterstitialPlacement.EnterHistory,
            interstitialPlacementFor(from = BookDest.Record, to = BookDest.Overview)
        )
        assertEquals(
            InterstitialPlacement.EnterHistory,
            interstitialPlacementFor(from = BookDest.Settings(), to = BookDest.Overview)
        )
    }

    @Test
    fun `going back to the overview from the details leaves the detail`() {
        assertEquals(
            InterstitialPlacement.LeaveDetail,
            interstitialPlacementFor(from = records, to = BookDest.Overview)
        )
        assertEquals(
            InterstitialPlacement.LeaveDetail,
            interstitialPlacementFor(from = BookDest.Search(RecordType.Expense), to = BookDest.Overview)
        )
    }

    @Test
    fun `no ad when the destination isn't the overview`() {
        assertNull(interstitialPlacementFor(from = BookDest.Overview, to = BookDest.Record))
        assertNull(interstitialPlacementFor(from = BookDest.Overview, to = records))
        assertNull(interstitialPlacementFor(from = BookDest.Record, to = records))
    }

    @Test
    fun `no ad on the first screen or when coming back from the screens without ads`() {
        assertNull(interstitialPlacementFor(from = null, to = BookDest.Overview))
        assertNull(interstitialPlacementFor(from = BookDest.UnlockPremium, to = BookDest.Overview))
        assertNull(interstitialPlacementFor(from = BookDest.Welcome, to = BookDest.Overview))
        assertNull(interstitialPlacementFor(from = BookDest.Auth(), to = BookDest.Overview))
        assertNull(interstitialPlacementFor(from = BookDest.Overview, to = BookDest.Overview))
    }
}
