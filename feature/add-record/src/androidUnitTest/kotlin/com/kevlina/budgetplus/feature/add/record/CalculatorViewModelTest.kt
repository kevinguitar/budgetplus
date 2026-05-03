package com.kevlina.budgetplus.feature.add.record

import app.cash.turbine.test
import com.kevlina.budgetplus.core.common.ExpressionEvaluator
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.data.fixtures.FakeVibratorManager
import com.kevlina.budgetplus.core.unit.test.BaseTest
import com.kevlina.budgetplus.feature.add.record.ui.CalculatorAction
import com.kevlina.budgetplus.feature.add.record.ui.CalculatorButton
import com.kevlina.budgetplus.feature.freeze.createFreezeBookVm
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculatorViewModelTest: BaseTest(observeComposeSnapshots = true) {

    @Test
    fun `clearing the pricing`() = runTest {
        val calculator = createCalculator()
        val statement = "36"
        calculator.input(statement)
        assertEquals(statement, calculator.priceText.text)

        calculator.clearPrice()
        assertEquals(CalculatorViewModel.EMPTY_PRICE, calculator.priceText.text)
    }

    @Test
    fun `evaluating the result`() = runTest {
        val calculator = createCalculator()
        calculator.needEvaluate.test {
            assertFalse(awaitItem())
            calculator.input("3×6")
            assertTrue(awaitItem())
            calculator.evaluate()
            assertFalse(awaitItem())
            assertEquals("18", calculator.priceText.text)
        }
    }

    @Test
    fun `complicated statement should be evaluated correctly`() = runTest {
        val calculator = createCalculator()
        calculator.input("2.54+1.65×64.2÷9.01")
        calculator.evaluate()
        assertEquals("14.3", calculator.priceText.text)
    }

    @Test
    fun `operator should be replaced correctly`() = runTest {
        val calculator = createCalculator()
        calculator.input("3+-1×+÷2")
        assertEquals("3-1÷2", calculator.priceText.text)
    }

    @Test
    fun `duplicated dot should be omitted`() = runTest {
        val calculator = createCalculator()
        calculator.input("1.5.4+2..1...2")
        assertEquals("1.54+2.12", calculator.priceText.text)
    }

    @Test
    fun `delete button should work correctly`() = runTest {
        val calculator = createCalculator()
        calculator.input("123")
        calculator.onInput(CalculatorButton.Delete)
        calculator.input("+321")
        calculator.onInput(CalculatorButton.Delete)
        calculator.evaluate()
        assertEquals("44", calculator.priceText.text)
    }

    private fun TestScope.createCalculator() = CalculatorViewModel(
        vibrator = FakeVibratorManager(),
        snackbarSender = FakeSnackbarSender,
        speakToRecordVm = fakeSpeakToRecordVm,
        freezeBookVm = createFreezeBookVm(),
        expressionEvaluator = ExpressionEvaluator(),
        appScope = backgroundScope
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
