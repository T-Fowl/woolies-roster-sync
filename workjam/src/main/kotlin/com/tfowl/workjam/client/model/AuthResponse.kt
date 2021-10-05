package com.tfowl.workjam.client.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val token: String,
    val userId: Long,
    val firstLogin: Boolean,
    val hasEmployers: Boolean,
    val userRole: String,
    val correlationId: String
)