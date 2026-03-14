package com.kevlina.budgetplus.feature.add.record

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kevlina.budgetplus.core.common.ExpressionEvaluator
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.data.fixtures.FakeVibratorManager
import com.kevlina.budgetplus.core.unit.test.SnapshotFlowRule
import com.kevlina.budgetplus.feature.add.record.ui.CalculatorAction
import com.kevlina.budgetplus.feature.add.record.ui.CalculatorButton
import com.kevlina.budgetplus.feature.freeze.fakeFreezeBookVm
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class CalculatorViewModelTest {

    @get:Rule
    val rule = SnapshotFlowRule()

    @Test
    fun `clearing the pricing`() {
        val statement = "36"
        calculator.input(statement)
        assertThat(calculator.priceText.text).isEqualTo(statement)

        calculator.clearPrice()
        assertThat(calculator.priceText.text).isEqualTo(CalculatorViewModel.EMPTY_PRICE)
    }

    @Test
    fun `evaluating the result`() = runTest {
        calculator.needEvaluate.test {
            assertThat(awaitItem()).isFalse()
            calculator.input("3×6")
            assertThat(awaitItem()).isTrue()
            calculator.evaluate()
            assertThat(awaitItem()).isFalse()
            assertThat(calculator.priceText.text).isEqualTo("18")
        }
    }

    @Test
    fun `complicated statement should be evaluated correctly`() {
        calculator.input("2.54+1.65×64.2÷9.01")
        calculator.evaluate()
        assertThat(calculator.priceText.text).isEqualTo("14.3")
    }

    @Test
    fun `operator should be replaced correctly`() {
        calculator.input("3+-1×+÷2")
        assertThat(calculator.priceText.text).isEqualTo("3-1÷2")
    }

    @Test
    fun `duplicated dot should be omitted`() {
        calculator.input("1.5.4+2..1...2")
        assertThat(calculator.priceText.text).isEqualTo("1.54+2.12")
    }

    @Test
    fun `delete button should work correctly`() {
        calculator.input("123")
        calculator.onInput(CalculatorButton.Delete)
        calculator.input("+321")
        calculator.onInput(CalculatorButton.Delete)
        calculator.evaluate()
        assertThat(calculator.priceText.text).isEqualTo("44")
    }

    private val calculator = CalculatorViewModel(
        vibrator = FakeVibratorManager(),
        snackbarSender = FakeSnackbarSender,
        speakToRecordVm = fakeSpeakToRecordVm,
        freezeBookVm = fakeFreezeBookVm,
        expressionEvaluator = ExpressionEvaluator()
    )
}

fun CalculatorViewModel.input(statement: String) {
    statement.toCharArray().forEach { char ->
        val btn = CalculatorButton.entries.first { it.text == char }
        onInput(btn)
    }
}

fun CalculatorViewModel.evaluate() {
    onCalculatorAction(CalculatorAction.Ok)
}
