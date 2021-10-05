package com.tfowl.workjam.client.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkingStatus(@SerialName("employeeWorking") val isWorking: Boolean)