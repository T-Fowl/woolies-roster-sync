package com.tfowl.workjam.client

import com.tfowl.workjam.client.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.time.OffsetDateTime

class WorkjamClient internal constructor(
    private val user: WorkjamUser,
    private val client: HttpClient,
    private val defaultUrl: Url,
) {
    val userId: String = user.userId.toString()

    fun forUser(user: WorkjamUser): WorkjamClient = WorkjamClient(user, client, defaultUrl)

    private suspend inline fun <reified T> get(requestUrlBuilder: URLBuilder.() -> Unit): T {
        return client.get {
            header(WorkjamTokenHeader, user.token)
            url.takeFrom(defaultUrl)
            url.requestUrlBuilder()
        }
    }

    suspend fun employees(company: String): List<Employee> = get {
        path("api", "v4", "companies", company, "employees")
    }

    suspend fun employee(company: String, employee: String): Employee = get {
        path("api", "v4", "companies", company, "employees", employee)
    }

    suspend fun employers(employee: String): Employers = get {
        path("api", "v1", "users", employee, "employers")
    }

    suspend fun employees(company: String, ids: List<String>): List<Employee> = get {
        path("api", "v4", "companies", company, "employees")
        parameters.appendAll("employeeIds", ids)
    }

    suspend fun workingStatus(company: String, employee: String): WorkingStatus = get {
        path("api", "v4", "companies", company, "employees", employee, "working_status")
    }

    suspend fun events(
        company: String,
        employee: String,
        startDateTime: OffsetDateTime,
        endDateTime: OffsetDateTime,
        includeOverlaps: Boolean = true,
    ): List<Event> = get {
        path("api", "v4", "companies", company, "employees", employee, "events")
        parameters.append("startDateTime", startDateTime.toString())
        parameters.append("endDateTime", endDateTime.toString())
        parameters.append("includeOverlaps", includeOverlaps.toString())
    }

    suspend fun coworkers(
        company: String,
        location: String,
        shift: String,
    ): Coworkers = get {
        path("api", "v4", "companies", company, "locations", location, "shifts", shift, "coworkers")
    }

    suspend fun shift(company: String, location: String, shift: String): Shift = get {
        path("api", "v4", "companies", company, "locations", location, "shifts", shift)
    }

    suspend fun availability(company: String, employee: String, event: String): Availability = get {
        path("api", "v4", "companies", company, "employees", employee, "availabilities", event)
    }
}