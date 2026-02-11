package com.tfowl.woolies.sync.transform

import com.tfowl.workjam.client.WorkjamClient
import com.tfowl.workjam.client.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.Location
import net.fortuna.ical4j.model.property.Uid
import java.time.LocalDate

internal class EventTransformerToICal(
    private val workjam: WorkjamClient,
    private val company: String,
    private val domain: String,
    private val descriptionGenerator: DescriptionGenerator,
    private val summaryGenerator: SummaryGenerator,
) {
    private fun transformShift(shift: Shift, store: Store, summary: String, description: String): VEvent {
        val event = shift.event

        return VEvent(event.startDateTime, event.endDateTime, summary)
            .withProperty(Uid("${event.id}@$domain"))
            .withProperty(Description(description))
            .withProperty(Location(store.renderAddress()))
            .fluentTarget as VEvent
    }

    private suspend fun transformShift(shift: Shift): VEvent = coroutineScope {
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

    private fun transformTimeOff(availability: Availability): VEvent {
        val event = availability.event

        return VEvent(event.startDateTime, event.endDateTime, TIME_OFF_SUMMARY)
            .withProperty(Uid("${event.id}@$domain"))
            .fluentTarget as VEvent
    }

    suspend fun transformAll(events: List<ScheduleEvent>): List<VEvent> {

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

    suspend fun transform(event: ScheduleEvent): VEvent? = when (event.type) {
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