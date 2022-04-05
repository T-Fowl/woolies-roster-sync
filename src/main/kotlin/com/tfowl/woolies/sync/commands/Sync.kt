package com.tfowl.woolies.sync.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.CalendarScopes
import com.tfowl.googleapi.GoogleApiServiceConfig
import com.tfowl.googleapi.GoogleCalendar
import com.tfowl.woolies.sync.CalendarSynchronizer
import com.tfowl.woolies.sync.toOffsetDateTimeOrNull
import com.tfowl.woolies.sync.toZoneIdOrNull
import com.tfowl.woolies.sync.transform.DefaultDescriptionGenerator
import com.tfowl.woolies.sync.transform.DefaultSummaryGenerator
import com.tfowl.woolies.sync.transform.EventTransformer
import com.tfowl.woolies.sync.utils.Cookie
import com.tfowl.woolies.sync.utils.DataStoreCredentialStorage
import com.tfowl.woolies.sync.utils.ICalManager
import com.tfowl.woolies.sync.utils.readCookies
import com.tfowl.workjam.client.WorkjamClientProvider
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal const val WOOLIES = "6773940"
internal const val APPLICATION_NAME = "APPLICATION_NAME"
internal const val WORKJAM_TOKEN_COOKIE_DOMAIN = "app.workjam.com"
internal const val WORKJAM_TOKEN_COOKIE_NAME = "token"
internal const val DEFAULT_CLIENT_SECRETS_FILE = "client-secrets.json"
internal const val ICAL_SUFFIX = "@workjam.tfowl.com"
internal const val DEFAULT_STORAGE_DIR = ".roster-sync"

class Sync : CliktCommand(name = "sync") {
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
            calendarZoneId,
            iCalManager,
            DefaultDescriptionGenerator,
            DefaultSummaryGenerator,
        )

        val synchronizer = CalendarSynchronizer(googleCalendar, iCalManager)

        val workjamShifts = workjam.events(WOOLIES, workjam.userId, syncPeriodStart, syncPeriodEnd)

        val workjamEvents = workjamShifts.mapNotNull { transformer.transform(it) }

        synchronizer.sync(googleCalendarId, syncPeriodStart.toInstant(), syncPeriodEnd.toInstant(), workjamEvents)
    }
}

private fun RawOption.offsetDateTime(formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME): NullableOption<OffsetDateTime, OffsetDateTime> =
    convert("OFFSET_DATE_TIME") {
        it.toOffsetDateTimeOrNull(formatter)
            ?: fail("A date in the $formatter format is required")
    }

private fun List<Cookie>.findWorkjamTokenOrNull(): String? =
    firstOrNull { it.domain == WORKJAM_TOKEN_COOKIE_DOMAIN && it.name == WORKJAM_TOKEN_COOKIE_NAME }?.value