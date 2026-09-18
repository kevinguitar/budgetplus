package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors
import androidx.compose.material3.ModalBottomSheet as MaterialModalBottomSheet

@Suppress("DEPRECATION")
@Composable
fun ModalBottomSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    MaterialModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LocalAppColors.current.light,
        dragHandle = { DragHandle(color = LocalAppColors.current.dark) },
        content = content
    )
}

@Preview
@Composable
private fun ModalBottomSheet_Preview() = AppTheme {
    ModalBottomSheet(onDismissRequest = {}) {
        Text("Hello World")
        Spacer(modifier = Modifier.height(16.dp))
        Text("This is the Bottom Sheet!")
    }
}