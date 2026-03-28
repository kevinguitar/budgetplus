package com.kevlina.budgetplus.feature.record.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.batch_record_deleted
import budgetplus.core.common.generated.resources.batch_record_edited
import budgetplus.core.common.generated.resources.record_deleted
import budgetplus.core.common.generated.resources.record_edited
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.RecordRepo
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.feature.category.pills.CategoriesViewModel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.ViewModelScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString

@ViewModelKey
@ContributesIntoMap(ViewModelScope::class)
class EditRecordViewModel(
    val categoriesVm: CategoriesViewModel,
    private val recordRepo: RecordRepo,
    private val bookRepo: BookRepo,
    private val snackbarSender: SnackbarSender,
) : ViewModel() {

    val canAddCategory: Boolean
        get() = bookRepo.canEdit

    fun editRecord(
        record: Record,
        newDate: LocalDate,
        newCategory: String,
        newName: String,
        newPriceText: String,
        editBatch: Boolean,
    ) {
        viewModelScope.launch {
            try {
                if (editBatch) {
                    val count = recordRepo.editBatch(
                        oldRecord = record,
                        newDate = newDate,
                        newCategory = newCategory,
                        newName = newName,
                        newPriceText = newPriceText
                    )
                    snackbarSender.send(getString(Res.string.batch_record_edited, count.toString()))
                } else {
                    recordRepo.editRecord(
                        oldRecord = record,
                        newDate = newDate,
                        newCategory = newCategory,
                        newName = newName,
                        newPriceText = newPriceText
                    )
                    snackbarSender.send(Res.string.record_edited)
                }
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    fun deleteRecord(record: Record, deleteBatch: Boolean = false) {
        viewModelScope.launch {
            try {
                if (deleteBatch) {
                    val count = recordRepo.deleteBatch(record)
                    snackbarSender.send(getString(Res.string.batch_record_deleted, count.toString()))
                } else {
                    recordRepo.deleteRecord(record.id)
                    snackbarSender.send(getString(Res.string.record_deleted, record.name))
                }
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    fun addCategory(type: RecordType, newCategory: String) {
        viewModelScope.launch {
            try {
                bookRepo.addCategory(type = type, category = newCategory, source = "edit")
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }
}