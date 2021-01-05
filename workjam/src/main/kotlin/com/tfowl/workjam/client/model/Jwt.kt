@file:UseSerializers(InstantEpochSecondsSerialiser::class)

package com.tfowl.workjam.client.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.*

fun String.decode64(startIndex: Int = 0, endIndex: Int = length): String {
    val decoder = Base64.getDecoder()
    return decoder.decode(encodeToByteArray(startIndex, endIndex)).decodeToString()
}

fun Json.decodeJwtPayload(token: String): WorkjamJwtPayload {
    val indexFirst = token.indexOf('.')
    require(indexFirst > 0) { "Malformed JWT" }
    val indexSecond = token.indexOf('.', startIndex = indexFirst + 1)
    require(indexSecond > 0) { "Malformed JWT" }

    return decodeFromString(token.decode64(indexFirst + 1, indexSecond))
}

@Serializable
data class WorkjamJwtPayload(
    @SerialName("sub") val subject: String,
    @SerialName("iss") val issuer: String,
    @SerialName("exp") val expires: Instant,
    @SerialName("iat") val issuedAt: Instant,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String,
    val isXToken: Boolean
)