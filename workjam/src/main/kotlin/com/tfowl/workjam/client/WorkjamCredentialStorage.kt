package com.tfowl.workjam.client

interface WorkjamCredentialStorage {
    suspend fun retrieve(employeeId: String): String?

    suspend fun store(employeeId: String, token: String)
}