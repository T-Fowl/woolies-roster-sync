package com.tfowl.workjam

import com.github.mustachejava.DefaultMustacheFactory
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

internal interface DescriptionGenerator {
    fun generate(vm: DescriptionViewModel): String
}

internal class MustacheDescriptionGenerator(templateName: String) : DescriptionGenerator {
    private val mf = DefaultMustacheFactory()
    private val template = mf.compile(templateName)

    override fun generate(vm: DescriptionViewModel): String {
        return template.execute(vm)
    }

}