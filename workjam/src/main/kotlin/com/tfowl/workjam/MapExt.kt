package com.tfowl.workjam

internal interface SuspendingMap<K, V> {
    suspend fun get(key: K): V?
    suspend fun set(key: K, value: V): V
    suspend fun computeIfAbsent(key: K, block: suspend (K) -> V): V
}

internal class MutableMapSuspendingMap<K, V>(private val underlying: MutableMap<K, V>) : SuspendingMap<K, V> {

    override suspend fun get(key: K): V? = underlying[key]

    override suspend fun set(key: K, value: V): V {
        underlying[key] = value
        return value
    }

    override suspend fun computeIfAbsent(key: K, block: suspend (K) -> V): V {
        if (key in underlying) return underlying[key]!!
        val value = block(key)
        underlying[key] = value
        return value
    }
}

internal fun <K, V> MutableMap<K, V>.asSuspendingMap(): SuspendingMap<K, V> = MutableMapSuspendingMap(this)