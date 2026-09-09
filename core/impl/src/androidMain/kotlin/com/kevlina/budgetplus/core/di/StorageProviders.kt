package com.kevlina.budgetplus.core.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import java.io.File

@ContributesTo(AppScope::class)
@BindingContainer
object StorageProviders {

    @Provides
    @Named("share_cache")
    fun provideShareCacheDir(context: Context): File {
        return File(context.cacheDir, "share").apply { mkdirs() }
    }
}