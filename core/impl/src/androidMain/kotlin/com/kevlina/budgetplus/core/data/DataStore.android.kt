package com.kevlina.budgetplus.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = dataStoreFileName,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "app_preference"))
    }
)

@ContributesTo(AppScope::class)
@BindingContainer
object DataStoreProvider {

    @Provides
    fun provideDataStore(context: Context): DataStore<Preferences> = context.dataStore
}