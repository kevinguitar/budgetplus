package com.kevlina.budgetplus.book

import com.kevlina.budgetplus.core.common.UiTestFlags
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
object BookProviders {

    @Provides
    @Named("allow_update_fcm_token")
    fun provideAllowUpdateFcmToken(): Boolean = !UiTestFlags.enabled
}
