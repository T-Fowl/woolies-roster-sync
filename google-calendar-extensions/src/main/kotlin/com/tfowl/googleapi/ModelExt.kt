package com.tfowl.googleapi

import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import java.time.LocalDate

fun Event.isCancelled() = "cancelled" == status

fun EventDateTime.pretty(): String = "${dateTime ?: date}"

fun Event.pretty(): String = "$summary [${start.pretty()}-${end.pretty()}, $status, $iCalUID]"

val EventDateTime.toLocalDate: LocalDate get() = dateTime.toOffsetDateTime().toLocalDate()