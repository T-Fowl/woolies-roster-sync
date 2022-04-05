@file:Suppress("unused")

package com.tfowl.woolies.sync

import java.time.Instant

internal data class ShiftViewModel(
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
    val avatarUrl: String? = null
) {
    val fullName: String get() = "$firstName $lastName"
}

internal interface DescriptionGenerator {
    fun generate(vm: ShiftViewModel): String
}

internal object DefaultDescriptionGenerator : DescriptionGenerator {
    override fun generate(vm: ShiftViewModel): String = buildString {
        vm.storePositions.forEach { sp ->
            appendLine("<b>${sp.position}</b>")

            sp.coworkers.forEach { cw ->
                appendLine("\t${cw.fullName}")
            }

            appendLine("<hr>")
        }
    }
}