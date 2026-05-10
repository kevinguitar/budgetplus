package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.remote.Book
import kotlinx.coroutines.flow.MutableStateFlow

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeBookRepo(
    book: Book? = null,
    books: List<Book>? = null,
    currentCurrencySymbol: String = "USD",
    override val currentBookId: String? = null,
    override val hasPendingJoinRequest: Boolean = false,
    override val canEdit: Boolean = true,
) : BookRepo {

    override val bookState = MutableStateFlow(book)
    override val booksState = MutableStateFlow(books)
    override val currencySymbol = MutableStateFlow(currentCurrencySymbol)

    override suspend fun generateJoinLink(): String {
        error("Not yet implemented")
    }

    override fun setPendingJoinRequest(joinId: String?) {
        error("Not yet implemented")
    }

    override suspend fun handlePendingJoinRequest(): String? {
        error("Not yet implemented")
    }

    override suspend fun removeMember(userId: String) {
        error("Not yet implemented")
    }

    override suspend fun isUserHasBooks(): Boolean {
        error("Not yet implemented")
    }

    override suspend fun createBook(name: String, source: String) {
        error("Not yet implemented")
    }

    override suspend fun renameBook(newName: String) {
        error("Not yet implemented")
    }

    override suspend fun leaveOrDeleteBook() {
        error("Not yet implemented")
    }

    override suspend fun selectBook(book: Book?) {
        error("Not yet implemented")
    }

    override suspend fun addCategory(type: RecordType, category: String, source: String) {
        error("Not yet implemented")
    }

    override fun formatPrice(price: Double, alwaysShowSymbol: Boolean): String {
        error("Not yet implemented")
    }

    override suspend fun updateCategories(type: RecordType, categories: List<String>) {
        error("Not yet implemented")
    }

    override suspend fun updateCurrency(currencyCode: String) {
        error("Not yet implemented")
    }

    override suspend fun setAllowMembersEdit(allow: Boolean) {
        error("Not yet implemented")
    }
}