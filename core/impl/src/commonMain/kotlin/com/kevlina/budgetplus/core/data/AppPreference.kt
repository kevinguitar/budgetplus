package com.kevlina.budgetplus.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.data.local.Preference
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class AppPreference(
    private val dataStore: DataStore<Preferences>,
    private val formatter: Json,
) : Preference {

    override fun <T> of(key: Preferences.Key<T>): Flow<T?> {
        return dataStore.data.map {
            try {
                it[key]
            } catch (e: Exception) {
                Logger.e(e) { "Preference: Read ${key.name} failed" }
                null
            }
        }
    }

    override fun <T> of(key: Preferences.Key<T>, default: T, scope: CoroutineScope): StateFlow<T> {
        return of(key)
            .map { it ?: default }
            .stateIn(scope, SharingStarted.WhileSubscribed(), default)
    }

    override fun <T> of(key: Preferences.Key<String>, serializer: KSerializer<T>): Flow<T?> {
        return dataStore.data
            .map {
                val savedValue = it[key] ?: return@map null
                try {
                    formatter.decodeFromString(serializer, savedValue)
                } catch (e: Exception) {
                    Logger.e(e) { "Preference: Decode failed, key=${key.name}, savedValue=$savedValue" }
                    null
                }
            }
    }

    override fun <T> of(
        key: Preferences.Key<String>,
        serializer: KSerializer<T>,
        default: T,
        scope: CoroutineScope,
    ): StateFlow<T> {
        return of(key, serializer)
            .map { it ?: default }
            .stateIn(scope, SharingStarted.WhileSubscribed(), default)
    }

    override suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        dataStore.edit { prefs ->
            prefs[key] = value
        }
    }

    override suspend fun <T> update(key: Preferences.Key<String>, serializer: KSerializer<T>, value: T) {
        val encoded = try {
            formatter.encodeToString(serializer, value)
        } catch (e: Exception) {
            Logger.e(e) { "Preference: Encode failed, key=${key.name}, value=$value" }
            return
        }
        dataStore.edit { prefs ->
            prefs[key] = encoded
        }
    }

    override suspend fun remove(key: Preferences.Key<*>) {
        dataStore.edit { prefs -> prefs.remove(key) }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}

internal const val dataStoreFileName = "app.preferences_pb"
