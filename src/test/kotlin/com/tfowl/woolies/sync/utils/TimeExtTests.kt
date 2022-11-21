package com.tfowl.woolies.sync.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter

class TimeExtTests {

    @Test
    fun `toLocalDateOrNull returns null on invalid input`() {
        assertNull("deadbeef".toLocalDateOrNull())
    }

    @Test
    fun `toLocalDateOrNull respects formatter`() {
        assertEquals(LocalDate.of(1997, Month.NOVEMBER, 21), "1997-11-21".toLocalDateOrNull())
        assertEquals(
            LocalDate.of(1997, Month.NOVEMBER, 21),
            "1997_21_11".toLocalDateOrNull(DateTimeFormatter.ofPattern("yyyy_dd_MM"))
        )
    }
}