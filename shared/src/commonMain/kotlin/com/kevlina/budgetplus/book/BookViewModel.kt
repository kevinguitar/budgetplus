package com.kevlina.budgetplus.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.book_join_success
import budgetplus.core.common.generated.resources.record_widget_book_unavailable
import budgetplus.core.common.generated.resources.record_widget_switched_book
import com.kevlina.budgetplus.core.ads.AdUnitId
import com.kevlina.budgetplus.core.ads.AdmobInitializer
import com.kevlina.budgetplus.core.ads.InterstitialAdsHandler
import com.kevlina.budgetplus.core.common.Logger
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.UiTestFlags
import com.kevlina.budgetplus.core.common.mapState
import com.kevlina.budgetplus.core.common.nav.APP_DEEPLINK_PREFIXES
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NAV_COLORS_PATH
import com.kevlina.budgetplus.core.common.nav.NAV_JOIN_PATH
import com.kevlina.budgetplus.core.common.nav.NAV_OVERVIEW_PATH
import com.kevlina.budgetplus.core.common.nav.NAV_RECORD_PATH
import com.kevlina.budgetplus.core.common.nav.NAV_RECORD_WIDGET_PATH
import com.kevlina.budgetplus.core.common.nav.NAV_SETTINGS_PATH
import com.kevlina.budgetplus.core.common.nav.NAV_UI_TEST_PREMIUM_PATH
import com.kevlina.budgetplus.core.common.nav.NAV_UNLOCK_PREMIUM_PATH
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.JoinBookException
import com.kevlina.budgetplus.core.data.RecordPrefill
import com.kevlina.budgetplus.core.data.RecordPrefillHolder
import com.kevlina.budgetplus.core.theme.ThemeManager
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetDeeplink
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.seconds

