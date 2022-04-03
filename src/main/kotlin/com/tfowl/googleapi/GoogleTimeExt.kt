package com.tfowl.googleapi

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import com.google.api.client.util.DateTime as GoogleDateTime
import com.google.api.services.calendar.model.EventDateTime as GoogleEventDateTime

//////////////        Base translations         //////////////
////////////// Instant <--> Google translations //////////////

fun Instant.toGoogleDateTime(zone: ZoneId = ZoneId.systemDefault()): GoogleDateTime =
    GoogleDateTime(Date.from(this), TimeZone.getTimeZone(zone))

fun Instant.toGoogleEventDateTime(zone: ZoneId = ZoneId.systemDefault()): GoogleEventDateTime =
    GoogleEventDateTime().setDateTime(toGoogleDateTime(zone))

fun GoogleDateTime.toInstant(): Instant = Instant.ofEpochMilli(value)

fun GoogleEventDateTime.toInstant(): Instant = Instant.ofEpochMilli((dateTime ?: date).value)

////////////// LocalDate <--> Google translations //////////////

fun LocalDate.toGoogleDateTime(zone: ZoneId = ZoneId.systemDefault()): GoogleDateTime =
    atStartOfDay(zone).toInstant().toGoogleDateTime(zone)

fun LocalDate.toGoogleEventDateTime(zone: ZoneId = ZoneId.systemDefault()): GoogleEventDateTime =
    atStartOfDay(zone).toInstant().toGoogleEventDateTime(zone)

fun GoogleDateTime.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    LocalDate.ofInstant(toInstant(), zone)

fun GoogleEventDateTime.toLocalDate(zone: ZoneId = ZoneId.systemDefault()) = LocalDate.ofInstant(toInstant(), zone)