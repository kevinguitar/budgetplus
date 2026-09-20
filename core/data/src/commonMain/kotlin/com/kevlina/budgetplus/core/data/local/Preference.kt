package com.kevlina.budgetplus.core.data.local

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer

interface Preference {

    fun <T> of(key: Preferences.Key<T>): Flow<T?>

    fun <T> of(
        key: Preferences.Key<T>,
        default: T,
        scope: CoroutineScope,
    ): StateFlow<T>

    fun <T> of(key: Preferences.Key<String>, serializer: KSerializer<T>): Flow<T?>

    fun <T> of(
        key: Preferences.Key<String>,
        serializer: KSerializer<T>,
        default: T,
        scope: CoroutineScope,
    ): StateFlow<T>

    suspend fun <T> update(key: Preferences.Key<T>, value: T)

    suspend fun <T> update(
        key: Preferences.Key<String>,
        serializer: KSerializer<T>,
        value: T,
    )

    /**
     * Atomically reads the current value for [key], applies [transform] to it, and writes the
     * result back. The read-modify-write is serialized by the underlying store, so concurrent
     * callers never clobber each other's changes (unlike a separate read then [update]).
     *
     * [transform] receives the freshest decoded value (or `null` if absent) and returns the new
     * value to persist, or `null` to remove the key. Returns the value that was persisted.
     */
    suspend fun <T> updateTransform(
        key: Preferences.Key<String>,
        serializer: KSerializer<T>,
        transform: suspend (current: T?) -> T?,
    ): T?

    suspend fun remove(key: Preferences.Key<*>)

    suspend fun clearAll()

}