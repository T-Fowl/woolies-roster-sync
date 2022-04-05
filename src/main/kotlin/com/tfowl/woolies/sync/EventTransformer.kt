package com.tfowl.woolies.sync

import com.tfowl.googleapi.setEnd
import com.tfowl.googleapi.setStart
import com.tfowl.googleapi.toGoogleEventDateTime
import com.tfowl.woolies.sync.utils.ICalManager
import com.tfowl.workjam.client.WorkjamClient
import com.tfowl.workjam.client.model.*
import java.time.ZoneId
import com.google.api.services.calendar.model.Event as GoogleEvent

internal const val TIME_OFF_SUMMARY = "Time Off"

internal class EventTransformer(
    private val workjam: WorkjamClient,
    private val company: String,
    private val calendarZoneId: ZoneId,
    private val iCalManager: ICalManager,
    private val descriptionGenerator: DescriptionGenerator,
    private val summaryGenerator: SummaryGenerator,
) {

    private suspend fun transformShift(shift: Shift): GoogleEvent {
        val event = shift.event

        val coworkers = workjam.coworkers(company, event.location.id, event.id)

        val summary = summaryGenerator.generate(shift)
        val vm = createViewModel(shift, summary, coworkers)
        val description = descriptionGenerator.generate(vm)

        return GoogleEvent()
            .setStart(event.startDateTime, calendarZoneId)
            .setEnd(event.endDateTime, calendarZoneId)
            .setSummary(summary)
            .setICalUID(iCalManager.generate(event))
            .setDescription(description)
    }

    private fun transformTimeOff(availability: Availability): GoogleEvent {
        val event = availability.event

        return GoogleEvent()
            .setStart(event.startDateTime.toGoogleEventDateTime())
            .setEnd(event.endDateTime.toGoogleEventDateTime())
            .setSummary(TIME_OFF_SUMMARY)
            .setICalUID(iCalManager.generate(event))
    }

    suspend fun transform(event: ScheduleEvent): GoogleEvent? = when (event.type) {
        ScheduleEventType.SHIFT -> {
            val shift = workjam.shift(company, event.location.id, event.id)
            transformShift(shift)
        }
        ScheduleEventType.AVAILABILITY_TIME_OFF -> {
            val availability = workjam.availability(company, workjam.userId, event.id)
            transformTimeOff(availability)
        }
        ScheduleEventType.N_IMPORTE_QUOI -> {
            // TODO: Log warning?
            null
        }
        else -> null
    }
}

private fun createViewModel(
    shift: Shift,
    title: String,
    storePositions: List<Coworker>,
): ShiftViewModel {

    fun Employee.toViewModel(): CoworkerViewModel {
        val avatarUrl = avatarUrl?.replace(
            "/image/upload",
            "/image/upload/f_auto,q_auto,w_64,h_64,c_thumb,g_face"
        )
        return CoworkerViewModel(firstName, lastName, avatarUrl)
    }

    val storePositionViewModels = storePositions.map { (position, coworkers) ->
        StorePositionViewModel(
            position = position.externalCode,
            coworkers = coworkers.map { it.toViewModel() }.sortedBy { it.firstName }
        )
    }.sortedBy { it.position }

    return ShiftViewModel(title, shift.event.startDateTime, shift.event.endDateTime, storePositionViewModels)
}