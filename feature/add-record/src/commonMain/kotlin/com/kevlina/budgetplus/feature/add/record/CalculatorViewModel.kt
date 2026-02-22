package com.kevlina.budgetplus.feature.add.record

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.ExpressionEvaluator
import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.VibratorManager
import com.kevlina.budgetplus.core.common.plainPriceString
import com.kevlina.budgetplus.core.common.sendEvent
import com.kevlina.budgetplus.feature.add.record.ui.CalculatorAction
import com.kevlina.budgetplus.feature.add.record.ui.CalculatorButton
import com.kevlina.budgetplus.feature.freeze.FreezeBookViewModel
import com.kevlina.budgetplus.feature.speak.record.SpeakToRecordViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Inject
class CalculatorViewModel(
    val vibrator: VibratorManager,
    val speakToRecordVm: SpeakToRecordViewModel,
    val freezeBookVm: FreezeBookViewModel,
    private val snackbarSender: SnackbarSender,
    private val expressionEvaluator: ExpressionEvaluator,
) : ViewModel() {

    val priceText = TextFieldState(EMPTY_PRICE)

    val needEvaluate: Flow<Boolean> = snapshotFlow { priceText.text }
        .map { text -> text.any { it in operatorChars } }
        .distinctUntilChanged()

    val recordFlow: EventFlow<Unit>
        field = MutableEventFlow<Unit>()

    private val operatorChars = listOf(
        CalculatorButton.Plus,
        CalculatorButton.Minus,
        CalculatorButton.Multiply,
        CalculatorButton.Divide
    )
        .map { it.text }
        .toCharArray()

    fun onInput(btn: CalculatorButton) {
        val currentText = priceText.text
        when (btn) {
            CalculatorButton.Delete -> delete()

            // Replace the latest operator
            CalculatorButton.Plus, CalculatorButton.Minus,
            CalculatorButton.Multiply, CalculatorButton.Divide,
                -> when {
                currentText == EMPTY_PRICE -> Unit
                currentText.last() in operatorChars -> {
                    priceText.edit {
                        replace(length - 1, length, btn.text.toString())
                    }
                }

                else -> appendText(btn)
            }

            // Do not allow multiple dots in the same number
            CalculatorButton.Dot -> when {
                currentText.any { it in operatorChars } -> {
                    val indexOfLastOp = currentText.indexOfLast { it in operatorChars }
                    val lastNumber = currentText.takeLast(currentText.length - indexOfLastOp - 1)
                    if (!lastNumber.contains(CalculatorButton.Dot.text)) {
                        appendText(btn)
                    }
                }

                currentText.contains(CalculatorButton.Dot.text) -> Unit
                else -> appendText(btn)
            }

            else -> if (currentText == EMPTY_PRICE) {
                priceText.setTextAndPlaceCursorAtEnd(btn.text.toString())
            } else {
                appendText(btn)
            }
        }
    }

    private fun appendText(btn: CalculatorButton) {
        priceText.edit { append(btn.text) }
    }

    private fun evaluate() {
        val text = priceText.text.toString()
            .replace(CalculatorButton.Multiply.text, '*')
            .replace(CalculatorButton.Divide.text, '/')

        when (val result = expressionEvaluator.evaluate(text)) {
            is ExpressionEvaluator.Result.Success -> setPrice(result.value)
            is ExpressionEvaluator.Result.Error -> {
                viewModelScope.launch { snackbarSender.send(result.message) }
                Logger.e(CalculatorException()) { "Validation error. Raw: $text" }
            }
        }
    }

    fun setPrice(priceNumber: Double) {
        priceText.setTextAndPlaceCursorAtEnd(priceNumber.plainPriceString)
    }

    fun clearPrice() {
        priceText.setTextAndPlaceCursorAtEnd(EMPTY_PRICE)
    }

    fun onCalculatorAction(action: CalculatorAction) {
        when (action) {
            CalculatorAction.Clear -> clearPrice()
            CalculatorAction.Evaluate -> evaluate()
            CalculatorAction.Ok -> {
                evaluate()
                recordFlow.sendEvent()
            }
        }
    }

    private fun delete() {
        priceText.edit {
            when {
                length == 1 -> replace(0, length, EMPTY_PRICE)
                length > 1 -> delete(length - 1, length)
            }
        }
    }

    companion object {
        const val EMPTY_PRICE: String = "0"
    }
}

internal class CalculatorException : RuntimeException("Calculation error")