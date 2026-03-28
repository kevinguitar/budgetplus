package com.kevlina.budgetplus.feature.add.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.book_create_success
import budgetplus.core.common.generated.resources.book_exceed_maximum
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.combineState
import com.kevlina.budgetplus.core.common.di.ViewModelKey
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.FREE_BOOKS_LIMIT
import com.kevlina.budgetplus.core.data.PREMIUM_BOOKS_LIMIT
import com.kevlina.budgetplus.core.data.remote.Book
import dev.zacsweers.metro.ContributesIntoMap
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@ViewModelKey(BookSelectorViewModel::class)
@ContributesIntoMap(ViewModelScope::class)
class BookSelectorViewModel(
    private val bookRepo: BookRepo,
    private val snackbarSender: SnackbarSender,
    private val navController: NavController<BookDest>,
    authManager: AuthManager,
) : ViewModel() {

    val book = bookRepo.bookState
    val books = bookRepo.booksState

    val createBookBtnState: StateFlow<CreateBookBtnState> = authManager.userState.combineState(
        other = bookRepo.booksState,
        scope = viewModelScope
    ) { user, books ->
        val isPremium = user?.premium == true
        val size = books.orEmpty().size
        when {
            isPremium && size >= PREMIUM_BOOKS_LIMIT -> CreateBookBtnState.ReachedMax
            !isPremium && size >= FREE_BOOKS_LIMIT -> CreateBookBtnState.NeedPremium
            else -> CreateBookBtnState.Enabled
        }
    }

    fun selectBook(book: Book) {
        viewModelScope.launch { bookRepo.selectBook(book) }
    }

    fun createBook(name: String) {
        viewModelScope.launch {
            try {
                bookRepo.createBook(name = name, source = "selector")
                snackbarSender.send(getString(Res.string.book_create_success, name))
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    fun unlockPremium() {
        navController.navigate(BookDest.UnlockPremium)
    }

    fun showReachedMaxMessage() {
        viewModelScope.launch { snackbarSender.send(Res.string.book_exceed_maximum) }
    }
}