package com.kevlina.budgetplus.feature.welcome

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.book_create_success
import budgetplus.core.common.generated.resources.book_join_success
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Toaster
import com.kevlina.budgetplus.core.common.di.ViewModelKey
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import com.kevlina.budgetplus.core.common.nav.NavigationAction
import com.kevlina.budgetplus.core.common.nav.NavigationFlow
import com.kevlina.budgetplus.core.common.sendEvent
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.JoinBookException
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Named
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@ViewModelKey(WelcomeViewModel::class)
@ContributesIntoMap(ViewModelScope::class)
class WelcomeViewModel(
    val navigation: NavigationFlow,
    val snackbarSender: SnackbarSender,
    private val bookRepo: BookRepo,
    private val authManager: AuthManager,
    private val toaster: Toaster,
    @Named("book") private val bookNavigationAction: NavigationAction,
) : ViewModel() {

    private var createBookJob: Job? = null

    val bookName = TextFieldState()

    val isCreatingBook: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            bookRepo.bookState.collect { book ->
                if (book != null) {
                    navigation.sendEvent(bookNavigationAction)
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

    fun handleJoinRequest() {
        if (!bookRepo.hasPendingJoinRequest) return

        viewModelScope.launch {
            try {
                val bookName = bookRepo.handlePendingJoinRequest() ?: return@launch
                toaster.showMessage(getString(Res.string.book_join_success, bookName))

                navigation.sendEvent(bookNavigationAction)
            } catch (e: JoinBookException.General) {
                snackbarSender.send(e.message)
            } catch (e: JoinBookException.JoinInfoNotFound) {
                Logger.e(e) { "WelcomeViewModel: Join info not found" }
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authManager.logout()
        }
    }
}