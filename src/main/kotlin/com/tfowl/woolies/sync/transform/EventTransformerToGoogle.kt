package com.tfowl.woolies.sync.transform

import com.tfowl.gcal.buildSafeExtendedProperties
import com.tfowl.gcal.setEnd
import com.tfowl.gcal.setStart
import com.tfowl.gcal.toGoogleEventDateTime
import com.tfowl.workjam.client.WorkjamClient
import com.tfowl.workjam.client.model.*
import com.tfowl.workjam.client.model.serialisers.InstantSerialiser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.google.api.services.calendar.model.Event as GoogleEvent

internal const val TIME_OFF_SUMMARY = "Time Off"

/**
 * Responsible for transforming [ScheduleEvent]s into [GoogleEvent]s
 */
internal class EventTransformerToGoogle(
    private val workjam: WorkjamClient,
    private val company: String,
    private val domain: String,
    private val descriptionGenerator: DescriptionGenerator,
    private val summaryGenerator: SummaryGenerator,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(InstantSerialiser(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS[XX][XXX][ZZZ][OOOO]")))
        }
    }

    private fun transformShift(shift: Shift, store: Store, summary: String, description: String): GoogleEvent {
        val event = shift.event

        return GoogleEvent().setStart(event.startDateTime, event.location.timeZoneID)
            .setEnd(event.endDateTime, event.location.timeZoneID)
            .setSummary(summary)
            .setICalUID("${event.id}@$domain")
            .setDescription(description)
            .setLocation(store.renderAddress())
            .buildSafeExtendedProperties {
                private {
                    prop("schedule-event-type" to event.type.name)
                    prop("shift:json" to json.encodeToString(shift))
                }
            }
    }

    private suspend fun transformShift(shift: Shift): GoogleEvent = coroutineScope {
        val event = shift.event
        val location = event.location
        val zone = location.timeZoneID

        val storeAsync = async(Dispatchers.IO) {
            workjam.employers(workjam.userId).companies.firstNotNullOf { company ->
                company.stores.find { store -> store.externalID == location.externalID }
            }
        }

        val storeRosterAsync = async(Dispatchers.IO) {
            workjam.shifts(
                company,
                location.id,
                startDateTime = LocalDate.ofInstant(event.startDateTime, zone).atStartOfDay(zone).toOffsetDateTime(),
                endDateTime = LocalDate.ofInstant(event.startDateTime, zone).plusDays(1).atStartOfDay(zone)
                    .toOffsetDateTime()
            )
        }

        val store = storeAsync.await()
        val storeRoster = storeRosterAsync.await()

        val summary = summaryGenerator.generate(shift)
        val describableShift = DescribableShift.create(shift, summary, storeRoster)
        val description = descriptionGenerator.generate(describableShift)

        transformShift(shift, store, summary, description)
    }

    private fun transformTimeOff(availability: Availability): GoogleEvent {
        val event = availability.event

        val startOfDay = event.startDateTime.atZone(event.location.timeZoneID)
            .with(LocalTime.MIDNIGHT)
            .toInstant()

        val endOfDay = event.endDateTime.atZone(event.location.timeZoneID)
            .plusDays(1).with(LocalTime.MIDNIGHT)
            .toInstant()

        return GoogleEvent()
            .setStart(startOfDay.toGoogleEventDateTime())
            .setEnd(endOfDay.toGoogleEventDateTime())
            .setSummary(TIME_OFF_SUMMARY)
            .setICalUID("${event.id}@$domain")
            .buildSafeExtendedProperties {
                private {
                    prop("schedule-event-type" to event.type.name)
                    prop("availability:json" to json.encodeToString(availability))
                }
            }
    }

    suspend fun transformAll(events: List<ScheduleEvent>): List<GoogleEvent> {

        val consolidatedTimeOff = events
            .filter { it.type == ScheduleEventType.AVAILABILITY_TIME_OFF }
            .sortedBy { it.startDateTime }
            .fold(emptyList<ScheduleEvent>()) { list, current ->
                /*
                    Combines all consecutive time-off events into one long event
                    TODO: When syncing we need to check if we can condense into a time-off event just before the sync period
                          Also should we find a way to store a reference of all the combined events? e.g. extendedProperties?
                */

                if (list.firstOrNull()?.type == ScheduleEventType.AVAILABILITY_TIME_OFF && current.type == ScheduleEventType.AVAILABILITY_TIME_OFF) {
                    list.dropLast(1) + list.last().copy(endDateTime = current.endDateTime)
                } else {
                    list + current
                }
            }

        val consolidatedEvents = events
            .filter { it.type != ScheduleEventType.AVAILABILITY_TIME_OFF }
            .plus(consolidatedTimeOff)
            .sortedBy { it.startDateTime }

        return coroutineScope {
            consolidatedEvents.map { async(Dispatchers.IO) { transform(it) } }.awaitAll().filterNotNull()
        }
    }

    suspend fun transform(event: ScheduleEvent): GoogleEvent? = when (event.type) {
        ScheduleEventType.SHIFT                 -> {
            val shift = workjam.shift(company, event.location.id, event.id)
            transformShift(shift)
        }

        ScheduleEventType.AVAILABILITY_TIME_OFF -> {
            val availability = workjam.availability(company, workjam.userId, event.id)
            transformTimeOff(availability)
        }

        ScheduleEventType.N_IMPORTE_QUOI        -> {
            // TODO: Log warning?
            null
        }

        else                                    -> null
    }
}

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