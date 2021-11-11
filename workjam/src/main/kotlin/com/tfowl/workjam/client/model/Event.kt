// To parse the JSON, install kotlin's serialization plugin and do:
//
// val json   = Json(JsonConfiguration.Stable)
// val events = json.parse(Events.serializer(), jsonString)

package com.tfowl.workjam.client.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.Instant

typealias Events = List<Event>

@Serializable
data class Event(
    val id: String,
    val type: EventType,
    @Contextual
    val startDateTime: Instant,
    @Contextual
    val endDateTime: Instant,
    val location: Location,
    val title: String? = null,
    val note: JsonObject? = null,
    val recurrence: JsonObject? = null
)

enum class EventType {
    SHIFT,
    AVAILABILITY_TIME_OFF,
}