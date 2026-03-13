package com.kevlina.budgetplus.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.settings_book_deleted
import budgetplus.core.common.generated.resources.settings_book_left
import budgetplus.core.common.generated.resources.settings_rename_book_success
import budgetplus.core.common.generated.resources.settings_rename_user_success
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.VibratorManager
import com.kevlina.budgetplus.core.common.combineState
import com.kevlina.budgetplus.core.common.di.ViewModelKey
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import com.kevlina.budgetplus.core.common.mapState
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.settings.api.ChartModeViewModel
import dev.zacsweers.metro.ContributesIntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@ViewModelKey(SettingsViewModel::class)
@ContributesIntoMap(ViewModelScope::class)
class SettingsViewModel private constructor(
    private val bookRepo: BookRepo,
    private val authManager: AuthManager,
    private val snackbarSender: SnackbarSender,
    private val tracker: Tracker,
    val vibrator: VibratorManager,
    val chartModel: ChartModeViewModel,
    internal val navigation: SettingsNavigationViewModel,
) : ViewModel() {

    val bookName = bookRepo.bookState.mapState { it?.name }
    val isBookOwner = bookRepo.bookState.combineState(
        other = authManager.userState,
        scope = viewModelScope
    ) { book, user ->
        book != null && book.ownerId == user?.id
    }

    val canEditBook: Boolean get() = bookRepo.canEdit
    val allowMembersEdit = bookRepo.bookState.mapState { it?.allowMembersEdit != false }

    val isPremium = authManager.isPremium

    val currentUsername get() = authManager.userState.value?.name
    val currentBookName get() = bookRepo.bookState.value?.name

    val isDeletingAccount: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun trackBatchRecordClicked() {
        tracker.logEvent("settings_batch_record_click")
    }

    fun renameUser(newName: String) {
        viewModelScope.launch {
            try {
                authManager.renameUser(newName)
                snackbarSender.send(getString(Res.string.settings_rename_user_success, newName))
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    fun renameBook(newName: String) {
        viewModelScope.launch {
            try {
                bookRepo.renameBook(newName)
                snackbarSender.send(getString(Res.string.settings_rename_book_success, newName))
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    fun setAllowMembersEdit(allow: Boolean) {
        viewModelScope.launch {
            try {
                bookRepo.setAllowMembersEdit(allow)
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    fun deleteOrLeaveBook() {
        viewModelScope.launch {
            val isBookOwner = isBookOwner.value
            val bookName = bookName.value
            try {
                bookRepo.leaveOrDeleteBook()
                snackbarSender.send(getString(if (isBookOwner) {
                    Res.string.settings_book_deleted
                } else {
                    Res.string.settings_book_left
                }, bookName.orEmpty()))
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            isDeletingAccount.value = true
            authManager.deleteUserAccount().join()
            isDeletingAccount.value = false
        }
    }
}