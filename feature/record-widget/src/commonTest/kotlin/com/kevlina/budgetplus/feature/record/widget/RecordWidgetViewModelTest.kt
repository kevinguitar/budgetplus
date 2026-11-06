package com.kevlina.budgetplus.feature.record.widget

import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.app_name
import budgetplus.core.common.generated.resources.record_widget_book_unavailable
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
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
        assertEquals(mapOf("source" to "os", "item_count" to 1, "dynamic_colors" to false), tracker.lastEvent?.second)
    }

    @Test
    fun `reconfiguring an existing widget should log the edited event instead of added`() = runTest {
        val model = createModel()
        model.toggleItem(dinner)
        model.saveConfigForWidget(widgetId = "42")

        model.toggleItem(lunch)
        model.saveConfigForWidget(widgetId = "42")

        assertEquals(listOf(dinner, lunch), configStore.configs.value.single().items)
        assertEquals("record_widget_edited", tracker.lastEventName)
        assertEquals(mapOf("item_count" to 2, "book_changed" to false, "dynamic_colors" to false), tracker.lastEvent?.second)
    }

    // region Editing an existing widget

    @Test
    fun `editing a widget preselects its items in the same order`() = runTest {
        configStore.save(existingConfig(items = listOf(salary, dinner, lunch)))

        val model = createModel(widgetId = "42")

        assertEquals(listOf(salary, dinner, lunch), model.selectedItems.value)
    }

    @Test
    fun `editing a widget lists its items first, including the ones no longer frequent`() = runTest {
        val rent = RecordWidgetItem(RecordType.Expense, "Rent", "Rent")
        configStore.save(existingConfig(items = listOf(rent, lunch)))

        val model = createModel(widgetId = "42")

        assertEquals(
            RecordWidgetUiState.Loaded(
                listOf(
                    FrequentItem(rent, count = 0),
                    FrequentItem(lunch, count = 1),
                    FrequentItem(dinner, count = 2),
                    FrequentItem(salary, count = 1),
                )
            ),
            model.uiState.value
        )
    }

    @Test
    fun `editing a widget uses the widget's book instead of the current book`() = runTest {
        val widgetBook = Book(id = "widget_book", name = "Travel")
        configStore.save(existingConfig(book = widgetBook, items = listOf(dinner)))

        val model = createModel(
            bookRepo = FakeBookRepo(book = book, books = listOf(book, widgetBook), currentBookId = book.id),
            widgetId = "42",
        )

        assertEquals(widgetBook, model.book.value)
        assertEquals("widget_book", frequentRecordsClient.lastQueriedBookId)
        assertEquals("widget_book", model.buildConfig(id = "42")?.bookId)
    }

    @Test
    fun `editing a widget falls back to the current book when its book is unavailable`() = runTest {
        configStore.save(existingConfig(book = Book(id = "gone", name = "Gone"), items = listOf(dinner)))

        val model = createModel(widgetId = "42")

        assertEquals(book, model.book.value)
        assertTrue(model.selectedItems.value.isEmpty())
        assertEquals("book", frequentRecordsClient.lastQueriedBookId)
        assertEquals(Res.string.record_widget_book_unavailable, snackbarSender.lastSentMessageRes)
    }

    @Test
    fun `editing a widget keeps its book when the books aren't loaded in time`() = runTest {
        val widgetBook = Book(id = "widget_book", name = "Travel")
        configStore.save(existingConfig(book = widgetBook, items = listOf(dinner)))

        val model = createModel(
            bookRepo = FakeBookRepo(book = book, books = null, currentBookId = book.id),
            widgetId = "42",
        )
        advanceUntilIdle()

        assertEquals("widget_book", model.book.value?.id)
        assertEquals("Travel", model.book.value?.name)
        assertEquals(listOf(dinner), model.selectedItems.value)
        assertNull(snackbarSender.lastSentMessageRes)
    }

    @Test
    fun `saving an edited widget keeps when it was created and logs the edited event`() = runTest {
        configStore.save(existingConfig(items = listOf(dinner), createdOn = 123))
        val model = createModel(widgetId = "42")
        model.toggleItem(lunch)

        model.saveConfigForWidget(widgetId = "42")

        val config = configStore.configs.value.single()
        assertEquals(listOf(dinner, lunch), config.items)
        assertEquals(123, config.createdOn)
        assertEquals("record_widget_edited", tracker.lastEventName)
    }

    @Test
    fun `a new widget from the launcher starts with nothing selected`() = runTest {
        val model = createModel(widgetId = "42")

        assertTrue(model.selectedItems.value.isEmpty())
        assertEquals("book", frequentRecordsClient.lastQueriedBookId)
    }

    @Test
    fun `start only takes effect once`() = runTest {
        configStore.save(existingConfig(items = listOf(dinner)))
        val model = createModel(widgetId = "42")
        model.toggleItem(lunch)

        model.start(widgetId = "42")

        assertEquals(listOf(dinner, lunch), model.selectedItems.value)
    }

    // endregion

    // region Material You

    @Test
    fun `dynamic colors are off by default`() = runTest {
        val model = createModel()
        model.toggleItem(dinner)

        assertFalse(model.useDynamicColors.value)
        assertEquals(false, model.buildConfig()?.useDynamicColors)
    }

    @Test
    fun `turning on dynamic colors is saved to the config and tracked`() = runTest {
        val model = createModel()
        model.toggleItem(dinner)

        model.setUseDynamicColors(true)
        model.saveConfigForWidget(widgetId = "42")

        assertTrue(configStore.configs.value.single().useDynamicColors)
        assertEquals(true, tracker.lastEvent?.second?.get("dynamic_colors"))
    }

    @Test
    fun `editing a widget restores its dynamic colors setting`() = runTest {
        configStore.save(existingConfig(items = listOf(dinner)).copy(useDynamicColors = true))

        val model = createModel(widgetId = "42")

        assertTrue(model.useDynamicColors.value)
    }

    @Test
    fun `editing a widget keeps its dynamic colors setting when its book is unavailable`() = runTest {
        configStore.save(
            existingConfig(book = Book(id = "gone", name = "Gone"), items = listOf(dinner)).copy(useDynamicColors = true)
        )

        val model = createModel(widgetId = "42")

        assertTrue(model.useDynamicColors.value)
    }

    @Test
    fun `configs saved before dynamic colors existed decode with it off`() {
        val json = """{"id":"42","bookId":"book","bookName":"Personal","items":[],"createdOn":0,"version":1}"""

        val config = Json.decodeFromString(RecordWidgetConfig.serializer(), json)

        assertFalse(config.useDynamicColors)
    }

    // endregion

    // region Switching book

    @Test
    fun `books lists the user's books`() = runTest {
        val model = createModel(bookRepo = FakeBookRepo(book = book, books = listOf(book, travelBook)))

        assertEquals(listOf(book, travelBook), model.books.value)
    }

    @Test
    fun `selecting another book reloads the items and clears the selection`() = runTest {
        frequentRecordsClient.recordsByBook[travelBook.id] = listOf(
            Record(type = RecordType.Expense, category = "Hotel", name = "Hotel"),
        )
        val model = createModel(bookRepo = FakeBookRepo(book = book, books = listOf(book, travelBook)))
        model.toggleItem(dinner)

        model.selectBook(travelBook)

        assertEquals(travelBook, model.book.value)
        assertTrue(model.selectedItems.value.isEmpty())
        assertEquals("travel", frequentRecordsClient.lastQueriedBookId)
        assertEquals(
            RecordWidgetUiState.Loaded(listOf(FrequentItem(RecordWidgetItem(RecordType.Expense, "Hotel", "Hotel"), count = 1))),
            model.uiState.value
        )
        assertEquals("record_widget_book_changed", tracker.lastEventName)
    }

    @Test
    fun `selecting another book ties the config to it without changing the app's book`() = runTest {
        // FakeBookRepo.selectBook throws, so this also verifies the app's book isn't changed.
        val bookRepo = FakeBookRepo(book = book, books = listOf(book, travelBook))
        val model = createModel(bookRepo = bookRepo)

        model.selectBook(travelBook)
        model.toggleItem(dinner)

        assertEquals("travel", model.buildConfig()?.bookId)
        assertEquals("Travel", model.buildConfig()?.bookName)
        assertEquals(book, bookRepo.bookState.value)
    }

    @Test
    fun `selecting the same book does nothing`() = runTest {
        val model = createModel(bookRepo = FakeBookRepo(book = book, books = listOf(book, travelBook)))
        model.toggleItem(dinner)
        tracker.lastEvent = null

        model.selectBook(book)

        assertEquals(listOf(dinner), model.selectedItems.value)
        assertNull(tracker.lastEvent)
    }

    @Test
    fun `switching back to the edited widget's book restores its selection`() = runTest {
        configStore.save(existingConfig(items = listOf(salary, dinner)))
        val model = createModel(
            bookRepo = FakeBookRepo(book = book, books = listOf(book, travelBook)),
            widgetId = "42",
        )

        model.selectBook(travelBook)
        assertTrue(model.selectedItems.value.isEmpty())

        model.selectBook(book)
        assertEquals(listOf(salary, dinner), model.selectedItems.value)
        assertEquals(FrequentItem(salary, count = 1), (model.uiState.value as RecordWidgetUiState.Loaded).items.first())
    }

    @Test
    fun `saving an edited widget with another book logs the book change`() = runTest {
        configStore.save(existingConfig(items = listOf(dinner)))
        val model = createModel(
            bookRepo = FakeBookRepo(book = book, books = listOf(book, travelBook)),
            widgetId = "42",
        )
        model.selectBook(travelBook)
        model.toggleItem(lunch)

        model.saveConfigForWidget(widgetId = "42")

        assertEquals("travel", configStore.configs.value.single().bookId)
        assertEquals(mapOf("item_count" to 1, "book_changed" to true, "dynamic_colors" to false), tracker.lastEvent?.second)
    }

    @Test
    fun `a slow load of the previous book doesn't overwrite the new book's items`() = runTest {
        val gate = CompletableDeferred<Unit>()
        frequentRecordsClient.gates[book.id] = gate
        frequentRecordsClient.recordsByBook[travelBook.id] = emptyList()
        val model = createModel(bookRepo = FakeBookRepo(book = book, books = listOf(book, travelBook)))
        assertEquals(RecordWidgetUiState.Loading, model.uiState.value)

        model.selectBook(travelBook)
        gate.complete(Unit)

        assertEquals(RecordWidgetUiState.Loaded(emptyList()), model.uiState.value)
        assertNull(snackbarSender.lastSentMessageRes)
    }

    // endregion

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
    private val travelBook = Book(id = "travel", name = "Travel")

    private fun existingConfig(
        book: Book = this.book,
        items: List<RecordWidgetItem>,
        createdOn: Long = 0,
    ) = RecordWidgetConfig(id = "42", bookId = book.id, bookName = book.name, items = items, createdOn = createdOn)
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
        bookRepo: FakeBookRepo = FakeBookRepo(book = book, books = listOf(book), currentBookId = book.id),
        navController: NavController<BookDest> = NavController(startRoot = BookDest.RecordWidget),
        widgetId: String? = null,
    ) = RecordWidgetViewModel(
        navController = navController,
        bookRepo = bookRepo,
        authManager = FakeAuthManager(user = User(id = "user")),
        frequentRecordsClient = frequentRecordsClient,
        configStore = configStore,
        widgetPlatform = widgetPlatform,
        snackbarSender = snackbarSender,
        tracker = tracker,
    ).apply { start(widgetId) }
}

private class FakeFrequentRecordsClient(
    private val records: List<Record>,
) : FrequentRecordsClient {

    var lastQueriedBookId: String? = null

    /** The records of the book, [records] by default. */
    val recordsByBook = mutableMapOf<String, List<Record>>()

    /** Suspend the query of the book until the gate completes. */
    val gates = mutableMapOf<String, CompletableDeferred<Unit>>()

    override suspend fun queryRecords(bookId: String, fromDate: LocalDate): List<Record> {
        lastQueriedBookId = bookId
        gates[bookId]?.await()
        return recordsByBook[bookId] ?: records
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

    override suspend fun updatePreview() = Unit
}
