package com.kevlina.budgetplus.book

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kevlina.budgetplus.core.ads.AdUnitId
import com.kevlina.budgetplus.core.ads.AdmobInitializer
import com.kevlina.budgetplus.core.ads.fixtures.FakeInterstitialAdsHandler
import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.common.nav.NavigationAction
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.theme.ThemeManager
import com.kevlina.budgetplus.core.ui.bubble.FakeBubbleRepo
import com.kevlina.budgetplus.core.unit.test.MainDispatcherRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookViewModelTest {

    @get:Rule
    val rule = MainDispatcherRule()

    @Test
    fun `welcome redirect is sent only once while redirect condition stays true`() = runTest {
        val authManager = FakeAuthManager(user = User(id = "user-1"))
        val bookRepo = FakeBookRepo(book = null)
        val model = createViewModel(authManager = authManager, bookRepo = bookRepo)

        backgroundScope.launch(rule.testDispatcher) {
            model.showBottomNav.collect {}
        }

        model.navigation.test {
            awaitItem()
            val firstAction = awaitItem().consume()
            assertThat(firstAction).isNotNull()

            bookRepo.bookState.value = Book(id = "book-1", name = "")
            advanceUntilIdle()

            expectNoEvents()

            firstAction?.navigate()
            assertThat(model.navController.backStack.last()).isEqualTo(BookDest.Welcome)
        }
    }

    private fun TestScope.createViewModel(
        authManager: FakeAuthManager = FakeAuthManager(),
        bookRepo: FakeBookRepo = FakeBookRepo(),
    ): BookViewModel {
        val navigation = MutableEventFlow<NavigationAction>()
        val navController = NavController(
            startRoot = BookDest.Record,
            serializer = BookDest.serializer(),
            savedStateHandle = SavedStateHandle(),
        )
        val themeManager = mockk<ThemeManager> {
            every { clearPreviewColors() } just runs
        }
        val admobInitializer = mockk<AdmobInitializer>()
        val bubbleViewModel = BubbleViewModel(FakeBubbleRepo())

        return BookViewModel(
            navController = navController,
            snackbarSender = FakeSnackbarSender,
            themeManager = themeManager,
            navigation = navigation,
            bubbleViewModel = bubbleViewModel,
            adUnitId = AdUnitId(banner = "banner", interstitial = "interstitial"),
            interstitialAdsHandler = FakeInterstitialAdsHandler(),
            admobInitializer = admobInitializer,
            bookRepo = bookRepo,
            welcomeNavigationAction = NavigationAction { navController.navigate(BookDest.Welcome) },
            authManager = authManager,
        )
    }
}
