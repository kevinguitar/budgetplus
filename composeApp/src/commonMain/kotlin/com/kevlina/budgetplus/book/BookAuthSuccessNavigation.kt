package com.kevlina.budgetplus.book

import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.feature.auth.AuthSuccessNavigation
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

@ContributesBinding(AppScope::class)
class BookAuthSuccessNavigation(
    private val navController: NavController<BookDest>,
    private val bookRepo: BookRepo,
    private val snackbarSender: SnackbarSender,
) : AuthSuccessNavigation {

    override suspend fun navigate() {
        val destination = try {
            if (bookRepo.isUserHasBooks()) {
                BookDest.Record
            } else {
                BookDest.Welcome
            }
        } catch (e: Exception) {
            snackbarSender.sendError(e)
            BookDest.Welcome
        }
        navController.selectRootAndClearAll(destination)
    }
}