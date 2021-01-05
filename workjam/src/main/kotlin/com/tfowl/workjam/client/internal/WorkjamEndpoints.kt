package com.tfowl.workjam.client.internal

import com.tfowl.workjam.client.model.*
import retrofit2.http.*
import java.time.OffsetDateTime

interface WorkjamEndpoints {

    @PATCH("/auth/v3")
    suspend fun auth(
        @Header("x-token") token: String
    ): AuthResponse

    @POST("/api/logout")
    suspend fun logout(
        @Header("x-token") token: String
    ): Unit

    @GET("/api/v4/companies/{company}/employees/{employee}")
    suspend fun employee(
        @Header("x-token") token: String,
        @Path("company") company: String,
        @Path("employee") employee: String
    ): Employee

    @GET("/api/v4/companies/{company}/employees/{employee}/working_status")
    suspend fun workingStatus(
        @Header("x-token") token: String,
        @Path("company") company: String,
        @Path("employee") employee: String
    ): WorkingStatus

    @GET("/api/v4/companies/{company}/employees/{employee}/events")
    suspend fun events(
        @Header("x-token") token: String,
        @Path("company") company: String,
        @Path("employee") employee: String,
        @Query("startDateTime") startDateTime: OffsetDateTime,
        @Query("endDateTime") endDateTime: OffsetDateTime,
        @Query("includeOverlaps") includeOverlaps: Boolean = true
    ): List<Event>

    @GET("/api/v4/companies/{company}/locations/{location}/shifts/{shift}/coworkers")
    suspend fun coworkers(
        @Header("x-token") token: String,
        @Path("company") company: String,
        @Path("location") location: String,
        @Path("shift") shift: String
    ): List<PositionedCoworkers>

    @GET("/api/v1/users/{employee}/employers")
    suspend fun employers(
        @Header("x-token") token: String,
        @Path("employee") employee: String
    ): Employers

    @GET("/api/v4/companies/{companyId}/employees")
    suspend fun employees(
        @Header("x-token") token: String,
        @Path("companyId") company: String
    ): List<Employee>

    @GET("/api/v4/companies/{companyId}/employees")
    suspend fun employees(
        @Header("x-token") token: String,
        @Path("companyId") company: String,
        @Query("employeeIds") ids: List<String>
    ): List<Employee>

    @GET("/api/v5/companies/{companyId}/shifts/{shiftId}")
    suspend fun shift(
        @Header("x-token") token: String,
        @Path("companyId") company: String,
        @Path("shiftId") shift: String
    ): Shift
}