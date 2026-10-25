package com.kevlina.budgetplus.feature.record.card

import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.record_edited
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.plainPriceString
import com.kevlina.budgetplus.core.data.BookRepo
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
import kotlin.test.assertSame

class EditRecordViewModelTest : BaseTest(useUnconfinedDispatcher = true) {

    private val record = Record(
        id = "record",
        date = LocalDate(2026, 10, 25).toEpochDays(),
        category = "Food",
        name = "Lunch",
        price = 10.0,
    )

    private val recordRepo = TrackingRecordRepo()
    private val bookRepo = TrackingBookRepo()
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
            assertEquals(EditCall(record, edited), recordRepo.lastEdit)
            assertEquals(0, recordRepo.batchEditCount)
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

    @Test
    fun `WHEN single record edit fails THEN send error without success snackbar`() = runTest {
        val error = IllegalStateException("Edit failed")
        recordRepo.failure = error

        save(record, record.copy(name = "Dinner"))
        runCurrent()

        assertEquals(1, recordRepo.editCount)
        assertEquals(0, recordRepo.batchEditCount)
        assertError(error)
    }

    @Test
    fun `WHEN batch edit fails THEN send error without success snackbar`() = runTest {
        val error = IllegalStateException("Batch edit failed")
        recordRepo.failure = error

        save(record.copy(batchId = "batch"), record.copy(name = "Dinner"), editBatch = true)
        runCurrent()

        assertEquals(0, recordRepo.editCount)
        assertEquals(1, recordRepo.batchEditCount)
        assertError(error)
    }

    @Test
    fun `WHEN record deletion fails THEN send error without success snackbar`() = runTest {
        val error = IllegalStateException("Delete failed")
        recordRepo.failure = error

        createModel().deleteRecord(record)
        runCurrent()

        assertEquals(listOf(record.id), recordRepo.deletedIds)
        assertError(error)
    }

    @Test
    fun `WHEN batch deletion fails THEN send error without success snackbar`() = runTest {
        val error = IllegalStateException("Batch delete failed")
        recordRepo.failure = error
        val batched = record.copy(batchId = "batch")

        createModel().deleteRecord(batched, deleteBatch = true)
        runCurrent()

        assertEquals(listOf(batched), recordRepo.deletedBatches)
        assertError(error)
    }

    @Test
    fun `WHEN adding category THEN forward type name and edit source`() = runTest {
        val model = createModel()

        RecordType.entries.forEach { type ->
            model.addCategory(type, "New category")
            runCurrent()

            assertEquals(CategoryCall(type, "New category", "edit"), bookRepo.lastCategory)
        }
        assertNoEdit()
    }

    @Test
    fun `WHEN adding category fails THEN send error without success snackbar`() = runTest {
        val error = IllegalStateException("Category failed")
        bookRepo.failure = error

        createModel().addCategory(RecordType.Expense, "Food")
        runCurrent()

        assertEquals(CategoryCall(RecordType.Expense, "Food", "edit"), bookRepo.lastCategory)
        assertError(error)
    }

    @Test
    fun `canAddCategory reflects book edit permission`() {
        listOf(true, false).forEach { canEdit ->
            val model = createModel(FakeBookRepo(canEdit = canEdit))

            assertEquals(canEdit, model.canAddCategory)
        }
    }

    @Test
    fun `currencySymbol reflects book currency changes`() {
        val repo = FakeBookRepo(currentCurrencySymbol = "$")
        val model = createModel(repo)

        assertSame(repo.currencySymbol, model.currencySymbol)
        assertEquals("$", model.currencySymbol.value)

        repo.currencySymbol.value = "€"

        assertEquals("€", model.currencySymbol.value)
    }

    private fun save(
        original: Record,
        edited: Record = original,
        newPriceText: String = (edited.preferredPrice ?: edited.price).plainPriceString,
        editBatch: Boolean = false,
    ) {
        createModel().editRecord(
            record = original,
            newDate = LocalDate.fromEpochDays(edited.date),
            newCategory = edited.category,
            newName = edited.name,
            newPriceText = newPriceText,
            editBatch = editBatch,
        )
    }

    private fun createModel(bookRepo: BookRepo = this.bookRepo) = EditRecordViewModel(
        categoriesVm = CategoriesViewModel(bookRepo),
        recordRepo = recordRepo,
        bookRepo = bookRepo,
        snackbarSender = snackbarSender,
    )

    private fun assertError(error: Exception) {
        assertSame(error, snackbarSender.lastSentError)
        assertNull(snackbarSender.lastSentMessageRes)
        assertNull(snackbarSender.lastSentMessage)
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
        var lastEdit: EditCall? = null
        var failure: Exception? = null
        val deletedIds = mutableListOf<String>()
        val deletedBatches = mutableListOf<Record>()

        override suspend fun editRecord(
            oldRecord: Record,
            newDate: LocalDate,
            newCategory: String,
            newName: String,
            newPriceText: String,
        ) {
            editCount++
            lastEdit = EditCall(oldRecord, newDate, newCategory, newName, newPriceText)
            failure?.let { throw it }
        }

        override suspend fun editBatch(
            oldRecord: Record,
            newDate: LocalDate,
            newCategory: String,
            newName: String,
            newPriceText: String,
        ): Int {
            batchEditCount++
            lastEdit = EditCall(oldRecord, newDate, newCategory, newName, newPriceText)
            failure?.let { throw it }
            return 1
        }

        override suspend fun deleteRecord(recordId: String) {
            deletedIds += recordId
            failure?.let { throw it }
        }

        override suspend fun deleteBatch(record: Record): Int {
            deletedBatches += record
            failure?.let { throw it }
            return 1
        }
    }

    private data class EditCall(
        val original: Record,
        val date: LocalDate,
        val category: String,
        val name: String,
        val priceText: String,
    ) {
        constructor(original: Record, edited: Record) : this(
            original = original,
            date = LocalDate.fromEpochDays(edited.date),
            category = edited.category,
            name = edited.name,
            priceText = (edited.preferredPrice ?: edited.price).plainPriceString,
        )
    }

    private data class CategoryCall(val type: RecordType, val category: String, val source: String)

    private class TrackingBookRepo : BookRepo by FakeBookRepo() {

        var lastCategory: CategoryCall? = null
        var failure: Exception? = null

        override suspend fun addCategory(type: RecordType, category: String, source: String) {
            lastCategory = CategoryCall(type, category, source)
            failure?.let { throw it }
        }
    }
}
