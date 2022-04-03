package com.tfowl.googleapi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import com.google.api.client.util.DateTime as GoogleDateTime


class GoogleTimeExtTests {

    private val rfc3339 = "2007-12-03T10:15:30.00Z"

    private val clock = Clock.fixed(
        Instant.parse(rfc3339),
        ZoneId.systemDefault()
    )

    @Test
    fun `Instant to Google DateTime`() {
        val instant = clock.instant()
        val googleDateTime = instant.toGoogleDateTime()

        assertEquals(instant.toEpochMilli(), googleDateTime.value)
    }

    @Test
    fun `Google DateTime to Instant`() {
        val googleDateTime = GoogleDateTime.parseRfc3339(rfc3339)
        val instant = googleDateTime.toInstant()

        assertEquals(googleDateTime.value, instant.toEpochMilli())
    }
}