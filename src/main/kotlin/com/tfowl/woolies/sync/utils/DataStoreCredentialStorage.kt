package com.tfowl.woolies.sync.utils

import com.google.api.client.util.store.DataStoreFactory
import com.tfowl.workjam.client.WorkjamCredentialStorage

class DataStoreCredentialStorage(factory: DataStoreFactory) : WorkjamCredentialStorage {
    private val store = factory.getDataStore<String>("WorkjamTokens")

    override suspend fun retrieve(ref: String): String? {
        return store[ref]
    }

    override suspend fun store(ref: String, token: String) {
        store[ref] = token
    }
}