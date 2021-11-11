package com.tfowl.workjam.client.model.serialisers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.format.DateTimeFormatter

class InstantSerialiser(private val formatter: DateTimeFormatter) : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant {
        return formatter.parse(decoder.decodeString(), Instant::from)
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(formatter.format(value))
    }
}