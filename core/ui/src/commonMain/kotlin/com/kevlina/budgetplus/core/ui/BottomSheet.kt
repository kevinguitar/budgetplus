package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Adaptive modal bottom sheet.
 *
 * On Android it renders the themed Material3 [androidx.compose.material3.ModalBottomSheet]
 * (unchanged), while on iOS it renders Calf's native `AdaptiveBottomSheet`.
 */
@Composable
expect fun ModalBottomSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
)

@Preview
@Composable
private fun ModalBottomSheet_Preview() = AppTheme {
    ModalBottomSheet(onDismissRequest = {}) {
        Text("Hello World")
        Spacer(modifier = Modifier.height(16.dp))
        Text("This is the Bottom Sheet!")
    }
}
