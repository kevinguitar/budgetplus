package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.LocalTypographyScale
import com.kevlina.budgetplus.core.theme.ThemeColors
import kotlin.time.Duration.Companion.milliseconds

internal const val PLACEHOLDER_ALPHA = 0.5F

// Delay for a short amount of time to let keyboard actually show up.
val FocusRequestDelay = 10.milliseconds

@Composable
fun TextField(
    state: TextFieldState,
    title: String,
    modifier: Modifier = Modifier,
    onTitleClick: (() -> Unit)? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    fontSize: TextUnit = FontSize.Normal,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Done
    ),
    scrollState: ScrollState = rememberScrollState(),
    onDone: (() -> Unit)? = null,
) {
    val scaledFontSize = fontSize * LocalTypographyScale.current.scale
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(56.dp * LocalTypographyScale.current.scale)
            .background(
                color = LocalAppColors.current.lightBg,
                shape = RoundedCornerShape(AppTheme.cornerRadius)
            )
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaledFontSize,
            modifier = Modifier.thenIfNotNull(onTitleClick) {
                Modifier.rippleClick(
                    borderless = true,
                    onClick = it
                )
            }
        )

        BasicTextField(
            state = state,
            enabled = enabled,
            readOnly = readOnly,
            scrollState = scrollState,
            modifier = Modifier.weight(1F),
            textStyle = TextStyle(
                color = LocalAppColors.current.dark,
                textAlign = TextAlign.End,
                fontSize = scaledFontSize,
                letterSpacing = letterSpacing
            ),
            keyboardOptions = keyboardOptions,
            onKeyboardAction = if (onDone == null) {
                null
            } else {
                { onDone.invoke() }
            },
            lineLimits = if (singleLine) {
                TextFieldLineLimits.SingleLine
            } else {
                TextFieldLineLimits.MultiLine()
            },
            cursorBrush = SolidColor(LocalAppColors.current.dark),
            // Cannot use lambda because of https://youtrack.jetbrains.com/issue/KT-84055/Reference-to-lambda-in-lambda-in-function-TextField-can-not-be-evaluated
            decorator = object : TextFieldDecorator {
                @Composable
                override fun Decoration(innerTextField: @Composable () -> Unit) {
                    if (state.text.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            textAlign = TextAlign.End,
                            fontSize = scaledFontSize,
                            modifier = Modifier.alpha(PLACEHOLDER_ALPHA)
                        )
                    }

                    innerTextField()
                }
            }
        )
    }
}

@Preview
@Composable
private fun TextField_Preview() = AppTheme(themeColors = ThemeColors.Barbie) {
    val input = rememberTextFieldState("")
    TextField(
        state = input,
        title = "Username",
        placeholder = "Your username",
    )
}