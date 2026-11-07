package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 *  Below this available width, the side by side panes look packed with the roomy spacing, e.g. a
 *  landscape phone or the closed iPhone Duo next to its side column.
 */
private val CompactWidthThreshold = 840.dp

/**
 *  The side by side panes of a wide layout. The outer padding and the gap between the panes shrink
 *  when the width available to the content is limited. It's based on the content width, not the
 *  window, so it accounts for side bars and safe areas.
 */
@Composable
fun WideContentRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val isCompact = maxWidth < CompactWidthThreshold
        Row(
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 16.dp else 24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isCompact) 16.dp else 32.dp),
            content = content,
        )
    }
}
