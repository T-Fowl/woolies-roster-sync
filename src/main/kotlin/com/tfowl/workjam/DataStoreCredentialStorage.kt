package com.tfowl.workjam

import com.google.api.client.util.store.DataStoreFactory
import com.tfowl.workjam.client.WorkjamCredentialStorage

class DataStoreCredentialStorage(factory: DataStoreFactory) : WorkjamCredentialStorage {
    private val store = factory.getDataStore<String>("WorkjamTokens")

    override suspend fun retrieve(employeeId: String): String? {
        return store[employeeId]
    }

    override suspend fun store(employeeId: String, token: String) {
        store[employeeId] = token
    }
}