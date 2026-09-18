package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kevlina.budgetplus.core.theme.LocalAppColors
import androidx.compose.material3.ModalBottomSheet as MaterialModalBottomSheet

@Suppress("DEPRECATION")
@Composable
actual fun ModalBottomSheet(
    modifier: Modifier,
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
