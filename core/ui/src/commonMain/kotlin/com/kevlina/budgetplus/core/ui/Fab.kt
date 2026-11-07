package com.kevlina.budgetplus.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_add
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.withTypographyScale
import org.jetbrains.compose.resources.vectorResource

private val FabMargin = 24.dp
private val FabSize = 56.dp

object FabDefaults {

    /**
     *  Bottom padding for a list under a [Fab], so the last item can scroll above it.
     */
    val listBottomPadding: Dp
        @Composable get() = FabSize.withTypographyScale() + FabMargin * 2
}

@Composable
fun BoxScope.Fab(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(),
        exit = scaleOut(),
        modifier = modifier
            .align(Alignment.BottomEnd)
            // No-op when an ancestor already consumed these insets, e.g. the bottom nav.
            .windowInsetsPadding(
                WindowInsets.safeArea.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            )
            .padding(FabMargin)
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