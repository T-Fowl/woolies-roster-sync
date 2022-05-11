// To parse the JSON, install kotlin's serialization plugin and do:
//
// val json   = Json(JsonConfiguration.Stable)
// val events = json.parse(Events.serializer(), jsonString)

package com.tfowl.workjam.client.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
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
    val note: String? = null,
    val recurrence: JsonElement? = null
)

@Serializable(with = ScheduleEventTypeSerializer::class)
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

object ScheduleEventTypeSerializer : KSerializer<ScheduleEventType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ScheduleEventType", PrimitiveKind.STRING)

    private val lookup = ScheduleEventType.values().associateBy { it.name }
    override fun deserialize(decoder: Decoder): ScheduleEventType {
        return lookup[decoder.decodeString()] ?: ScheduleEventType.N_IMPORTE_QUOI
    }

    override fun serialize(encoder: Encoder, value: ScheduleEventType) {
        encoder.encodeString(value.name)
    }
}
