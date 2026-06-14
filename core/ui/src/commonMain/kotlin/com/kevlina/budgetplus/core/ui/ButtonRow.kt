package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors

@Composable
fun ButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable (FlowRowScope) -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Preview
@Composable
private fun ButtonRow_Preview() = AppTheme {
    ButtonRow {
        Button(onClick = {}) {
            Text(text = "Button 1", color = LocalAppColors.current.light)
        }
        Button(onClick = {}) {
            Text(text = "Button 2", color = LocalAppColors.current.light)
        }
        Button(onClick = {}) {
            Text(text = "Button with a very long text should be in the next line", color = LocalAppColors.current.light)
        }
        Button(onClick = {}) {
            Text(text = "Button 3", color = LocalAppColors.current.light)
        }
        Button(onClick = {}) {
            Text(text = "Button 4", color = LocalAppColors.current.light)
        }
        Button(onClick = {}) {
            Text(text = "Button 5", color = LocalAppColors.current.light)
        }
        Button(onClick = {}) {
            Text(text = "OK", color = LocalAppColors.current.light)
        }
    }
}