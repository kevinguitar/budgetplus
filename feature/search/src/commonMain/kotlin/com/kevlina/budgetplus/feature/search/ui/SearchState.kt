package com.kevlina.budgetplus.feature.search.ui

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.data.remote.Author
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.feature.category.pills.CategoriesGridState
import com.kevlina.budgetplus.feature.record.card.RecordCardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

@Stable
data class SearchState(
    val query: TextFieldState,
    val filter: SearchFilterState,
    val result: SearchResultState,
) {
    companion object {
        val preview = SearchState(
            query = TextFieldState("query"),
            filter = SearchFilterState.preview,
            result = SearchResultState.preview,
        )
    }
}

@Stable
data class SearchFilterState(
    val type: StateFlow<RecordType>,
    val selectType: (RecordType) -> Unit,
    val category: StateFlow<SearchCategory>,
    val categoryGrid: CategoriesGridState,
    val selectCategory: (SearchCategory) -> Unit,
    val period: StateFlow<SearchPeriod>,
    val selectPeriod: (SearchPeriod) -> Unit,
    val author: StateFlow<User?>,
    val allAuthors: StateFlow<List<User>>,
    val selectAuthor: (User?) -> Unit,
) {
    companion object {
        val preview = run {
            val type = MutableStateFlow(RecordType.Expense)
            val category = MutableStateFlow<SearchCategory>(SearchCategory.None)
            val period = MutableStateFlow<SearchPeriod>(SearchPeriod.PastMonth)
            val author = MutableStateFlow<User?>(null)

            SearchFilterState(
                type = type,
                selectType = { type.value = it },
                category = category,
                categoryGrid = CategoriesGridState.preview,
                selectCategory = { category.value = it },
                period = period,
                selectPeriod = { period.value = it },
                author = author,
                allAuthors = MutableStateFlow(
                    listOf(
                        User(id = "1", name = "Kevin"),
                        User(id = "2", name = "Alina"),
                    )
                ),
                selectAuthor = { author.value = it }
            )
        }
    }
}

@Stable
data class SearchResultState(
    val result: StateFlow<SearchResult>,
    val editRecordEvent: EventFlow<Record>,
    val deleteRecordEvent: EventFlow<Record>,
) {
    companion object {
        val preview = SearchResultState(
            result = MutableStateFlow(
                SearchResult.Success(
                    List(24) { index ->
                        RecordCardState.preview.copy(
                            item = Record(
                                id = index.toString(),
                                type = RecordType.Income,
                                date = LocalDate.now().toEpochDays(),
                                category = "Food",
                                name = "Fancy Restaurant $index",
                                price = 453.93,
                                author = Author(id = "", name = "Kevin")
                            )
                        )
                    }
                )
            ),
            editRecordEvent = MutableEventFlow(),
            deleteRecordEvent = MutableEventFlow()
        )
    }
}

sealed interface SearchCategory {
    val name: String?

    object None : SearchCategory {
        override val name: String? get() = null
    }

    data class Selected(override val name: String) : SearchCategory
}

sealed interface SearchPeriod {
    data object PastMonth : SearchPeriod
    data object PastHalfYear : SearchPeriod
    data object PastYear : SearchPeriod
    data class Custom(val from: LocalDate, val until: LocalDate) : SearchPeriod {
        companion object {
            val Unselected = Custom(LocalDate.now(), LocalDate.now())
        }
    }

    companion object {
        val SearchPeriod.requiresPremium: Boolean
            get() = this !is PastMonth
    }
}

sealed interface SearchResult {
    data object Empty : SearchResult
    data object Loading : SearchResult
    data class Success(val records: List<RecordCardState>) : SearchResult
}