package com.tfowl.workjam.client.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Availability(
    val event: Event,
    val id: String,
    val name: String,
    val note: JsonElement,
    val type: String,
)