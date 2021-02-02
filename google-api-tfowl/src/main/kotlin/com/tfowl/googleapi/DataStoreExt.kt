@file:OptIn(ExperimentalSerializationApi::class)

package com.tfowl.googleapi

import com.google.api.client.util.store.DataStore
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

inline fun <reified V> DataStore<String>.get(format: StringFormat, key: String): V? =
    get(key)?.let { format.decodeFromString(it) }

inline fun <reified V> DataStore<String>.set(
    format: StringFormat,
    key: String,
    value: V,
): DataStore<String> = set(key, format.encodeToString(value))

@Suppress("RedundantSuspendModifier")
suspend inline fun <reified V> DataStore<String>.computeIfAbsent(
    format: StringFormat,
    key: String,
    provider: (String) -> V,
): V = get(format, key) ?: provider(key).also { set(format, key, it) }