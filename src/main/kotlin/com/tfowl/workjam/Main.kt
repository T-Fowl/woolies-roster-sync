package com.tfowl.workjam

import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.Calendar
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
private const val EMPLOYEE_DATASTORE_ID = "EmployeeDetails"
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

private fun Event.toGoogleEvent(description: String, zoneId: ZoneId = ZoneId.systemDefault()): GEvent {
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
    workjam: WorkjamClient,
    employeeId: String,
    employeeDataStorage: DataStorage<Employee>,
    syncPeriodStart: OffsetDateTime,
    syncPeriodEnd: OffsetDateTime
): List<GEvent> {

    val workjamShifts = workjam.events(
        WOOLIES, employeeId,
        syncPeriodStart, syncPeriodEnd
    )

    val descriptionGenerator = MustacheDescriptionGenerator(EVENT_DESCRIPTION_TEMPLATE)

    return workjamShifts.map { shift ->
        val coworkingPositions = when (shift.type) {
            EventType.SHIFT                 -> workjam.coworkers(WOOLIES, shift.location.id, shift.id)
            EventType.AVAILABILITY_TIME_OFF -> emptyList()
        }

        val vm = createViewModel(workjam, coworkingPositions, employeeDataStorage)
        val description = descriptionGenerator.generate(vm)

        shift.toGoogleEvent(description, ZoneId.of("Australia/Sydney"))
    }
}

private fun syncShiftsToGoogleCalendar(
    calendar: Calendar,
    calendarId: String,
    syncPeriodStart: OffsetDateTime,
    syncPeriodEnd: OffsetDateTime,
    shifts: List<GEvent>
) {
    val timetable = calendar.calendarView(calendarId)

    val events = timetable.list()
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
    calendar.batched {
        timetable.queue(batch, actions)
    }
}

private fun createGoogleSyncActions(currents: List<GEvent>, targets: List<GEvent>): List<SyncAction> {
    val create = targets.filter { shift -> currents.none { it.iCalUID == shift.iCalUID } }
        .map { SyncAction.Create(it) }

    val update = targets.mapNotNull { shift ->
        currents.find { it.iCalUID == shift.iCalUID }?.let { existing ->
            SyncAction.Update(existing, shift)
        }
    }

    val delete = currents
        .filter { event -> !event.isCancelled() && targets.none { it.iCalUID == event.iCalUID } }
        .map { SyncAction.Delete(it) }

    return create + update + delete
}

private suspend fun sync(
    workjamUserId: String,
    workjamTokenOverride: String?,
    googleCalendarId: String,
    syncPeriodStart: OffsetDateTime,
    syncPeriodEnd: OffsetDateTime
) {
    val dsf: DataStoreFactory = FileDataStoreFactory(File(DEFAULT_TOKENS_DIR))

    val employeeDataStorage = dsf.getDataStorage<Employee>(EMPLOYEE_DATASTORE_ID, Json {
        ignoreUnknownKeys = true
    })

    val workjam = WorkjamProvider.create(DataStoreCredentialStorage(dsf))
        .create(workjamUserId, workjamTokenOverride)
    val workjamShifts =
        retrieveWorkjamShifts(workjam, workjamUserId, employeeDataStorage, syncPeriodStart, syncPeriodEnd)


    val googleCalendar = GoogleCalendar.create(
        GoogleApiServiceConfig(
            secrets = File(CLIENT_SECRETS_FILE),
            applicationName = APPLICATION_NAME,
            scopes = listOf(CalendarScopes.CALENDAR),
            dataStoreFactory = dsf
        )
    )

    syncShiftsToGoogleCalendar(googleCalendar, googleCalendarId, syncPeriodStart, syncPeriodEnd, workjamShifts)
}

@ExperimentalSerializationApi
suspend fun main(vararg args: String) = coroutineScope {
    require(args.size >= 2) { "Usage: workjam-schedule-sync id calendar [workjam jwt]" }

    val workjamUserId = args[0]
    val googleCalendarId = args[1]
    val workjamTokenOverride = args.elementAtOrNull(2)

    val syncPeriodStart = OffsetDateTime.now()
    val syncPeriodEnd = OffsetDateTime.now().plusDays(15)

    sync(workjamUserId, workjamTokenOverride, googleCalendarId, syncPeriodStart, syncPeriodEnd)
}