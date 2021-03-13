package com.tfowl.workjam

import com.google.api.client.util.store.DataStore
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.tfowl.googleapi.*
import com.tfowl.workjam.client.Workjam
import com.tfowl.workjam.client.WorkjamProvider
import com.tfowl.workjam.client.model.EventType
import com.tfowl.workjam.client.model.PositionedCoworkers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.system.exitProcess

const val WOOLIES = "6773940"

private fun String.removeWorkjamPositionPrefix(): String = removePrefix("*SUP-").removePrefix("SUP-")

fun shiftSummary(
    start: LocalTime,
    end: LocalTime,
    default: String,
): String {

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
    return default.removeWorkjamPositionPrefix()
}

private suspend fun createViewModel(
    workjam: Workjam,
    json: Json,
    coworkingPositions: List<PositionedCoworkers>,
    employeeDataStore: DataStore<String>
): DescriptionViewModel = DescriptionViewModel(coworkerPositions = coworkingPositions.map { (coworkers, position) ->
    CoworkerPositionViewModel(
        position = position.externalCode,
        coworkers = coworkers.map { coworker ->
            val employeeDetails = employeeDataStore.computeIfAbsent(json, coworker.id) { id ->
                workjam.employee(WOOLIES, id)
            }
            val employeeNumber = employeeDetails.externalCode ?: ""
            val avatarUrl = coworker.avatarUrl?.replace(
                "/image/upload",
                "/image/upload/f_auto,q_auto,w_64,h_64,c_thumb,g_face"
            )
            CoworkerViewModel(coworker.firstName, coworker.lastName, employeeNumber, avatarUrl)
        }.sortedBy { it.firstName })
}.sortedBy { it.position })

fun Event.isSameDay(other: Event): Boolean {
    val thisDay = start.dateTime.toOffsetDateTime().toLocalDate()
    val thatDay = other.start.dateTime.toOffsetDateTime().toLocalDate()
    return thisDay == thatDay
}

@ExperimentalSerializationApi
suspend fun main(vararg args: String) = coroutineScope {
    require(args.size >= 2) { "Usage: workjam-schedule-sync id calendar [workjam jwt]" }

    val USER_ID = args[0]
    val CALENDAR_ID = args[1]
    val TOKEN_OVERRIDE = args.elementAtOrNull(2)

    /* Included by google apis, might as well use for our own serialisation */
    val dsf: DataStoreFactory = FileDataStoreFactory(File(TOKENS_DIRECTORY))

    val json = Json {
        ignoreUnknownKeys = true
    }

    val workjamProvider = WorkjamProvider.create(DataStoreCredentialStorage(dsf))
    val workjam = workjamProvider.create(USER_ID, TOKEN_OVERRIDE)

    val syncPeriodStart = OffsetDateTime.now()
    val syncPeriodEnd = OffsetDateTime.now().plusDays(15)

    val workjamShifts = workjam.events(
        WOOLIES, USER_ID,
        syncPeriodStart, syncPeriodEnd
    )

    val timetableCalendar = GoogleCalendar.calendar.calendarEvents(CALENDAR_ID)

    val googleEvents = timetableCalendar.list()
        .setMaxResults(2500)
        .setTimeMin(syncPeriodStart.toGoogleDateTime())
        .setTimeMax(syncPeriodEnd.toGoogleDateTime())
        .setShowDeleted(true)
        .execute().items

    val zoneId = ZoneId.of("Australia/Sydney")

    val employeeDataStore = dsf.getDataStore<String>("EmployeeDetails")

    val descriptionGenerator = MustacheDescriptionGenerator("event-description.hbs")

    val batch = GoogleCalendar.calendar.batch()

    for (shift in workjamShifts) {
        val coworkingPositions = when (shift.type) {
            EventType.SHIFT                 -> workjam.coworkers(WOOLIES, shift.location.id, shift.id)
            EventType.AVAILABILITY_TIME_OFF -> emptyList()
        }

        val start = shift.startDateTime.toLocalDateTime(zoneId)
        val end = shift.endDateTime.toLocalDateTime(zoneId)

        val vm = createViewModel(workjam, json, coworkingPositions, employeeDataStore)
        val description = descriptionGenerator.generate(vm)

        val summary = when (shift.type) {
            EventType.SHIFT                 -> shiftSummary(
                start.toLocalTime(),
                end.toLocalTime(),
                shift.title ?: "ERROR: MISSING TITLE"
            )
            EventType.AVAILABILITY_TIME_OFF -> "Time Off"
        }

        val event = Event()
            .setStart(EventDateTime().setDateTime(shift.startDateTime.toGoogleDateTime()))
            .setEnd(EventDateTime().setDateTime(shift.endDateTime.toGoogleDateTime()))
            .setSummary(summary)
            .setICalUID("${shift.id}@workjam.tfowl.com")
            .setDescription(description)

        val existingGoogleEvent = googleEvents.find { it.iCalUID == event.iCalUID }

        if (null != existingGoogleEvent) {
            timetableCalendar.update(
                    existingGoogleEvent.id,
                    event.setId(existingGoogleEvent.id).setStatus("confirmed")
                )
                .queue(batch,
                    success = { println("Updated existing event ${event.iCalUID}") },
                    failure = { println("Failed to update existing event ${event.iCalUID}: ${it.toPrettyString()}") }
                )
        } else {
            timetableCalendar.insert(event)
                .queue(batch,
                    success = { println("Created event ${event.iCalUID}") },
                    failure = { println("Failed to create event ${event.iCalUID}: ${it.toPrettyString()}") }
                )
        }

        println("Queueued shift ${event.summary} [${event.start}-${event.end}] (${event.iCalUID})")
    }

    batch.execute()
}