package com.kevlina.budgetplus.feature.edit.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.category_title
import budgetplus.core.common.generated.resources.cta_add
import budgetplus.core.common.generated.resources.cta_delete
import budgetplus.core.common.generated.resources.cta_rename
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppDialog
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Button
import com.kevlina.budgetplus.core.ui.ButtonRow
import com.kevlina.budgetplus.core.ui.FocusRequestDelay
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.TextField
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditCategoryDialog(
    mode: CategoryEditMode,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {

    val currentName = (mode as? CategoryEditMode.Rename)?.currentName.orEmpty()

    val name = rememberTextFieldState(initialText = currentName)

    val focusRequester = remember { FocusRequester() }

    AppDialog(onDismissRequest = onDismiss) {

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            TextField(
                state = name,
                title = stringResource(Res.string.category_title),
                modifier = Modifier.focusRequester(focusRequester),
                onDone = {
                    if (name.text.isNotBlank() && name.text != currentName) {
                        onConfirm(name.text.trim().toString())
                        onDismiss()
                    }
                }
            )

            ButtonRow {
                if (mode is CategoryEditMode.Rename) {
                    Button(onClick = {
                        onDelete()
                        onDismiss()
                    }) {
                        Text(
                            text = stringResource(Res.string.cta_delete),
                            color = LocalAppColors.current.light,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Button(
                    onClick = {
                        onConfirm(name.text.trim().toString())
                        onDismiss()
                    },
                    enabled = name.text.isNotBlank() && name.text != currentName,
                ) {
                    val textRes = when (mode) {
                        CategoryEditMode.Add -> Res.string.cta_add
                        is CategoryEditMode.Rename -> Res.string.cta_rename
                    }
                    Text(
                        text = stringResource(textRes),
                        color = LocalAppColors.current.light,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(FocusRequestDelay)
        focusRequester.requestFocus()
    }
}

@Serializable
sealed class CategoryEditMode {

    data object Add : CategoryEditMode()

    data class Rename(val currentName: String) : CategoryEditMode()

}

@Preview
@Composable
private fun EditCategoryDialog() = AppTheme {
    Box(modifier = Modifier.fillMaxSize()) {
        EditCategoryDialog(
            mode = CategoryEditMode.Rename("My Awesome Category"),
            onConfirm = {},
            onDismiss = {},
            onDelete = {}
        )
    }
}