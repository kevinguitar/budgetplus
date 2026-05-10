package com.kevlina.budgetplus.feature.overview.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.common.roundUpRatioText
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.withTypographyScale
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.core.ui.isPreview
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// Hide the text in case the ratio is below 4%
private const val TEXT_DISPLAY_PERCENT_THRESHOLD = 0.04

@Composable
internal fun PieChart(
    modifier: Modifier = Modifier,
    totalPrice: Double,
    recordGroups: Map<String, List<Record>>,
    formatPrice: (Double) -> String,
    highlightPieChart: (BubbleDest) -> Unit,
    onClick: (category: String) -> Unit,
) {

    val recordGroupSums = remember(recordGroups) {
        recordGroups.mapValues { (_, group) ->
            group.sumOf { it.price }
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        color = LocalAppColors.current.dark,
        fontSize = FontSize.Large.withTypographyScale(),
        textAlign = TextAlign.Center,
    )

    val isPreview = isPreview()
    var isDrawn by rememberSaveable(totalPrice, recordGroups) { mutableStateOf(isPreview) }
    val animateAngle by animateIntAsState(
        targetValue = if (isDrawn) 360 else 0,
        label = "animateAngle"
    )

    var canvasPositionInWindow by remember { mutableStateOf(Offset.Unspecified) }
    var tapPosition by remember { mutableStateOf<Offset?>(null) }
    var pressPosition by remember { mutableStateOf<Offset?>(null) }
    var pressCategory by remember { mutableStateOf<String?>(null) }

    val pressEffectPx = with(LocalDensity.current) { 8.dp.toPx() }
    val hintBgColor = LocalAppColors.current.light
    val animateHint by animateFloatAsState(
        targetValue = if (pressPosition == null) 0f else 1f,
        label = "animateHint"
    )

    val animateGroup = recordGroups.keys.map { key ->
        animateFloatAsState(
            targetValue = if (pressCategory == key) 1f else 0f,
            label = "animateGroup_$key"
        )
    }

    fun resetTouchHandle() {
        pressPosition = null
        pressCategory = null
    }

    SideEffect { isDrawn = true }

    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(pressCategory) {
        if (pressCategory != null) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }

    Canvas(
        modifier = modifier
            .padding(8.dp)
            .clip(CircleShape)
            .aspectRatio(1F)
            .onPlaced {
                highlightPieChart(BubbleDest.OverviewPieChart(
                    size = it.size,
                    offset = it::positionInRoot
                ))
            }
            .onGloballyPositioned { canvasPositionInWindow = it.positionInWindow() }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    tapPosition = offset
                }
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> pressPosition = offset },
                    onDrag = { change, _ -> pressPosition = change.position },
                    onDragEnd = { resetTouchHandle() },
                    onDragCancel = { resetTouchHandle() }
                )
            }
    ) {
        var startAngle = 270f

        val diameter = size.width - pressEffectPx * 2
        val drawDiameter = diameter * (animateAngle / 360f)


        // Draw the circle background
        recordGroupSums.onEachIndexed { index, (category, sum) ->
            val ratio = sum / totalPrice
            val angle = (animateAngle * ratio).toFloat()
            val isPressed = pressPosition?.isWithIn(startAngle, angle) ?: false
            if (isPressed) {
                pressCategory = category
            }

            // Consume the tap action
            tapPosition?.let { tap ->
                if (tap.isWithIn(startAngle, angle)) {
                    onClick(category)
                    tapPosition = null
                }
            }

            val animatePart = animateGroup[index].value
            val centerOffset = pressEffectPx + (diameter - drawDiameter) / 2
            val animatedOffset = centerOffset * (1 - animatePart)
            val animatedSize = diameter + (pressEffectPx * 2 * animatePart)
            drawArc(
                color = chartColorsStaggered[index % chartColorsStaggered.size],
                startAngle = startAngle,
                sweepAngle = angle,
                useCenter = true,
                topLeft = Offset(animatedOffset, animatedOffset),
                size = Size(animatedSize, animatedSize)
            )

            startAngle += angle
        }

        // Draw the labels
        recordGroupSums.onEachIndexed { index, (category, sum) ->
            val ratio = sum / totalPrice
            val angle = (animateAngle * ratio).toFloat()

            if (animateAngle >= 270 && ratio >= TEXT_DISPLAY_PERCENT_THRESHOLD) {
                val text = "$category\n${formatPrice(sum)}"
                val textMeasure = textMeasurer.measure(text, style = textStyle)

                val animatePart = animateGroup[index].value
                val textRadius = center.x * (0.5 + 0.05 * animatePart)
                val textAngle = startAngle + (angle / 2)

                val deltaX = textRadius * cos(textAngle.toRadians())
                val deltaY = textRadius * sin(textAngle.toRadians())

                drawText(
                    textMeasurer = textMeasurer,
                    text = text,
                    style = textStyle,
                    topLeft = Offset(
                        x = center.x + deltaX.toFloat() - (textMeasure.size.width / 2),
                        y = center.y + deltaY.toFloat() - (textMeasure.size.height / 2)
                    )
                )
            }

            startAngle += angle
        }

        if (pressCategory != null) {
            // Draw the hint area
            drawCircle(
                color = hintBgColor,
                radius = diameter / 8,
                alpha = animateHint
            )

            val sum = recordGroupSums[pressCategory] ?: 0.0
            val percent = sum / totalPrice
            val text = if (percent >= TEXT_DISPLAY_PERCENT_THRESHOLD) {
                "${(percent * 100).roundUpRatioText}%"
            } else {
                "$pressCategory\n${formatPrice(sum)}"
            }
            val textMeasure = textMeasurer.measure(text, style = textStyle)

            if (animateHint > 0.7f) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = text,
                    style = textStyle,
                    topLeft = Offset(
                        x = center.x - (textMeasure.size.width / 2),
                        y = center.y - (textMeasure.size.height / 2)
                    )
                )
            }
        }
    }
}

context(scope: DrawScope)
private fun Offset.isWithIn(
    startAngle: Float,
    sweepAngle: Float,
): Boolean {
    val center = scope.size.width / 2

    val distanceToCenter = sqrt((x - center).pow(2) + (y - center).pow(2))
    if (distanceToCenter > center) {
        return false
    }

    // Calculate the angle of a tap related to center
    val angle = atan2(y - center, x - center) * 180f / PI
    val normalizedAngle = (angle + 360) % 360
    val normalizedStartAngle = (startAngle + 360) % 360

    val range = normalizedStartAngle..(normalizedStartAngle + sweepAngle)
    return normalizedAngle in range || normalizedAngle + 360 in range
}

private fun Float.toRadians(): Double = this / 180.0 * PI

@Preview
@Composable
private fun PieChart_Preview() = AppTheme {
    PieChart(
        modifier = Modifier.size(300.dp),
        totalPrice = OverviewListState.totalPricePreview,
        recordGroups = OverviewListState.recordGroupsPreview,
        formatPrice = { it.toString() },
        highlightPieChart = {},
        onClick = {}
    )
}