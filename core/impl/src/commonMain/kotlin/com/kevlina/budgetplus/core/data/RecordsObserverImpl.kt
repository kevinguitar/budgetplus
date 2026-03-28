package com.kevlina.budgetplus.core.data

import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.common.tickerFlow
import com.kevlina.budgetplus.core.data.local.Preference
import com.kevlina.budgetplus.core.data.remote.BooksDb
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.TimePeriod
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.Direction
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.time.Duration.Companion.seconds

/**
 *  Use a singleton class to register the records listener, this will significantly
 *  reduce the db requests for us.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RecordsObserverImpl(
    @AppCoroutineScope private val appScope: CoroutineScope,
    @BooksDb private val booksDb: Lazy<CollectionReference>,
    private val preference: Preference,
    bookRepo: BookRepo,
) : RecordsObserver {

    // A ticker to refresh the records when a day has passed.
    private val ticker = tickerFlow(10.seconds)
        .map { LocalDate.now() }
        .distinctUntilChanged()
        .drop(1)
        .onEach {
            val bookId = bookRepo.bookState.value?.id ?: return@onEach

            currentRegistrationConfig = null
            observeRecords(bookId, timePeriod.first())
        }

    override val records: StateFlow<Sequence<Record>?>
        field = MutableStateFlow<Sequence<Record>?>(null)

    // Cache the time period by book id
    private val timePeriodMapKey = stringPreferencesKey("periodCache")
    private val timePeriodMapSerializer = MapSerializer(String.serializer(), TimePeriod.serializer())
    private val timePeriodMap = preference.of(key = timePeriodMapKey, serializer = timePeriodMapSerializer)

    override val timePeriod: Flow<TimePeriod> = combine(
        timePeriodMap,
        bookRepo.bookState
    ) { periodMap, book ->
        if (periodMap == null || book?.id == null) {
            TimePeriod.Month
        } else {
            periodMap[book.id] ?: TimePeriod.Month
        }
    }

    private var currentRegistrationConfig: Pair<String, TimePeriod>? = null
    private var recordsJob: Job? = null

    init {
        combine(
            bookRepo.bookState.mapNotNull { it?.id },
            timePeriod,
            ::observeRecords
        )
            .flowOn(Dispatchers.Default)
            .launchIn(appScope)

        ticker
            .flowOn(Dispatchers.Default)
            .launchIn(appScope)
    }

    override fun setTimePeriod(bookId: String, period: TimePeriod) {
        // Leverage custom equal check to verify if the custom period matches the preset period
        val normalizedPeriod = when (period) {
            TimePeriod.Today -> TimePeriod.Today
            TimePeriod.Week -> TimePeriod.Week
            TimePeriod.Month -> TimePeriod.Month
            TimePeriod.LastMonth -> TimePeriod.LastMonth
            is TimePeriod.Custom -> period
        }

        appScope.launch {
            val newMapping = timePeriodMap.first().orEmpty()
                .toMutableMap()
                .apply { put(bookId, normalizedPeriod) }
            preference.update(timePeriodMapKey, timePeriodMapSerializer, newMapping)
        }
    }

    private fun observeRecords(bookId: String, period: TimePeriod) {
        val newConfig = bookId to period
        if (currentRegistrationConfig == newConfig) {
            // Do not establish the listener again if the config is the same
            return
        }

        Logger.d("RecordsObserver: Starting observation for bookId=$bookId, period=$period")
        currentRegistrationConfig = newConfig
        recordsJob?.cancel()
        recordsJob = booksDb.value
            .document(bookId)
            .collection("records")
            .orderBy("date", Direction.DESCENDING)
            .where { "date" greaterThanOrEqualTo period.from.toEpochDays() }
            .where { "date" lessThanOrEqualTo period.until.toEpochDays() }
            .snapshots
            .catch { Logger.e(it) { "RecordsObserver: Listen failed." } }
            .onEach { snapshot ->
                records.value = snapshot.documents
                    .map { doc -> doc.data<Record>().copy(id = doc.id) }
                    .asSequence()
            }
            .launchIn(appScope)
    }
}