package com.tfowl.workjam.client

interface WorkjamCredentialStorage {
    suspend fun retrieve(ref: String): String?

    suspend fun store(ref: String, token: String)
}