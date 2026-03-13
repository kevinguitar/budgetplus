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
import com.kevlina.budgetplus.core.data.remote.RecordsDb
import com.kevlina.budgetplus.core.data.remote.createdOn
import com.kevlina.budgetplus.core.data.remote.isBatched
import com.kevlina.budgetplus.core.data.remote.toAuthor
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.QuerySnapshot
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Provider
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
class RecordRepoImpl(
    @RecordsDb private val recordsDb: Provider<CollectionReference>,
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val authManager: AuthManager,
    private val tracker: Tracker,
    private val snackbarSender: SnackbarSender,
) : RecordRepo {

    override fun createRecord(record: Record) {
        appScope.launch {
            try {
                recordsDb().add(record)
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
        recordsDb().document(oldRecord.id).set(newRecord)
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

        val records = getAllTheFutureRecords(oldRecord)
        records.documents.forEach { doc ->
            val record = doc.data<Record>().copy(id = doc.id)
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
        return records.documents.size
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
                recordsDb().add(duplicatedRecord)
                snackbarSender.send(Res.string.record_duplicated)
                tracker.logEvent("record_duplicated")
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    override suspend fun deleteRecord(recordId: String) {
        recordsDb().document(recordId).delete()
        tracker.logEvent("record_deleted")
    }

    override suspend fun deleteBatch(record: Record): Int {
        // If record isn't batched for some reason, simply delete it.
        if (!record.isBatched) {
            deleteRecord(record.id)
            return 1
        }

        val records = getAllTheFutureRecords(record)
        records.documents.forEach { snapshot ->
            deleteRecord(snapshot.id)
        }

        tracker.logEvent("record_batch_deleted")
        return records.documents.size
    }

    private suspend fun getAllTheFutureRecords(record: Record): QuerySnapshot {
        return recordsDb()
            .where { "batchId" equalTo record.batchId }
            .where { "date" greaterThanOrEqualTo record.date }
            .get()
    }

    override fun renameCategories(
        events: List<CategoryRenameEvent>,
    ) {
        appScope.launch(Dispatchers.IO) {
            val db = recordsDb()
            var dbUpdateCount = 0

            events.forEach { event ->
                try {
                    val records = db
                        .where { "category" equalTo event.from }
                        .get()

                    records.documents.forEach { doc ->
                        val newRecord = doc.data<Record>().copy(category = event.to)
                        db.document(doc.id).set(newRecord)
                    }
                    dbUpdateCount += records.documents.size
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