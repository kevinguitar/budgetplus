package com.kevlina.budgetplus.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_add
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.withTypographyScale
import org.jetbrains.compose.resources.vectorResource

private val FabMargin = 24.dp
private val FabSize = 56.dp

@Composable
fun BoxScope.Fab(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    // The safe area that's left after the ancestors handled theirs, e.g. the bottom nav handles the
    // bottom insets, and the side bars handle the insets on their side.
    var consumedInsets by remember { mutableStateOf(WindowInsets(0, 0, 0, 0)) }
    val remainingInsets = WindowInsets.safeArea.exclude(consumedInsets)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    // The margin, or the remaining safe area if it's larger, e.g. above the home indicator.
    val padding = with(density) {
        PaddingValues.Absolute(
            left = max(FabMargin, remainingInsets.getLeft(density, layoutDirection).toDp()),
            right = max(FabMargin, remainingInsets.getRight(density, layoutDirection).toDp()),
            bottom = max(FabMargin, remainingInsets.getBottom(density).toDp()),
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(),
        exit = scaleOut(),
        modifier = modifier
            .align(Alignment.BottomEnd)
            .onConsumedWindowInsetsChanged { consumedInsets = it }
            .padding(padding)
    ) {
        Surface(
            shape = CircleShape,
            color = LocalAppColors.current.dark,
            onClick = onClick,
            modifier = Modifier.size(FabSize.withTypographyScale())
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = LocalAppColors.current.light,
                modifier = Modifier.padding(all = 8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun Fab_Preview() = AppTheme {
    Box {
        Fab(
            icon = vectorResource(Res.drawable.ic_add),
            contentDescription = "Add",
            onClick = {}
        )
    }
}