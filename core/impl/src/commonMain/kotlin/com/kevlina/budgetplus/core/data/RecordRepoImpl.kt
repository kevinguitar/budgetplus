package com.kevlina.budgetplus.core.data

import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.record_duplicated
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.parseToPrice
import com.kevlina.budgetplus.core.common.randomUUID
import com.kevlina.budgetplus.core.common.withCurrentTime
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.createdOn
import com.kevlina.budgetplus.core.data.remote.isBatched
import com.kevlina.budgetplus.core.data.remote.toAuthor
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class RecordRepoImpl(
    private val recordDbClient: RecordDbClient,
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val authManager: AuthManager,
    private val tracker: Tracker,
    private val snackbarSender: SnackbarSender,
) : RecordRepo {

    override fun createRecord(record: Record) {
        appScope.launch {
            try {
                recordDbClient.add(record)
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
        tracker.logEvent("record_created")
    }

    override fun batchRecord(
        record: Record,
        startDate: LocalDate,
        frequency: BatchFrequency,
        times: Int,
    ): String {
        val batchId = randomUUID()
        var currentDate: LocalDate

        repeat(times) { index ->
            val multiplier = index * frequency.duration
            val unit = when (frequency.unit) {
                BatchUnit.Month -> DateTimeUnit.MONTH
                BatchUnit.Week -> DateTimeUnit.WEEK
                BatchUnit.Day -> DateTimeUnit.DAY
            }
            currentDate = startDate.plus(multiplier, unit)

            createRecord(record.copy(
                date = currentDate.toEpochDays(),
                timestamp = currentDate.withCurrentTime,
                batchId = batchId
            ))
        }
        tracker.logEvent("record_batched")
        return batchId
    }

    override suspend fun editRecord(
        oldRecord: Record,
        newDate: LocalDate,
        newCategory: String,
        newName: String,
        newPriceText: String,
    ) {
        // Keep the record's original time
        val originalTime = Instant.fromEpochSeconds(oldRecord.createdOn)
            .toLocalDateTime(TimeZone.UTC)
            .time

        val newRecord = oldRecord.copy(
            date = newDate.toEpochDays(),
            timestamp = LocalDateTime(newDate, originalTime).toInstant(TimeZone.UTC).epochSeconds,
            category = newCategory,
            name = newName,
            price = newPriceText.parseToPrice()
        )
        recordDbClient.set(oldRecord.id, newRecord)
        tracker.logEvent("record_edited")
    }

    override suspend fun editBatch(
        oldRecord: Record,
        newDate: LocalDate,
        newCategory: String,
        newName: String,
        newPriceText: String,
    ): Int {
        // If record isn't batched for some reason, simply edit it.
        if (!oldRecord.isBatched) {
            editRecord(
                oldRecord = oldRecord,
                newDate = newDate,
                newCategory = newCategory,
                newName = newName,
                newPriceText = newPriceText
            )
            return 1
        }

        val oldDate = LocalDate.fromEpochDays(oldRecord.date)
        val daysDiff = newDate.minus(oldDate).days

        val records = recordDbClient.queryByBatchAndDate(oldRecord.batchId, oldRecord.date)
        records.forEach { (_, record) ->
            val date = LocalDate.fromEpochDays(record.date)
            editRecord(
                oldRecord = record,
                newDate = date.plus(daysDiff, DateTimeUnit.DAY),
                newCategory = newCategory,
                newName = newName,
                newPriceText = newPriceText
            )
        }

        tracker.logEvent("record_batch_edited")
        return records.size
    }

    override fun duplicateRecord(record: Record) {
        val duplicatedRecord = record.copy(
            id = "",
            // The person who duplicates it should be the author
            author = authManager.userState.value?.toAuthor(),
            // Do not carry the batch info to duplicates
            batchId = null
        )
        appScope.launch {
            try {
                recordDbClient.add(duplicatedRecord)
                snackbarSender.send(Res.string.record_duplicated)
                tracker.logEvent("record_duplicated")
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    override suspend fun deleteRecord(recordId: String) {
        recordDbClient.delete(recordId)
        tracker.logEvent("record_deleted")
    }

    override suspend fun deleteBatch(record: Record): Int {
        // If record isn't batched for some reason, simply delete it.
        if (!record.isBatched) {
            deleteRecord(record.id)
            return 1
        }

        val records = recordDbClient.queryByBatchAndDate(record.batchId, record.date)
        records.forEach { (id, _) ->
            deleteRecord(id)
        }

        tracker.logEvent("record_batch_deleted")
        return records.size
    }

    override fun renameCategories(
        events: List<CategoryRenameEvent>,
    ) {
        appScope.launch(Dispatchers.IO) {
            var dbUpdateCount = 0

            events.forEach { event ->
                try {
                    val records = recordDbClient.queryByCategory(event.from)

                    records.forEach { (docId, record) ->
                        val newRecord = record.copy(category = event.to)
                        recordDbClient.set(docId, newRecord)
                    }
                    dbUpdateCount += records.size
                } catch (e: Exception) {
                    Logger.e(e) { "RecordRepo: renameCategories failed" }
                }
            }

            if (dbUpdateCount > 0) {
                tracker.logEvent(
                    event = "categories_renamed",
                    params = mapOf("db_update_count" to dbUpdateCount)
                )
            }
        }
    }
}