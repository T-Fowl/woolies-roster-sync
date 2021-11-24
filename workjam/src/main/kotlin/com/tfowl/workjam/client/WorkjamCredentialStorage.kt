package com.tfowl.workjam.client

import java.util.concurrent.ConcurrentHashMap

interface WorkjamCredentialStorage {
    suspend fun retrieve(ref: String): String?

    suspend fun store(ref: String, token: String)
}

class InMemoryWorkjamCredentialStorage(vararg initial: Pair<String, String>) : WorkjamCredentialStorage {
    private val memory = ConcurrentHashMap<String, String>()

    init {
        memory.putAll(initial)
    }

    override suspend fun retrieve(ref: String): String? = memory[ref]

    override suspend fun store(ref: String, token: String) {
        memory[ref] = token
    }
}