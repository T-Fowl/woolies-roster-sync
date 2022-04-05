package com.tfowl.googleapi

import com.google.api.client.util.store.DataStore

/**
 * Superset of google's [DataStore] which uses custom serialisation instead
 * of relying on [java.io.Serializable]
 */
class DataStorage<V>(
    private val store: DataStore<String>,
    private val serialiser: StringSerialiser<V>
) {
    fun get(key: String): V? = store.get(key)?.let { serialiser.deserialise(it) }

    fun set(key: String, value: V): DataStorage<V> {
        store.set(key, serialiser.serialise(value))
        return this
    }
}

interface StringSerialiser<T> {
    fun serialise(value: T): String
    fun deserialise(value: String): T

    companion object {
        object Identity : StringSerialiser<String> {
            override fun deserialise(value: String): String = value
            override fun serialise(value: String): String = value
        }
    }
}

fun <T> DataStore<String>.asDataStorage(serialiser: StringSerialiser<T>): DataStorage<T> =
    DataStorage(this, serialiser)

suspend fun <V> DataStorage<V>.computeIfAbsent(key: String, provider: suspend (String) -> V): V {
    return get(key) ?: provider(key).also { set(key, it) }
}