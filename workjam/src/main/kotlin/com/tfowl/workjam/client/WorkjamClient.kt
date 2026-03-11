package com.tfowl.workjam.client

import com.tfowl.workjam.client.model.*
import com.tfowl.workjam.client.model.serialisers.InstantSerialiser
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

interface WorkjamClient {
    val user: WorkjamUser

    val userId: String get() = user.userId.toString()

    suspend fun employees(company: String): List<Employee>

    suspend fun employee(company: String, employee: String): Employee

    suspend fun employers(employee: String): Employers

    suspend fun employees(company: String, ids: Iterable<String>): List<Employee>

    suspend fun workingStatus(company: String, employee: String): WorkingStatus

    suspend fun events(
        company: String,
        employee: String,
        startDateTime: OffsetDateTime,
        endDateTime: OffsetDateTime,
        includeOverlaps: Boolean = true,
    ): List<ScheduleEvent>

    suspend fun coworkers(
        company: String,
        location: String,
        shift: String,
    ): Coworkers

    suspend fun shift(company: String, location: String, shift: String): Shift

    suspend fun availability(company: String, employee: String, event: String): Availability

    suspend fun shifts(
        company: String,
        location: String,
        startDateTime: OffsetDateTime,
        endDateTime: OffsetDateTime,
        includeOverlaps: Boolean = true,
    ): List<Shift>

    suspend fun periodicTimecards(company: String, employee: String): List<PeriodicTimecard>
}

class KtorWorkjamClient internal constructor(
    override val user: WorkjamUser,
    private val client: HttpClient,
) : WorkjamClient {

    override suspend fun employees(company: String): List<Employee> = get {
        path("api/v4/companies/$company/employees")
    }

    override suspend fun employee(company: String, employee: String): Employee = get {
        path("api/v4/companies/$company/employees/$employee")
    }

    override suspend fun employers(employee: String): Employers = get {
        path("api/v1/users/$employee/employers")
    }

    override suspend fun employees(company: String, ids: Iterable<String>): List<Employee> = get {
        path("api/v4/companies/$company/employees")
        parameters.append("employeeIds", ids.joinToString(","))
    }

    override suspend fun workingStatus(company: String, employee: String): WorkingStatus = get {
        path("api/v4/companies/$company/employees/$employee/working_status")
    }

    override suspend fun events(
        company: String,
        employee: String,
        startDateTime: OffsetDateTime,
        endDateTime: OffsetDateTime,
        includeOverlaps: Boolean,
    ): List<ScheduleEvent> = get {
        path("api/v4/companies/$company/employees/$employee/events")
        parameters.append("startDateTime", startDateTime.toString())
        parameters.append("endDateTime", endDateTime.toString())
        parameters.append("includeOverlaps", includeOverlaps.toString())
    }

    override suspend fun coworkers(
        company: String,
        location: String,
        shift: String,
    ): Coworkers = get {
        path("api/v4/companies/$company/locations/$location/shifts/$shift/coworkers")
    }

    override suspend fun shift(company: String, location: String, shift: String): Shift = get {
        path("api/v4/companies/$company/locations/$location/shifts/$shift")
    }

    override suspend fun availability(company: String, employee: String, event: String): Availability = get {
        path("api/v4/companies/$company/employees/$employee/availabilities/$event")
    }

    override suspend fun shifts(
        company: String,
        location: String,
        startDateTime: OffsetDateTime,
        endDateTime: OffsetDateTime,
        includeOverlaps: Boolean,
    ): List<Shift> = get {
        path("api/v4/companies/$company/locations/$location/shifts")
        parameters.append("startDateTime", startDateTime.toString())
        parameters.append("endDateTime", endDateTime.toString())
        parameters.append("includeOverlaps", includeOverlaps.toString())

    }

    override suspend fun periodicTimecards(company: String, employee: String): List<PeriodicTimecard> = get {
        path("api/v4/companies/$company/employees/$employee/periodic_timecards")
    }

    private suspend inline fun <reified T> get(requestUrlBuilder: URLBuilder.() -> Unit): T {
        return client.get {
            header(HttpHeaders.Authorization, "Bearer ${user.token}")
//            header("x-token", user.token)
            url.requestUrlBuilder()
        }.body()
    }

    companion object {
        suspend fun create(
            token: String,
            engine: HttpClientEngine = Java.create(),
            clientConfig: HttpClientConfig<*>.() -> Unit = {},
        ): KtorWorkjamClient {
            val client = HttpClient(engine) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000
                }
                install(DefaultRequest) {
                    url("https://prod-aus-gcp-woolworths-api.workjam.com")
                    header(HttpHeaders.AcceptLanguage, Locale.ENGLISH)
                    header(HttpHeaders.Origin, "https://app.workjam.com")
                    header(HttpHeaders.Referrer, "https://app.workjam.com/")
                }
                install(HttpRequestRetry) {
                    retryOnServerErrors(maxRetries = 5)
                    exponentialDelay()
                }
                install(ContentNegotiation) {
                    json(Json {
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
                // Hehe sneaky
                install(UserAgent) {
                    agent =
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.75 Safari/537.36"
                }

                this.apply(clientConfig)
            }

            val user = client.patch("/auth/v3") {
                header(HttpHeaders.Authorization, "Bearer $token")
//                header("x-token", token)
            }.body<WorkjamUser>()

            return KtorWorkjamClient(user, client)
        }
    }
}