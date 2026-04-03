package com.kevlina.budgetplus.core.data

import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.common.Currency
import com.kevlina.budgetplus.core.common.formatPriceWithCurrency
import com.kevlina.budgetplus.core.common.getAvailableCurrencies
import com.kevlina.budgetplus.core.common.getDefaultCurrencyCode
import com.kevlina.budgetplus.core.data.local.Preference
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
private data class ExchangeRate(
    val rates: Map<String, Double>,
    val cachedAt: Instant,
)

/**
 * A map of currency codes to exchange rates.
 */
@Serializable
private data class ExchangeRates(
    val map: Map<String, ExchangeRate>,
)

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<CurrencyExchangeRepo>())
@ContributesIntoSet(AppScope::class, binding = binding<AppStartAction>())
class CurrencyExchangeRepoImpl(
    private val bookRepo: BookRepo,
    private val preference: Preference,
    private val json: Json,
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val httpClient: HttpClient,
) : CurrencyExchangeRepo, AppStartAction {

    private val preferredCurrencyKey = stringPreferencesKey("preferredCurrencyCode")
    private val preferredCurrency = preference.of(preferredCurrencyKey)
    private val preferredCurrencyState = preferredCurrency
        .map { it ?: getDefaultCurrencyCode() }
        .stateIn(appScope, SharingStarted.Eagerly, getDefaultCurrencyCode())

    private val cachedRatesKey = stringPreferencesKey("cachedExchangeRates")
    private val cachedRates = preference.of(
        key = cachedRatesKey,
        serializer = ExchangeRates.serializer(),
    )
    private val cachedRatesState = cachedRates
        .map { it ?: ExchangeRates(emptyMap()) }
        .stateIn(appScope, SharingStarted.Eagerly, ExchangeRates(emptyMap()))

    override val exchangeRateChange: Flow<Unit>
        field = MutableSharedFlow<Unit>(replay = 1).apply {
            onSubscription { emit(Unit) }
        }

    override val preferredCurrencySymbol: Flow<String?>
        get() = preferredCurrencyState.map { preferred ->
            getAvailableCurrencies().firstOrNull { it.currencyCode == preferred }?.symbol
        }

    override val preferredCurrencyCode: String
        get() = preferredCurrencyState.value

    override val displayInPreferredCurrency: StateFlow<Boolean>
        field = MutableStateFlow(false)

    override fun onAppStart() {
        appScope.launch { refreshRate() }
    }

    override fun updatePreferredCurrency(currency: Currency) {
        appScope.launch {
            preference.update(preferredCurrencyKey, currency.currencyCode)
            refreshRate()
        }
    }

    override fun formatPreferredCurrency(price: Double, alwaysShowSymbol: Boolean): String? {
        val bookCurrencyCode = bookRepo.bookState.value?.currencyCode?.lowercase() ?: return null
        val preferred = (preferredCurrencyState.value).lowercase()

        // No conversion needed if currencies match.
        if (bookCurrencyCode == preferred) return null

        val rate = getRateFor(preferred, bookCurrencyCode) ?: return null
        val convertedPrice = price / rate

        return formatPriceWithCurrency(convertedPrice, preferred, alwaysShowSymbol)
    }

    override fun toggleDisplayInPreferredCurrency() {
        displayInPreferredCurrency.value = !displayInPreferredCurrency.value
    }

    private suspend fun refreshRate() {
        val baseCurrency = (preferredCurrency.first())?.lowercase() ?: getDefaultCurrencyCode()
        val ratesMap = safeFetchRates(baseCurrency) ?: return

        val currentRates = cachedRates.first() ?: ExchangeRates(emptyMap())
        val updatedRates = currentRates.copy(
            map = currentRates.map + (baseCurrency to ExchangeRate(
                rates = ratesMap,
                cachedAt = Clock.System.now(),
            ))
        )

        preference.update(
            key = cachedRatesKey,
            serializer = ExchangeRates.serializer(),
            value = updatedRates,
        )
        exchangeRateChange.emit(Unit)
    }

    private suspend fun safeFetchRates(baseCurrency: String): Map<String, Double>? {
        val primaryUrl = "$CDN_BASE_URL/currencies/$baseCurrency.min.json"
        val fallbackUrl = "$FALLBACK_BASE_URL/currencies/$baseCurrency.min.json"

        return try {
            fetchRates(primaryUrl, baseCurrency)
        } catch (e: Exception) {
            Logger.w(e) { "CurrencyExchangeRepo: Primary URL failed, trying fallback" }
            try {
                fetchRates(fallbackUrl, baseCurrency)
            } catch (e2: Exception) {
                Logger.e(e2) { "CurrencyExchangeRepo: Fallback URL also failed" }
                null
            }
        }
    }

    private suspend fun fetchRates(url: String, baseCurrency: String): Map<String, Double> {
        val response = httpClient.get(url).bodyAsText()
        val jsonObject = json.parseToJsonElement(response).jsonObject
        val ratesObject = jsonObject[baseCurrency] ?: error("Rates not found for $baseCurrency")
        return json.decodeFromJsonElement<Map<String, Double>>(ratesObject)
    }

    private fun getRateFor(
        baseCurrency: String,
        targetCurrency: String,
    ): Double? {
        return cachedRatesState.value.map[baseCurrency]?.rates?.get(targetCurrency)
    }

    private companion object {
        const val CDN_BASE_URL = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1"
        const val FALLBACK_BASE_URL = "https://latest.currency-api.pages.dev/v1"
    }
}
