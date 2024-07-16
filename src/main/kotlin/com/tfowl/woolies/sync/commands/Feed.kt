package com.tfowl.woolies.sync.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.unwrap
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.tfowl.woolies.sync.connectToBrowser
import com.tfowl.woolies.sync.createWebDriver
import com.tfowl.woolies.sync.findTokenCookie
import com.tfowl.woolies.sync.login
import com.tfowl.woolies.sync.transform.DefaultDescriptionGenerator
import com.tfowl.woolies.sync.transform.DefaultSummaryGenerator
import com.tfowl.woolies.sync.transform.EventTransformerToICal
import com.tfowl.woolies.sync.utils.DataStoreCredentialStorage
import com.tfowl.woolies.sync.utils.toLocalDateOrNull
import com.tfowl.workjam.client.WorkjamClientProvider
import kotlinx.coroutines.runBlocking
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.property.LastModified
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val LOGGER = LoggerFactory.getLogger(Feed::class.java)

class Feed : CliktCommand(name = "feed", help = "Convert your workjam schedule to an ical feed") {
    private val email by option("--email", envvar = "WORKJAM_EMAIL").required()
    private val password by option("--password", envvar = "WORKJAM_PASSWORD").required()

    private val fetchFrom by option(
        help = "Local date to start syncing shifts, will sync from midnight at the start of the day",
        helpTags = mapOf("Example" to "2007-12-03")
    ).localDate().default(LocalDate.now(), defaultForHelp = "today")

    private val fetchTo by option(
        help = "Local date to finish syncing shifts, will sync until midnight at the end of the day",
        helpTags = mapOf("Example" to "2007-12-03")
    ).localDate().defaultLazy(defaultForHelp = "a month from sync-from") { fetchFrom.plusMonths(1) }

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

        val transformer = EventTransformerToICal(
            workjam,
            company.id.toString(),
            DOMAIN,
            DefaultDescriptionGenerator,
            DefaultSummaryGenerator,
        )

        val startDateTime = fetchFrom.atStartOfDay(storeZoneId).toOffsetDateTime()
        val endDateTime = fetchTo.plusDays(1).atStartOfDay(storeZoneId).toOffsetDateTime()

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

private fun RawOption.localDate(formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE): NullableOption<LocalDate, LocalDate> =
    convert("LOCAL_DATE") { it.toLocalDateOrNull(formatter) ?: fail("A date in the $formatter format is required") }