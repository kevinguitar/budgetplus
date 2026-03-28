package com.kevlina.budgetplus.core.common.nav

import androidx.navigation3.runtime.NavKey
import com.kevlina.budgetplus.core.common.RecordType

sealed interface BookDest : NavKey {

    data class Auth(val enableAutoSignIn: Boolean = true) : BookDest

    data object Welcome : BookDest

    /**
     * Destinations of Add tab
     */
    data object Record : BookDest

    data class EditCategory(val type: RecordType) : BookDest

    data class Settings(val showMembers: Boolean = false) : BookDest

    data object UnlockPremium : BookDest

    data object BatchRecord : BookDest

    data class Colors(val hex: String? = null) : BookDest

    data object CurrencyPicker : BookDest

    /**
     * Destinations of History tab
     */
    data object Overview : BookDest

    data class Records(
        val type: RecordType,
        val category: String,
        val authorId: String?,
    ) : BookDest

    data class Search(val type: RecordType) : BookDest
}

sealed interface InsiderDest : NavKey {

    data object Insider : InsiderDest

    data object PushNotifications : InsiderDest
}