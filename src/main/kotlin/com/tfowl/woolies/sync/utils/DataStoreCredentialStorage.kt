package com.tfowl.woolies.sync.utils

import com.google.api.client.util.store.DataStoreFactory
import com.tfowl.workjam.client.WorkjamCredentialStorage

class DataStoreCredentialStorage(factory: DataStoreFactory) : WorkjamCredentialStorage {
    private val store = factory.getDataStore<String>("WorkjamTokens")

    override suspend fun retrieve(id: String): String? {
        return store[id]
    }

    override suspend fun store(id: String, token: String) {
        store[id] = token
    }
}