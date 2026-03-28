package com.kevlina.budgetplus.feature.settings.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.UserRepo
import com.kevlina.budgetplus.core.data.remote.User
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.ViewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@ViewModelKey
@ContributesIntoMap(ViewModelScope::class)
class MembersViewModel(
    private val bookRepo: BookRepo,
    private val authManager: AuthManager,
    private val userRepo: UserRepo,
    private val snackbarSender: SnackbarSender,
) : ViewModel() {

    val bookMembers: StateFlow<List<User>> = bookRepo.bookState
        .map { book ->
            val authors = book?.authors ?: emptyList()
            val users = authors
                .mapNotNull { id -> userRepo.getUser(id) }
                .toMutableList()

            // Move the owner to the first of the list
            val owner = users.find { it.id == ownerId }
            if (owner != null) {
                users.remove(owner)
                users.add(0, owner)
            }
            users
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val userId get() = authManager.requireUserId()

    val ownerId get() = bookRepo.bookState.value?.ownerId
    val bookName get() = bookRepo.bookState.value?.name

    fun removeMember(userId: String) {
        viewModelScope.launch {
            try {
                bookRepo.removeMember(userId)
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }
}