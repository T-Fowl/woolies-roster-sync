package com.tfowl.workjam.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.OffsetDateTime


@Serializable
data class Location(
    val id: String,
    val name: String,
    val type: String,
    val timeZoneId: String,
    val externalId: String? = null,
    val externalCode: String? = null
)

// TODO: Work out a way to make this more future-proof
enum class EventType {
    SHIFT,
    AVAILABILITY_TIME_OFF,
}


@Serializable
data class Event(
    val id: String,
    val title: String? = null, // Missing when type == AVAILABILITY_TIME_OFF as opposed to SHIFT
    val type: EventType,
    @Serializable(with = OffsetDateTimeSerialiser::class)
    val startDateTime: OffsetDateTime,
    @Serializable(with = OffsetDateTimeSerialiser::class)
    val endDateTime: OffsetDateTime,
    val location: Location,
    val note: String? = null, // Haven't seen a non-null value
    val recurrence: String? = null, // Haven't seen a non-null value
)

@Serializable
data class Shift(
    val id: String,
    val status: String,
    val primaryLocation: Location,
    val assignees: List<ShiftAssignee>,
    val segments: List<ShiftSegment>
)

@Serializable
data class ShiftSegment(
    val type: String,
    @Serializable(with = OffsetDateTimeSerialiser::class)
    val startDateTime: OffsetDateTime,
    @Serializable(with = OffsetDateTimeSerialiser::class)
    val endDateTime: OffsetDateTime,
    val location: Location
)

@Serializable
data class ShiftAssignee(
    val profile: ShiftAssigneeProfile,
    val status: String,
    val bookingMethod: String
)

@Serializable
data class ShiftAssigneeProfile(
    val id: String,
    val firstName: String,
    val lastName: String
)

@Serializable
data class PositionedCoworkers(
    val employees: List<Coworker>,
    val position: Position
)

@Serializable
data class Coworker(
    val id: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String? = null
)

@Serializable
data class Position(
    val id: String, val name: String,
    val externalId: String, val externalCode: String
)

@Serializable
data class Employee(
    val id: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String? = null,
    val externalId: String? = null,
    val externalCode: String? = null,
    val username: String? = null,
    val workerType: String? = null,
    val status: String? = null
)

@Serializable
data class WorkingStatus(@SerialName("employeeWorking") val isWorking: Boolean)

@Serializable
data class Employers(val companies: List<Company>)

@Serializable
data class Company(
    @SerialName("companyName") val name: String,
    @SerialName("companyUrl") val url: String,
    val description: String,
    val id: Int,
    val logoUrl: String,
    val marketplaceCode: String,
    val startDayOfWeek: DayOfWeek,
//    @SerialName("companyAddress") val address: CompanyAddress,
//    val stores: List<CompanyStore>,
//    val telephone: CompanyTelephone
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: Long,
    val firstLogin: Boolean,
    val hasEmployers: Boolean,
    val userRole: String,
    val correlationId: String
)