package com.kevlina.budgetplus.core.data

import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.kevlina.budgetplus.core.common.Currency
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.fixtures.FakePreference
import com.kevlina.budgetplus.core.data.remote.Book
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

class CurrencyExchangeRepoImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `onAppStart refreshes rates`() = runTest {
        val mockEngine = createMockEngine(
            "usd" to """{"usd": {"eur": 0.9}}"""
        )
        val repo = createRepo(mockEngine = mockEngine)

        repo.onAppStart()
        
        // Wait for potential async refresh
        repo.exchangeRateChange.first()

        assertThat(repo.preferredCurrencyCode).isEqualTo("USD")
    }

    @Test
    fun `updatePreferredCurrency updates preference and refreshes rates`() = runTest {
        val mockEngine = createMockEngine(
            "eur" to """{"eur": {"usd": 1.1}}"""
        )
        val repo = createRepo(mockEngine = mockEngine)

        repo.updatePreferredCurrency(Currency(name = "Euro", currencyCode = "EUR", symbol = "€"))

        repo.exchangeRateChange.first()

        assertThat(repo.preferredCurrencyCode).isEqualTo("EUR")
    }

    @Test
    fun `formatPreferredCurrency returns null when currencies match`() = runTest {
        val repo = createRepo(bookCurrency = "USD")
        // Preferred is USD by default (from getDefaultCurrencyCode mock or fallback)
        
        val result = repo.formatPreferredCurrency(100.0, false)

        assertThat(result).isNull()
    }

    @Test
    fun `formatPreferredCurrency converts price when rates are available`() = runTest {
        val mockEngine = createMockEngine(
            "usd" to """{"usd": {"eur": 0.5}}"""
        )
        val repo = createRepo(mockEngine = mockEngine, bookCurrency = "EUR")
        
        repo.onAppStart()
        repo.exchangeRateChange.first()

        // book is EUR, preferred is USD. rate from USD to EUR is 0.5.
        // price in EUR is 100.0. converted to USD: 100.0 / 0.5 = 200.0
        val result = repo.formatPreferredCurrency(100.0, false)

        assertThat(result).contains("200")
    }

    @Test
    fun `toggleDisplayInPreferredCurrency toggles state`() = runTest {
        val repo = createRepo()
        assertThat(repo.displayInPreferredCurrency.value).isFalse()
        
        repo.toggleDisplayInPreferredCurrency()
        assertThat(repo.displayInPreferredCurrency.value).isTrue()
        
        repo.toggleDisplayInPreferredCurrency()
        assertThat(repo.displayInPreferredCurrency.value).isFalse()
    }

    @Test
    fun `refreshRate uses fallback when primary fails`() = runTest {
        var callCount = 0
        val mockEngine = MockEngine { request ->
            callCount++
            if (request.url.host.contains("jsdelivr")) {
                respond("Error", status = HttpStatusCode.InternalServerError)
            } else {
                respond(
                    content = """{"usd": {"eur": 0.9}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json")
                )
            }
        }
        
        val repo = createRepo(mockEngine = mockEngine, bookCurrency = "EUR")
        repo.onAppStart()
        repo.exchangeRateChange.first()
        
        assertThat(callCount).isEqualTo(2)
        // Verify it actually worked by checking conversion
        val result = repo.formatPreferredCurrency(1.0, false)

        assertThat(result).isNotNull()
    }

    private fun createMockEngine(vararg rates: Pair<String, String>): MockEngine {
        return MockEngine { request ->
            val content = rates.find { (code, _) -> request.url.encodedPath.contains(code) }?.second
            if (content != null) {
                respond(
                    content = content,
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json")
                )
            } else {
                respond("Not Found", status = HttpStatusCode.NotFound)
            }
        }
    }

    private fun TestScope.createRepo(
        mockEngine: MockEngine = createMockEngine(),
        bookCurrency: String = "USD",
    ): CurrencyExchangeRepoImpl {
        val httpClient = HttpClient(mockEngine)
        val preference = FakePreference {
            set(stringPreferencesKey("preferredCurrencyCode"), "USD")
        }
        return CurrencyExchangeRepoImpl(
            bookRepo = FakeBookRepo(book = Book(currencyCode = bookCurrency)),
            preference = preference,
            json = json,
            appScope = backgroundScope,
            httpClient = httpClient
        )
    }
}
