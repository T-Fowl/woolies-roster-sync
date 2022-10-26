package com.tfowl.woolies.sync.transform

import com.tfowl.workjam.client.model.ScheduleEventType
import com.tfowl.workjam.client.model.Shift
import com.tfowl.workjam.client.model.endTime
import com.tfowl.workjam.client.model.startTime
import java.time.LocalTime

/**
 * Responsible for generating the description for [com.google.api.services.calendar.model.Event]s
 */
internal interface DescriptionGenerator {
    fun generate(describableShift: DescribableShift): String
}

internal data class DescribableShift(
    val shift: Shift,
    val title: String,
    val storePositions: List<DescribableStorePosition>
)

internal data class DescribableStorePosition(
    val position: String,
    val coworkers: List<DescribableShiftAssignee>
)

internal data class DescribableShiftAssignee(
    val firstName: String,
    val lastName: String,
    val avatarUrl: String? = null,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val type: ScheduleEventType
) {
    val fullName: String get() = "$firstName $lastName"
}

internal object DefaultDescriptionGenerator : DescriptionGenerator {
    override fun generate(describableShift: DescribableShift): String = buildString {
        appendLine("<h1>Segments</h1>")
        describableShift.shift.segments.forEach { segment ->
            append("${segment.startTime} - ${segment.endTime}: ")
            append(segment.position.name.removeSupPrefix())
            appendLine()
        }
        appendLine("<hr>")

        appendLine("<h1>Coworkers</h1>")
        describableShift.storePositions.forEach { sp ->
            appendLine("<b>${sp.position}</b>")

            sp.coworkers.forEach { cw ->
                append("\t${cw.startTime} - ${cw.endTime} ${cw.fullName}")
                if(cw.type != ScheduleEventType.SHIFT) append(" OFF") // TODO
                appendLine()
            }

            appendLine("<hr>")
        }
    }
}