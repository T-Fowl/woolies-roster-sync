package com.tfowl.workjam.client

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tfowl.workjam.client.internal.WorkjamEndpoints
import com.tfowl.workjam.client.model.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.create
import java.time.OffsetDateTime

interface WorkjamCredentialStorage {
    suspend fun retrieve(employeeId: String): String?

    suspend fun store(employeeId: String, token: String)
}

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

class Workjam internal constructor(
    private val endpoints: WorkjamEndpoints,
    private val token: String,
) {
    suspend fun employee(company: String, employee: String): Employee =
        endpoints.employee(token, company, employee)

    suspend fun workingStatus(company: String, employee: String): WorkingStatus =
        endpoints.workingStatus(token, company, employee)

    suspend fun events(
        company: String,
        employee: String,
        startDateTime: OffsetDateTime,
        endDateTime: OffsetDateTime,
        includeOverlaps: Boolean = true,
    ): List<Event> =
        endpoints.events(token, company, employee, startDateTime, endDateTime, includeOverlaps)

    suspend fun coworkers(
        company: String,
        location: String,
        shift: String,
    ): List<PositionedCoworkers> =
        endpoints.coworkers(token, company, location, shift)

    suspend fun employers(employee: String): Employers =
        endpoints.employers(token, employee)

    suspend fun employees(company: String): List<Employee> =
        endpoints.employees(token, company)

    suspend fun employees(company: String, ids: List<String>): List<Employee> =
        endpoints.employees(token, company, ids)

    suspend fun shift(company: String, shift: String): Shift =
        endpoints.shift(token, company, shift)
}