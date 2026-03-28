package com.kevlina.budgetplus.book.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavEntry
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.feature.add.record.ui.RecordScreen
import com.kevlina.budgetplus.feature.auth.AuthViewModel
import com.kevlina.budgetplus.feature.auth.ui.AuthBinding
import com.kevlina.budgetplus.feature.batch.record.ui.BatchRecordScreen
import com.kevlina.budgetplus.feature.color.tone.picker.ColorTonePickerScreen
import com.kevlina.budgetplus.feature.currency.picker.CurrencyPickerScreen
import com.kevlina.budgetplus.feature.edit.category.EditCategoryScreen
import com.kevlina.budgetplus.feature.overview.ui.OverviewScreen
import com.kevlina.budgetplus.feature.records.RecordsScreen
import com.kevlina.budgetplus.feature.records.RecordsViewModel
import com.kevlina.budgetplus.feature.search.SearchScreen
import com.kevlina.budgetplus.feature.search.SearchViewModel
import com.kevlina.budgetplus.feature.settings.SettingsScreen
import com.kevlina.budgetplus.feature.unlock.premium.PremiumScreen
import com.kevlina.budgetplus.feature.welcome.ui.WelcomeBinding
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel

internal fun bookNavGraph(bookDest: BookDest): NavEntry<BookDest> {
    return when (bookDest) {
        is BookDest.Auth -> NavEntry(bookDest) {
            val vm = metroViewModel<AuthViewModel>()
            LaunchedEffect(bookDest) {
                vm.checkAuthorizedAccounts(enableAutoSignIn = bookDest.enableAutoSignIn)
            }
            AuthBinding(
                vm = vm.commonAuthViewModel,
                signInWithGoogle = vm::signInWithGoogle,
                signInWithApple = vm::signInWithApple,
            )
        }

        BookDest.Welcome -> NavEntry(bookDest) {
            WelcomeBinding(vm = metroViewModel())
        }

        BookDest.Record -> NavEntry(bookDest) {
            RecordScreen()
        }

        is BookDest.EditCategory -> NavEntry(bookDest) {
            EditCategoryScreen(type = bookDest.type)
        }

        is BookDest.Settings -> NavEntry(bookDest) {
            SettingsScreen(showMembers = bookDest.showMembers)
        }

        BookDest.UnlockPremium -> NavEntry(bookDest) {
            PremiumScreen()
        }

        BookDest.BatchRecord -> NavEntry(bookDest) {
            BatchRecordScreen()
        }

        is BookDest.Colors -> NavEntry(bookDest) {
            ColorTonePickerScreen(hexFromLink = bookDest.hex)
        }

        BookDest.CurrencyPicker -> NavEntry(bookDest) {
            CurrencyPickerScreen()
        }

        BookDest.Overview -> NavEntry(bookDest) {
            OverviewScreen()
        }

        is BookDest.Records -> NavEntry(bookDest) {
            RecordsScreen(
                vm = assistedMetroViewModel<RecordsViewModel, RecordsViewModel.Factory> { create(bookDest) }
            )
        }

        is BookDest.Search -> NavEntry(bookDest) {
            SearchScreen(
                vm = assistedMetroViewModel<SearchViewModel, SearchViewModel.Factory> { create(bookDest) }
            )
        }
    }
}