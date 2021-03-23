package com.tfowl.workjam

import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.CalendarScopes
import com.tfowl.googleapi.*
import com.tfowl.workjam.client.WorkjamClient
import com.tfowl.workjam.client.WorkjamProvider
import com.tfowl.workjam.client.model.*
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import com.google.api.services.calendar.model.Event as GEvent

private const val WOOLIES = "6773940"
private const val CLIENT_SECRETS_FILE = "client-secrets.json"
private const val APPLICATION_NAME = "APPLICATION_NAME"
private const val EVENT_DESCRIPTION_TEMPLATE = "event-description.hbs"
private const val EMPLOYEE_DETAILS_NAME = "EmployeeDetails"

private fun String.removeWorkjamPositionPrefix(): String = removePrefix("*SUP-").removePrefix("SUP-")

fun shiftSummary(
    start: LocalTime,
    end: LocalTime,
    default: String?,
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
    return default?.removeWorkjamPositionPrefix() ?: "Error: Missing Title"
}

private suspend fun createViewModel(
    workjam: WorkjamClient,
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

    return DescriptionViewModel(coworkerPositionsViewModels)
}

val Event.iCalUID get() = "$id@workjam.tfowl.com"

fun Event.createGoogleEvent(description: String, zoneId: ZoneId = ZoneId.systemDefault()): GEvent {
    val start = startDateTime.toLocalDateTime(zoneId)
    val end = endDateTime.toLocalDateTime(zoneId)

    val summary = when (type) {
        EventType.SHIFT                 -> shiftSummary(start.toLocalTime(), end.toLocalTime(), title)
        EventType.AVAILABILITY_TIME_OFF -> "Time Off"
    }

    return GEvent()
        .setStart(startDateTime.toGoogleEventDateTime())
        .setEnd(endDateTime.toGoogleEventDateTime())
        .setSummary(summary)
        .setICalUID(iCalUID)
        .setDescription(description)
}

@ExperimentalSerializationApi
suspend fun main(vararg args: String) = coroutineScope {
    require(args.size >= 2) { "Usage: workjam-schedule-sync id calendar [workjam jwt]" }

    val USER_ID = args[0]
    val CALENDAR_ID = args[1]
    val TOKEN_OVERRIDE = args.elementAtOrNull(2)

    /* Included by google apis, might as well use for our own serialisation */
    val dsf: DataStoreFactory = FileDataStoreFactory(File(DEFAULT_TOKENS_DIR))

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

    val calendar = GoogleCalendar.create(
        GoogleApiServiceConfig(
            secrets = File(CLIENT_SECRETS_FILE),
            applicationName = APPLICATION_NAME,
            scopes = listOf(CalendarScopes.CALENDAR)
        )
    )
    val timetableEvents = calendar.calendarEvents(CALENDAR_ID)

    val googleEvents = timetableEvents.list()
        .setMaxResults(2500)
        .setTimeMin(syncPeriodStart.toGoogleDateTime())
        .setTimeMax(syncPeriodEnd.toGoogleDateTime())
        .setShowDeleted(true)
        .execute().items

    val zoneId = ZoneId.of("Australia/Sydney")

    val employeeDataStorage = dsf.getDataStore<String>(EMPLOYEE_DETAILS_NAME).asDataStorage<Employee>(json)

    val descriptionGenerator = MustacheDescriptionGenerator(EVENT_DESCRIPTION_TEMPLATE)

    val batch = calendar.batch()

    for (shift in workjamShifts) {
        val coworkingPositions = when (shift.type) {
            EventType.SHIFT                 -> workjam.coworkers(WOOLIES, shift.location.id, shift.id)
            EventType.AVAILABILITY_TIME_OFF -> emptyList()
        }

        val vm = createViewModel(workjam, coworkingPositions, employeeDataStorage)
        val description = descriptionGenerator.generate(vm)

        val event = shift.createGoogleEvent(description, zoneId)

        val existingGoogleEvent = googleEvents.find { it.iCalUID == event.iCalUID }

        if (null != existingGoogleEvent) {
            timetableEvents.update(
                existingGoogleEvent.id,
                event.setId(existingGoogleEvent.id).setStatus("confirmed")
            )
                .queue(batch,
                    success = { println("Updated existing event ${event.iCalUID}") },
                    failure = { println("Failed to update existing event ${event.iCalUID}: ${it.toPrettyString()}") }
                )
        } else {
            timetableEvents.insert(event)
                .queue(batch,
                    success = { println("Created event ${event.iCalUID}") },
                    failure = { println("Failed to create event ${event.iCalUID}: ${it.toPrettyString()}") }
                )
        }

        println("Queueued shift ${event.summary} [${event.start}-${event.end}] (${event.iCalUID})")
    }

    batch.execute()
}