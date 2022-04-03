// To parse the JSON, install kotlin's serialization plugin and do:
//
// val json   = Json(JsonConfiguration.Stable)
// val events = json.parse(Events.serializer(), jsonString)

package com.tfowl.workjam.client.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.Instant

@Serializable
data class ScheduleEvent(
    val id: String,
    val type: ScheduleEventType,
    @Contextual
    val startDateTime: Instant,
    @Contextual
    val endDateTime: Instant,
    val location: Location,
    val title: String? = null,
    val note: JsonObject? = null,
    val recurrence: JsonObject? = null
)

enum class ScheduleEventType {
    N_IMPORTE_QUOI,
    AVAILABILITY_AVAILABLE,
    AVAILABILITY_AVAILABLE_NOT_PREFERRED,
    AVAILABILITY_AVAILABLE_PREFERRED,
    AVAILABILITY_TIME_OFF,
    AVAILABILITY_UNAVAILABLE,
    AVAILABILITY_UNKNOWN,
    SHIFT
}