package com.tfowl.workjam.client.model

import kotlinx.serialization.Serializable

@Serializable
data class Employee(
    val id: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String? = null,
    val externalId: String? = null,
    val externalCode: String? = null,
    val username: String? = null,
    val workerType: String? = null,
    val status: String? = null
)