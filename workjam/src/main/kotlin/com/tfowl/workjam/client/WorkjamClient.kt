package com.tfowl.workjam.client

import com.tfowl.workjam.client.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.time.OffsetDateTime

interface WorkjamClient {
    val userId: String

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

    suspend fun shifts(company: String, location: String, startDateTime: OffsetDateTime, endDateTime: OffsetDateTime, includeOverlaps: Boolean = true): List<Shift>

    suspend fun periodicTimecards(company: String, employee: String): List<PeriodicTimecard>
}

class DefaultWorkjamClient internal constructor(
    private val user: WorkjamUser,
    private val client: HttpClient,
    private val defaultUrl: Url,
) : WorkjamClient {
    override val userId: String = user.userId.toString()

    private suspend inline fun <reified T> get(requestUrlBuilder: URLBuilder.() -> Unit): T {
        return client.get {
            header(WorkjamTokenHeader, user.token)
            url.takeFrom(defaultUrl)
            url.requestUrlBuilder()
        }.body()
    }

    override suspend fun employees(company: String): List<Employee> = get {
        path("api", "v4", "companies", company, "employees")
    }

    override suspend fun employee(company: String, employee: String): Employee = get {
        path("api", "v4", "companies", company, "employees", employee)
    }

    override suspend fun employers(employee: String): Employers = get {
        path("api", "v1", "users", employee, "employers")
    }

    override suspend fun employees(company: String, ids: Iterable<String>): List<Employee> = get {
        path("api", "v4", "companies", company, "employees")
        parameters.append("employeeIds", ids.joinToString(","))
    }

    override suspend fun workingStatus(company: String, employee: String): WorkingStatus = get {
        path("api", "v4", "companies", company, "employees", employee, "working_status")
    }

    override suspend fun events(
        company: String,
        employee: String,
        startDateTime: OffsetDateTime,
        endDateTime: OffsetDateTime,
        includeOverlaps: Boolean,
    ): List<ScheduleEvent> = get {
        path("api", "v4", "companies", company, "employees", employee, "events")
        parameters.append("startDateTime", startDateTime.toString())
        parameters.append("endDateTime", endDateTime.toString())
        parameters.append("includeOverlaps", includeOverlaps.toString())
    }

    override suspend fun coworkers(
        company: String,
        location: String,
        shift: String,
    ): Coworkers = get {
        path("api", "v4", "companies", company, "locations", location, "shifts", shift, "coworkers")
    }

    override suspend fun shift(company: String, location: String, shift: String): Shift = get {
        path("api", "v4", "companies", company, "locations", location, "shifts", shift)
    }

    override suspend fun availability(company: String, employee: String, event: String): Availability = get {
        path("api", "v4", "companies", company, "employees", employee, "availabilities", event)
    }

    override suspend fun shifts(
        company: String,
        location: String,
        startDateTime: OffsetDateTime,
        endDateTime: OffsetDateTime,
        includeOverlaps: Boolean
    ): List<Shift> = get {
        path("api", "v4", "companies", company, "locations", location, "shifts")
        parameters.append("startDateTime", startDateTime.toString())
        parameters.append("endDateTime", endDateTime.toString())
        parameters.append("includeOverlaps", includeOverlaps.toString())

    }

    override suspend fun periodicTimecards(company: String, employee: String): List<PeriodicTimecard> = get {
        path("api", "v4", "companies", company, "employees", employee, "periodic_timecards")
    }
}

class DevelopmentWorkjamClient(private val delegate: WorkjamClient) : WorkjamClient by delegate {
    
}