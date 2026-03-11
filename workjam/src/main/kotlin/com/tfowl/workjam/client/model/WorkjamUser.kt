package com.tfowl.workjam.client.model

import com.auth0.jwt.interfaces.DecodedJWT
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class WorkjamUser(
    @Contextual
    val token: DecodedJWT,
    val userId: Long,
    val firstLogin: Boolean,
    val hasEmployers: Boolean,
    val userRole: String,
    val correlationId: String,
    val employers: List<String>,
)