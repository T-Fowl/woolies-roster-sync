package com.tfowl.workjam.client.model.serialisers

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object JwtSerialiser : KSerializer<DecodedJWT> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.auth0.jwt.interfaces.DecodedJWT", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: DecodedJWT,
    ) = encoder.encodeString(value.token)

    override fun deserialize(decoder: Decoder): DecodedJWT {
        return JWT.decode(decoder.decodeString())
    }
}