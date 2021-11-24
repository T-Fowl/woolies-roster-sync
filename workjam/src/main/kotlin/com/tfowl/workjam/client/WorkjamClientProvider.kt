package com.tfowl.workjam.client

import com.tfowl.workjam.client.model.WorkjamUser
import com.tfowl.workjam.client.model.serialisers.InstantSerialiser
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.time.format.DateTimeFormatter
import java.util.*

internal const val WorkjamTokenHeader = "x-token"
private const val ORIGIN_URL = "https://app.workjam.com"
private const val REFERRER_URL = "https://app.workjam.com/"
private val ACCEPTED_LANGUAGE = Locale.ENGLISH

class WorkjamClientProvider(
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
                serializersModule = SerializersModule {
                    contextual(InstantSerialiser(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS[XX][XXX][ZZZ][OOOO]")))
                }
            })
        }
        install(Logging) {
            level = LogLevel.INFO
            logger = Logger.DEFAULT
        }
        BrowserUserAgent() // Hehe sneaky
    }

    private suspend fun authenticateUser(oldToken: String): WorkjamUser {
        return client.patch(httpEngineProvider.defaultUrlBuilder().build()) {
            url.path("auth", "v3")
            header(WorkjamTokenHeader, oldToken)
        }
    }

    private suspend fun retrieveAuthenticatedUser(
        ref: String,
        tokenOverride: String?
    ): WorkjamUser {
        val oldToken = requireNotNull(tokenOverride ?: credentials.retrieve(ref)) {
            "No token available for user reference id: $ref"
        }

        return authenticateUser(oldToken).also {
            credentials.store(ref, it.token)
        }
    }

    suspend fun createClient(ref: String, tokenOverride: String? = null): WorkjamClient {
        val auth = retrieveAuthenticatedUser(ref, tokenOverride)

        return WorkjamClient(auth, client, httpEngineProvider.defaultUrlBuilder().build())
    }

    companion object {
        fun create(
            credentialsStorage: WorkjamCredentialStorage,
            httpEngineProvider: HttpEngineProvider = DefaultHttpEngineProvider()
        ): WorkjamClientProvider {
            return WorkjamClientProvider(credentialsStorage, httpEngineProvider)
        }
    }
}