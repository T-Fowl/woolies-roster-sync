package com.tfowl.workjam.client

import com.tfowl.workjam.client.model.AuthResponse
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.system.exitProcess

private const val WorkjamTokenHeader = "x-token"
private const val ORIGIN_URL = "https://app.workjam.com"
private const val REFERRER_URL = "https://app.workjam.com/"
private val ACCEPTED_LANGUAGE = Locale.ENGLISH

class WorkjamProvider(
    private val credentials: WorkjamCredentialStorage,
    private val httpEngineProvider: HttpEngineProvider = DefaultHttpEngineProvider(),
) {
    private val client = HttpClient(httpEngineProvider.provide()) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
        install(DefaultRequest) {
            header(HttpHeaders.AcceptLanguage, ACCEPTED_LANGUAGE)
            header(HttpHeaders.Origin, ORIGIN_URL)
            header(HttpHeaders.Referrer, REFERRER_URL)
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
        BrowserUserAgent() // Hehe sneaky
    }

    private suspend fun auth(old: String): AuthResponse {
        return client.patch(httpEngineProvider.defaultUrlBuilder().path("auth", "v3").build()) {
            header(WorkjamTokenHeader, old)
        }
    }

    private suspend fun reauthenticate(
        id: String,
        tokenOverride: String?
    ): AuthResponse {
        val old = tokenOverride ?: credentials.retrieve(id)
        requireNotNull(old) { "No token retrievable for id: $id" }

        val response = auth(old)
        credentials.store(id, response.token)
        return response
    }

    suspend fun create(id: String, tokenOverride: String? = null): WorkjamClient {
        val auth = reauthenticate(id, tokenOverride)

        return WorkjamClient("${auth.userId}", client.config {
            install(DefaultRequest) {
                header(WorkjamTokenHeader, auth.token)
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