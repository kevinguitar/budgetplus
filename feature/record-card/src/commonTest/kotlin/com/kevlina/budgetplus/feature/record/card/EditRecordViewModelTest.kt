package com.kevlina.budgetplus.feature.record.card

import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.record_edited
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.plainPriceString
import com.kevlina.budgetplus.core.data.RecordRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeRecordRepo
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.unit.test.BaseTest
import com.kevlina.budgetplus.feature.category.pills.CategoriesViewModel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EditRecordViewModelTest : BaseTest(useUnconfinedDispatcher = true) {

    private val record = Record(
        id = "record",
        date = LocalDate(2026, 10, 25).toEpochDays(),
        category = "Food",
        name = "Lunch",
        price = 10.0,
    )

    private val recordRepo = TrackingRecordRepo()
    private val snackbarSender = FakeSnackbarSender()

    @Test
    fun `WHEN saving unchanged record THEN skip editing and snackbar`() = runTest {
        save(record)
        runCurrent()

        assertNoEdit()
    }

    @Test
    fun `WHEN saving unchanged batch THEN skip editing and snackbar`() = runTest {
        save(record.copy(batchId = "batch"), editBatch = true)
        runCurrent()

        assertNoEdit()
    }

    @Test
    fun `WHEN only price formatting changes THEN skip editing and snackbar`() = runTest {
        save(record, newPriceText = "10.00")
        runCurrent()

        assertNoEdit()
    }

    @Test
    fun `WHEN preferred price is unchanged THEN skip editing and snackbar`() = runTest {
        save(record.copy(preferredPrice = 2.0, preferredCurrencyCode = "USD"))
        runCurrent()

        assertNoEdit()
    }

    @Test
    fun `WHEN any editable field changes THEN edit record and show snackbar`() = runTest {
        val edits = listOf(
            record.copy(date = record.date + 1),
            record.copy(category = "Transport"),
            record.copy(name = "Dinner"),
            record.copy(price = 20.0),
        )

        edits.forEachIndexed { index, edited ->
            save(record, edited)
            runCurrent()

            assertEquals(index + 1, recordRepo.editCount)
            assertEquals(Res.string.record_edited, snackbarSender.lastSentMessageRes)
            assertNull(snackbarSender.lastSentError)
        }
    }

    @Test
    fun `WHEN preferred price changes to book price THEN edit record`() = runTest {
        save(
            record.copy(preferredPrice = 2.0, preferredCurrencyCode = "USD"),
            newPriceText = record.price.plainPriceString,
        )
        runCurrent()

        assertEquals(1, recordRepo.editCount)
        assertEquals(Res.string.record_edited, snackbarSender.lastSentMessageRes)
        assertNull(snackbarSender.lastSentError)
    }

    private fun save(
        original: Record,
        edited: Record = original,
        newPriceText: String = (edited.preferredPrice ?: edited.price).plainPriceString,
        editBatch: Boolean = false,
    ) {
        val bookRepo = FakeBookRepo()
        val model = EditRecordViewModel(
            categoriesVm = CategoriesViewModel(bookRepo),
            recordRepo = recordRepo,
            bookRepo = bookRepo,
            snackbarSender = snackbarSender,
        )
        model.editRecord(
            record = original,
            newDate = LocalDate.fromEpochDays(edited.date),
            newCategory = edited.category,
            newName = edited.name,
            newPriceText = newPriceText,
            editBatch = editBatch,
        )
    }

    private fun assertNoEdit() {
        assertEquals(0, recordRepo.editCount)
        assertEquals(0, recordRepo.batchEditCount)
        assertNull(snackbarSender.lastSentMessageRes)
        assertNull(snackbarSender.lastSentMessage)
        assertNull(snackbarSender.lastSentError)
    }

    private class TrackingRecordRepo : RecordRepo by FakeRecordRepo {

        var editCount = 0
        var batchEditCount = 0

        override suspend fun editRecord(
            oldRecord: Record,
            newDate: LocalDate,
            newCategory: String,
            newName: String,
            newPriceText: String,
        ) {
            editCount++
        }

        override suspend fun editBatch(
            oldRecord: Record,
            newDate: LocalDate,
            newCategory: String,
            newName: String,
            newPriceText: String,
        ): Int {
            batchEditCount++
            return 1
        }
    }
}
