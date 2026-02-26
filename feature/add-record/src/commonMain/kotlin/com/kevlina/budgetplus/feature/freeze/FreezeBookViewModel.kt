package com.kevlina.budgetplus.feature.freeze

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.local.Preference
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * When a user's premium is expired, we freeze their books and leave only one book to continue recording.
 */
@SingleIn(ViewModelScope::class)
@Inject
class FreezeBookViewModel(
    authManager: AuthManager,
    private val preference: Preference,
    bookRepo: BookRepo,
) : ViewModel() {

    private val activatedBookIdKey = stringPreferencesKey("activatedBookIdKey")
    val activatedBookId = preference.of(activatedBookIdKey)

    val showFreezeDialog: StateFlow<Boolean> = combine(
        authManager.userState.filterNotNull(),
        bookRepo.booksState.filterNotNull(),
        activatedBookId
    ) { user, books, activatedBookId ->
        // User already chose a book to continue recording.
        if (activatedBookId != null) {
            if (books.none { it.id == activatedBookId }) {
                // The chosen book is deleted, reset the activated book id to let user choose again.
                preference.remove(activatedBookIdKey)
            }
            return@combine false
        }

        val isFreeUser = user.premium == false
        val hasMultipleBooks = books.size > 1
        val shouldShowDialog = isFreeUser && hasMultipleBooks
        shouldShowDialog
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val isBookFrozen = combine(
        bookRepo.bookState,
        activatedBookId
    ) { book, activatedBookId ->
        activatedBookId != null && book?.id != activatedBookId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val books = bookRepo.booksState
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    init {
        authManager.isPremium
            .onEach { isPremium ->
                if (isPremium) {
                    preference.remove(activatedBookIdKey)
                }
            }
            .launchIn(viewModelScope)
    }

    fun activateBook(bookId: String) {
        viewModelScope.launch {
            preference.update(activatedBookIdKey, bookId)
        }
    }
}