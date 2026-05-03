package com.kevlina.budgetplus.feature.settings.member

import app.cash.turbine.test
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.data.UserRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.unit.test.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class MembersViewModelTest {

    @get:Rule
    val rule = MainDispatcherRule()

    private val owner = User(id = "owner_id", name = "Owner")
    private val member1 = User(id = "member_1", name = "Member 1")
    private val member2 = User(id = "member_2", name = "Member 2")

    @Test
    fun `bookMembers places owner at the top of list`() = runTest {
        val model = createModel(
            book = Book(
                ownerId = "owner_id",
                authors = listOf("member_1", "owner_id", "member_2")
            ),
            users = mapOf(
                "owner_id" to owner,
                "member_1" to member1,
                "member_2" to member2,
            ),
        )

        model.bookMembers.test {
            skipItems(1) // Skip initial empty list
            val members = awaitItem()
            assertEquals("owner_id", members.first().id)
            assertEquals(3, members.size)
        }
    }

    @Test
    fun `bookMembers is empty when book has no authors`() = runTest {
        val model = createModel(
            book = Book(authors = emptyList()),
            users = emptyMap(),
        )

        model.bookMembers.test {
            assertEquals(emptyList(), awaitItem())
        }
    }

    @Test
    fun `bookMembers handles unknown users gracefully`() = runTest {
        val model = createModel(
            book = Book(
                ownerId = "owner_id",
                authors = listOf("owner_id", "unknown_user")
            ),
            users = mapOf("owner_id" to owner),
        )

        model.bookMembers.test {
            skipItems(1) // Skip initial empty list
            val members = awaitItem()
            assertEquals(1, members.size)
            assertEquals("owner_id", members.first().id)
        }
    }

    @Test
    fun `ownerId reflects current book owner`() = runTest {
        val model = createModel(book = Book(ownerId = "owner_id"))
        assertEquals("owner_id", model.ownerId)
    }

    @Test
    fun `bookName reflects current book name`() = runTest {
        val model = createModel(book = Book(name = "My Budget"))
        assertEquals("My Budget", model.bookName)
    }

    private fun createModel(
        book: Book = Book(),
        users: Map<String, User> = emptyMap(),
    ): MembersViewModel {
        val userRepo = object : UserRepo {
            override fun getUser(userId: String): User? = users[userId]
        }
        return MembersViewModel(
            bookRepo = FakeBookRepo(book = book),
            authManager = FakeAuthManager(user = User(id = "current_user")),
            userRepo = userRepo,
            snackbarSender = FakeSnackbarSender,
        )
    }
}
