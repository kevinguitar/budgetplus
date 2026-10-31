package com.kevlina.budgetplus.feature.record.widget

import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.app_name
import budgetplus.core.common.generated.resources.record_widget_limit
import budgetplus.core.common.generated.resources.record_widget_pin_unsupported
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.unit.test.BaseTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordWidgetViewModelTest : BaseTest(useUnconfinedDispatcher = true) {

    @Test
    fun `frequent items of the current book should be loaded`() = runTest {
        val model = createModel()

        assertEquals("book", frequentRecordsClient.lastQueriedBookId)
        assertEquals(
            RecordWidgetUiState.Loaded(
                listOf(FrequentItem(dinner, count = 2), FrequentItem(lunch, count = 1), FrequentItem(salary, count = 1))
            ),
            model.uiState.value
        )
    }

    @Test
    fun `done is disabled until 1 to 5 items are selected`() = runTest {
        val model = createModel()
        val items = (1..5).map { RecordWidgetItem(RecordType.Expense, "Food", "Item $it") }

        assertFalse(model.isDoneEnabled.first())

        model.toggleItem(items[0])
        assertTrue(model.isDoneEnabled.first { it })

        items.drop(1).forEach(model::toggleItem)
        assertEquals(5, model.selectedItems.value.size)

        items.forEach(model::toggleItem)
        assertFalse(model.isDoneEnabled.first { !it })
    }

    @Test
    fun `selecting more than 5 items is blocked with a message`() = runTest {
        val model = createModel()
        val items = (1..6).map { RecordWidgetItem(RecordType.Expense, "Food", "Item $it") }

        items.forEach(model::toggleItem)

        assertEquals(items.take(5), model.selectedItems.value)
        assertEquals(Res.string.record_widget_limit, snackbarSender.lastSentMessageRes)
        assertEquals(listOf<Any>(5), snackbarSender.lastSentFormatArgs)
    }

    @Test
    fun `toggle a selected item should unselect it`() = runTest {
        val model = createModel()

        model.toggleItem(dinner)
        model.toggleItem(dinner)

        assertTrue(model.selectedItems.value.isEmpty())
    }

    @Test
    fun `config should be tied to the current book with items in selection order`() = runTest {
        val model = createModel()
        model.toggleItem(lunch)
        model.toggleItem(dinner)

        val config = model.buildConfig(id = "widget")

        assertEquals("widget", config?.id)
        assertEquals("book", config?.bookId)
        assertEquals("Personal", config?.bookName)
        assertEquals(listOf(lunch, dinner), config?.items)
    }

    @Test
    fun `config is null when nothing is selected`() = runTest {
        assertNull(createModel().buildConfig())
    }

    @Test
    fun `switching book after the screen opens doesn't change the config's book`() = runTest {
        val bookRepo = FakeBookRepo(book = book, currentBookId = book.id)
        val model = createModel(bookRepo = bookRepo)
        model.toggleItem(dinner)

        bookRepo.bookState.value = Book(id = "other", name = "Other")

        assertEquals("book", model.buildConfig()?.bookId)
    }

    @Test
    fun `save config for os widget should persist and refresh and log the event`() = runTest {
        val model = createModel()
        model.toggleItem(dinner)

        model.saveConfigForWidget(widgetId = "42")

        assertEquals("42", configStore.configs.value.single().id)
        assertEquals(1, widgetPlatform.refreshCount)
        assertEquals("record_widget_added", tracker.lastEventName)
        assertEquals(mapOf("source" to "os", "item_count" to 1), tracker.lastEvent?.second)
    }

    @Test
    fun `add widget from settings navigates up after the pin is requested`() = runTest {
        val nav = NavController<BookDest>(startRoot = BookDest.Settings()).apply {
            navigate(BookDest.RecordWidget)
        }
        val model = createModel(navController = nav)
        model.toggleItem(dinner)

        model.addWidgetFromSettings()

        assertEquals(dinner, widgetPlatform.lastAddedConfig?.items?.single())
        assertEquals(BookDest.Settings(), nav.backStack.last())
    }

    @Test
    fun `add widget from settings shows a message when pinning is unsupported`() = runTest {
        widgetPlatform.result = AddWidgetResult.PinUnsupported
        val model = createModel()
        model.toggleItem(dinner)

        model.addWidgetFromSettings()

        assertEquals(Res.string.record_widget_pin_unsupported, snackbarSender.lastSentMessageRes)
        assertEquals(listOf<Any>(Res.string.app_name), snackbarSender.lastSentFormatArgs)
    }

    @Test
    fun `add widget from settings shows the guide when the preset is saved`() = runTest {
        widgetPlatform.result = AddWidgetResult.PresetSaved
        val model = createModel()
        model.toggleItem(dinner)

        model.addWidgetFromSettings()

        assertEquals(Unit, model.showPresetSavedEvent.value.consume())
    }

    private val book = Book(id = "book", name = "Personal")
    private val dinner = RecordWidgetItem(RecordType.Expense, "Food", "Dinner")
    private val lunch = RecordWidgetItem(RecordType.Expense, "Food", "Lunch")
    private val salary = RecordWidgetItem(RecordType.Income, "Salary", "Salary")

    private val frequentRecordsClient = FakeFrequentRecordsClient(
        records = listOf(
            Record(type = RecordType.Expense, category = "Food", name = "Dinner", timestamp = 3),
            Record(type = RecordType.Expense, category = "Food", name = "Dinner", timestamp = 2),
            Record(type = RecordType.Expense, category = "Food", name = "Lunch", timestamp = 2),
            Record(type = RecordType.Income, category = "Salary", name = "", timestamp = 1),
        )
    )
    private val configStore = FakeRecordWidgetConfigStore()
    private val widgetPlatform = FakeRecordWidgetPlatform()
    private val snackbarSender = FakeSnackbarSender()
    private val tracker = FakeTracker()

    private fun createModel(
        bookRepo: FakeBookRepo = FakeBookRepo(book = book, currentBookId = book.id),
        navController: NavController<BookDest> = NavController(startRoot = BookDest.RecordWidget),
    ) = RecordWidgetViewModel(
        navController = navController,
        bookRepo = bookRepo,
        authManager = FakeAuthManager(user = User(id = "user")),
        frequentRecordsClient = frequentRecordsClient,
        configStore = configStore,
        widgetPlatform = widgetPlatform,
        snackbarSender = snackbarSender,
        tracker = tracker,
    )
}

