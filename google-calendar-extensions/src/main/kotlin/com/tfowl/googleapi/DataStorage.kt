package com.tfowl.googleapi

import com.google.api.client.util.store.DataStore

interface DataStorageSerialiser<T> {
    fun serialise(value: T): String
    fun deserialise(value: String): T
}

class DataStorage<V>(
    private val store: DataStore<String>,
    private val serialiser: DataStorageSerialiser<V>
) {
    fun get(key: String): V? = store.get(key)?.let { serialiser.deserialise(it) }

    fun set(key: String, value: V): DataStorage<V> {
        store.set(key, serialiser.serialise(value))
        return this
    }
}

suspend fun <V> DataStorage<V>.computeIfAbsent(key: String, provider: suspend (String) -> V): V {
    return get(key) ?: provider(key).also { set(key, it) }
}