package com.kevlina.budgetplus.feature.freeze

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.book_selection
import budgetplus.core.common.generated.resources.cta_confirm
import budgetplus.core.common.generated.resources.ic_arrow_drop_down
import budgetplus.core.common.generated.resources.premium_unlock
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppDialog
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Button
import com.kevlina.budgetplus.core.ui.DropdownItem
import com.kevlina.budgetplus.core.ui.DropdownMenu
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.rippleClick
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun FreezeBookDialog(
    books: List<Book>,
    unlockPremium: () -> Unit,
    activateBook: (String) -> Unit,
) {
    if (books.isEmpty()) return

    AppDialog(
        // Do not provide a way to dismiss
        onDismissRequest = {},
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight()
                .padding(all = 8.dp)
        ) {
            Text(
                //TODO: Update text
                text = "重新購買？",
                fontSize = FontSize.SemiLarge,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp
            )

            Button(onClick = unlockPremium) {
                Text(
                    text = stringResource(Res.string.premium_unlock),
                    color = LocalAppColors.current.light,
                )
            }

            Text(
                //TODO: Update text
                text = "您的帳本資料不會遺失，但您只能選擇一本帳本繼續記帳",
                fontSize = FontSize.SemiLarge,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp
            )

            var isSelectorShown by remember { mutableStateOf(false) }
            var selectedBook by remember { mutableStateOf(books.first()) }

            Box {
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
                        text = selectedBook.name,
                        fontSize = FontSize.Header,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalAppColors.current.dark
                    )

                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_arrow_drop_down),
                        contentDescription = stringResource(Res.string.book_selection),
                        tint = LocalAppColors.current.dark
                    )
                }

                DropdownMenu(
                    expanded = isSelectorShown,
                    onDismissRequest = { isSelectorShown = false },
                    offset = DpOffset(0.dp, 8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    books.forEach { book ->
                        DropdownItem(onClick = {
                            selectedBook = book
                            isSelectorShown = false
                        }) {
                            Text(
                                text = book.name,
                                color = LocalAppColors.current.dark,
                                fontSize = FontSize.SemiLarge
                            )
                        }
                    }
                }
            }

            Button(onClick = { activateBook(selectedBook.id) }) {
                Text(
                    text = stringResource(Res.string.cta_confirm),
                    color = LocalAppColors.current.light,
                )
            }
        }
    }
}

@Preview
@Composable
private fun FreezeBookDialog_Preview() = AppTheme {
    FreezeBookDialog(
        books = listOf(
            Book(id = "1", name = "Book 1"),
            Book(id = "2", name = "Book 2"),
            Book(id = "3", name = "Book 3"),
        ),
        unlockPremium = {},
        activateBook = {}
    )
}