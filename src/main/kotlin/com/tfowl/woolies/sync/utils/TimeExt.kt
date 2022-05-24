package com.tfowl.woolies.sync.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun String.toLocalDateOrNull(formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE): LocalDate? = try {
    LocalDate.parse(this, formatter)
} catch (ignored: DateTimeParseException) {
    null
}
