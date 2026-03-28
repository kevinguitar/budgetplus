package com.kevlina.budgetplus.feature.batch.record

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.batch_record_created
import com.kevlina.budgetplus.core.common.EventTrigger
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.di.ViewModelKey
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BatchFrequency
import com.kevlina.budgetplus.core.data.BatchUnit
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.RecordRepo
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.toAuthor
import com.kevlina.budgetplus.feature.add.record.CalculatorViewModel.Companion.EMPTY_PRICE
import com.kevlina.budgetplus.feature.add.record.RecordDateState
import com.kevlina.budgetplus.feature.category.pills.CategoriesViewModel
import dev.zacsweers.metro.ContributesIntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString

@ViewModelKey(BatchRecordViewModel::class)
@ContributesIntoMap(ViewModelScope::class)
class BatchRecordViewModel(
    val categoriesVm: CategoriesViewModel,
    val bookRepo: BookRepo,
    val navController: NavController<BookDest>,
    private val recordRepo: RecordRepo,
    private val authManager: AuthManager,
    private val snackbarSender: SnackbarSender,
) : ViewModel() {

    val type: StateFlow<RecordType>
        field = MutableStateFlow(RecordType.Expense)

    val startRecordDate: StateFlow<RecordDateState>
        field = MutableStateFlow<RecordDateState>(RecordDateState.Now)

    val note = TextFieldState()
    val priceText = TextFieldState()

    val frequency: StateFlow<BatchFrequency>
        field = MutableStateFlow<BatchFrequency>(BatchFrequency(duration = 1, unit = BatchUnit.Month))

    val batchTimes = (BATCH_TIMES_MIN..BATCH_TIMES_MAX).toList()
    val times: StateFlow<Int>
        field = MutableStateFlow<Int>(batchTimes.first())

    val recordEvent = EventTrigger<Unit>()

    val isBatchButtonEnabled: StateFlow<Boolean> = combine(
        categoriesVm.category,
        snapshotFlow { priceText.text }
    ) { category, priceText ->
        category != null && priceText.isNotEmpty() && priceText != EMPTY_PRICE
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun setType(newType: RecordType) {
        type.value = newType
    }

    fun setStartDate(date: LocalDate) {
        startRecordDate.value = if (date == LocalDate.now()) {
            RecordDateState.Now
        } else {
            RecordDateState.Other(date)
        }
    }

    fun setDuration(duration: Int) {
        frequency.update { it.copy(duration = duration) }
    }

    fun setUnit(unit: BatchUnit) {
        frequency.update { it.copy(unit = unit) }
    }

    fun setTimes(newTimes: Int) {
        times.value = newTimes
    }

    fun batchRecord() {
        val category = categoriesVm.category.value ?: return
        val price = try {
            priceText.text.toString().toDouble()
        } catch (e: Exception) {
            snackbarSender.sendError(e)
            return
        }

        val record = Record(
            type = type.value,
            category = category,
            name = note.text.trim().ifEmpty { category }.toString(),
            price = price,
            author = authManager.userState.value?.toAuthor()
        )

        recordRepo.batchRecord(
            record = record,
            startDate = startRecordDate.value.date,
            frequency = frequency.value,
            times = times.value
        )
        recordEvent.sendEvent(Unit)
        resetScreen()

        viewModelScope.launch {
            snackbarSender.send(getString(Res.string.batch_record_created, times.value.toString(), category))
        }
    }

    private fun resetScreen() {
        categoriesVm.setCategory(null)
        note.clearText()
        priceText.clearText()
    }

    private companion object {
        const val BATCH_TIMES_MIN = 2
        const val BATCH_TIMES_MAX = 30
    }
}