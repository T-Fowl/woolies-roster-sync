package com.tfowl.workjam

import com.github.mustachejava.Mustache
import java.io.StringWriter

internal fun Mustache.execute(scope: Any): String {
    val sw = StringWriter()
    execute(sw, scope)
    return sw.toString()
}

internal data class DescriptionViewModel(val coworkerPositions: List<CoworkerPositionViewModel>)

internal data class CoworkerPositionViewModel(
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
}