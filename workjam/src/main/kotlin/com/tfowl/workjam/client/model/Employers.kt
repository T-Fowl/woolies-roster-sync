// To parse the JSON, install kotlin's serialization plugin and do:
//
// val json      = Json(JsonConfiguration.Stable)
// val employers = json.parse(Employers.serializer(), jsonString)

package com.tfowl.workjam.client.model

import com.tfowl.workjam.client.model.serialisers.ZoneIdSerialiser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.DayOfWeek
import java.time.ZoneId

@Serializable
data class Employers(
    val companies: List<Company>
)

@Serializable
data class Company(
    val id: Long,
    val companyName: String,

    @SerialName("companyUrl")
    val companyURL: String,

    val description: String,
    val startDayOfWeek: DayOfWeek,

    @SerialName("logoUrl")
    val logoURL: String,

    val marketplaceCode: String,
    val isVisible: Boolean,
    val telephone: Telephone,
    val companyAddress: CompanyAddress,
    val stores: List<Store>,
)

@Serializable
data class CompanyAddress(
    val id: Long,
    val streetLine1: String,
    val streetLine2: String? = null,
    val streetLine3: String? = null,
    val postalCode: String,
    val latitude: Double,
    val longitude: Double,
    val isDirty: Boolean,
    val isDefault: Boolean? = null,
    val nickname: String? = null,
    val city: City,
    val province: City,
    val country: Country,
    val geolocationConfigs: List<GeolocationConfig>
)

@Serializable
data class City(
    val id: Long,
    val name: String,
    val abbr: String,

    @SerialName("timeZoneId")
    @Serializable(with = ZoneIdSerialiser::class)
    val timeZoneID: ZoneId? = null
)

@Serializable
data class Country(
    val id: Long,
    val name: String,
    val iso2: String,
    val sequence: Long,
    val postalCodeFormat: String,
    val postalCodeRequired: String,
    val dialingCode: String,
    val number: Long
)

@Serializable
data class GeolocationConfig(
    val latitude: Double,
    val longitude: Double,
    val radius: Double
)

@Serializable
data class Store(
    val id: Long,
    val storeName: String,
    val storeAddress: CompanyAddress,
    val telephone: Telephone,

    @SerialName("externalId")
    val externalID: String,

    val externalCode: String,
    val isVisible: Boolean,
    val geofencingRadiusMeter: Long,

    val region: Region,
    val networkMasks: List<String>? = null,
    val onSiteGeofencing: Boolean,
    val onSiteNetworkMasks: Boolean,
    val primary: Boolean,
    val visible: Boolean
)

@Serializable
data class Region(
    val id: Long,
    val name: JsonObject? = null
)

@Serializable
data class Telephone(
    val id: Long,
    val type: String? = null,
    val countryCode: String? = null,
    val number: String? = null,
    val extension: String? = null,
    val fullNumber: JsonObject? = null,
    val isDirty: Boolean
)
