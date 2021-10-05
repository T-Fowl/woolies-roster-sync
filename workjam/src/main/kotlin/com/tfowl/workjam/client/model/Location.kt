// To parse the JSON, install kotlin's serialization plugin and do:
//
// val json     = Json(JsonConfiguration.Stable)
// val location = json.parse(Location.serializer(), jsonString)

package com.tfowl.workjam.client.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZoneId

@Serializable
data class Location(
    val id: String,
    val name: String,
    val type: String,

    @SerialName("externalId")
    val externalID: String,

    val externalCode: String,

    @SerialName("timeZoneId")
    @Serializable(with = ZoneIdSerialiser::class)
    val timeZoneID: ZoneId,

    val address: Address? = null,
    val geolocation: Geolocation? = null,
)

@Serializable
data class Address(
    val streetAddress: String,
    val city: String,
    val province: String,
    val countryIso2: String,
    val postalCode: String
)

@Serializable
data class Geolocation(
    val latitude: Double,
    val longitude: Double
)
