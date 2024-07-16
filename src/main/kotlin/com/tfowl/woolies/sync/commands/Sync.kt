package com.tfowl.woolies.sync.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.*
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.unwrap
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.CalendarScopes
import com.tfowl.gcal.GoogleApiServiceConfig
import com.tfowl.gcal.GoogleCalendar
import com.tfowl.gcal.calendarView
import com.tfowl.gcal.sync
import com.tfowl.woolies.sync.*
import com.tfowl.woolies.sync.transform.DefaultDescriptionGenerator
import com.tfowl.woolies.sync.transform.DefaultSummaryGenerator
import com.tfowl.woolies.sync.transform.EventTransformerToGoogle
import com.tfowl.woolies.sync.utils.Cookie
import com.tfowl.woolies.sync.utils.DataStoreCredentialStorage
import com.tfowl.woolies.sync.utils.toLocalDateOrNull
import com.tfowl.workjam.client.WorkjamClientProvider
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal const val APPLICATION_NAME = "APPLICATION_NAME"
internal const val WORKJAM_TOKEN_COOKIE_DOMAIN = "api.workjam.com"
internal const val WORKJAM_TOKEN_COOKIE_NAME = "token"
internal const val DEFAULT_CLIENT_SECRETS_FILE = "client-secrets.json"
internal const val DOMAIN = "workjam.tfowl.com"
internal const val DEFAULT_STORAGE_DIR = ".woolies-roster"

private val LOGGER = LoggerFactory.getLogger(Sync::class.java)

class Sync : CliktCommand(name = "sync", help = "Sync your roster from workjam to your calendar") {
    private val googleCalendarId by googleCalendarOption().required()

    private val googleClientSecrets by googleClientSecretsOption().required()

    private val email by option("--email", envvar = "WORKJAM_EMAIL").required()
    private val password by option("--password", envvar = "WORKJAM_PASSWORD").required()

    private val syncFrom by option(
        help = "Local date to start syncing shifts, will sync from midnight at the start of the day",
        helpTags = mapOf("Example" to "2007-12-03")
    ).localDate().default(LocalDate.now(), defaultForHelp = "today")

    private val syncTo by option(
        help = "Local date to finish syncing shifts, will sync until midnight at the end of the day",
        helpTags = mapOf("Example" to "2007-12-03")
    ).localDate().defaultLazy(defaultForHelp = "a month from sync-from") { syncFrom.plusMonths(1) }

    private val playwrightDriverUrl by option("--playwright-driver-url", envvar = "PLAYWRIGHT_DRIVER_URL").required()

    override fun run() = runBlocking {
        val dsf: DataStoreFactory = FileDataStoreFactory(File(DEFAULT_STORAGE_DIR))

        val token = binding {
            LOGGER.debug("Creating web driver")
            createWebDriver().bind().use { driver ->
                LOGGER.debug("Connecting to browser")
                val browser = connectToBrowser(driver, playwrightDriverUrl).bind()

                LOGGER.debug("Logging into workjam")
                val homePage = login(browser, email, password).bind()

                findTokenCookie(homePage).bind()
            }
        }.unwrap()
        LOGGER.debug("Success")

        val workjam = WorkjamClientProvider.create(DataStoreCredentialStorage(dsf))
            .createClient("user", token)

        val company = workjam.employers(workjam.userId).companies.singleOrNull()
            ?: error("More than 1 company")
        val store = company.stores.singleOrNull { it.primary }
            ?: error("More than 1 primary store")
        val storeZoneId = store.storeAddress.city.timeZoneID ?: error("Primary store does not have a zone id")

        val calendarApi = GoogleCalendar.create(
            GoogleApiServiceConfig(
                secretsProvider = { googleClientSecrets },
                applicationName = APPLICATION_NAME,
                scopes = listOf(CalendarScopes.CALENDAR),
                dataStoreFactory = dsf
            )
        )

        val transformer = EventTransformerToGoogle(
            workjam,
            company.id.toString(),
            DOMAIN,
            DefaultDescriptionGenerator,
            DefaultSummaryGenerator,
        )

        val syncStart = syncFrom.atStartOfDay(storeZoneId).toOffsetDateTime()
        val syncEnd = syncTo.plusDays(1).atStartOfDay(storeZoneId).toOffsetDateTime()

        val workjamShifts = workjam.events(company.id.toString(), workjam.userId, syncStart, syncEnd)

        val workjamEvents = transformer.transformAll(workjamShifts)

        sync(
            calendarApi,
            calendarApi.calendarView(googleCalendarId),
            syncFrom..syncTo,
            workjamEvents,
            ZoneId.of("Australia/Melbourne"), // TODO
            DOMAIN
        )
    }
}

private fun RawOption.localDate(formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE): NullableOption<LocalDate, LocalDate> =
    convert("LOCAL_DATE") { it.toLocalDateOrNull(formatter) ?: fail("A date in the $formatter format is required") }

internal fun List<Cookie>.findWorkjamTokenOrNull(): String? =
    firstOrNull { it.domain.endsWith(WORKJAM_TOKEN_COOKIE_DOMAIN) && it.name == WORKJAM_TOKEN_COOKIE_NAME }?.value