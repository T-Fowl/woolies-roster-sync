package com.tfowl.workjam.client.model.serialisers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

object InstantEpochSecondsSerialiser : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Instant = Instant.ofEpochSecond(decoder.decodeLong())

    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeLong(value.epochSecond)
}