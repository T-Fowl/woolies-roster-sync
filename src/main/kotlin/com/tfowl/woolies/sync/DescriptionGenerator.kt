@file:Suppress("unused")

package com.tfowl.woolies.sync

import java.time.Instant

internal data class DescriptionViewModel(
    val title: String,
    val startDateTime: Instant,
    val endDateTime: Instant,
    val storePositions: List<StorePositionViewModel>
)

internal data class StorePositionViewModel(
    val position: String,
    val coworkers: List<CoworkerViewModel>
)

internal data class CoworkerViewModel(
    val firstName: String,
    val lastName: String,
    val employeeNumber: String? = null,
    val avatarUrl: String? = null
) {
    val hasEmployeeNumber: Boolean = employeeNumber != null
    val hasAvatarUrl: Boolean = avatarUrl != null

    val fullName: String get() = "$firstName $lastName"
}

internal interface DescriptionGenerator {
    fun generate(vm: DescriptionViewModel): String
}

internal object DefaultDescriptionGenerator : DescriptionGenerator {
    override fun generate(vm: DescriptionViewModel): String = buildString {
        vm.storePositions.forEach { sp ->
            appendLine("<b>${sp.position}</b>")

            sp.coworkers.forEach { cw ->
                append("\t${cw.fullName}")

                cw.employeeNumber?.let { append(" (${cw.employeeNumber})") }
                appendLine()
            }

            appendLine("<hr>")
        }
    }
}