package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.common.withCurrentTime
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeRecordDbClient
import com.kevlina.budgetplus.core.data.remote.Author
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.unit.test.BaseTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

class RecordRepoImplTest : BaseTest(useUnconfinedDispatcher = true) {

    @Test
    fun `createRecord should store the record in DB`() = runTest(testDispatcher) {
        createRepo().createRecord(testRecord)
        runCurrent()

        assertEquals(listOf(testRecord), recordDbClient.addedRecords)
        assertEquals("record_created", tracker.lastEventName)
    }

    @Test
    fun `batchRecord should record correct amount of records`() = runTest(testDispatcher) {
        val nTimes = 5
        val startDate = LocalDate.now()

        val batchId = createRepo().batchRecord(
            record = testRecord,
            startDate = startDate,
            frequency = BatchFrequency(duration = 3, unit = BatchUnit.Week),
            times = nTimes
        )
        runCurrent()

        assertEquals(nTimes, recordDbClient.addedRecords.size)
        repeat(nTimes) { index ->
            val multiplier = 3 * index.toLong()
            val batchDate = startDate.plus(multiplier, DateTimeUnit.WEEK)
            val addedRecord = recordDbClient.addedRecords[index]
            assertEquals(batchDate.toEpochDays(), addedRecord.date)
            assertEquals(batchId, addedRecord.batchId)
            assertEquals(testRecord.type, addedRecord.type)
            assertEquals(testRecord.category, addedRecord.category)
            assertEquals(testRecord.name, addedRecord.name)
            assertEquals(testRecord.price, addedRecord.price)
            assertEquals(testRecord.author, addedRecord.author)
        }
        assertEquals("record_batched", tracker.lastEventName)
    }

    @Test
    fun `editRecord should keep the record's original time`() = runTest {
        val localDateTime = (Clock.System.now() - 1.hours).toLocalDateTime(TimeZone.currentSystemDefault())
        val timestamp = localDateTime.toInstant(TimeZone.UTC).epochSeconds
        val oldRecord = testRecord.copy(
            id = "old_record_id",
            timestamp = timestamp
        )
        val newDate = LocalDate.now().plus(1, DateTimeUnit.YEAR)

        createRepo().editRecord(
            oldRecord = oldRecord,
            newDate = newDate,
            newCategory = "New category",
            newName = "New name",
            newPriceText = "12345.6"
        )

        assertEquals(1, recordDbClient.setRecords.size)
        val (id, record) = recordDbClient.setRecords.first()
        assertEquals("old_record_id", id)
        assertEquals(newDate.toEpochDays(), record.date)
        assertEquals(LocalDateTime(newDate, localDateTime.time).toInstant(TimeZone.UTC).epochSeconds, record.timestamp)
        assertEquals("New category", record.category)
        assertEquals("New name", record.name)
        assertEquals(12345.6, record.price)
        assertEquals("record_edited", tracker.lastEventName)
    }

    @Test
    @Ignore("Implement it when DB abstraction is done")
    fun `editBatch should edit all the batched records`() = runTest { }

    @Test
    fun `duplicateRecord should use the current user as author, and do not carry batch info`() =
        runTest(testDispatcher) {
            createRepo().duplicateRecord(testRecord)
            runCurrent()

            assertEquals(1, recordDbClient.addedRecords.size)
            val addedRecord = recordDbClient.addedRecords.first()
            assertEquals(Author(name = "My user"), addedRecord.author)
            assertEquals("record_duplicated", tracker.lastEventName)
        }

    @Test
    fun `deleteRecord should delete the record`() = runTest {
        createRepo().deleteRecord("old_record_id")

        assertEquals(listOf("old_record_id"), recordDbClient.deletedRecordIds)
        assertEquals("record_deleted", tracker.lastEventName)
    }

    @Test
    @Ignore("Implement it when DB abstraction is done")
    fun `deleteBatch should delete all the batched records`() = runTest { }


    private val testRecord = Record(
        type = RecordType.Expense,
        category = "Test category",
        name = "Dinner",
        price = 124.56,
        author = Author(name = "Test user"),
        date = LocalDate.now().toEpochDays(),
        timestamp = LocalDate.now().withCurrentTime,
    )

    private val recordDbClient = FakeRecordDbClient()
    private val authManager = FakeAuthManager(user = User(name = "My user"))
    private val tracker = FakeTracker()

    private fun TestScope.createRepo() = RecordRepoImpl(
        recordDbClient = recordDbClient,
        appScope = backgroundScope,
        authManager = authManager,
        tracker = tracker,
        snackbarSender = FakeSnackbarSender
    )
}