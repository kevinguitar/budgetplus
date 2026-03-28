package com.kevlina.budgetplus.book

import androidx.lifecycle.SavedStateHandle
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.BottomNavTab
import com.kevlina.budgetplus.core.common.nav.NavController
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface NavControllerModule {

    @SingleIn(AppScope::class)
    @Provides
    fun provideNavController(): NavController<BookDest> {
        return NavController(
            startRoot = BottomNavTab.Add.root,
            serializer = BookDest.serializer(),
            savedStateHandle = SavedStateHandle()
        )
    }
}