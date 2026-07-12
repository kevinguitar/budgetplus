package com.kevlina.budgetplus.feature.add.record.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_currency_exchange
import budgetplus.core.common.generated.resources.record_currency_exchange
import com.kevlina.budgetplus.core.lottie.PremiumCrown
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.withTypographyScale
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.DatePickerDialog
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.SingleDatePicker
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.TextField
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.core.ui.bubble.BubbleShape
import com.kevlina.budgetplus.core.ui.rippleClick
import com.kevlina.budgetplus.feature.add.record.CalculatorViewModel
import com.kevlina.budgetplus.feature.add.record.RecordDateState
import com.kevlina.budgetplus.feature.add.record.SelectedCurrency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun DateAndPricing(
    state: DateAndPricingState,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val recordDate by state.recordDate.collectAsStateWithLifecycle()
    val currencySymbol by state.currencySymbol.collectAsStateWithLifecycle()
    val preferredCurrencySymbol by state.preferredCurrencySymbol.collectAsStateWithLifecycle()
    val selectedCurrency by state.selectedCurrency.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    if (state.scrollable) {
        val priceText = state.priceText.text
        LaunchedEffect(key1 = priceText) {
            if (priceText != CalculatorViewModel.EMPTY_PRICE) {
                keyboardController?.hide()

                if (scrollState.value != scrollState.maxValue) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            SingleDatePicker(
                date = recordDate.date,
                modifier = Modifier
                    .rippleClick { showDatePicker = true }
                    .padding(vertical = 8.dp)
            )

            // A little hack to scroll price text automatically to the end while typing,
            // this is because price text is read-only and doesn't take the focus from the UI tree.
            val priceTextScrollState = rememberScrollState()
            LaunchedEffect(priceTextScrollState.maxValue) {
                if (priceTextScrollState.maxValue > 0) {
                    priceTextScrollState.animateScrollTo(priceTextScrollState.maxValue)
                }
            }

            TextField(
                state = state.priceText,
                fontSize = FontSize.Header,
                letterSpacing = 0.5.sp,
                readOnly = true,
                scrollState = priceTextScrollState,
                modifier = Modifier.weight(1F),
                leadingContent = {
                    CurrencyToggle(
                        bookCurrencySymbol = currencySymbol,
                        preferredCurrencySymbol = preferredCurrencySymbol,
                        selectedCurrency = selectedCurrency,
                        onBookCurrencyClick = state.onBookCurrencyClick,
                        onPreferredCurrencyClick = state.onPreferredCurrencyClick,
                        highlightCurrencyToggle = state.highlightCurrencyToggle,
                    )
                }
            )
        }

        val isPremium by state.isPremium.collectAsStateWithLifecycle()
        val convertedPrice = state.convertedPrice.collectAsStateWithLifecycle().value

        if (convertedPrice != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.End)
                    .rippleClick(onClick = state.editPreferredCurrency)
                    .padding(all = 8.dp),
            ) {
                if (isPremium) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_currency_exchange),
                        tint = LocalAppColors.current.dark,
                        size = 20.dp
                    )
                    Text(text = convertedPrice)
                } else {
                    PremiumCrown(modifier = Modifier.size(24.dp))
                    Text(text = stringResource(Res.string.record_currency_exchange))
                }
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (showDatePicker) {
            DatePickerDialog(
                date = recordDate.date,
                onDatePicked = state.setDate,
                onDismiss = { showDatePicker = false }
            )
        }
    }
}

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
private fun CurrencyToggle(
    bookCurrencySymbol: String,
    preferredCurrencySymbol: String?,
    selectedCurrency: SelectedCurrency,
    onBookCurrencyClick: () -> Unit,
    onPreferredCurrencyClick: () -> Unit,
    highlightCurrencyToggle: (BubbleDest) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Derive the item height from the actual rendered text instead of hardcoding it, so it always
    // matches the currency symbol's line height (respecting the typography scale).
    val textMeasurer = rememberTextMeasurer()
    val symbolStyle = TextStyle(
        fontSize = FontSize.Header.withTypographyScale(),
        fontWeight = FontWeight.SemiBold,
    )
    val itemHeightPx = remember(textMeasurer, symbolStyle) {
        textMeasurer.measure(text = "0", style = symbolStyle).size.height.toFloat()
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
    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .clipToBounds()
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
        val containerPx = constraints.maxHeight.toFloat()

        // The translation applied to the stacked column so the selected item lands in the center.
        // Column order is [Preferred, Book], so the preferred symbol sits on top.
        val preferredOffset = (containerPx - itemHeightPx) / 2f
        val bookOffset = preferredOffset - itemHeightPx
        fun targetOffset(currency: SelectedCurrency): Float =
            if (currency == SelectedCurrency.Book) bookOffset else preferredOffset

        val offset = remember { Animatable(targetOffset(selectedCurrency)) }

        // Animate to the selection whenever it changes, including when set by the view model.
        LaunchedEffect(selectedCurrency) {
            offset.animateTo(targetOffset(selectedCurrency))
        }

        Column(
            modifier = Modifier
                .graphicsLayer { translationY = offset.value }
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
                            offset.snapTo((offset.value + dragAmount).coerceIn(bookOffset, preferredOffset))
                        }
                    }
                },
        ) {
            CurrencySymbol(
                symbol = preferredCurrencySymbol,
                selected = selectedCurrency == SelectedCurrency.Preferred,
                itemHeight = itemHeight,
                onClick = onPreferredCurrencyClick,
            )

            CurrencySymbol(
                symbol = bookCurrencySymbol,
                selected = selectedCurrency == SelectedCurrency.Book,
                itemHeight = itemHeight,
                onClick = onBookCurrencyClick,
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
            .height(itemHeight)
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

private const val CURRENCY_DRAG_THRESHOLD_PX = 24f
private const val DISABLED_ALPHA = 0.38f

@Stable
internal class DateAndPricingState(
    val recordDate: StateFlow<RecordDateState>,
    val currencySymbol: StateFlow<String>,
    val preferredCurrencySymbol: StateFlow<String?>,
    val selectedCurrency: StateFlow<SelectedCurrency>,
    val priceText: TextFieldState,
    val isPremium: StateFlow<Boolean>,
    val convertedPrice: StateFlow<String?>,
    val scrollable: Boolean,
    val setDate: (LocalDate) -> Unit,
    val onBookCurrencyClick: () -> Unit,
    val onPreferredCurrencyClick: () -> Unit,
    val editPreferredCurrency: () -> Unit,
    val highlightCurrencyToggle: (BubbleDest) -> Unit,
) {
    companion object {
        val preview = DateAndPricingState(
            recordDate = MutableStateFlow(RecordDateState.Now),
            currencySymbol = MutableStateFlow("$"),
            preferredCurrencySymbol = MutableStateFlow("¥"),
            selectedCurrency = MutableStateFlow(SelectedCurrency.Book),
            priceText = TextFieldState("2344"),
            isPremium = MutableStateFlow(true),
            convertedPrice = MutableStateFlow("USD100"),
            scrollable = false,
            setDate = {},
            onBookCurrencyClick = {},
            onPreferredCurrencyClick = {},
            editPreferredCurrency = {},
            highlightCurrencyToggle = {}
        )
    }
}

@Preview
@Composable
private fun DateAndPricing_Preview() = AppTheme {
    DateAndPricing(
        state = DateAndPricingState.preview,
        scrollState = rememberScrollState(),
        modifier = Modifier
            .background(LocalAppColors.current.light)
            .padding(horizontal = 16.dp)
    )
}