private class FakeFrequentRecordsClient(
    private val records: List<Record>,
) : FrequentRecordsClient {

    var lastQueriedBookId: String? = null

    override suspend fun queryRecords(bookId: String, fromDate: LocalDate): List<Record> {
        lastQueriedBookId = bookId
        return records
    }
}

internal class FakeRecordWidgetConfigStore : RecordWidgetConfigStore {

    override val configs = MutableStateFlow<List<RecordWidgetConfig>>(emptyList())

    override suspend fun get(id: String): RecordWidgetConfig? = configs.value.firstOrNull { it.id == id }

    override suspend fun save(config: RecordWidgetConfig) {
        configs.value = configs.value.filterNot { it.id == config.id } + config
    }

    override suspend fun delete(id: String) {
        configs.value = configs.value.filterNot { it.id == id }
    }

    override suspend fun updateBookName(bookId: String, bookName: String): Boolean {
        var updated = false
        configs.value = configs.value.map { config ->
            if (config.bookId == bookId && config.bookName != bookName) {
                updated = true
                config.copy(bookName = bookName)
            } else {
                config
            }
        }
        return updated
    }
}

internal class FakeRecordWidgetPlatform : RecordWidgetPlatform {

    var result = AddWidgetResult.PinRequested
    var lastAddedConfig: RecordWidgetConfig? = null
    var refreshCount = 0

    override suspend fun addWidget(config: RecordWidgetConfig): AddWidgetResult {
        lastAddedConfig = config
        return result
    }

    override suspend fun refreshWidgets() {
        refreshCount++
    }
}
