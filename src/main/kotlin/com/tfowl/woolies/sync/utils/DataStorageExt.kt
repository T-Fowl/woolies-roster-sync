package com.tfowl.woolies.sync.utils

import com.google.api.client.util.store.DataStore
import com.google.api.client.util.store.DataStoreFactory
import com.tfowl.googleapi.DataStorage
import com.tfowl.googleapi.DataStorageSerialiser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializer

private class KotlinXSerializer<T>(
    private val format: StringFormat,
    private val serialiser: KSerializer<T>
) : DataStorageSerialiser<T> {
    override fun serialise(value: T): String {
        return format.encodeToString(serialiser, value)
    }

    override fun deserialise(value: String): T {
        return format.decodeFromString(serialiser, value)
    }

}

fun <V> DataStore<String>.asDataStorage(format: StringFormat, serialiser: KSerializer<V>): DataStorage<V> =
    DataStorage(this, KotlinXSerializer(format, serialiser))

inline fun <reified V> DataStore<String>.asDataStorage(format: StringFormat): DataStorage<V> =
    asDataStorage(format, serializer())

inline fun <reified V> DataStoreFactory.getDataStorage(id: String, format: StringFormat): DataStorage<V> {
    return getDataStore<String>(id).asDataStorage(format)
}