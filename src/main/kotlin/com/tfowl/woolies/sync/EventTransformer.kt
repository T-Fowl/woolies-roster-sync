package com.tfowl.woolies.sync

import com.tfowl.googleapi.DataStorage
import com.tfowl.googleapi.computeIfAbsent
import com.tfowl.googleapi.toGoogleEventDateTime
import com.tfowl.workjam.client.WorkjamClient
import com.tfowl.workjam.client.model.*
import java.time.LocalTime
import java.time.ZoneId
import com.google.api.services.calendar.model.Event as GoogleEvent

internal class EventTransformer(
    val workjamClient: WorkjamClient,
    val employeeDataStore: DataStorage<Employee>,
    val calendarZoneId: ZoneId,
    val iCalManager: ICalManager,
    val descriptionGenerator: DescriptionGenerator,
) {
    private suspend fun createViewModel(
        workjam: WorkjamClient,
        shift: Event,
        coworkingPositions: List<PositionedCoworkers>,
        employeeDataStore: DataStorage<Employee>
    ): DescriptionViewModel {
        suspend fun Coworker.toViewModel(): CoworkerViewModel {
            val employeeDetails = employeeDataStore.computeIfAbsent(id) { id ->
                workjam.employee(WOOLIES, id)
            }
            val employeeNumber = employeeDetails.externalCode ?: ""
            val avatarUrl = avatarUrl?.replace(
                "/image/upload",
                "/image/upload/f_auto,q_auto,w_64,h_64,c_thumb,g_face"
            )
            return CoworkerViewModel(firstName, lastName, employeeNumber, avatarUrl)
        }

        val coworkerPositionsViewModels = coworkingPositions.map { (coworkers, position) ->
            CoworkerPositionViewModel(
                position = position.externalCode,
                coworkers = coworkers.map { it.toViewModel() }.sortedBy { it.firstName }
            )
        }.sortedBy { it.position }

        return DescriptionViewModel(shift.startDateTime, shift.endDateTime, coworkerPositionsViewModels)
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

    private fun Event.toGoogleEvent(
        description: String,
        zoneId: ZoneId
    ): GoogleEvent {
        val start = startDateTime.toLocalDateTime(zoneId)
        val end = endDateTime.toLocalDateTime(zoneId)

        val summary = when (type) {
            EventType.SHIFT                 -> shiftSummary(start.toLocalTime(), end.toLocalTime(), title)
            EventType.AVAILABILITY_TIME_OFF -> TIME_OFF_SUMMARY
        }

        return GoogleEvent()
            .setStart(startDateTime.toGoogleEventDateTime())
            .setEnd(endDateTime.toGoogleEventDateTime())
            .setSummary(summary)
            .setICalUID(iCalManager.generate(this))
            .setDescription(description)
    }

    suspend fun transform(event: Event): GoogleEvent {
        val coworkingPositions = when (event.type) {
            EventType.SHIFT                 -> workjamClient.coworkers(WOOLIES, event.location.id, event.id)
            EventType.AVAILABILITY_TIME_OFF -> emptyList()
        }

        val vm = createViewModel(workjamClient, event, coworkingPositions, employeeDataStore)
        val description = descriptionGenerator.generate(vm)

        return event.toGoogleEvent(description, calendarZoneId)
    }
}