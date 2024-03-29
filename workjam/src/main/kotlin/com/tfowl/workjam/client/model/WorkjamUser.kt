package com.tfowl.workjam.client.model

import kotlinx.serialization.Serializable

@Serializable
data class WorkjamUser(
    val token: String,
    val userId: Long,
    val firstLogin: Boolean,
    val hasEmployers: Boolean,
    val userRole: String,
    val correlationId: String,
    val employers: List<String>,
)