package com.tfowl.googleapi

import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import java.time.Instant
import java.time.ZoneId

fun Event.isCancelled() = "cancelled" == status

fun EventDateTime.pretty(): String = "${dateTime ?: date}"

fun Event.pretty(): String = "$summary [${start.pretty()}-${end.pretty()}, $status, $iCalUID]"

fun Event.setStart(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): Event =
    setStart(instant.toGoogleEventDateTime(zone))

fun Event.setEnd(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): Event =
    setEnd(instant.toGoogleEventDateTime(zone))

fun Event.ExtendedProperties.setSharedChecked(properties: Map<String, String>): Event.ExtendedProperties =
    setShared(properties.checkExtendedProperties())

fun Event.ExtendedProperties.setPrivateChecked(properties: Map<String, String>): Event.ExtendedProperties =
    setPrivate(properties.checkExtendedProperties())

private fun Map<String, String>.checkExtendedProperties(): Map<String, String> {
    check(size <= 300) { "$size is more than the 300 extended properties count limit" }

    var sum = 0
    for ((key, value) in this) {
        sum += key.length + value.length
        check(key.length <= 44) { "Property key is longer than 44 characters and will be silently dropped: $key" }
        check(value.length <= 1024) { "Property is longer than 1024 characters and will be silently truncated: $value" }
    }

    check(sum <= 32000) { "${sum / 1024}kB is larger than the 32kB size limit for extended properties" }


    return this
}