@ViewModelKey
@ContributesIntoMap(AppScope::class)
internal class BookViewModel(
    val navController: NavController<BookDest>,
    val snackbarSender: SnackbarSender,
    val themeManager: ThemeManager,
    val bubbleViewModel: BubbleViewModel,
    val adUnitId: AdUnitId,
    val interstitialAdsHandler: InterstitialAdsHandler,
    val admobInitializer: AdmobInitializer,
    private val bookRepo: BookRepo,
    private val authManager: AuthManager,
    private val recordPrefillHolder: RecordPrefillHolder,
    private val tracker: Tracker,
) : ViewModel() {

    private val hideBottomNavDestinations =
        setOf(BookDest.Auth::class, BookDest.Welcome::class, BookDest.UnlockPremium::class)
    private val hideAdsDestinations =
        setOf(BookDest.Auth::class, BookDest.Welcome::class, BookDest.UnlockPremium::class)

    val showBottomNav = navController.currentNavKeyFlow
        .map { it::class !in hideBottomNavDestinations }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val showBannerAd = combine(
        authManager.isPremium,
        navController.currentNavKeyFlow
    ) { isPremium, currentNavKey ->
        !isPremium && currentNavKey::class !in hideAdsDestinations
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    // Interstitial ads are shown on a record-count cycle, which makes count-dependent UI
    // test flows flaky (an ad can pop over the screen mid-flow). Disable them in UI tests.
    val isEligibleForInterstitialAds = authManager.isPremium.mapState { !it && !UiTestFlags.enabled }

    init {
        // If the user has no active book, navigate them to the welcome screen to create or join a book.
        combine(
            authManager.userState.map { it?.id }.distinctUntilChanged(),
            bookRepo.booksState
        ) { userId, books ->
            if (userId != null && books?.isEmpty() == true) {
                navController.selectRootAndClearAll(BookDest.Welcome)
            }
        }.launchIn(viewModelScope)

        navController.currentNavKeyFlow
            .onEach { key ->
                // Clear the preview colors if the user navigates out of the picker screen.
                if (key !is BookDest.Colors) {
                    themeManager.clearPreviewColors()
                }

                if (key is BookDest.Welcome) {
                    handleJoinRequest()
                }
            }
            .launchIn(viewModelScope)
    }

    fun handleDeeplink(url: String?): DeeplinkType? {
        if (url == null) return null
        val prefix = APP_DEEPLINK_PREFIXES.firstOrNull { url.startsWith(it) } ?: return null
        Logger.i("Handle Deeplink: $url")

        val pathAndQuery = url.removePrefix(prefix).removePrefix("/")
        val path = pathAndQuery.substringBefore("?")
        val query = pathAndQuery.substringAfter("?", "")

        val segments = path.split("/").filter { it.isNotEmpty() }
        val queries = query.split("&")
            .filter { it.contains("=") }
            .associate {
                val key = it.substringBefore("=")
                val value = it.substringAfter("=", "")
                key to value
            }

        when (val firstSegment = segments.firstOrNull()) {
            NAV_UI_TEST_PREMIUM_PATH -> if (UiTestFlags.enabled) {
                viewModelScope.launch { authManager.markPremium(true) }
            }

            NAV_JOIN_PATH -> {
                bookRepo.setPendingJoinRequest(segments.getOrNull(1))
                return DeeplinkType.JoinRequest
            }

            NAV_RECORD_PATH -> {
                val prefill = RecordWidgetDeeplink.parse(queries)
                if (prefill == null) {
                    navController.navigate(BookDest.Record)
                } else {
                    handleRecordWidgetTap(prefill)
                }
            }

            NAV_RECORD_WIDGET_PATH -> navController.navigate(BookDest.RecordWidget)

            NAV_OVERVIEW_PATH -> navController.navigate(BookDest.Overview)
            NAV_UNLOCK_PREMIUM_PATH -> navController.navigate(BookDest.UnlockPremium)

            NAV_SETTINGS_PATH -> {
                val showMembers = queries["showMembers"]?.toBoolean() ?: false
                navController.navigate(BookDest.Settings(showMembers = showMembers))
            }

            NAV_COLORS_PATH -> {
                val hex = queries["hex"]
                navController.navigate(BookDest.Colors(hex = hex))
            }

            else -> Logger.d("Deeplink: Unknown segment $firstSegment. Url=$url")
        }
        return DeeplinkType.Normal
    }

    /**
     * The user tapped an item on the record widget. Switch to the widget's book if needed, and
     * show the record screen with the item prefilled.
     */
    private fun handleRecordWidgetTap(prefill: RecordPrefill) {
        tracker.logEvent("record_widget_item_tapped")
        viewModelScope.launch {
            // The app could be cold started from the widget, wait for the user to be restored.
            // When the user isn't signed in, the auth screen is displayed instead.
            withTimeoutOrNull(DATA_LOADING_TIMEOUT) {
                authManager.userState.filterNotNull().first()
            } ?: return@launch

            val books = withTimeoutOrNull(DATA_LOADING_TIMEOUT) {
                bookRepo.booksState.filterNotNull().first()
            }
            val book = books?.firstOrNull { it.id == prefill.bookId }
            if (book == null) {
                snackbarSender.send(Res.string.record_widget_book_unavailable)
                navController.selectRootAndPopToRoot(BookDest.Record)
                return@launch
            }

            val isBookSwitched = bookRepo.currentBookId != book.id
            if (isBookSwitched) {
                bookRepo.selectBook(book)
            }

            recordPrefillHolder.set(prefill)
            navController.selectRootAndPopToRoot(BookDest.Record)

            if (isBookSwitched) {
                snackbarSender.send(Res.string.record_widget_switched_book, formatArgs = listOf(book.name))
            }
        }
    }

    fun handleJoinRequest() {
        if (!bookRepo.hasPendingJoinRequest) return

        viewModelScope.launch {
            try {
                val bookName = bookRepo.handlePendingJoinRequest() ?: return@launch
                snackbarSender.send(getString(Res.string.book_join_success, bookName))
            } catch (e: JoinBookException.ExceedFreeLimit) {
                navController.navigate(BookDest.UnlockPremium)
                snackbarSender.send(e.message)
            } catch (e: JoinBookException.General) {
                snackbarSender.send(e.message)
            } catch (e: JoinBookException.JoinInfoNotFound) {
                Logger.e(e, "JoinInfo not found in DB")
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }
}

private val DATA_LOADING_TIMEOUT = 5.seconds

enum class DeeplinkType {
    Normal,
    JoinRequest
}