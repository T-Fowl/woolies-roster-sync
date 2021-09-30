package com.tfowl.woolies.sync

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

fun OffsetDateTime.toLocalDateTime(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    atZoneSameInstant(zone).toLocalDateTime()

fun OffsetDateTime.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    atZoneSameInstant(zone).toLocalDate()

internal fun String.toOffsetDateTimeOrNull(): OffsetDateTime? = try {
    OffsetDateTime.parse(this)
} catch (ignored: DateTimeParseException) {
    null
}
