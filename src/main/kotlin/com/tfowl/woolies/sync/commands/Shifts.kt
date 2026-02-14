package com.tfowl.woolies.sync.commands

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.michaelbull.result.unwrap
import com.google.api.client.util.store.FileDataStoreFactory
import com.tfowl.woolies.sync.commands.options.BrowserAuthentication
import com.tfowl.woolies.sync.commands.options.TokenAuthentication
import com.tfowl.woolies.sync.commands.options.token
import com.tfowl.woolies.sync.transform.DefaultDescriptionGenerator
import com.tfowl.woolies.sync.transform.DefaultSummaryGenerator
import com.tfowl.woolies.sync.transform.EventTransformerToICal
import com.tfowl.woolies.sync.utils.DataStoreCredentialStorage
import com.tfowl.workjam.client.WorkjamClientProvider
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.property.LastModified
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.LocalDate

private val LOGGER = LoggerFactory.getLogger(Shifts::class.java)

enum class OutputFormat {
    ICAL, JSON, GOOGLE,
}

class Shifts : SuspendingCliktCommand(name = "shifts") {
    override fun help(context: Context): String = context.theme.info("Get your schedule from workjam")

    val format by option(
        "--f", "--format",
        help = "Output format"
    ).enum<OutputFormat>().default(OutputFormat.ICAL)


    val auth by option().groupChoice(
        "token" to TokenAuthentication(),
        "browser" to BrowserAuthentication(),
    )

    val period by option(
        "--period",
        help = "Period to fetch your schedule for",
        helpTags = mapOf("Format" to "YYYY-MM-DD/YYYY-MM-DD")
    ).convert("local_date/local_date") { str ->
        val (start, end) = str.split('/', limit = 2)
        LocalDate.parse(start)..LocalDate.parse(end)
    }.default(LocalDate.now()..LocalDate.now().plusDays(14), defaultForHelp = "next 14 days")

    override suspend fun run() {
        LOGGER.atDebug()
            .addKeyValue("format", format)
            .addKeyValue("auth", auth)
            .addKeyValue("period", period)
            .log("Running Shifts command")

        val workjam = WorkjamClientProvider.create(
            DataStoreCredentialStorage(
                FileDataStoreFactory(
                    File(
                        DEFAULT_STORAGE_DIR
                    )
                )
            )
        ).createClient("user", auth?.token()?.unwrap())

        val company = workjam.employers(workjam.userId).companies.singleOrNull()
            ?: error("Employee is employed at more than 1 company - Not currently supported")
        val store = company.stores.singleOrNull { it.primary }
            ?: error("Employee has more than 1 primary store - Not currently supported")
        val storeZoneId = store.storeAddress.city.timeZoneID ?: error("Primary store does not have a time zone id")

        val transformer = EventTransformerToICal(
            workjam,
            company.id.toString(),
            DOMAIN,
            DefaultDescriptionGenerator,
            DefaultSummaryGenerator,
        )

        val startDateTime = period.start.atStartOfDay(storeZoneId).toOffsetDateTime()
        val endDateTime = period.endInclusive.plusDays(1).atStartOfDay(storeZoneId).toOffsetDateTime()

        val workjamEvents = workjam.events(company.id.toString(), workjam.userId, startDateTime, endDateTime)

        val calendar = Calendar()
            .withProdId("-//github.com/tfowl.com//woolies-roster-sync//EN")
            .withDefaults()
            .withProperty(LastModified(Instant.now()))


        transformer.transformAll(workjamEvents)
            .forEach(calendar::withComponent)


        println(calendar.fluentTarget)
    }
}