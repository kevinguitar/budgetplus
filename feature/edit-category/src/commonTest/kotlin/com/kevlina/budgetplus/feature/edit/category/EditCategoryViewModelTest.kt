package com.kevlina.budgetplus.feature.edit.category

import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.CategoryRenameEvent
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeRecordRepo
import com.kevlina.budgetplus.core.ui.bubble.FakeBubbleRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditCategoryViewModelTest {

    @Test
    fun `WHEN rename categories THEN sanitize the events`() = runTest {
        val model = createModel()
        model.onCategoryRenamed("Food", "Restaurant")
        model.onCategoryRenamed("Sport", "Exercise")
        model.onCategoryRenamed("Util", "Utility")
        model.onCategoryRenamed("Exercise", "Health")
        model.onCategoryRenamed("Restaurant", "Food")

        assertEquals(
            setOf(
                CategoryRenameEvent(from = "Sport", "Health"),
                CategoryRenameEvent(from = "Util", "Utility")
            ),
            model.categoryRenameEvents.toSet()
        )
    }

    @Test
    fun `WHEN delete a category THEN drop it from the rename events`() = runTest {
        val model = createModel()
        model.onCategoryRenamed("Food", "Restaurant")
        model.onCategoryDeleted("Restaurant")

        assertTrue(model.categoryRenameEvents.isEmpty())
    }

    private fun createModel() = EditCategoryViewModel(
        navController = NavController.preview,
        bookRepo = FakeBookRepo(),
        recordRepo = FakeRecordRepo,
        bubbleRepo = FakeBubbleRepo(),
        snackbarSender = FakeSnackbarSender,
    )
}
