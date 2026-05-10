package com.kevlina.budgetplus.core.data

import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.Tracker
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class TrackerImpl(
    authManager: Lazy<AuthManager>,
    @AppCoroutineScope appScope: CoroutineScope,
    @Named("is_debug") private val isDebug: Boolean,
) : Tracker {

    private val analytics by lazy { Firebase.analytics }

    init {
        authManager.value
            .userState
            .onEach { analytics.setUserId(it?.id) }
            .launchIn(appScope)
    }

    override fun logEvent(event: String, params: Map<String, Any>?) {
        Logger.d { "Analytics:: $event" + if (params == null) "" else ", $params" }
        if (!isDebug) {
            analytics.logEvent(event, params)
        }
    }
}