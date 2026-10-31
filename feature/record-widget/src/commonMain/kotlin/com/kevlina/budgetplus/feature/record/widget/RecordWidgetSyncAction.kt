package com.kevlina.budgetplus.feature.record.widget

import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.common.Logger
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.theme.ThemeManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Keeps the widgets up to date with the app:
 * - The book name snapshot is renamed when the book is renamed.
 * - The widgets are re-rendered on app start and when the theme colors change.
 */
@ContributesIntoSet(AppScope::class)
internal class RecordWidgetSyncAction(
    private val bookRepo: BookRepo,
    private val themeManager: ThemeManager,
    private val configStore: RecordWidgetConfigStore,
    private val widgetPlatform: RecordWidgetPlatform,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : AppStartAction {

    override fun onAppStart() {
        bookRepo.booksState
            .filterNotNull()
            .onEach { books ->
                var isUpdated = false
                books.forEach { book ->
                    isUpdated = configStore.updateBookName(bookId = book.id, bookName = book.name) || isUpdated
                }
                if (isUpdated) {
                    refreshWidgets()
                }
            }
            .launchIn(appScope)

        themeManager.themeColors
            .onEach { refreshWidgets() }
            .launchIn(appScope)
    }

    private suspend fun refreshWidgets() {
        try {
            widgetPlatform.refreshWidgets()
        } catch (e: Exception) {
            Logger.e(e, "Fail to refresh the record widgets")
        }
    }
}
