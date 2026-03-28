package com.kevlina.budgetplus.feature.welcome

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.book_create_success
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Toaster
import com.kevlina.budgetplus.core.common.di.ViewModelKey
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import dev.zacsweers.metro.ContributesIntoMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@ViewModelKey(WelcomeViewModel::class)
@ContributesIntoMap(ViewModelScope::class)
class WelcomeViewModel(
    private val snackbarSender: SnackbarSender,
    private val bookRepo: BookRepo,
    private val authManager: AuthManager,
    private val toaster: Toaster,
    private val navController: NavController<BookDest>,
) : ViewModel() {

    private var createBookJob: Job? = null

    val bookName = TextFieldState()

    val isCreatingBook: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            bookRepo.booksState.collect { books ->
                if (!books.isNullOrEmpty()) {
                    navController.selectRootAndClearAll(BookDest.Record)
                }
            }
        }
    }

    fun createBook() {
        if (createBookJob?.isActive == true) {
            return
        }

        createBookJob = viewModelScope.launch {
            isCreatingBook.value = true
            val name = bookName.text.toString()
            try {
                bookRepo.createBook(name = name, source = "welcome")
                toaster.showMessage(getString(Res.string.book_create_success, name))
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            } finally {
                isCreatingBook.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authManager.logout()
        }
    }
}