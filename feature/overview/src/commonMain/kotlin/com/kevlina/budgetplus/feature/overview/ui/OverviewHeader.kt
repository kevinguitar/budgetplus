package com.kevlina.budgetplus.feature.overview.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.RecordTypeTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun OverviewHeader(
    state: OverviewHeaderState,
    navController: NavController<BookDest>,
    modifier: Modifier = Modifier,
) {

    val type by state.type.collectAsStateWithLifecycle()
    val totalPrice by state.totalPrice.collectAsStateWithLifecycle()
    val balance by state.balance.collectAsStateWithLifecycle()
    val recordGroups by state.recordGroups.collectAsStateWithLifecycle()
    val authors by state.authors.collectAsStateWithLifecycle()
    val selectedAuthor by state.selectedAuthor.collectAsStateWithLifecycle()

    val isBalanceCardVisible = recordGroups?.isNotEmpty() == true

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        RecordTypeTab(
            selected = type,
            onTypeSelected = state.setRecordType,
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        if (authors.size > 1) {
            AuthorSelector(
                authors = authors,
                selectedAuthor = selectedAuthor,
                setAuthor = state.setAuthor
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        TimePeriodSelector(
            state = state.timePeriodSelectorState,
            navController = navController,
        )

        AnimatedVisibility(
            visible = isBalanceCardVisible,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically()
        ) {
            BalanceCard(
                totalPrice = totalPrice,
                balance = balance,
            )
        }
    }
}

@Stable
internal data class OverviewHeaderState(
    val type: StateFlow<RecordType>,
    val totalPrice: StateFlow<String>,
    val balance: StateFlow<String>,
    val recordGroups: StateFlow<Map<String, List<Record>>?>,
    val authors: StateFlow<List<User>>,
    val selectedAuthor: StateFlow<User?>,
    val timePeriodSelectorState: TimePeriodSelectorState,
    val setRecordType: (RecordType) -> Unit,
    val setAuthor: (User?) -> Unit,
) {
    companion object {
        val preview = OverviewHeaderState(
            type = MutableStateFlow(RecordType.Expense),
            totalPrice = MutableStateFlow("$245.25"),
            balance = MutableStateFlow("$52.45"),
            recordGroups = MutableStateFlow(mapOf("Food" to emptyList())),
            authors = MutableStateFlow(listOf(User(name = "Kevin"), User(name = "Alina"))),
            selectedAuthor = MutableStateFlow(User(name = "Kevin")),
            timePeriodSelectorState = TimePeriodSelectorState.preview,
            setRecordType = {},
            setAuthor = {}
        )
    }
}

@Preview
@Composable
private fun OverviewHeader_Preview() = AppTheme {
    OverviewHeader(
        state = OverviewHeaderState.preview,
        navController = NavController.preview,
        modifier = Modifier
            .background(LocalAppColors.current.light)
            .padding(horizontal = 16.dp)
    )
}