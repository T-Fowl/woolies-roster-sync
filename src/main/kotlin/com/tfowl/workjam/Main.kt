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
private const val DEFAULT_ICAL_SUFFIX = "@workjam.tfowl.com"
private const val TIME_OFF_SUMMARY = "Time Off"

private val Event.iCalUID get() = "$id$DEFAULT_ICAL_SUFFIX"

private fun String.removeWorkjamPositionPrefix(): String = removePrefix("*SUP-").removePrefix("SUP-")

private fun shiftSummary(
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

private fun Event.createGoogleEvent(description: String, zoneId: ZoneId = ZoneId.systemDefault()): GEvent {
    val start = startDateTime.toLocalDateTime(zoneId)
    val end = endDateTime.toLocalDateTime(zoneId)

    val summary = when (type) {
        EventType.SHIFT                 -> shiftSummary(start.toLocalTime(), end.toLocalTime(), title)
        EventType.AVAILABILITY_TIME_OFF -> TIME_OFF_SUMMARY
    }

    return GEvent()
        .setStart(startDateTime.toGoogleEventDateTime())
        .setEnd(endDateTime.toGoogleEventDateTime())
        .setSummary(summary)
        .setICalUID(iCalUID)
        .setDescription(description)
}

private suspend fun retrieveWorkjamShifts(
    USER_ID: String,
    TOKEN_OVERRIDE: String?,
    syncPeriodStart: OffsetDateTime,
    syncPeriodEnd: OffsetDateTime
): List<GEvent> {
    /* Included by google apis, might as well use for our own serialisation */
    val dsf: DataStoreFactory = FileDataStoreFactory(File(DEFAULT_TOKENS_DIR))
    val workjamProvider = WorkjamProvider.create(DataStoreCredentialStorage(dsf))
    val workjam = workjamProvider.create(USER_ID, TOKEN_OVERRIDE)

    val workjamShifts = workjam.events(
        WOOLIES, USER_ID,
        syncPeriodStart, syncPeriodEnd
    )

    val json = Json { ignoreUnknownKeys = true }
    val employeeDataStorage = dsf.getDataStore<String>(EMPLOYEE_DETAILS_NAME).asDataStorage<Employee>(json)
    val descriptionGenerator = MustacheDescriptionGenerator(EVENT_DESCRIPTION_TEMPLATE)

    return workjamShifts.map { shift ->
        val coworkingPositions = when (shift.type) {
            EventType.SHIFT                 -> workjam.coworkers(WOOLIES, shift.location.id, shift.id)
            EventType.AVAILABILITY_TIME_OFF -> emptyList()
        }

        val vm = createViewModel(workjam, coworkingPositions, employeeDataStorage)
        val description = descriptionGenerator.generate(vm)

        shift.createGoogleEvent(description, ZoneId.of("Australia/Sydney"))
    }
}

private fun syncShiftsToGoogleCalendar(
    CALENDAR_ID: String,
    syncPeriodStart: OffsetDateTime,
    syncPeriodEnd: OffsetDateTime,
    shifts: List<GEvent>
) {
    val calendar = GoogleCalendar.create(
        GoogleApiServiceConfig(
            secrets = File(CLIENT_SECRETS_FILE),
            applicationName = APPLICATION_NAME,
            scopes = listOf(CalendarScopes.CALENDAR)
        )
    )
    val timetableCalendar = calendar.calendarEvents(CALENDAR_ID)

    val events = timetableCalendar.list()
        .setMaxResults(2500)
        .setTimeMin(syncPeriodStart.toGoogleDateTime())
        .setTimeMax(syncPeriodEnd.toGoogleDateTime())
        .setShowDeleted(true)
        .execute().items
        .filter { DEFAULT_ICAL_SUFFIX in it.iCalUID }

    // TODO: How to handle this? Remove / Recreate / ignore etc
    events.forEach { e ->
        requireNotNull(e.start.dateTime) { "Unsupported null start datetime for $e" }
    }

    val actions = createGoogleSyncActions(events, shifts)
    calendar.batched { batch ->
        timetableCalendar.queue(batch, actions)
    }
}

private fun deleteRedundantShiftsActions(events: List<GEvent>, shifts: List<GEvent>) =
    events
        .filter { event -> !event.isCancelled() }
        .filter { event -> shifts.none { it.iCalUID == event.iCalUID } }
        .map { SyncAction.Delete(it) }

private fun updateExistingShiftsActions(events: List<GEvent>, shifts: List<GEvent>) =
    shifts.mapNotNull { shift ->
        events.find { it.iCalUID == shift.iCalUID }?.let { existing ->
            SyncAction.Update(existing, shift)
        }
    }

private fun createNewShiftsActions(events: List<GEvent>, shifts: List<GEvent>) =
    shifts.filter { shift -> events.none { it.iCalUID == shift.iCalUID } }
        .map { SyncAction.Create(it) }

private fun createGoogleSyncActions(events: List<GEvent>, shifts: List<GEvent>) =
    createNewShiftsActions(events, shifts) +
            updateExistingShiftsActions(events, shifts) +
            deleteRedundantShiftsActions(events, shifts)

@ExperimentalSerializationApi
suspend fun main(vararg args: String) = coroutineScope {
    require(args.size >= 2) { "Usage: workjam-schedule-sync id calendar [workjam jwt]" }

    val USER_ID = args[0]
    val CALENDAR_ID = args[1]
    val TOKEN_OVERRIDE = args.elementAtOrNull(2)


    val syncPeriodStart = OffsetDateTime.now()
    val syncPeriodEnd = OffsetDateTime.now().plusDays(15)

    val shiftEvents = retrieveWorkjamShifts(USER_ID, TOKEN_OVERRIDE, syncPeriodStart, syncPeriodEnd)

    syncShiftsToGoogleCalendar(CALENDAR_ID, syncPeriodStart, syncPeriodEnd, shiftEvents)
}