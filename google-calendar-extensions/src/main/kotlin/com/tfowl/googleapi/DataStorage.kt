@file:OptIn(ExperimentalSerializationApi::class)

package com.tfowl.googleapi

import com.google.api.client.util.store.DataStore
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializer

class DataStorage<V>(
    private val store: DataStore<String>,
    private val format: StringFormat,
    private val serializer: KSerializer<V>,
) {
    fun get(key: String): V? = store.get(key)?.let { format.decodeFromString(serializer, it) }

    fun set(key: String, value: V): DataStorage<V> {
        store.set(key, format.encodeToString(serializer, value))
        return this
    }
}

suspend fun <V> DataStorage<V>.computeIfAbsent(key: String, provider: suspend (String) -> V): V {
    return get(key) ?: provider(key).also { set(key, it) }
}

inline fun <reified V> DataStore<String>.asDataStorage(format: StringFormat): DataStorage<V> =
    asDataStorage(format, serializer())

fun <V> DataStore<String>.asDataStorage(format: StringFormat, serialiser: KSerializer<V>): DataStorage<V> =
    DataStorage(this, format, serialiser)