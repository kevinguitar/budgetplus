package com.kevlina.budgetplus.feature.add.record.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.book_name_placeholder
import budgetplus.core.common.generated.resources.book_name_title
import budgetplus.core.common.generated.resources.book_selection
import budgetplus.core.common.generated.resources.cta_create
import budgetplus.core.common.generated.resources.ic_arrow_drop_down
import budgetplus.core.common.generated.resources.ic_check
import budgetplus.core.common.generated.resources.ic_lock
import budgetplus.core.common.generated.resources.menu_create_book
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.DropdownDivider
import com.kevlina.budgetplus.core.ui.DropdownItem
import com.kevlina.budgetplus.core.ui.DropdownMenu
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.InputDialog
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.rippleClick
import com.kevlina.budgetplus.core.utils.metroViewModel
import com.kevlina.budgetplus.feature.add.record.BookSelectorViewModel
import com.kevlina.budgetplus.feature.add.record.CreateBookBtnState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun BookSelector(navController: NavController<BookDest>) {

    val viewModel = metroViewModel<BookSelectorViewModel>()

    val bookState by viewModel.book.collectAsStateWithLifecycle()
    val booksState by viewModel.books.collectAsStateWithLifecycle()
    val createBookBtnState by viewModel.createBookBtnState.collectAsStateWithLifecycle()

    var isSelectorShown by remember { mutableStateOf(false) }
    var isBookCreationDialogShown by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .rippleClick(
                    color = LocalAppColors.current.light,
                    onClick = { isSelectorShown = true }
                )
                .padding(vertical = 8.dp)
        ) {

            Text(
                text = bookState?.name.orEmpty(),
                fontSize = FontSize.Header,
                fontWeight = FontWeight.SemiBold,
                color = LocalAppColors.current.light
            )

            Icon(
                imageVector = vectorResource(Res.drawable.ic_arrow_drop_down),
                contentDescription = stringResource(Res.string.book_selection),
                tint = LocalAppColors.current.light
            )
        }

        DropdownMenu(
            expanded = isSelectorShown,
            onDismissRequest = { isSelectorShown = false },
            offset = DpOffset(0.dp, 8.dp),
            modifier = Modifier.heightIn(max = 300.dp)
        ) {

            booksState.orEmpty().forEach { book ->

                DropdownItem(onClick = {
                    viewModel.selectBook(book)
                    isSelectorShown = false
                }) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Text(
                            text = book.name,
                            color = LocalAppColors.current.dark,
                            fontSize = FontSize.SemiLarge
                        )

                        if (bookState?.id == book.id) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.ic_check),
                                contentDescription = null,
                                tint = LocalAppColors.current.dark,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            DropdownDivider()

            DropdownItem(
                name = stringResource(Res.string.menu_create_book),
                icon = if (createBookBtnState != CreateBookBtnState.Enabled) {
                    vectorResource(Res.drawable.ic_lock)
                } else {
                    null
                },
                onClick = {
                    when (createBookBtnState) {
                        CreateBookBtnState.Enabled -> isBookCreationDialogShown = true
                        CreateBookBtnState.NeedPremium -> navController.navigate(BookDest.UnlockPremium)
                        CreateBookBtnState.ReachedMax -> viewModel.showReachedMaxMessage()
                    }
                    isSelectorShown = false
                }
            )
        }

    }

    if (isBookCreationDialogShown) {
        InputDialog(
            buttonText = stringResource(Res.string.cta_create),
            title = stringResource(Res.string.book_name_title),
            placeholder = stringResource(Res.string.book_name_placeholder),
            onButtonClicked = viewModel::createBook,
            onDismiss = { isBookCreationDialogShown = false }
        )
    }
}