package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_search
import com.kevlina.budgetplus.core.theme.LocalAppColors
import org.jetbrains.compose.resources.vectorResource

@Composable
fun SearchField(
    keyword: TextFieldState,
    modifier: Modifier = Modifier,
    hint: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Done
    ),
    onDone: (() -> Unit)? = null,
) {
    TextField(
        state = keyword,
        modifier = modifier,
        placeholder = hint,
        fontSize = FontSize.Large,
        keyboardOptions = keyboardOptions,
        onDone = onDone,
        leadingContent = {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_search),
                tint = LocalAppColors.current.dark,
            )
        }
    )
}

@Preview
@Composable
private fun SearchField_Preview() = AppTheme {
    SearchField(
        keyword = TextFieldState(),
        hint = "USD"
    )
}