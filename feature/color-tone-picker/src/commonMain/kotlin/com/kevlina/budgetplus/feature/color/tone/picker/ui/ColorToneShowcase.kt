package com.kevlina.budgetplus.feature.color.tone.picker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 *  @param isVertical stack the colors from top to bottom instead of from left to right.
 */
@Composable
internal fun ColorToneShowcase(
    colors: List<Color>,
    outlineColor: Color,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false,
) {

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .border(
                width = 1.dp,
                color = outlineColor,
                shape = RoundedCornerShape(ColorToneConstants.CardCornerRadius)
            )
    ) {
        // Lay out the bands along the main axis, and fill the cross axis.
        val mainSize = if (isVertical) size.height else size.width
        val crossSize = if (isVertical) size.width else size.height
        val itemSize = mainSize / colors.size
        val cornerRadiusPx = CornerRadius(ColorToneConstants.CardCornerRadius.toPx())

        fun offsetAt(main: Float) = if (isVertical) Offset(x = 0F, y = main) else Offset(x = main, y = 0F)
        fun bandOf(main: Float) = if (isVertical) Size(width = crossSize, height = main) else Size(width = main, height = crossSize)

        colors.forEachIndexed { index, color ->
            when (index) {
                // Round the outer corners of the first band, the next band covers its inner corners.
                0 -> drawRoundRect(
                    color = color,
                    topLeft = Offset.Zero,
                    size = bandOf(itemSize * 2),
                    cornerRadius = cornerRadiusPx,
                )

                colors.lastIndex -> {
                    drawRoundRect(
                        color = color,
                        topLeft = offsetAt(itemSize * (colors.size - 1)),
                        size = bandOf(itemSize),
                        cornerRadius = cornerRadiusPx,
                    )

                    // To cover the inner part of the corners
                    drawRect(
                        color = color,
                        topLeft = offsetAt(itemSize * (colors.size - 1)),
                        size = bandOf(itemSize / 2),
                    )
                }

                else -> drawRect(
                    color = color,
                    topLeft = offsetAt(itemSize * index),
                    size = bandOf(itemSize),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ColorToneCard_Preview() = ColorToneShowcase(
    colors = listOf(Color.Blue, Color.Cyan, Color.DarkGray),
    outlineColor = Color.Black,
    modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
)

@Preview
@Composable
private fun ColorToneCardVertical_Preview() = ColorToneShowcase(
    colors = listOf(Color.Blue, Color.Cyan, Color.DarkGray),
    outlineColor = Color.Black,
    isVertical = true,
    modifier = Modifier
        .width(160.dp)
        .height(300.dp)
)
