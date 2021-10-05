// To parse the JSON, install kotlin's serialization plugin and do:
//
// val json   = Json(JsonConfiguration.Stable)
// val events = json.parse(Events.serializer(), jsonString)

package com.tfowl.workjam.client.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.OffsetDateTime

typealias Events = List<Event>

@Serializable
data class Event(
    val id: String,
    val type: EventType,
    @Serializable(with = OffsetDateTimeSerialiser::class)
    val startDateTime: OffsetDateTime,
    @Serializable(with = OffsetDateTimeSerialiser::class)
    val endDateTime: OffsetDateTime,
    val location: Location,
    val title: String? = null,
    val note: JsonObject? = null,
    val recurrence: JsonObject? = null
)

enum class EventType {
    SHIFT,
    AVAILABILITY_TIME_OFF,
}