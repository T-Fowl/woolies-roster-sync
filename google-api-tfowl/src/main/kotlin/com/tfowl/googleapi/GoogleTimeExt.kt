package com.tfowl.googleapi

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import com.google.api.client.util.DateTime as GoogDateTime

fun OffsetDateTime.toGoogleDateTime(): GoogDateTime = GoogDateTime(format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))

fun GoogDateTime.toOffsetDateTime(): OffsetDateTime = OffsetDateTime.parse(toStringRfc3339())