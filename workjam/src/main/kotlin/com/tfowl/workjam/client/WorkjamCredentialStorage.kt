package com.tfowl.workjam.client

interface WorkjamCredentialStorage {
    suspend fun retrieve(id: String): String?

    suspend fun store(id: String, token: String)
}