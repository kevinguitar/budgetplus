package com.kevlina.budgetplus.core.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

@ContributesBinding(AppScope::class)
class CrashlyticsProviderImpl : CrashlyticsProvider {

    override fun setUserId(userId: String) {
        Firebase.crashlytics.setUserId(userId)
    }
}
