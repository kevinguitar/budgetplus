package com.kevlina.budgetplus.book

import com.kevlina.budgetplus.core.ads.AdUnitId
import com.kevlina.budgetplus.core.ads.AdmobInitializer
import com.kevlina.budgetplus.core.ads.fixtures.FakeInterstitialAdsHandler
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.BottomNavTab
import com.kevlina.budgetplus.core.common.nav.NavController
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.record_widget_book_unavailable
import budgetplus.core.common.generated.resources.record_widget_switched_book
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.RecordPrefill
import com.kevlina.budgetplus.core.data.RecordPrefillHolder
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.fixtures.FakePreference
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.theme.ThemeManager
import com.kevlina.budgetplus.core.ui.bubble.FakeBubbleRepo
import com.kevlina.budgetplus.core.unit.test.BaseTest
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetDeeplink
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetItem
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BookViewModelTest : BaseTest(useUnconfinedDispatcher = true) {

    // region Universal links (https://budgetplus.cchi.tw/...)

    @Test
    fun `record universal link navigates to Record`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val type = model.handleDeeplink("https://budgetplus.cchi.tw/record")

        assertEquals(DeeplinkType.Normal, type)
        assertEquals(BookDest.Record, nav.backStack.last())
    }

    @Test
    fun `overview universal link navigates to Overview`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val type = model.handleDeeplink("https://budgetplus.cchi.tw/overview")

        assertEquals(DeeplinkType.Normal, type)
        assertEquals(BookDest.Overview, nav.backStack.last())
    }

    @Test
    fun `unlockPremium universal link navigates to UnlockPremium`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val type = model.handleDeeplink("https://budgetplus.cchi.tw/unlockPremium")

        assertEquals(DeeplinkType.Normal, type)
        assertEquals(BookDest.UnlockPremium, nav.backStack.last())
    }

    @Test
    fun `settings universal link navigates to Settings without members`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val type = model.handleDeeplink("https://budgetplus.cchi.tw/settings")

        assertEquals(DeeplinkType.Normal, type)
        assertEquals(BookDest.Settings(showMembers = false), nav.backStack.last())
    }

    @Test
    fun `settings universal link with showMembers query navigates to Settings with members`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val type = model.handleDeeplink("https://budgetplus.cchi.tw/settings?showMembers=true")

        assertEquals(DeeplinkType.Normal, type)
        assertEquals(BookDest.Settings(showMembers = true), nav.backStack.last())
    }

    @Test
    fun `colors universal link with hex query navigates to Colors with decoded hex`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val hex = "cff1ff%3bdaf2cb%3b84c18f%3b596980"
        val type = model.handleDeeplink("https://budgetplus.cchi.tw/colors?hex=$hex")

        assertEquals(DeeplinkType.Normal, type)
        assertEquals(BookDest.Colors(hex = hex), nav.backStack.last())
    }

    @Test
    fun `join universal link sets pending join request and returns JoinRequest`() {
        val bookRepo = RecordingBookRepo()
        val model = createModel(bookRepo = bookRepo)

        val type = model.handleDeeplink("https://budgetplus.cchi.tw/join/join-id")

        assertEquals(DeeplinkType.JoinRequest, type)
        assertEquals("join-id", bookRepo.pendingJoinRequest)
    }

    // endregion

    // region Custom scheme (budgetplus://...)

    @Test
    fun `record custom scheme navigates to Record`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val type = model.handleDeeplink("budgetplus://record")

        assertEquals(DeeplinkType.Normal, type)
        assertEquals(BookDest.Record, nav.backStack.last())
    }

    @Test
    fun `overview custom scheme navigates to Overview`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val type = model.handleDeeplink("budgetplus://overview")

        assertEquals(DeeplinkType.Normal, type)
        assertEquals(BookDest.Overview, nav.backStack.last())
    }

    @Test
    fun `unlockPremium custom scheme navigates to UnlockPremium`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val type = model.handleDeeplink("budgetplus://unlockPremium")

        assertEquals(DeeplinkType.Normal, type)
        assertEquals(BookDest.UnlockPremium, nav.backStack.last())
    }

    @Test
    fun `settings custom scheme with showMembers query navigates to Settings with members`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val type = model.handleDeeplink("budgetplus://settings?showMembers=true")

        assertEquals(DeeplinkType.Normal, type)
        assertEquals(BookDest.Settings(showMembers = true), nav.backStack.last())
    }

    @Test
    fun `colors custom scheme with hex query navigates to Colors with decoded hex`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val hex = "cff1ff%3bdaf2cb%3b84c18f%3b596980"
        val type = model.handleDeeplink("budgetplus://colors?hex=$hex")

        assertEquals(DeeplinkType.Normal, type)
        assertEquals(BookDest.Colors(hex = hex), nav.backStack.last())
    }

    @Test
    fun `join custom scheme sets pending join request and returns JoinRequest`() {
        val bookRepo = RecordingBookRepo()
        val model = createModel(bookRepo = bookRepo)

        val type = model.handleDeeplink("budgetplus://join/join-id")

        assertEquals(DeeplinkType.JoinRequest, type)
        assertEquals("join-id", bookRepo.pendingJoinRequest)
    }

    // endregion

    // region Edge cases

    @Test
    fun `null url returns null and does not navigate`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val type = model.handleDeeplink(null)

        assertNull(type)
        assertEquals(BookDest.Record, nav.backStack.last())
    }

    @Test
    fun `unrecognized prefix returns null and does not navigate`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val type = model.handleDeeplink("https://example.com/record")

        assertNull(type)
        assertEquals(BookDest.Record, nav.backStack.last())
    }

    @Test
    fun `unknown segment returns Normal without navigating`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val type = model.handleDeeplink("https://budgetplus.cchi.tw/unknown")

        assertEquals(DeeplinkType.Normal, type)
        assertEquals(BookDest.Record, nav.backStack.last())
    }

    @Test
    fun `join without id sets null pending join request and returns JoinRequest`() {
        val bookRepo = RecordingBookRepo()
        val model = createModel(bookRepo = bookRepo)

        val type = model.handleDeeplink("https://budgetplus.cchi.tw/join")

        assertEquals(DeeplinkType.JoinRequest, type)
        assertNull(bookRepo.pendingJoinRequest)
    }

    @Test
    fun `settings with non-boolean showMembers defaults to false`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        val type = model.handleDeeplink("https://budgetplus.cchi.tw/settings?showMembers=notabool")

        assertEquals(DeeplinkType.Normal, type)
        assertEquals(BookDest.Settings(showMembers = false), nav.backStack.last())
    }

    // endregion

    // region Record widget

    @Test
    fun `widget link switches to the widget's book and prefills the record screen`() {
        val nav = createNavController().apply { navigate(BookDest.Settings()) }
        val bookRepo = WidgetBookRepo(currentBook = bookA, books = listOf(bookA, bookB))
        val snackbarSender = FakeSnackbarSender()
        val prefillHolder = RecordPrefillHolder()
        val model = createModel(
            navController = nav,
            bookRepo = bookRepo,
            snackbarSender = snackbarSender,
            recordPrefillHolder = prefillHolder,
        )

        val type = model.handleDeeplink(RecordWidgetDeeplink.build(bookB.id, dinner))

        assertEquals(DeeplinkType.Normal, type)
        assertEquals(1, bookRepo.selectBookCount)
        assertEquals(bookB, bookRepo.bookState.value)
        assertEquals(Res.string.record_widget_switched_book, snackbarSender.lastSentMessageRes)
        assertEquals(listOf<Any>(bookB.name), snackbarSender.lastSentFormatArgs)
        assertEquals(
            RecordPrefill(bookId = bookB.id, type = RecordType.Expense, category = "Food", name = "Dinner & Drinks"),
            prefillHolder.prefill.value
        )
        assertEquals(listOf<BookDest>(BookDest.Record), nav.backStack.toList())
    }

    @Test
    fun `widget link doesn't switch book when it's already the current book`() {
        val bookRepo = WidgetBookRepo(currentBook = bookB, books = listOf(bookA, bookB))
        val snackbarSender = FakeSnackbarSender()
        val prefillHolder = RecordPrefillHolder()
        val model = createModel(
            bookRepo = bookRepo,
            snackbarSender = snackbarSender,
            recordPrefillHolder = prefillHolder,
        )

        model.handleDeeplink(RecordWidgetDeeplink.build(bookB.id, dinner))

        assertEquals(0, bookRepo.selectBookCount)
        assertNull(snackbarSender.lastSentMessageRes)
        assertNotNull(prefillHolder.prefill.value)
    }

    @Test
    fun `widget link shows an error when the book is no longer available`() {
        val nav = createNavController()
        val bookRepo = WidgetBookRepo(currentBook = bookA, books = listOf(bookA))
        val snackbarSender = FakeSnackbarSender()
        val prefillHolder = RecordPrefillHolder()
        val model = createModel(
            navController = nav,
            bookRepo = bookRepo,
            snackbarSender = snackbarSender,
            recordPrefillHolder = prefillHolder,
        )

        model.handleDeeplink(RecordWidgetDeeplink.build(bookB.id, dinner))

        assertEquals(Res.string.record_widget_book_unavailable, snackbarSender.lastSentMessageRes)
        assertEquals(bookA, bookRepo.bookState.value)
        assertNull(prefillHolder.prefill.value)
        assertEquals(BookDest.Record, nav.backStack.last())
    }

    @Test
    fun `widget link logs the tap event`() {
        val tracker = FakeTracker()
        val model = createModel(
            bookRepo = WidgetBookRepo(currentBook = bookA, books = listOf(bookA, bookB)),
            tracker = tracker,
        )

        model.handleDeeplink(RecordWidgetDeeplink.build(bookB.id, dinner))

        assertEquals("record_widget_item_tapped", tracker.lastEventName)
    }

    @Test
    fun `recordWidget link navigates to RecordWidget`() {
        val nav = createNavController()
        val model = createModel(navController = nav)

        model.handleDeeplink("budgetplus://recordWidget")

        assertEquals(BookDest.RecordWidget, nav.backStack.last())
    }

    @Test
    fun `malformed widget link falls back to the record screen without prefill`() {
        val nav = createNavController().apply { navigate(BookDest.Settings()) }
        val prefillHolder = RecordPrefillHolder()
        val model = createModel(navController = nav, recordPrefillHolder = prefillHolder)

        model.handleDeeplink("budgetplus://record?source=widget&bookId=book_b&type=Unknown&category=Food")

        assertEquals(BookDest.Record, nav.backStack.last())
        assertNull(prefillHolder.prefill.value)
    }

    // endregion

    private val bookA = Book(id = "book_a", name = "Personal")
    private val bookB = Book(id = "book_b", name = "Family Book")
    private val dinner = RecordWidgetItem(type = RecordType.Expense, category = "Food", name = "Dinner & Drinks")

    private fun createNavController(): NavController<BookDest> =
        NavController(startRoot = BottomNavTab.Add.root)

    private fun createModel(
        navController: NavController<BookDest> = createNavController(),
        bookRepo: BookRepo = RecordingBookRepo(),
        snackbarSender: FakeSnackbarSender = FakeSnackbarSender(),
        recordPrefillHolder: RecordPrefillHolder = RecordPrefillHolder(),
        tracker: FakeTracker = FakeTracker(),
    ): BookViewModel {
        val authManager = FakeAuthManager(user = User(id = "user"))
        val themeManager = ThemeManager(
            appScope = TestScope(testDispatcher),
            preference = FakePreference(),
            authManager = authManager,
            tracker = FakeTracker(),
        )
        return BookViewModel(
            navController = navController,
            snackbarSender = snackbarSender,
            themeManager = themeManager,
            bubbleViewModel = BubbleViewModel(FakeBubbleRepo()),
            adUnitId = AdUnitId(banner = "", interstitial = ""),
            interstitialAdsHandler = FakeInterstitialAdsHandler(),
            admobInitializer = FakeAdmobInitializer(),
            bookRepo = bookRepo,
            authManager = authManager,
            recordPrefillHolder = recordPrefillHolder,
            tracker = tracker,
        )
    }

    private class FakeAdmobInitializer : AdmobInitializer {
        override fun requestTrackingAuthorization() = Unit
    }

    /**
     * A [BookRepo] that records the pending join request instead of throwing,
     * so deeplink join handling can be asserted. Delegates all other behavior
     * to [FakeBookRepo].
     */
    private class RecordingBookRepo(
        private val delegate: FakeBookRepo = FakeBookRepo(),
    ) : BookRepo by delegate {

        var pendingJoinRequest: String? = null
            private set

        override fun setPendingJoinRequest(joinId: String?) {
            pendingJoinRequest = joinId
        }
    }

    /**
     * A [BookRepo] whose current book can be switched, so the widget link handling can be
     * asserted. Delegates all other behavior to [FakeBookRepo].
     */
    private class WidgetBookRepo(
        currentBook: Book,
        books: List<Book>,
        private val delegate: FakeBookRepo = FakeBookRepo(book = currentBook, books = books),
    ) : BookRepo by delegate {

        var selectBookCount = 0
            private set

        override val currentBookId: String?
            get() = delegate.bookState.value?.id

        override suspend fun selectBook(book: Book?) {
            selectBookCount++
            delegate.bookState.value = book
        }
    }
}
