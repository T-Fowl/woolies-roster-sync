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


class EventExtendedPropertiesBuilder {
    private val shared = mutableMapOf<String, String>()
    private val private = mutableMapOf<String, String>()

    fun shared(block: ExtendedPropertiesBuilder.() -> Unit): Unit = ExtendedPropertiesBuilder(shared).block()
    fun private(block: ExtendedPropertiesBuilder.() -> Unit): Unit = ExtendedPropertiesBuilder(private).block()

    fun build(): Event.ExtendedProperties = Event.ExtendedProperties()
        .setShared(shared.takeIf { it.isNotEmpty() })
        .setPrivate(private.takeIf { it.isNotEmpty() })
}

class ExtendedPropertiesBuilder(private val map: MutableMap<String, String>) {
    infix fun String.prop(value: String): Unit {
        check(map.size + 1 <= 300) { "Cannot have more than 300 extended properties" }

        val key = this

        if (value.length <= 1024) {
            check(key.length <= 44) { "Key \"$key\" is ${key.length} characters long. Over 44 is silently dropped." }

            map[key] = value
        } else {
            val chunks = value.chunked(1024)
            val maxChunkKeyIndexWidth = chunks.lastIndex.toString().length + 2

            check(key.length + maxChunkKeyIndexWidth <= 44) { "Key \"$key\" is ${key.length + maxChunkKeyIndexWidth} characters long including value index. Over 44 is silently dropped." }

            chunks.forEachIndexed { i, chunk ->
                map["$key[$i]"] = chunk
            }
        }

    }
}

fun Event.buildExtendedProperties(block: EventExtendedPropertiesBuilder.() -> Unit): Event =
    setExtendedProperties(EventExtendedPropertiesBuilder().also(block).build())