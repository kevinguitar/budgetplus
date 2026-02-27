package com.kevlina.budgetplus.book

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.book_join_success
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.ads.AdUnitId
import com.kevlina.budgetplus.core.ads.InterstitialAdsHandler
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.di.ViewModelKey
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import com.kevlina.budgetplus.core.common.mapState
import com.kevlina.budgetplus.core.common.nav.APP_DEEPLINK
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NAV_COLORS_PATH
import com.kevlina.budgetplus.core.common.nav.NAV_JOIN_PATH
import com.kevlina.budgetplus.core.common.nav.NAV_OVERVIEW_PATH
import com.kevlina.budgetplus.core.common.nav.NAV_RECORD_PATH
import com.kevlina.budgetplus.core.common.nav.NAV_SETTINGS_PATH
import com.kevlina.budgetplus.core.common.nav.NAV_UNLOCK_PREMIUM_PATH
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.common.nav.NavigationAction
import com.kevlina.budgetplus.core.common.nav.NavigationFlow
import com.kevlina.budgetplus.core.common.sendEvent
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.JoinBookException
import com.kevlina.budgetplus.core.theme.ThemeManager
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Named
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@ViewModelKey(BookViewModel::class)
@ContributesIntoMap(ViewModelScope::class)
class BookViewModel(
    val navController: NavController<BookDest>,
    val snackbarSender: SnackbarSender,
    val themeManager: ThemeManager,
    val navigation: NavigationFlow,
    val bubbleViewModel: BubbleViewModel,
    val adUnitId: AdUnitId,
    val interstitialAdsHandler: InterstitialAdsHandler,
    private val bookRepo: BookRepo,
    @Named("welcome") private val welcomeNavigationAction: NavigationAction,
    authManager: AuthManager,
) : ViewModel() {

    private val currentNavKeyFlow = snapshotFlow { navController.backStack.lastOrNull() }.filterNotNull()

    private val hideBottomNavDestinations = setOf(BookDest.Auth, BookDest.Welcome)
    private val hideAdsDestinations = hideBottomNavDestinations + BookDest.UnlockPremium

    val showBottomNav = currentNavKeyFlow
        .map { it !in hideBottomNavDestinations }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val showBannerAd = combine(
        authManager.isPremium,
        currentNavKeyFlow
    ) { isPremium, currentNavKey ->
        !isPremium && currentNavKey !in hideAdsDestinations
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val isEligibleForInterstitialAds = authManager.isPremium.mapState { !it }

    init {
        // If the user has no active book, navigate them to the welcome screen to create or join a book.
        combine(authManager.userState, bookRepo.booksState) { user, books ->
            if (user?.id != null && books?.isEmpty() == true) {
                navigation.sendEvent(welcomeNavigationAction)
            }
        }.launchIn(viewModelScope)

        currentNavKeyFlow
            .onEach { key ->
                // Clear the preview colors if the user navigates out of the picker screen.
                if (key != BookDest.Colors) {
                    themeManager.clearPreviewColors()
                }
            }
            .launchIn(viewModelScope)
    }

    fun handleDeeplink(url: String?): DeeplinkType? {
        if (url == null || !url.startsWith(APP_DEEPLINK)) return null
        Logger.i { "Handle Deeplink: $url" }

        val pathAndQuery = url.removePrefix(APP_DEEPLINK).removePrefix("/")
        val path = pathAndQuery.substringBefore("?")
        val query = pathAndQuery.substringAfter("?", "")

        val segments = path.split("/").filter { it.isNotEmpty() }
        val queries = query.split("&")
            .filter { it.contains("=") }
            .associate {
                val (key, value) = it.split("=")
                key to value
            }

        when (val firstSegment = segments.firstOrNull()) {
            NAV_JOIN_PATH -> {
                bookRepo.setPendingJoinRequest(segments.getOrNull(1))
                return DeeplinkType.JoinRequest
            }

            NAV_RECORD_PATH -> navController.navigate(BookDest.Record)
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

            else -> Logger.d { "Deeplink: Unknown segment $firstSegment. Url=$url" }
        }
        return DeeplinkType.Normal
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
                Logger.e(e) { "JoinInfo not found in DB" }
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }
}

enum class DeeplinkType {
    Normal,
    JoinRequest
}