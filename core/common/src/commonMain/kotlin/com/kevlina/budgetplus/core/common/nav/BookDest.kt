package com.kevlina.budgetplus.core.common.nav

import androidx.navigation3.runtime.NavKey
import com.kevlina.budgetplus.core.common.RecordType
import kotlinx.serialization.Serializable

@Serializable
sealed interface BookDest : NavKey {

    @Serializable
    data class Auth(val enableAutoSignIn: Boolean = true) : BookDest

    @Serializable
    data object Welcome : BookDest

    /**
     * Destinations of Add tab
     */
    @Serializable
    data object Record : BookDest

    @Serializable
    data class EditCategory(val type: RecordType) : BookDest

    @Serializable
    data class Settings(val showMembers: Boolean = false) : BookDest

    @Serializable
    data object UnlockPremium : BookDest

    @Serializable
    data object BatchRecord : BookDest

    @Serializable
    data class Colors(val hex: String? = null) : BookDest

    @Serializable
    data object CurrencyPicker : BookDest


    /**
     * Destinations of History tab
     */
    @Serializable
    data object Overview : BookDest

    @Serializable
    data class Records(
        val type: RecordType,
        val category: String,
        val authorId: String?,
    ) : BookDest

    @Serializable
    data class Search(val type: RecordType) : BookDest
}

sealed interface InsiderDest : NavKey {

    @Serializable
    data object Insider : InsiderDest

    @Serializable
    data object PushNotifications : InsiderDest
}