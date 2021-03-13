package com.tfowl.workjam

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

fun OffsetDateTime.toLocalDateTime(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    atZoneSameInstant(zone).toLocalDateTime()

fun OffsetDateTime.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    atZoneSameInstant(zone).toLocalDate()
