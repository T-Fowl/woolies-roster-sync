package com.tfowl.workjam.client.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class OffsetDateTimeSerialiser : KSerializer<OffsetDateTime> {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("OffsetDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): OffsetDateTime {
        return OffsetDateTime.parse(decoder.decodeString(), formatter)
    }

    override fun serialize(encoder: Encoder, value: OffsetDateTime) {
        encoder.encodeString(value.format(formatter))
    }
}

class InstantSerializer(private val formatter: DateTimeFormatter) : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant {
        return formatter.parse(decoder.decodeString(), Instant::from)
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(formatter.format(value))
    }
}

object InstantEpochSecondsSerialiser : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Instant = Instant.ofEpochSecond(decoder.decodeLong())

    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeLong(value.epochSecond)
}

object ZoneIdSerialiser : KSerializer<ZoneId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ZoneId", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ZoneId = ZoneId.of(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: ZoneId) = encoder.encodeString(value.id)
}
