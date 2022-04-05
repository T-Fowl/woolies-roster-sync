package com.tfowl.woolies.sync

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.CalendarScopes
import com.tfowl.googleapi.DEFAULT_STORAGE_DIR
import com.tfowl.googleapi.GoogleApiServiceConfig
import com.tfowl.googleapi.GoogleCalendar
import com.tfowl.woolies.sync.utils.*
import com.tfowl.woolies.sync.utils.readCookies
import com.tfowl.workjam.client.WorkjamClientProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val WOOLIES = "6773940"
private const val APPLICATION_NAME = "APPLICATION_NAME"
private const val EMPLOYEE_DATASTORE_ID = "EmployeeDetails"
private const val WORKJAM_TOKEN_COOKIE_DOMAIN = "app.workjam.com"
private const val WORKJAM_TOKEN_COOKIE_NAME = "token"
private const val DEFAULT_CLIENT_SECRETS_FILE = "client-secrets.json"
private const val ICAL_SUFFIX = "@workjam.tfowl.com"

private fun RawOption.offsetDateTime(formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME): NullableOption<OffsetDateTime, OffsetDateTime> =
    convert("OFFSET_DATE_TIME") {
        it.toOffsetDateTimeOrNull(formatter)
            ?: fail("A date in the $formatter format is required")
    }

private fun List<Cookie>.findWorkjamTokenOrNull(): String? =
    firstOrNull { it.domain == WORKJAM_TOKEN_COOKIE_DOMAIN && it.name == WORKJAM_TOKEN_COOKIE_NAME }?.value

class SyncCommand : CliktCommand(name = "woolies-roster-sync") {
    init {
        context {
            helpFormatter = CliktHelpFormatter(
                showDefaultValues = true,
                showRequiredTag = true,
            )
        }
    }

    private val googleCalendarId by option("--calendar-id", help = "ID of the destination google calendar")
        .required()

    private val googleClientSecrets by option("--secrets", help = "Google calendar api secrets file")
        .file(mustBeReadable = true, canBeDir = false)
        .default(File(DEFAULT_CLIENT_SECRETS_FILE))

    private val tokenFromCookies = option(
        "--cookies",
        help = "Cookies file in netscape format. Only needed on first run or when stored tokens have expired"
    )
        .path(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert("FILE") {
            it.readCookies().findWorkjamTokenOrNull() ?: fail("Cookies file did not contain a workjam token cookie")
        }

    private val tokenFromArgs = option("--token", help = "Workjam jwt")

    private val workjamTokenOverride by mutuallyExclusiveOptions(tokenFromCookies, tokenFromArgs).single()

    private val syncPeriodStart by option(
        help = "Date to start syncing shifts, in the ISO_OFFSET_DATE_TIME format",
        helpTags = mapOf("Example" to "2007-12-03T10:15:30+01:00")
    )
        .offsetDateTime()
        .default(OffsetDateTime.now(), defaultForHelp = "now")

    private val syncPeriodEnd by option(
        help = "Date to finish syncing shifts, in the ISO_OFFSET_DATE_TIME format",
        helpTags = mapOf("Example" to "2007-12-03T10:15:30+01:00")
    )
        .offsetDateTime()
        .default(OffsetDateTime.now().plusDays(15), defaultForHelp = "15 days from now")

    override fun run() = runBlocking {
        val dsf: DataStoreFactory = FileDataStoreFactory(File(DEFAULT_STORAGE_DIR))

        val workjam = WorkjamClientProvider.create(DataStoreCredentialStorage(dsf))
            .createClient("user", workjamTokenOverride)

        val googleCalendar = GoogleCalendar.create(
            GoogleApiServiceConfig(
                secrets = googleClientSecrets,
                applicationName = APPLICATION_NAME,
                scopes = listOf(CalendarScopes.CALENDAR),
                dataStoreFactory = dsf
            )
        )

        val iCalManager = ICalManager(suffix = ICAL_SUFFIX)

        val calendarZoneId =
            googleCalendar.calendars().get(googleCalendarId).execute().timeZone?.toZoneIdOrNull()
                ?: googleCalendar.settings().get("timezone").execute().value.toZoneIdOrNull()
                ?: ZoneId.systemDefault()

        val transformer = EventTransformer(
            workjam,
            WOOLIES,
            dsf.getDataStorage(EMPLOYEE_DATASTORE_ID, Json),
            calendarZoneId,
            iCalManager,
            DefaultDescriptionGenerator,
        )

        val synchronizer = CalendarSynchronizer(googleCalendar, iCalManager)

        val workjamShifts = workjam.events(WOOLIES, workjam.userId, syncPeriodStart, syncPeriodEnd)

        val workjamEvents = workjamShifts.mapNotNull { transformer.transform(it) }

        synchronizer.sync(googleCalendarId, syncPeriodStart.toInstant(), syncPeriodEnd.toInstant(), workjamEvents)
    }
}