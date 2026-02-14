package com.tfowl.woolies.sync.commands.options

import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.convert
import com.tfowl.woolies.sync.utils.toLocalDateOrNull
import java.time.LocalDate

// TODO: Support other formats such as LOCAL_DATE/PERIOD, PERIOD, PERIOD/LOCAL_DATE or natural language date parser?
fun RawOption.localDateRange(): NullableOption<ClosedRange<LocalDate>, ClosedRange<LocalDate>> =
    convert("yyyy-mm-dd/yyyy-mm-dd") { str ->
        if ("/" !in str) fail("Does not contain a slash")

        val (startStr, endStr) = str.split("/", limit = 2)

        val start = startStr.toLocalDateOrNull() ?: fail("Invalid start date format: $startStr")
        val end = endStr.toLocalDateOrNull() ?: fail("Invalid end date format: $endStr")

        return@convert start..end
    }

