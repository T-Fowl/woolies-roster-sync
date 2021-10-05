// To parse the JSON, install kotlin's serialization plugin and do:
//
// val json      = Json(JsonConfiguration.Stable)
// val coworkers = json.parse(Coworkers.serializer(), jsonString)

package com.tfowl.workjam.client.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias Coworkers = List<Coworker>

@Serializable
data class Coworker(
    val position: Position,
    val employees: List<Employee>
)

@Serializable
data class Position(
    val id: String,
    val name: String,

    @SerialName("externalId")
    val externalID: String,

    val externalCode: String
)
