package com.kevlina.budgetplus.core.ui.bubble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.export_cta
import budgetplus.core.common.generated.resources.ic_file_download
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.MenuAction
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.clickableWithoutRipple
import com.kevlina.budgetplus.core.ui.isPreview
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun Bubble(
    dest: BubbleDest?,
    dismissBubble: () -> Unit,
) {
    val isPreview = isPreview()
    var isBubbleVisible by remember { mutableStateOf(isPreview) }
    var textSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(key1 = dest) {
        isBubbleVisible = dest != null
    }

    dest ?: return

    // If the offset is no longer available, dismiss the current bubble
    val offset = dest.getOffsetSafe()
    if (offset == null) {
        isBubbleVisible = false
        dismissBubble()
        return
    }

    AnimatedVisibility(
        visible = isBubbleVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickableWithoutRipple {
                    isBubbleVisible = false
                    dismissBubble()
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(HollowShape(dest = dest))
                    .background(color = LocalAppColors.current.dark.copy(alpha = 0.8F))
            )

            Text(
                text = stringResource(dest.textRes),
                modifier = Modifier
                    .align(
                        when (dest.textDirection) {
                            BubbleTextDirection.TopStart, BubbleTextDirection.BottomStart -> Alignment.TopStart
                            BubbleTextDirection.TopEnd, BubbleTextDirection.BottomEnd -> Alignment.TopEnd
                            BubbleTextDirection.TopCenter, BubbleTextDirection.BottomCenter -> Alignment.TopCenter
                        }
                    )
                    .offset(
                        x = 0.dp,
                        y = with(LocalDensity.current) {
                            when (dest.textDirection) {
                                BubbleTextDirection.TopStart,
                                BubbleTextDirection.TopEnd,
                                BubbleTextDirection.TopCenter,
                                    -> offset.y.toDp() - textSize.height.toDp()

                                BubbleTextDirection.BottomStart,
                                BubbleTextDirection.BottomEnd,
                                BubbleTextDirection.BottomCenter,
                                    -> offset.y.toDp() + dest.size.height.toDp()
                            }
                        }
                    )
                    .onPlaced { textSize = it.size }
                    .padding(all = 8.dp)
                    .background(
                        color = LocalAppColors.current.light,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(all = 16.dp)
            )
        }
    }
}

private class HollowShape(private val dest: BubbleDest) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {

        val screenRectPath = Path().apply {
            addRect(
                Rect(
                    offset = Offset.Zero,
                    size = size
                )
            )
        }

        val ovalPath = Path().apply {

            val rect = Rect(
                offset = dest.getOffsetSafe() ?: return@apply,
                size = dest.size.toSize()
            )

            when (val shape = dest.shape) {
                BubbleShape.Circle -> addOval(rect)
                is BubbleShape.RoundedRect -> addRoundRect(
                    RoundRect(rect, CornerRadius(shape.corner))
                )
            }
        }

        return Outline.Generic(Path().apply {
            op(screenRectPath, ovalPath, PathOperation.Difference)
        })
    }
}

@Preview(widthDp = 360, heightDp = 240)
@Composable
private fun Bubble_Preview(
    @PreviewParameter(BubbleTextDirectionParams::class)
    textDirection: BubbleTextDirection,
) = AppTheme(ThemeColors.Lavender) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        MenuAction(
            imageVector = vectorResource(Res.drawable.ic_file_download),
            description = stringResource(Res.string.export_cta),
            onClick = {},
            modifier = Modifier
                .align(Alignment.Center)
                .onPlaced {
                    size = it.size
                    offset = it.positionInRoot()
                }
        )

        Bubble(
            dest = BubbleDest.OverviewExport(
                size = size,
                offset = { offset },
                textDirection = textDirection
            ),
            dismissBubble = {}
        )
    }
}

// This could fail if the element is no longer attached to the screen.
private fun BubbleDest.getOffsetSafe(): Offset? = try {
    offset()
} catch (e: Exception) {
    Logger.d(e) { "Fail to retrieve offset for ${key}, skip showing" }
    null
}

private class BubbleTextDirectionParams : PreviewParameterProvider<BubbleTextDirection> {
    override val values = BubbleTextDirection.entries.asSequence()
}