package com.kevlina.budgetplus.feature.add.record.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.withTypographyScale
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.core.ui.bubble.BubbleShape
import com.kevlina.budgetplus.core.ui.rippleClick
import com.kevlina.budgetplus.feature.add.record.SelectedCurrency
import kotlinx.coroutines.launch

private const val CURRENCY_DRAG_THRESHOLD_PX = 24f
private const val DISABLED_ALPHA = 0.38f

/**
 * A vertical, wheel-like toggle between the preferred currency (top) and the book's currency (bottom).
 *
 * The currently [selectedCurrency] is snapped into the center of the viewport while the other
 * currency peeks in from the neighboring edge. The toggle fills the field's full height so the
 * peeking symbol reaches the top/bottom edge. The user can drag vertically to switch the
 * selection (scroll up to select the book's currency, scroll down to select the preferred
 * currency), and any selection driven by the view model is reflected with a spring animation.
 *
 * When there is no [preferredCurrencySymbol] we simply render the book's symbol without the toggle.
 */
@Composable
internal fun CurrencySelector(
    bookCurrencySymbol: String,
    preferredCurrencySymbol: String?,
    selectedCurrency: SelectedCurrency,
    onBookCurrencyClick: () -> Unit,
    onPreferredCurrencyClick: () -> Unit,
    highlightCurrencyToggle: (BubbleDest) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Derive the item height from the actual rendered symbols instead of hardcoding it, so it always
    // matches the currency symbol's line height (respecting the typography scale). We measure the
    // real symbols (not "0") to capture the tallest glyph.
    val textMeasurer = rememberTextMeasurer()
    val symbolStyle = TextStyle(
        fontSize = FontSize.Header.withTypographyScale(),
        fontWeight = FontWeight.SemiBold,
    )
    val itemHeightPx = remember(textMeasurer, symbolStyle, bookCurrencySymbol, preferredCurrencySymbol) {
        listOfNotNull(bookCurrencySymbol, preferredCurrencySymbol)
            .maxOf { textMeasurer.measure(text = it, style = symbolStyle).size.height }
            .toFloat()
    }
    val density = LocalDensity.current
    val itemHeight = with(density) { itemHeightPx.toDp() }

    if (preferredCurrencySymbol == null) {
        CurrencySymbol(
            symbol = bookCurrencySymbol,
            selected = true,
            itemHeight = itemHeight,
            onClick = onBookCurrencyClick,
            modifier = modifier,
        )
        return
    }

    val scope = rememberCoroutineScope()

    val bubbleExtendHeight = with(density) { 8.dp.toPx() }
    val bubbleExtendWidth = with(density) { 16.dp.toPx() }
    val bubbleShape = with(LocalDensity.current) {
        BubbleShape.RoundedRect(AppTheme.cornerRadius.toPx())
    }

    // Fill the field's full height so the neighboring symbol peeks all the way to the top/bottom
    // edge instead of leaving padding from the TextField's vertical centering.
    Box(
        modifier = modifier
            .fillMaxHeight()
            // Clip the wheel so the neighboring symbol only peeks in from the top/bottom edge,
            // but keep horizontal room so the borderless ripple isn't cropped, and expand the
            // vertical clip when the symbol is taller than the container (e.g. large system font
            // scales) so the selected symbol is never cropped at the bottom.
            .drawWithContent {
                val extraVertical = ((itemHeightPx - size.height) / 2f).coerceAtLeast(0f)
                val horizontalMargin = maxOf(size.height, itemHeightPx)
                clipRect(
                    left = -horizontalMargin,
                    top = -extraVertical,
                    right = size.width + horizontalMargin,
                    bottom = size.height + extraVertical,
                ) {
                    this@drawWithContent.drawContent()
                }
            }
            .onPlaced {
                highlightCurrencyToggle(
                    BubbleDest.ScrollToSelectCurrency(
                        size = IntSize(
                            it.size.width + bubbleExtendWidth.toInt(),
                            it.size.height + bubbleExtendHeight.toInt()
                        ),
                        offset = {
                            it.positionInRoot() - Offset(
                                x = bubbleExtendWidth / 2,
                                y = bubbleExtendHeight / 2
                            )
                        },
                        shape = bubbleShape
                    )
                )
            },
    ) {
        val bookOffset = 0f
        fun targetOffset(currency: SelectedCurrency): Float = when (currency) {
            SelectedCurrency.Book -> bookOffset
            SelectedCurrency.Preferred -> itemHeightPx
        }

        val offset = remember { Animatable(targetOffset(selectedCurrency)) }

        // Animate to the selection whenever it changes, including when set by the view model.
        val hapticFeedback = LocalHapticFeedback.current
        var isInitialCurrency by remember { mutableStateOf(true) }
        LaunchedEffect(selectedCurrency) {
            // Always vibrate on a currency switch, regardless of the vibrateOnInput setting.
            if (isInitialCurrency) {
                isInitialCurrency = false
            } else {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
            }
            offset.animateTo(targetOffset(selectedCurrency))
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .pointerInput(selectedCurrency) {
                    var totalDrag = 0f
                    detectVerticalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragEnd = {
                            when {
                                // Dragging down reveals the preferred symbol sitting above.
                                totalDrag >= CURRENCY_DRAG_THRESHOLD_PX &&
                                    selectedCurrency == SelectedCurrency.Book -> onPreferredCurrencyClick()

                                // Dragging up reveals the book symbol sitting below.
                                totalDrag <= -CURRENCY_DRAG_THRESHOLD_PX &&
                                    selectedCurrency == SelectedCurrency.Preferred -> onBookCurrencyClick()

                                // Not enough drag to switch, settle back to the current selection.
                                else -> scope.launch { offset.animateTo(targetOffset(selectedCurrency)) }
                            }
                        },
                    ) { _, dragAmount ->
                        totalDrag += dragAmount
                        scope.launch {
                            offset.snapTo((offset.value + dragAmount).coerceIn(bookOffset, itemHeightPx))
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            CurrencySymbol(
                symbol = preferredCurrencySymbol,
                selected = selectedCurrency == SelectedCurrency.Preferred,
                itemHeight = itemHeight,
                onClick = onPreferredCurrencyClick,
                modifier = Modifier.graphicsLayer { translationY = offset.value - itemHeightPx },
            )

            CurrencySymbol(
                symbol = bookCurrencySymbol,
                selected = selectedCurrency == SelectedCurrency.Book,
                itemHeight = itemHeight,
                onClick = onBookCurrencyClick,
                modifier = Modifier.graphicsLayer { translationY = offset.value },
            )
        }
    }
}

@Composable
private fun CurrencySymbol(
    symbol: String,
    selected: Boolean,
    itemHeight: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (selected) {
            LocalAppColors.current.dark
        } else {
            LocalAppColors.current.dark.copy(alpha = DISABLED_ALPHA)
        }
    )

    Box(
        modifier = modifier
            .requiredHeight(itemHeight)
            .rippleClick(borderless = true, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            fontWeight = FontWeight.SemiBold,
            fontSize = FontSize.Header,
            color = color,
        )
    }
}

@PreviewFontScale
@Composable
private fun CurrencySelector_Preview() = AppTheme {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(LocalAppColors.current.light)
            .size(28.dp, 56.dp)
    ) {
        CurrencySelector(
            bookCurrencySymbol = "$",
            preferredCurrencySymbol = "¥",
            selectedCurrency = SelectedCurrency.Book,
            onBookCurrencyClick = {},
            onPreferredCurrencyClick = {},
            highlightCurrencyToggle = {}
        )
    }
}