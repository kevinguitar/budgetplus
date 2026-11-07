package com.kevlina.budgetplus.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.record_expense
import budgetplus.core.common.generated.resources.record_income
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

private const val PILL_WIDTH_FRACTION = 0.3F
private val PillMinWidth = 80.dp
private val PillMaxWidth = 160.dp

@Composable
fun RecordTypeTab(
    selected: RecordType,
    onTypeSelected: (RecordType) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Shrink the pills in narrow panes, e.g. next to a side bar, and expand them with more room.
        val pillWidth = (maxWidth * PILL_WIDTH_FRACTION).coerceIn(PillMinWidth, PillMaxWidth)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TypePill(
                text = stringResource(Res.string.record_expense),
                isSelected = selected == RecordType.Expense,
                width = pillWidth,
                onClick = { onTypeSelected(RecordType.Expense) }
            )

            Spacer(
                modifier = Modifier
                    .size(width = 2.dp, height = 24.dp)
                    .background(
                        color = LocalAppColors.current.dark,
                        shape = CircleShape
                    )
            )

            TypePill(
                text = stringResource(Res.string.record_income),
                isSelected = selected == RecordType.Income,
                width = pillWidth,
                onClick = { onTypeSelected(RecordType.Income) }
            )
        }
    }
}

@Composable
private fun TypePill(
    text: String,
    isSelected: Boolean,
    width: Dp,
    onClick: () -> Unit,
) {

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            LocalAppColors.current.dark
        } else {
            LocalAppColors.current.light
        },
        label = "bgColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            LocalAppColors.current.light
        } else {
            LocalAppColors.current.dark
        },
        label = "textColor"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier.heightIn(min = AppTheme.minSurfaceSize),
    ) {

        Text(
            text = text,
            color = textColor,
            fontSize = FontSize.SemiLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                // Grow beyond the width if the text needs it, e.g. in other languages.
                .widthIn(min = width)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun RecordTypeTab_Preview() = AppTheme {
    RecordTypeTab(selected = RecordType.Expense, onTypeSelected = {})
}