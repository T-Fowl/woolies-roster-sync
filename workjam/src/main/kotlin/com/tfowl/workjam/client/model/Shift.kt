// To parse the JSON, install kotlin's serialization plugin and do:
//
// val json  = Json(JsonConfiguration.Stable)
// val shift = json.parse(Shift.serializer(), jsonString)

package com.tfowl.workjam.client.model

import com.tfowl.workjam.client.model.serialisers.ZoneIdSerialiser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import java.time.ZoneId

@Serializable
data class Shift(
    val id: String,

    @SerialName("externalId")
    val externalID: String,

    val status: String,
    val event: ScheduleEvent,
    val assignees: List<Assignee>,
    val position: ShiftPosition,
    val quantity: Long,
    val offeredSpots: OfferedSpots,
    val openSpots: OpenSpots,
    val allowedActions: JsonArray,
    val createdBy: CreatedBy,
    val segments: List<Segment>,
    val approvalRequests: JsonArray,
    val locked: Boolean,
    val createdViaIntegration: Boolean
)

@Serializable
data class Assignee(
    val profile: Profile,
    val status: String,
    val bookingMethod: String,
    val breaks: JsonArray
)

@Serializable
data class Profile(
    val id: String,

    @SerialName("externalId")
    val externalID: String,

    val externalCode: String,
    val firstName: String,
    val lastName: String,

    @SerialName("avatarUrl")
    val avatarURL: String
)

@Serializable
data class CreatedBy(
    val id: String,
    val firstName: String,
    val lastName: String
)

@Serializable
data class OfferedSpots(
    val remainingQuantity: Long
)

@Serializable
data class OpenSpots(
    val remainingQuantity: Long,
    val broadcast: Boolean,
    val requiresApproval: Boolean,
    val useSeniorityList: Boolean,
    val useMarketplace: Boolean
)

@Serializable
data class ShiftPosition(
    val id: String,
    val name: String,

    @SerialName("externalId")
    val externalID: String
)

@Serializable
data class Segment(
    val type: SegmentType,
    val startDateTime: String,
    val endDateTime: String,
    val position: SegmentPosition,
    val location: SegmentLocation,
    val badgeTargetAudiences: JsonArray
)

@Serializable(with = SegmentTypeSerializer::class)
enum class SegmentType {
    N_IMPORTE_QUOI,
    BREAK_MEAL,
    BREAK_REST,
    SHIFT
}

object SegmentTypeSerializer : KSerializer<SegmentType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SegmentType", PrimitiveKind.STRING)

    private val lookup = SegmentType.values().associateBy { it.name }

    override fun deserialize(decoder: Decoder): SegmentType {
        return lookup[decoder.decodeString()] ?: SegmentType.N_IMPORTE_QUOI
    }

    override fun serialize(encoder: Encoder, value: SegmentType) {
        encoder.encodeString(value.name)
    }
}

@Serializable
data class SegmentLocation(
    val id: String,
    val name: String,
    val type: String,

    @SerialName("timeZoneId")
    @Serializable(with = ZoneIdSerialiser::class)
    val timeZoneID: ZoneId
)

@Serializable
data class SegmentPosition(
    val id: String,
    val name: String
)
