package com.tfowl.woolies.sync

import com.tfowl.googleapi.*
import com.tfowl.woolies.sync.utils.ICalManager
import com.tfowl.workjam.client.WorkjamClient
import com.tfowl.workjam.client.model.*
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import com.google.api.services.calendar.model.Event as GoogleEvent

internal const val TIME_OFF_SUMMARY = "Time Off"

internal class EventTransformer(
    val workjamClient: WorkjamClient,
    val company: String,
    val employeeDataStore: DataStorage<Employee>,
    val calendarZoneId: ZoneId,
    val iCalManager: ICalManager,
    val descriptionGenerator: DescriptionGenerator,
) {
    private suspend fun createViewModel(
        workjam: WorkjamClient,
        shift: ScheduleEvent,
        title: String,
        coworkingPositions: List<Coworker>,
        employeeDataStore: DataStorage<Employee>
    ): DescriptionViewModel {
        suspend fun Employee.toViewModel(): CoworkerViewModel {
            val employeeDetails = employeeDataStore.computeIfAbsent(id) { id ->
                workjam.employee(company, id)
            }
            val employeeNumber = employeeDetails.externalCode ?: ""
            val avatarUrl = avatarUrl?.replace(
                "/image/upload",
                "/image/upload/f_auto,q_auto,w_64,h_64,c_thumb,g_face"
            )
            return CoworkerViewModel(firstName, lastName, employeeNumber, avatarUrl)
        }

        val coworkerPositionsViewModels = coworkingPositions.map { (position, coworkers) ->
            CoworkerPositionViewModel(
                position = position.externalCode,
                coworkers = coworkers.map { it.toViewModel() }.sortedBy { it.firstName }
            )
        }.sortedBy { it.position }

        return DescriptionViewModel(title, shift.startDateTime, shift.endDateTime, coworkerPositionsViewModels)
    }


    private fun shiftSummary(
        start: LocalTime,
        end: LocalTime,
        default: String?,
    ): String {
        fun String.removeWorkjamPositionPrefix(): String = removePrefix("*SUP-").removePrefix("SUP-")

        /* Trucks */
        when (start) {
            LocalTime.of(5, 30) -> return "AM Trucks"
            LocalTime.of(13, 0) -> return "PM Trucks"
        }

        /* General Picking */
        when (start.hour) {
            in 0..13  -> return "Picking AM"
            in 16..23 -> return "Picking PM"
        }

        /* idk */
        return default?.removeWorkjamPositionPrefix() ?: "Error: Missing Title"
    }

    private suspend fun transformShift(shift: Shift): GoogleEvent {
        val event = shift.event
        val start = LocalDateTime.ofInstant(event.startDateTime, calendarZoneId)
        val end = LocalDateTime.ofInstant(event.endDateTime, calendarZoneId)

        val coworkers = workjamClient.coworkers(company, event.location.id, event.id)

        val summary = shiftSummary(start.toLocalTime(), end.toLocalTime(), event.title)
        val vm = createViewModel(workjamClient, event, summary, coworkers, employeeDataStore)
        val description = descriptionGenerator.generate(vm)

        return GoogleEvent()
            .setStart(event.startDateTime, calendarZoneId)
            .setEnd(event.endDateTime, calendarZoneId)
            .setSummary(summary)
            .setICalUID(iCalManager.generate(event))
            .setDescription(description)
    }

    private suspend fun transformTimeOff(availability: Availability): GoogleEvent {
        val event = availability.event

        return GoogleEvent()
            .setStart(event.startDateTime.toGoogleEventDateTime())
            .setEnd(event.endDateTime.toGoogleEventDateTime())
            .setSummary(TIME_OFF_SUMMARY)
            .setICalUID(iCalManager.generate(event))
    }

    suspend fun transform(event: ScheduleEvent): GoogleEvent? = when (event.type) {
        ScheduleEventType.SHIFT                 -> {
            val shift = workjamClient.shift(company, event.location.id, event.id)
            transformShift(shift)
        }
        ScheduleEventType.AVAILABILITY_TIME_OFF -> {
            val availability = workjamClient.availability(company, workjamClient.userId, event.id)
            transformTimeOff(availability)
        }
        else                                    -> null
    }
}