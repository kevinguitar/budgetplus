package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.kevlina.budgetplus.core.adaptive.WindowSizeClass
import com.kevlina.budgetplus.core.adaptive.WindowSizeClass.Size
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.LocalTypographyScale
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.theme.TypographyScale
import com.kevlina.budgetplus.core.ui.AppTheme.containerMaxWidth

@Composable
fun AppTheme(
    themeColors: ThemeColors = ThemeColors.MilkTea,
    content: @Composable () -> Unit,
) {
    val windowSize = WindowSizeClass.calculate()
    val typographyScale = when {
        windowSize.height == Size.Compat -> TypographyScale(1.0F)
        windowSize.width == Size.Compat -> TypographyScale(1.0F)
        windowSize.width == Size.Medium -> TypographyScale(1.2F)
        windowSize.width == Size.Expanded -> TypographyScale(1.35F)
        else -> TypographyScale(1.0F)
    }

    CompositionLocalProvider(
        LocalAppColors provides themeColors,
        LocalTypographyScale provides typographyScale,
        content = content
    )
}

object AppTheme {

    val containerMaxWidth: Dp get() = 680.dp
    val dialogShape: Shape get() = RoundedCornerShape(cornerRadius)
    val cardShape: Shape get() = RoundedCornerShape(cornerRadius)
    val cornerRadius: Dp get() = 12.dp
    val minSurfaceSize: Dp get() = 48.dp

    const val DIVIDER_ALPHA: Float = 0.4F

    /**
     *  Useful in LazyList, to make sure the entire container is scrollable, but still limit
     *  the children within the [containerMaxWidth].
     */
    context(scope: BoxWithConstraintsScope)
    fun listContentPaddings(
        all: Dp = 0.dp,
        horizontal: Dp = all,
        vertical: Dp = all,
    ): PaddingValues {
        val containerHorizontalPadding = (scope.maxWidth - containerMaxWidth).coerceAtLeast(0.dp) / 2
        return PaddingValues(
            horizontal = containerHorizontalPadding + horizontal,
            vertical = vertical
        )
    }
}

/**
 *  Apply this modifier after vertical scroll will ensure the padding part is also scrollable.
 */
fun Modifier.containerPadding(): Modifier = this then ContainerPaddingElement()


private class ContainerPaddingElement : ModifierNodeElement<ContainerPaddingNode>() {

    override fun create() = ContainerPaddingNode()

    override fun update(node: ContainerPaddingNode) = Unit

    override fun equals(other: Any?): Boolean {
        return other is ContainerPaddingElement
    }

    override fun hashCode(): Int = 394

    override fun InspectorInfo.inspectableProperties() {
        name = "containerPadding"
    }
}

private class ContainerPaddingNode : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val maxWidthPx = constraints.maxWidth.takeIf { it != Constraints.Infinity } ?: 0
        val containerWidthPx = AppTheme.containerMaxWidth.roundToPx()

        val horizontalPaddingPx = (maxWidthPx - containerWidthPx).coerceAtLeast(0)
        val startPaddingPx = horizontalPaddingPx / 2

        val placeable = measurable.measure(constraints.offset(-horizontalPaddingPx, 0))

        val width = constraints.constrainWidth(placeable.width + horizontalPaddingPx)
        val height = constraints.constrainHeight(placeable.height)
        return layout(width, height) {
            placeable.placeRelative(startPaddingPx, 0)
        }
    }
}