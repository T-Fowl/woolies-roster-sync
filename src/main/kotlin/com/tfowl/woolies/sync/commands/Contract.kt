package com.tfowl.woolies.sync.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.michaelbull.result.unwrap
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole
import com.tfowl.woolies.sync.connectToBrowser
import com.tfowl.woolies.sync.createWebDriver
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.LastModified
import net.fortuna.ical4j.model.property.Location
import net.fortuna.ical4j.model.property.RRule
import org.slf4j.LoggerFactory
import java.time.*
import java.time.temporal.TemporalAdjusters

@Serializable
data class TeamMemberContract(
    val weeks: List<ContractWeek>,
)

@Serializable
data class ContractWeek(
    val mon: List<ContractSegment>? = null,
    val tue: List<ContractSegment>? = null,
    val wed: List<ContractSegment>? = null,
    val thu: List<ContractSegment>? = null,
    val fri: List<ContractSegment>? = null,
    val sat: List<ContractSegment>? = null,
    val sun: List<ContractSegment>? = null,
)

@Serializable
data class ContractSegment(
    val start: String,
    val end: String,
    val breaks: List<String>,
)

@Serializable
data class TeamMemberProfile(
    val name: String,
    val costCentre: CostCentre,
    val timezone: String,
    val position: TeamMemberPosition,
)

@Serializable
data class CostCentre(
    val addressFormatted: String,
)

@Serializable
data class TeamMemberPosition(
    val startDate: String,
)


private val LOGGER = LoggerFactory.getLogger(Contract::class.java)

class Contract : CliktCommand(name = "contract") {
    override fun help(context: Context): String = context.theme.info("Convert your contract schedule to an ical feed")

    private val email by option("--email", envvar = "WORKJAM_EMAIL").required()
    private val password by option("--password", envvar = "WORKJAM_PASSWORD").required()

    private val playwrightDriverUrl by option("--playwright-driver-url", envvar = "PLAYWRIGHT_DRIVER_URL").required()

    override fun run() = runBlocking {

        LOGGER.debug("Creating web driver")
        createWebDriver().unwrap().use { driver ->
            val browser = connectToBrowser(driver, playwrightDriverUrl).unwrap()


            LOGGER.debug("Logging into team data")
            val page = browser.newPage()
            page.navigate("https://teamdata.woolworths.com.au")

            page.getByPlaceholder("someone@example.com").fill(email)
            page.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Next")).click()

            page.getByPlaceholder("Password").fill(password)
            page.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Sign in")).click()

            page.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("No")).click()

            page.waitForURL("https://teamdata.woolworths.com.au/team-movements/")

            LOGGER.debug("Waiting for profile and contract information")
            val profileResponse =
                page.waitForResponse("https://api.teamdata.woolworths.com.au/team-members/public/team-members/me") {}

            val format = Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            }

            val profile = format.decodeFromString<TeamMemberProfile>(profileResponse.text())

            val contractResponse =
                page.waitForResponse("https://api.teamdata.woolworths.com.au/team-members/public/team-members-contract/me") {}

            val contract = format.decodeFromString<TeamMemberContract>(contractResponse.text())

            LOGGER.debug("Generating ical")


            val calendar = Calendar()
                .withProdId("-//github.com/tfowl.com//woolies-roster-sync//EN")
                .withDefaults()
                .withProperty(LastModified(Instant.parse(profile.position.startDate)))
                .withProperty(net.fortuna.ical4j.model.property.TzId(profile.timezone))
                .fluentTarget

            val contractStart = Instant.parse(profile.position.startDate)
                .atZone(ZoneId.of(profile.timezone))
                .toLocalDate()
                .with(DayOfWeek.MONDAY)

            fun createRepeatingEvent(weekStart: LocalDate, segments: List<ContractSegment>, dow: DayOfWeek) {
                segments.forEach { segment ->
                    val start = LocalTime.parse(segment.start)
                    val end = LocalTime.parse(segment.end)
                    val date = weekStart.with(TemporalAdjusters.nextOrSame(dow))

                    calendar.withComponent(
                        VEvent(
                            LocalDateTime.of(date, start).atZone(ZoneId.of(profile.timezone)).toInstant(),
                            LocalDateTime.of(date, end).atZone(ZoneId.of(profile.timezone)).toInstant(),
                            "Contracted Shift"
                        )
                            .withProperty(RRule<LocalDate>("FREQ=WEEKLY;INTERVAL=${contract.weeks.size};WKST=MO"))
                            .withProperty(Location(profile.costCentre.addressFormatted))
                            .withProperty(net.fortuna.ical4j.model.property.TzId(profile.timezone))
                            .fluentTarget as VEvent
                    ).fluentTarget
                }
            }

            contract.weeks.forEachIndexed { i, week ->
                week.mon?.let { createRepeatingEvent(contractStart.plusWeeks(i.toLong()), it, DayOfWeek.MONDAY) }
                week.tue?.let { createRepeatingEvent(contractStart.plusWeeks(i.toLong()), it, DayOfWeek.TUESDAY) }
                week.wed?.let { createRepeatingEvent(contractStart.plusWeeks(i.toLong()), it, DayOfWeek.WEDNESDAY) }
                week.thu?.let { createRepeatingEvent(contractStart.plusWeeks(i.toLong()), it, DayOfWeek.THURSDAY) }
                week.fri?.let { createRepeatingEvent(contractStart.plusWeeks(i.toLong()), it, DayOfWeek.FRIDAY) }
                week.sat?.let { createRepeatingEvent(contractStart.plusWeeks(i.toLong()), it, DayOfWeek.SATURDAY) }
                week.sun?.let { createRepeatingEvent(contractStart.plusWeeks(i.toLong()), it, DayOfWeek.SUNDAY) }
            }


            println(calendar.fluentTarget)
        }
    }
}