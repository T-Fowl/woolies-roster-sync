package com.tfowl.workjam.client

import com.tfowl.workjam.client.model.AuthResponse
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import kotlinx.serialization.json.Json

class WorkjamProvider(
    private val credentials: WorkjamCredentialStorage,
    private val httpEngineProvider: HttpEngineProvider = DefaultHttpEngineProvider(),
) {
    private val client = HttpClient(httpEngineProvider.provide()) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
        install(DefaultRequest) {
            header("Accept-Language", "en")
            header("Origin", "https://app.workjam.com")
            header("Referer", "https://app.workjam.com/")
            BrowserUserAgent() // Hehe sneaky
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer(Json {
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
            logger = Logger.DEFAULT
        }
    }

    private suspend fun auth(old: String): AuthResponse {
        return client.patch(httpEngineProvider.defaultUrlBuilder().path("auth", "v3").build()) {
            header("x-token", old)
        }
    }

    private suspend fun reauthenticate(
        credentials: WorkjamCredentialStorage,
        employeeId: String,
        tokenOverride: String?
    ): String {
        val old = tokenOverride ?: credentials.retrieve(employeeId)
        requireNotNull(old) { "No token retrievable for $employeeId" }

        val response = auth(old)
        credentials.store(employeeId, response.token)
        return response.token
    }

    suspend fun create(employee: String, tokenOverride: String? = null): WorkjamClient {
        val token = reauthenticate(credentials, employee, tokenOverride)
        return WorkjamClient(client.config {
            install(DefaultRequest) {
                header("x-token", token)
                header("Accept-Language", "en")
                header("Origin", "https://app.workjam.com")
                header("Referer", "https://app.workjam.com/")
                BrowserUserAgent()
            }
        }, httpEngineProvider::defaultUrlBuilder)
    }

    companion object {
        fun create(
            credentialsStorage: WorkjamCredentialStorage,
            httpEngineProvider: HttpEngineProvider = DefaultHttpEngineProvider()
        ): WorkjamProvider {
            return WorkjamProvider(credentialsStorage, httpEngineProvider)
        }
    }
}