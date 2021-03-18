package com.tfowl.workjam.client

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tfowl.workjam.client.internal.WorkjamEndpoints
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.create

class WorkjamProvider(
    private val endpoints: WorkjamEndpoints,
    private val credentials: WorkjamCredentialStorage,
) {

    private suspend fun reauthenticate(
        credentials: WorkjamCredentialStorage,
        employeeId: String,
        tokenOverride: String?
    ): String {
        val old =
            requireNotNull(tokenOverride ?: credentials.retrieve(employeeId)) { "No token retrievable for $employeeId" }
        val response = endpoints.auth(old)
        credentials.store(employeeId, response.token)
        return response.token
    }

    suspend fun create(employee: String, tokenOverride: String? = null): Workjam {
        val token = reauthenticate(credentials, employee, tokenOverride)
        return Workjam(endpoints, token)
    }

    companion object {

        @OptIn(ExperimentalSerializationApi::class)
        private fun createWorkjamEndpoints(json: Json): WorkjamEndpoints {
            val httpClient = OkHttpClient.Builder()
                .addInterceptor(
                    HeadersInterceptor(
                        "Accept-Language: en",
                        "Origin: https://app.workjam.com",
                        "Referer: https://app.workjam.com/"
                    )
                )
                .addInterceptor(LoggingInterceptor())


            val retrofit = Retrofit.Builder()
                .client(httpClient.build())
                .baseUrl("https://prod-aus-gcp-woolworths-api.workjam.com")
                .addConverterFactory(json.asConverterFactory(MediaType.get("application/json")))
                .build()

            return retrofit.create()
        }

        fun create(credentialsStorage: WorkjamCredentialStorage): WorkjamProvider {
            val json = Json { ignoreUnknownKeys = true }
            val endpoints = createWorkjamEndpoints(json)

            return WorkjamProvider(endpoints, credentialsStorage)
        }
    }
}