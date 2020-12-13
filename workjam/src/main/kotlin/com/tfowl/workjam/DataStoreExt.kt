package com.tfowl.workjam

import com.google.api.client.util.store.DataStore
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified V> DataStore<String>.getSerializable(format: StringFormat, key: String): V? =
    get(key)?.let { format.decodeFromString(it) }

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified V> DataStore<String>.setSerializable(
    format: StringFormat,
    key: String,
    value: V
): DataStore<String> =
    set(key, format.encodeToString(value))

@Suppress("RedundantSuspendModifier")
@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified V> DataStore<String>.computeSerializableIfAbsent(
    format: StringFormat,
    key: String,
    provider: (String) -> V
): V =
    getSerializable(format, key) ?: provider(key).also { setSerializable(format, key, it) }