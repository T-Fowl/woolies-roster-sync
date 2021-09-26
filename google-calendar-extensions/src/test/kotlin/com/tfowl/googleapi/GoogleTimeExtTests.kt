package com.tfowl.googleapi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import com.google.api.client.util.DateTime as GoogDateTime


class GoogleTimeExtTests {

    @Test
    fun `OffsetDateTime to Google DateTime`() {
        val rfc3339 = "2020-11-21T19:57:12.123+10:00"
        val offsetDateTime = OffsetDateTime.parse(rfc3339)
        val googleDateTime = offsetDateTime.toGoogleDateTime()

        assertEquals(rfc3339, googleDateTime.toStringRfc3339())
    }

    @Test
    fun `Google DateTime to OffsetDateTime`() {
        val rfc3339 = "2020-11-21T19:57:12.123+10:00"
        val googleDateTime = GoogDateTime.parseRfc3339(rfc3339)
        val offsetDateTime = googleDateTime.toOffsetDateTime()

        assertEquals(rfc3339, offsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }
}