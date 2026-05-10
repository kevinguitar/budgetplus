package com.kevlina.budgetplus.notification

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension

@GraphExtension
interface FcmServiceGraph {

    fun inject(target: FcmService)

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    fun interface Factory {
        fun create(): FcmServiceGraph
    }
}