package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.data.remote.Book
import kotlinx.coroutines.flow.StateFlow

interface BookRepo {

    val bookState: StateFlow<Book?>
    val booksState: StateFlow<List<Book>?>

    val currentBookId: String?
    val currencySymbol: StateFlow<String>

    val canEdit: Boolean
    val hasPendingJoinRequest: Boolean

    suspend fun generateJoinLink(): String

    fun setPendingJoinRequest(joinId: String?)

    /**
     *  @return The book's name if the user joined successfully.
     *  @throws com.kevlina.budgetplus.core.data.JoinBookException
     */
    suspend fun handlePendingJoinRequest(): String?

    suspend fun removeMember(userId: String)

    /**
     * Await the auth state to propagate and check if the user has a book.
     * @return true if the user has a book, false otherwise.
     */
    suspend fun isUserHasBooks(): Boolean

    /**
     * @param fromBook The book to copy the categories from. When null, the default
     *  categories are used.
     */
    suspend fun createBook(name: String, source: String, fromBook: Book? = null)

    suspend fun renameBook(newName: String)

    suspend fun leaveOrDeleteBook()

    suspend fun selectBook(book: Book?)

    suspend fun addCategory(type: RecordType, category: String, source: String)

    fun formatPrice(price: Double, alwaysShowSymbol: Boolean = false): String

    suspend fun updateCategories(type: RecordType, categories: List<String>)

    suspend fun updateCurrency(currencyCode: String)

    suspend fun setAllowMembersEdit(allow: Boolean)

}