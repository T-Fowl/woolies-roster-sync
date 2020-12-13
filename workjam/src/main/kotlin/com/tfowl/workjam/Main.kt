package com.tfowl.workjam

import com.github.mustachejava.DefaultMustacheFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.DataStore
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tfowl.workjam.internal.WorkjamEndpoints
import com.tfowl.workjam.model.EventType
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.create
import java.io.File
import java.time.*

const val WOOLIES = "6773940"

fun OffsetDateTime.toLocalDateTime(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    atZoneSameInstant(zone).toLocalDateTime()

fun OffsetDateTime.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate = atZoneSameInstant(zone).toLocalDate()

private suspend fun reauthenticate(
    ds: DataStore<String>,
    userIdentifier: String,
    workjam: WorkjamEndpoints,
    override: String? = null
): String {
    val old = requireNotNull(override ?: ds[userIdentifier]) { "No token stored for $userIdentifier" }
    val response = workjam.auth(old)
    ds[userIdentifier] = response.token
    return response.token
}

private fun String.removeWorkjamPositionPrefix(): String = removePrefix("*SUP-").removePrefix("SUP-")

fun shiftSummary(
    start: LocalTime,
    end: LocalTime,
    default: String
): String {

    /* Trucks */
    when (start) {
        LocalTime.of(5, 30) -> return "AM Trucks"
        LocalTime.of(13, 0) -> return "PM Trucks"
    }

    /* General Picking */
    when (start.hour) {
        in 0..13 -> return "Picking AM"
        in 16..23 -> return "Picking PM"
    }

    /* idk */
    return default.removeWorkjamPositionPrefix()
}

private fun OffsetDateTime.toGoogleDateTime(): DateTime = DateTime(toInstant().toEpochMilli())

@OptIn(ExperimentalSerializationApi::class)
private fun createWorkjamEndpoints(json: Json): WorkjamEndpoints {
    val httpClient = OkHttpClient.Builder()
        .addInterceptor(
            HeadersInterceptor(
                "Accept-Language: en",
                "Origin: https://app.workjam.com",
                "Referer: https://app.workjam.com/"
            )
        )
        .addInterceptor(LoggingInterceptor())


    val retrofit = Retrofit.Builder()
        .client(httpClient.build())
        .baseUrl("https://prod-aus-gcp-api.workjam.com")
        .addConverterFactory(json.asConverterFactory(MediaType.get("application/json")))
        .build()

    return retrofit.create()
}

@ExperimentalSerializationApi
suspend fun main(vararg args: String) = coroutineScope {
    require(args.size >= 2) { "Usage: workjam-schedule-sync id calendar [workjam jwt]" }

    val USER_ID = args[0]
    val CALENDAR_ID = args[1]
    val TOKEN_OVERRIDE = args.elementAtOrNull(2)

    /* Included by google apis, might as well use for our own serialisation */
    val dsf: DataStoreFactory = FileDataStoreFactory(File(TOKENS_DIRECTORY))

    val json = Json {
        ignoreUnknownKeys = true
    }

    val workjam = createWorkjamEndpoints(json)
    val token = reauthenticate(dsf.getDataStore("WorkjamTokens"), "user", workjam, TOKEN_OVERRIDE)

    val syncPeriodStart = OffsetDateTime.now()
    val syncPeriodEnd = OffsetDateTime.now().plusMonths(2)

    val workjamShifts = workjam.events(
        token,
        WOOLIES, USER_ID,
        syncPeriodStart, syncPeriodEnd
    )

    val googleEvents = GoogleCalendar.calendar.events().list(CALENDAR_ID)
        .setMaxResults(2500)
        .setTimeMin(syncPeriodStart.toGoogleDateTime())
        .setTimeMax(syncPeriodEnd.toGoogleDateTime())
        .setShowDeleted(true)
        .execute().items

    val zoneId = ZoneId.of("Australia/Sydney")

    val employeeDataStore = dsf.getDataStore<String>("EmployeeDetails")

    val mf = DefaultMustacheFactory()
    val descriptionTemplate = mf.compile("event-description.hbs")

    val batch = GoogleCalendar.calendar.batch()

    for (shift in workjamShifts) {

        val coworkingPositions = when (shift.type) {
            EventType.SHIFT -> workjam.coworkers(token, WOOLIES, shift.location.id, shift.id)
            EventType.AVAILABILITY_TIME_OFF -> emptyList()
        }

        val start = shift.startDateTime.toLocalDateTime(zoneId)
        val end = shift.endDateTime.toLocalDateTime(zoneId)

        val vm = DescriptionViewModel(coworkerPositions = coworkingPositions.map { (coworkers, position) ->
            CoworkerPositionViewModel(
                position = position.externalCode,
                coworkers = coworkers.map { coworker ->
                    val employeeDetails = employeeDataStore.computeSerializableIfAbsent(json, coworker.id) { id ->
                        workjam.employee(token, WOOLIES, id)
                    }
                    val employeeNumber = employeeDetails.externalCode ?: ""
                    val avatarUrl = coworker.avatarUrl?.let {
                        it.replace(
                            "/image/upload",
                            "/image/upload/f_auto,q_auto,w_64,h_64,c_thumb,g_face"
                        )
                    }
                    CoworkerViewModel(coworker.firstName, coworker.lastName, employeeNumber, avatarUrl)
                }.sortedBy { it.firstName })
        }.sortedBy { it.position })

        val description = descriptionTemplate.execute(vm)

        val summary = when (shift.type) {
            EventType.SHIFT -> shiftSummary(
                start.toLocalTime(),
                end.toLocalTime(),
                shift.title ?: "ERROR: MISSING TITLE"
            )
            EventType.AVAILABILITY_TIME_OFF -> "Time Off"
        }

        val event = Event()
            .setStart(EventDateTime().setDateTime(shift.startDateTime.toGoogleDateTime()))
            .setEnd(EventDateTime().setDateTime(shift.endDateTime.toGoogleDateTime()))
            .setSummary(summary)
            .setICalUID("${shift.id}@workjam.tfowl.com")
            .setDescription(description)

        val existingGoogleEvent = googleEvents.find { it.iCalUID == event.iCalUID }

        if (null != existingGoogleEvent) {
            GoogleCalendar.calendar.events()
                .update(
                    CALENDAR_ID,
                    existingGoogleEvent.id,
                    event.setId(existingGoogleEvent.id).setStatus("confirmed")
                )
                .queue(batch,
                    success = { println("Updated existing event ${event.iCalUID}") },
                    failure = { println("Failed to update existing event ${event.iCalUID}: ${it.toPrettyString()}") }
                )
        } else {
            GoogleCalendar.calendar.events()
                .insert(CALENDAR_ID, event).queue(batch,
                    success = { println("Created event ${event.iCalUID}") },
                    failure = { println("Failed to create event ${event.iCalUID}: ${it.toPrettyString()}") }
                )
        }

        println("Queueued shift ${event.summary} [${event.start}-${event.end}] (${event.iCalUID})")
    }

    batch.execute()
}