package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.RestrictTo
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.kevlina.budgetplus.core.data.local.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

@RestrictTo(RestrictTo.Scope.TESTS)
class FakePreference(
    mapBuilder: MutablePreferences.() -> Unit = {},
) : Preference {

    private val prefsFlow = MutableStateFlow(mutablePreferencesOf().apply(mapBuilder))

    override fun <T> of(key: Preferences.Key<T>): Flow<T?> {
        return prefsFlow.map { it[key] }
    }

    override fun <T> of(key: Preferences.Key<T>, default: T, scope: CoroutineScope): StateFlow<T> {
        return of(key)
            .map { it ?: default }
            .stateIn(scope, SharingStarted.WhileSubscribed(), default)
    }

    override fun <T> of(key: Preferences.Key<String>, serializer: KSerializer<T>): Flow<T?> {
        return prefsFlow.map {
            it[key]?.let { value ->
                Json.decodeFromString(serializer, value)
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
        prefsFlow.update {
            it.toMutablePreferences().apply { set(key, value) }
        }
    }

    override suspend fun <T> update(
        key: Preferences.Key<String>,
        serializer: KSerializer<T>,
        value: T,
    ) {
        prefsFlow.update {
            it.toMutablePreferences().apply {
                set(key, Json.encodeToString(serializer, value))
            }
        }
    }

    override suspend fun remove(key: Preferences.Key<*>) {
        prefsFlow.update {
            it.toMutablePreferences().apply { remove(key) }
        }
    }

    override suspend fun clearAll() {
        prefsFlow.value = mutablePreferencesOf()
    }
}