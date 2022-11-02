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

val Event.extendedPropertiesUnsplit: Event.ExtendedProperties get() = extendedProperties.unchunk()
private fun Event.ExtendedProperties.unchunk(): Event.ExtendedProperties {
    fun String.parse(): Pair<String, Int> {
        val iStart = lastIndexOf('[')
        val iEnd = lastIndexOf(']')

        return Pair(substring(0, iStart), substring(iStart + 1, iEnd).toInt())
    }

    fun Map<String, String>.unchunk(): Map<String, String> {
        val input = this
        val result = mutableMapOf<String, String>()

        // Do all the non-chunked properties first
        for (key in keys) {
            if (!key.endsWith("]"))
                result[key] = this[key]!!
        }

        val k = keys.filter { '[' in it && it.endsWith(']') }
            .map { it.parse() }
            .groupBy { it.first }
            .mapValues { it.value.map { it.second }.sorted() }

        k.forEach { key, indices ->
            result[key] = indices.map { i -> input["$key[$i]"]!! }.joinToString(separator = "")
        }


        return result
    }

    return Event.ExtendedProperties()
        .setPrivate(private?.unchunk())
        .setShared(shared?.unchunk())
}