package com.tfowl.workjam

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.tfowl.googleapi.*
import com.tfowl.workjam.client.WorkjamClient
import com.tfowl.workjam.client.WorkjamProvider
import com.tfowl.workjam.client.model.*
import kotlinx.coroutines.runBlocking
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
    employeeDataStorage: DataStorage<Employee>,
    syncPeriodStart: OffsetDateTime,
    syncPeriodEnd: OffsetDateTime
): List<GEvent> {

    val workjamShifts = workjam.events(
        WOOLIES, workjam.userId,
        syncPeriodStart, syncPeriodEnd
    )

    val descriptionGenerator = MustacheDescriptionGenerator(EVENT_DESCRIPTION_TEMPLATE)

    return workjamShifts.map { shift ->
        val coworkingPositions = when (shift.type) {
            EventType.SHIFT                 -> workjam.coworkers(WOOLIES, shift.location.id, shift.id)
            EventType.AVAILABILITY_TIME_OFF -> emptyList()
        }

        val vm = createViewModel(workjam, shift, coworkingPositions, employeeDataStorage)
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

class Sync : CliktCommand() {
    val googleCalendarId by option("--calendar-id", help = "ID of the destination google calendar")
        .required()

    val cookies by option(
        "--cookies",
        help = "Cookies file, in netscape format. Only needed when first run or after the currently stored token has expired"
    )
        .path(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert { it.readCookies() }

    val token by option("--token", help = "Workjam jwt")

    val syncPeriodStart by option(help = "Date to start syncing shifts, in the ISO_OFFSET_DATE_TIME (e.g. 2007-12-03T10:15:30+01:00) format")
        .convert("OFFSET_DATE_TIME") {
            it.toOffsetDateTimeOrNull()
                ?: fail("A date in the ISO_OFFSET_DATE_TIME (e.g. 2007-12-03T10:15:30+01:00) format is required")
        }
        .default(OffsetDateTime.now())

    val syncPeriodEnd by option(help = "Date to finish syncing shifts, in the ISO_OFFSET_DATE_TIME (e.g. 2007-12-03T10:15:30+01:00) format")
        .convert("OFFSET_DATE_TIME") {
            it.toOffsetDateTimeOrNull()
                ?: fail("A date in the ISO_OFFSET_DATE_TIME (e.g. 2007-12-03T10:15:30+01:00) format is required")
        }
        .default(OffsetDateTime.now().plusDays(15))

    override fun run() = runBlocking {
        val dsf: DataStoreFactory = FileDataStoreFactory(File(DEFAULT_TOKENS_DIR))

        val employeeDataStorage = dsf.getDataStorage<Employee>(EMPLOYEE_DATASTORE_ID, Json)

        val workjamTokenOverride =
            token ?: cookies?.firstOrNull { it.domain == "app.workjam.com" && it.name == "token" }
                ?.value

        val workjam = WorkjamProvider.create(DataStoreCredentialStorage(dsf))
            .create("user", workjamTokenOverride)

        val googleCalendar = GoogleCalendar.create(
            GoogleApiServiceConfig(
                secrets = File(CLIENT_SECRETS_FILE),
                applicationName = APPLICATION_NAME,
                scopes = listOf(CalendarScopes.CALENDAR),
                dataStoreFactory = dsf
            )
        )

        val workjamShifts =
            retrieveWorkjamShifts(workjam, employeeDataStorage, syncPeriodStart, syncPeriodEnd)

        syncShiftsToGoogleCalendar(googleCalendar, googleCalendarId, syncPeriodStart, syncPeriodEnd, workjamShifts)
    }
}

fun main(vararg args: String) = Sync().main(args)