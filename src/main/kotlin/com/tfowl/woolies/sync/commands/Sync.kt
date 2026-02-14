package com.tfowl.woolies.sync.commands

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.michaelbull.result.unwrap
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.CalendarScopes
import com.tfowl.gcal.GoogleApiServiceConfig
import com.tfowl.gcal.GoogleCalendar
import com.tfowl.gcal.calendarView
import com.tfowl.gcal.sync
import com.tfowl.woolies.sync.commands.options.BrowserAuthentication
import com.tfowl.woolies.sync.commands.options.TokenAuthentication
import com.tfowl.woolies.sync.commands.options.localDateRange
import com.tfowl.woolies.sync.commands.options.token
import com.tfowl.woolies.sync.googleCalendarOption
import com.tfowl.woolies.sync.googleClientSecretsOption
import com.tfowl.woolies.sync.transform.DefaultDescriptionGenerator
import com.tfowl.woolies.sync.transform.DefaultSummaryGenerator
import com.tfowl.woolies.sync.transform.EventTransformerToGoogle
import com.tfowl.woolies.sync.utils.Cookie
import com.tfowl.woolies.sync.utils.DataStoreCredentialStorage
import com.tfowl.workjam.client.WorkjamClientProvider
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

internal const val APPLICATION_NAME = "APPLICATION_NAME"
internal const val WORKJAM_TOKEN_COOKIE_DOMAIN = "api.workjam.com"
internal const val WORKJAM_TOKEN_COOKIE_NAME = "token"
internal const val DEFAULT_CLIENT_SECRETS_FILE = "client-secrets.json"
internal const val DOMAIN = "workjam.tfowl.com"
internal const val DEFAULT_STORAGE_DIR = ".woolies-roster"

private val LOGGER = LoggerFactory.getLogger(Sync::class.java)

class Sync : SuspendingCliktCommand(name = "sync") {
    override fun help(context: Context): String = context.theme.info("Sync your roster from workjam to your calendar")

    private val googleCalendarId by googleCalendarOption().required()

    private val googleClientSecrets by googleClientSecretsOption().required()

    val auth by option().groupChoice(
        "token" to TokenAuthentication(),
        "browser" to BrowserAuthentication(),
    )

    val period by option(
        "--period",
        help = "Period to fetch your schedule for",
        helpTags = mapOf("Format" to "YYYY-MM-DD/YYYY-MM-DD")
    ).localDateRange()
        .default(LocalDate.now()..LocalDate.now().plusDays(14), "next 14 days")


    override suspend fun run() {
        LOGGER.atDebug()
            .addKeyValue("auth", auth)
            .addKeyValue("period", period)
            .log("Running Sync command")

        val dsf = FileDataStoreFactory(
            File(
                DEFAULT_STORAGE_DIR
            )
        )

        val workjam = WorkjamClientProvider.create(
            DataStoreCredentialStorage(dsf)
        ).createClient("user", auth?.token()?.unwrap())

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

        val syncStart = period.start.atStartOfDay(storeZoneId).toOffsetDateTime()
        val syncEnd = period.endInclusive.plusDays(1).atStartOfDay(storeZoneId).toOffsetDateTime()

        val workjamShifts = workjam.events(company.id.toString(), workjam.userId, syncStart, syncEnd)

        val workjamEvents = transformer.transformAll(workjamShifts)

        sync(
            calendarApi,
            calendarApi.calendarView(googleCalendarId),
            period,
            workjamEvents,
            ZoneId.of("Australia/Melbourne"), // TODO
            DOMAIN
        )
    }
}

internal fun List<Cookie>.findWorkjamTokenOrNull(): String? =
    firstOrNull { it.domain.endsWith(WORKJAM_TOKEN_COOKIE_DOMAIN) && it.name == WORKJAM_TOKEN_COOKIE_NAME }?.value