package com.tfowl.googleapi

import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import java.time.Instant
import java.time.ZoneId

fun Event.isCancelled() = "cancelled" == status

fun EventDateTime.pretty(): String = "${dateTime ?: date}"

fun Event.pretty(): String = "$summary [${start.pretty()}-${end.pretty()}, $status, $iCalUID]"

fun Event.setStart(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): Event =
    setStart(instant.toGoogleEventDateTime(zone))

fun Event.setEnd(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): Event =
    setEnd(instant.toGoogleEventDateTime(zone))