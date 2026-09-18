package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.mohamedrejeb.calf.ui.sheet.AdaptiveBottomSheet
import com.mohamedrejeb.calf.ui.sheet.rememberAdaptiveSheetState

@Composable
actual fun ModalBottomSheet(
    modifier: Modifier,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AdaptiveBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        adaptiveSheetState = rememberAdaptiveSheetState(skipPartiallyExpanded = true),
        containerColor = LocalAppColors.current.light,
        dragHandle = { DragHandle(color = LocalAppColors.current.dark) },
        content = content
    )
}
