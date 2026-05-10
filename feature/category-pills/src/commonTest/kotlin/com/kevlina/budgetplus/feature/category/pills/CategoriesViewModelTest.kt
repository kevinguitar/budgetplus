package com.kevlina.budgetplus.feature.category.pills

import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.remote.Book
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CategoriesViewModelTest {

    @Test
    fun `category is initially null`() {
        val model = createModel()
        assertNull(model.category.value)
    }

    @Test
    fun `setCategory updates category state`() {
        val model = createModel()
        model.setCategory("Food")
        assertEquals("Food", model.category.value)
    }

    @Test
    fun `setCategory to null clears category`() {
        val model = createModel()
        model.setCategory("Food")
        model.setCategory(null)
        assertNull(model.category.value)
    }

    @Test
    fun `expenseCategories reflects book expense categories`() {
        val book = Book(expenseCategories = listOf("Food", "Transport", "Shopping"))
        val model = createModel(book = book)
        assertEquals(listOf("Food", "Transport", "Shopping"), model.expenseCategories.value)
    }

    @Test
    fun `incomeCategories reflects book income categories`() {
        val book = Book(incomeCategories = listOf("Salary", "Bonus"))
        val model = createModel(book = book)
        assertEquals(listOf("Salary", "Bonus"), model.incomeCategories.value)
    }

    @Test
    fun `expenseCategories is empty when book is null`() {
        val model = createModel(book = null)
        assertEquals(emptyList(), model.expenseCategories.value)
    }

    @Test
    fun `incomeCategories is empty when book is null`() {
        val model = createModel(book = null)
        assertEquals(emptyList(), model.incomeCategories.value)
    }

    private fun createModel(
        book: Book? = Book(),
    ) = CategoriesViewModel(
        bookRepo = FakeBookRepo(book = book),
    )
}
