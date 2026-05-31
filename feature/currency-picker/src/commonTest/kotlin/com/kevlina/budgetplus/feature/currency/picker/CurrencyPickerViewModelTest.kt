package com.kevlina.budgetplus.feature.currency.picker

import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.kevlina.budgetplus.core.common.Currency
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.BookDest.CurrencyPicker.Purpose
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeCurrencyExchangeRepo
import com.kevlina.budgetplus.core.data.fixtures.FakePreference
import com.kevlina.budgetplus.core.data.remote.Book
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CurrencyPickerViewModelTest {

    private val pinnedCurrenciesKey = stringSetPreferencesKey("pinnedCurrencies")

    @Test
    fun `WHEN purpose is Book THEN title differs from Preferred`() {
        val bookVm = createModel(purpose = Purpose.Book)
        val preferredVm = createModel(purpose = Purpose.Preferred)
        assertNotEquals(bookVm.title, preferredVm.title)
    }

    @Test
    fun `WHEN currency is picked for Book and update fails THEN error is sent`() = runTest {
        val bookRepo = FakeBookRepo(book = Book(name = "Test Book", currencyCode = "USD"))
        val vm = createModel(purpose = Purpose.Book, bookRepo = bookRepo)
        val currency = Currency(name = "Japanese Yen", currencyCode = "JPY", symbol = "¥")

        vm.onCurrencyPicked(currency)

        assertNotNull(FakeSnackbarSender.lastSentError)
    }

    @Test
    fun `WHEN currency is picked for Preferred THEN preferred currency is updated`() = runTest {
        val currencyExchangeRepo = FakeCurrencyExchangeRepo(preferredCurrencyCode = "USD")
        val vm = createModel(purpose = Purpose.Preferred, currencyExchangeRepo = currencyExchangeRepo)
        val currency = Currency(name = "Euro", currencyCode = "EUR", symbol = "€")

        try {
            vm.onCurrencyPicked(currency)
        } catch (_: Exception) {
            // getString may throw in unit test environment
        }

        assertEquals("EUR", currencyExchangeRepo.preferredCurrencyCode)
    }

    @Test
    fun `WHEN currency is pinned THEN it is added to pinned set`() = runTest {
        val preference = FakePreference()
        val vm = createModel(preference = preference)
        val currency = Currency(name = "Euro", currencyCode = "EUR", symbol = "€")

        vm.onCurrencyPinned(currency)

        val pinned = preference.of(pinnedCurrenciesKey).first()
        assertTrue("EUR" in pinned.orEmpty())
    }

    @Test
    fun `WHEN pinned currency is unpinned THEN it is removed from pinned set`() = runTest {
        val preference = FakePreference()
        val vm = createModel(preference = preference)
        val currency = Currency(name = "Euro", currencyCode = "EUR", symbol = "€")

        vm.onCurrencyPinned(currency)
        vm.onCurrencyPinned(currency)

        val pinned = preference.of(pinnedCurrenciesKey).first()
        assertFalse("EUR" in pinned.orEmpty())
    }

    @Test
    fun `WHEN navigateUp is called THEN nav controller pops back`() {
        val navController = NavController<BookDest>(startRoot = BookDest.Record)
        navController.navigate(BookDest.CurrencyPicker(Purpose.Book))
        val vm = createModel(purpose = Purpose.Book, navController = navController)

        vm.navigateUp()

        assertEquals(BookDest.Record, navController.backStack.last())
    }

    private fun createModel(
        purpose: Purpose = Purpose.Book,
        bookRepo: FakeBookRepo = FakeBookRepo(book = Book(name = "Test", currencyCode = "USD")),
        currencyExchangeRepo: FakeCurrencyExchangeRepo = FakeCurrencyExchangeRepo(),
        preference: FakePreference = FakePreference(),
        navController: NavController<BookDest> = NavController(startRoot = BookDest.Record),
    ): CurrencyPickerViewModel {
        return CurrencyPickerViewModel(
            params = BookDest.CurrencyPicker(purpose),
            navController = navController,
            bookRepo = bookRepo,
            currencyExchangeRepo = currencyExchangeRepo,
            preference = preference,
            snackbarSender = FakeSnackbarSender,
        )
    }
}
