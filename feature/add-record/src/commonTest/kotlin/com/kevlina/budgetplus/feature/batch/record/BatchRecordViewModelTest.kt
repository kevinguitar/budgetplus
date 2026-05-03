package com.kevlina.budgetplus.feature.batch.record

import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.BatchFrequency
import com.kevlina.budgetplus.core.data.BatchUnit
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeRecordRepo
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.feature.category.pills.CategoriesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class BatchRecordViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `type is initially Expense`() {
        val model = createModel()
        assertEquals(RecordType.Expense, model.type.value)
    }

    @Test
    fun `setType updates type`() {
        val model = createModel()
        model.setType(RecordType.Income)
        assertEquals(RecordType.Income, model.type.value)
    }

    @Test
    fun `frequency is initially 1 Month`() {
        val model = createModel()
        assertEquals(BatchFrequency(duration = 1, unit = BatchUnit.Month), model.frequency.value)
    }

    @Test
    fun `setDuration updates frequency duration`() {
        val model = createModel()
        model.setDuration(3)
        assertEquals(3, model.frequency.value.duration)
        assertEquals(BatchUnit.Month, model.frequency.value.unit)
    }

    @Test
    fun `setUnit updates frequency unit`() {
        val model = createModel()
        model.setUnit(BatchUnit.Week)
        assertEquals(BatchUnit.Week, model.frequency.value.unit)
        assertEquals(1, model.frequency.value.duration)
    }

    @Test
    fun `times is initially the minimum batch times`() {
        val model = createModel()
        assertEquals(2, model.times.value)
    }

    @Test
    fun `setTimes updates times`() {
        val model = createModel()
        model.setTimes(10)
        assertEquals(10, model.times.value)
    }

    @Test
    fun `batchTimes range is 2 to 30`() {
        val model = createModel()
        assertEquals(2, model.batchTimes.first())
        assertEquals(30, model.batchTimes.last())
    }

    @Test
    fun `isBatchButtonEnabled is false when category is null`() {
        val model = createModel()
        // Category is null by default
        assertFalse(model.isBatchButtonEnabled.value)
    }

    private fun createModel(): BatchRecordViewModel {
        val bookRepo = FakeBookRepo()
        return BatchRecordViewModel(
            categoriesVm = CategoriesViewModel(bookRepo = bookRepo),
            bookRepo = bookRepo,
            navController = NavController(startRoot = BookDest.Record),
            recordRepo = FakeRecordRepo,
            authManager = FakeAuthManager(user = User(id = "user")),
            snackbarSender = FakeSnackbarSender,
        )
    }
}
