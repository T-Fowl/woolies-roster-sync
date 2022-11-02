package com.tfowl.woolies.sync.transform

import com.tfowl.googleapi.*
import com.tfowl.woolies.sync.utils.ICalManager
import com.tfowl.workjam.client.WorkjamClient
import com.tfowl.workjam.client.model.*
import com.tfowl.workjam.client.model.serialisers.InstantSerialiser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.google.api.services.calendar.model.Event as GoogleEvent

internal const val TIME_OFF_SUMMARY = "Time Off"

/**
 * Responsible for transforming [ScheduleEvent]s into [GoogleEvent]s
 */
internal class EventTransformer(
    private val workjam: WorkjamClient,
    private val company: String,
    private val iCalManager: ICalManager,
    private val descriptionGenerator: DescriptionGenerator,
    private val summaryGenerator: SummaryGenerator,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(InstantSerialiser(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS[XX][XXX][ZZZ][OOOO]")))
        }
    }

    private suspend fun transformShift(shift: Shift): GoogleEvent {
        val event = shift.event
        val location = event.location
        val zone = location.timeZoneID

        val store = workjam.employers(workjam.userId).companies.firstNotNullOf { company ->
            company.stores.find { store -> store.externalID == location.externalID }
        }

        val storeRoster = workjam.shifts(
            company, location.id,
            startDateTime = LocalDate.ofInstant(event.startDateTime, zone).atStartOfDay(zone).toOffsetDateTime(),
            endDateTime = LocalDate.ofInstant(event.startDateTime, zone).plusDays(1).atStartOfDay(zone)
                .toOffsetDateTime()
        )

        val summary = summaryGenerator.generate(shift)
        val describableShift = createDescribableShift(shift, summary, storeRoster)
        val description = descriptionGenerator.generate(describableShift)

        GoogleEvent()
            .setStart(event.startDateTime, event.location.timeZoneID)
            .setEnd(event.endDateTime, event.location.timeZoneID)
            .setSummary(summary)
            .setICalUID(iCalManager.generate(event))
            .setDescription(description)
            .setLocation(store.renderAddress())
            .buildExtendedProperties {
                private {
                    "schedule-event-type" prop event.type.name
                    "shift:json" prop json.encodeToString(shift)
                }
            }
    }

    private fun transformTimeOff(availability: Availability): GoogleEvent {
        val event = availability.event

        return GoogleEvent()
            .setStart(event.startDateTime.toGoogleEventDateTime())
            .setEnd(event.endDateTime.toGoogleEventDateTime())
            .setSummary(TIME_OFF_SUMMARY)
            .setICalUID(iCalManager.generate(event))
            .buildExtendedProperties {
                private {
                    "schedule-event-type" prop event.type.name
                    "availability:json" prop json.encodeToString(availability)
                }
            }
    }

    suspend fun transformAll(events: List<ScheduleEvent>): List<GoogleEvent> {
        val sortedEvents = events.sortedBy { it.startDateTime }

        val condensedEvents = sortedEvents.fold(emptyList<ScheduleEvent>()) { list, current ->
            /*
                Combines all consecutive time-off events into one long event
                TODO: When syncing we need to check if we can condense into a time-off event just before the sync period
                      Also should we find a way to store a reference of all the combined events? e.g. extendedProperties?
            */
            if (list.isNotEmpty() && list.last().type == ScheduleEventType.AVAILABILITY_TIME_OFF && current.type == ScheduleEventType.AVAILABILITY_TIME_OFF) {
                list.dropLast(1) + list.last().copy(endDateTime = current.endDateTime)
            } else {
                list + current
            }
        }

        return condensedEvents.mapNotNull { transform(it) }
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

private fun createDescribableShift(
    shift: Shift,
    title: String,
    storeRoster: List<Shift>,
): DescribableShift {

    fun Shift.toDescribableAsignees(): List<DescribableShiftAssignee> {
        return assignees.map { assignee ->
            val profile = assignee.profile

            DescribableShiftAssignee(
                profile.firstName, profile.lastName,
                profile.avatarURL?.replace(
                    "/image/upload",
                    "/image/upload/f_auto,q_auto,w_64,h_64,c_thumb,g_face"
                ),
                startTime, endTime, event.type
            )
        }
    }

    val describableStorePositions = storeRoster.groupBy { it.position.name.removeSupPrefix() }
        .toSortedMap()
        .map { (position, positionedShifts) ->
            DescribableStorePosition(
                position,
                positionedShifts.flatMap { it.toDescribableAsignees() }.sortedBy { it.startTime })
        }

    return DescribableShift(shift, title, describableStorePositions)
}

// e.g: Woolworths, 224-238 Mt Dandenong Rd, Croydon VIC 3136, Australia
private fun Store.renderAddress(): String = buildString {
    val address = storeAddress

    // TODO: This is only really designed to work with stores
    append("Woolworths, ")
    append(address.streetLine1)
    address.streetLine2?.let { append(' ').append(it) }
    address.streetLine3?.let { append(' ').append(it) }
    append(", ").append(address.city.name)
    append(" ").append(address.province.name)
    append(" ").append(address.postalCode)
    append(", ").append(address.country.name)
}