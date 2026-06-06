package com.kevlina.budgetplus.feature.add.record.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.book_copy_categories_default
import budgetplus.core.common.generated.resources.book_copy_categories_from
import budgetplus.core.common.generated.resources.book_name_placeholder
import budgetplus.core.common.generated.resources.book_name_title
import budgetplus.core.common.generated.resources.cta_create
import budgetplus.core.common.generated.resources.ic_arrow_drop_down
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.AppDialog
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Button
import com.kevlina.budgetplus.core.ui.DropdownItem
import com.kevlina.budgetplus.core.ui.DropdownMenu
import com.kevlina.budgetplus.core.ui.FocusRequestDelay
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.TextField
import com.kevlina.budgetplus.core.ui.rippleClick
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun CreateBookDialog(
    books: List<Book>,
    onCreate: (name: String, fromBook: Book?) -> Unit,
    onDismiss: () -> Unit,
) {

    val input = rememberTextFieldState()
    val focusRequester = remember { FocusRequester() }

    // The book to copy the categories from. Null represents the Default option.
    var copyFromBook by remember { mutableStateOf<Book?>(null) }
    var isCopyFromDropdownShown by remember { mutableStateOf(false) }

    AppDialog(onDismissRequest = onDismiss) {

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            TextField(
                state = input,
                title = stringResource(Res.string.book_name_title),
                placeholder = stringResource(Res.string.book_name_placeholder),
                modifier = Modifier.focusRequester(focusRequester),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = stringResource(Res.string.book_copy_categories_from),
                    fontSize = FontSize.SemiLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .rippleClick { isCopyFromDropdownShown = true }
                            .padding(vertical = 4.dp)
                    ) {

                        Text(
                            text = copyFromBook?.name
                                ?: stringResource(Res.string.book_copy_categories_default),
                            fontSize = FontSize.SemiLarge,
                        )

                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_arrow_drop_down),
                            contentDescription = null,
                            tint = LocalAppColors.current.dark
                        )
                    }

                    DropdownMenu(
                        expanded = isCopyFromDropdownShown,
                        onDismissRequest = { isCopyFromDropdownShown = false }
                    ) {

                        DropdownItem(
                            name = stringResource(Res.string.book_copy_categories_default),
                            onClick = {
                                copyFromBook = null
                                isCopyFromDropdownShown = false
                            }
                        )

                        books.forEach { book ->
                            DropdownItem(
                                name = book.name,
                                onClick = {
                                    copyFromBook = book
                                    isCopyFromDropdownShown = false
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onCreate(input.text.trim().toString(), copyFromBook)
                    onDismiss()
                },
                enabled = input.text.isNotBlank(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {

                Text(
                    text = stringResource(Res.string.cta_create),
                    color = LocalAppColors.current.light,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(FocusRequestDelay)
        focusRequester.requestFocus()
    }
}

@Preview
@Composable
private fun CreateBookDialog_Preview() = AppTheme(themeColors = ThemeColors.Countryside) {
    CreateBookDialog(
        books = listOf(
            Book(id = "1", name = "Travel"),
            Book(id = "2", name = "Home"),
        ),
        onCreate = { _, _ -> },
        onDismiss = {}
    )
}
