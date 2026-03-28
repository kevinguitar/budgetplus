package com.kevlina.budgetplus.feature.edit.category

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.category_already_exist
import budgetplus.core.common.generated.resources.category_edit_successful
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.di.ViewModelKey
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.CategoryRenameEvent
import com.kevlina.budgetplus.core.data.RecordRepo
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.core.ui.bubble.BubbleRepo
import dev.zacsweers.metro.ContributesIntoMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.seconds

@ViewModelKey(EditCategoryViewModel::class)
@ContributesIntoMap(ViewModelScope::class)
class EditCategoryViewModel(
    val navController: NavController<BookDest>,
    private val bookRepo: BookRepo,
    private val recordRepo: RecordRepo,
    private val bubbleRepo: BubbleRepo,
    private val snackbarSender: SnackbarSender,
) : ViewModel() {

    val expenseCategories
        get() = bookRepo.bookState.value?.expenseCategories.orEmpty()

    val incomeCategories
        get() = bookRepo.bookState.value?.incomeCategories.orEmpty()

    @VisibleForTesting
    val categoryRenameEvents = mutableListOf<CategoryRenameEvent>()

    private var saveBubbleJob: Job? = null

    fun updateCategories(type: RecordType, newCategories: List<String>) {
        viewModelScope.launch {
            try {
                bookRepo.updateCategories(type, newCategories)
                if (categoryRenameEvents.isNotEmpty()) {
                    recordRepo.renameCategories(categoryRenameEvents)
                }
                snackbarSender.send(Res.string.category_edit_successful)
                navController.navigateUp()
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    fun highlightCategoryHint(dest: BubbleDest) {
        viewModelScope.launch { bubbleRepo.addBubbleToQueue(dest) }
    }

    fun highlightSaveButton(dest: BubbleDest) {
        if (saveBubbleJob != null) return
        saveBubbleJob = viewModelScope.launch {
            // Display save hint after edit hint
            delay(1.seconds)
            bubbleRepo.addBubbleToQueue(dest)
        }
    }

    fun showCategoryExistError(category: String) {
        viewModelScope.launch {
            snackbarSender.send(getString(Res.string.category_already_exist, category))
        }
    }

    fun onCategoryRenamed(oldName: String, newName: String) {
        if (oldName == newName) return

        val eventToReplace = categoryRenameEvents.find { it.to == oldName }
        if (eventToReplace == null) {
            categoryRenameEvents.add(CategoryRenameEvent(oldName, newName))
        } else {
            categoryRenameEvents.remove(eventToReplace)
            val newEvent = eventToReplace.copy(to = newName)
            if (newEvent.from != newEvent.to) {
                categoryRenameEvents.add(newEvent)
            }
        }
    }

    fun onCategoryDeleted(name: String) {
        categoryRenameEvents.removeAll { event ->
            event.to == name
        }
    }
}