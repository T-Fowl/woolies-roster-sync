package com.tfowl.workjam.client

import com.tfowl.workjam.client.internal.WorkjamEndpoints
import com.tfowl.workjam.client.model.*
import java.time.OffsetDateTime

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