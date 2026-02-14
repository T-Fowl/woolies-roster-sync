package com.tfowl.woolies.sync.commands.options

import com.github.ajalt.clikt.parameters.options.*
import com.tfowl.woolies.sync.utils.toLocalDateOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun RawOption.localDate(formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE): NullableOption<LocalDate, LocalDate> =
    convert("LOCAL_DATE") { it.toLocalDateOrNull(formatter) ?: fail("A date in the $formatter format is required") }
