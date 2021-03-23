package com.tfowl.googleapi

import com.google.api.services.calendar.model.EventDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import com.google.api.client.util.DateTime as GoogDateTime

fun OffsetDateTime.toGoogleDateTime(): GoogDateTime = GoogDateTime(format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))

fun GoogDateTime.toOffsetDateTime(): OffsetDateTime = OffsetDateTime.parse(toStringRfc3339())

fun OffsetDateTime.toGoogleEventDateTime(): EventDateTime = EventDateTime().setDateTime(toGoogleDateTime())

fun EventDateTime.toOffsetDateTime(): OffsetDateTime = (dateTime ?: date).toOffsetDateTime()