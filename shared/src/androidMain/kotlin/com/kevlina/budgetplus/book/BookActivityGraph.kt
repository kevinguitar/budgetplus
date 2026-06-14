package com.kevlina.budgetplus.book

import androidx.activity.ComponentActivity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension
interface BookActivityGraph {

    @Binds val BookActivity.bind: ComponentActivity

    fun inject(target: BookActivity)

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    fun interface Factory {
        fun create(@Provides activity: BookActivity): BookActivityGraph
    }
}