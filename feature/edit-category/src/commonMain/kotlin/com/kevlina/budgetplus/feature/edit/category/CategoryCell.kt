package com.kevlina.budgetplus.feature.edit.category

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_drag_handle
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.Surface
import com.kevlina.budgetplus.core.ui.Text
import org.jetbrains.compose.resources.vectorResource

@Composable
fun CategoryCell(
    category: String,
    isDragging: Boolean,
    modifier: Modifier,
    handlerModifier: Modifier,
    onClick: () -> Unit,
) {

    val elevation by animateDpAsState(if (isDragging) 16.dp else 0.dp, label = "elevationAnimation")

    Surface(
        shape = AppTheme.cardShape,
        color = LocalAppColors.current.lightBg,
        elevation = elevation,
        onClick = onClick,
        modifier = modifier
    ) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Icon(
                imageVector = vectorResource(Res.drawable.ic_drag_handle),
                contentDescription = null,
                tint = LocalAppColors.current.dark,
                modifier = handlerModifier.size(20.dp)
            )

            Text(
                text = category,
            )
        }
    }
}