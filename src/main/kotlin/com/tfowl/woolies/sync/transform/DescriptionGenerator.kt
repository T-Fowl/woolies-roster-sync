package com.tfowl.woolies.sync.transform

import com.tfowl.workjam.client.model.*
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
    val storePositions: List<DescribableStorePosition>,
) {
    companion object {
        fun create(
            shift: Shift,
            title: String,
            storeRoster: List<Shift>,
        ): DescribableShift {

            fun Shift.toDescribableAsignees(): List<DescribableShiftAssignee> {
                return assignees.map { assignee ->
                    val profile = assignee.profile

                    DescribableShiftAssignee(
                        profile.firstName, profile.lastName, profile.avatarURL?.replace(
                            "/image/upload", "/image/upload/f_auto,q_auto,w_64,h_64,c_thumb,g_face"
                        ), startTime, endTime, event.type
                    )
                }
            }

            val describableStorePositions =
                storeRoster.groupBy { it.position.name.removeSupPrefix() }.toSortedMap()
                    .map { (position, positionedShifts) ->
                        DescribableStorePosition(
                            position,
                            positionedShifts.flatMap { it.toDescribableAsignees() }.sortedBy { it.startTime })
                    }

            return DescribableShift(shift, title, describableStorePositions)
        }
    }
}

internal data class DescribableStorePosition(
    val position: String,
    val coworkers: List<DescribableShiftAssignee>,
)

internal data class DescribableShiftAssignee(
    val firstName: String,
    val lastName: String,
    val avatarUrl: String? = null,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val type: ScheduleEventType,
) {
    val fullName: String get() = "$firstName $lastName"
}

internal object DefaultDescriptionGenerator : DescriptionGenerator {
    override fun generate(describableShift: DescribableShift): String = buildString {
        appendLine("<h1>Segments</h1>")
        describableShift.shift.segments.forEach { segment ->
            append("${segment.startTime} - ${segment.endTime}: ")
            when (segment.type) {
                SegmentType.BREAK_MEAL -> append("Meal Break")
                SegmentType.BREAK_REST -> append("Rest Break")
                else                   -> append(segment.position.name.removeSupPrefix())
            }
            appendLine()
        }
        appendLine("<hr>")

        appendLine("<h1>Coworkers</h1>")
        describableShift.storePositions.forEach { sp ->
            appendLine("<b>${sp.position}</b>")

            sp.coworkers.forEach { cw ->
                append("\t${cw.startTime} - ${cw.endTime} ${cw.fullName}")
                if (cw.type != ScheduleEventType.SHIFT) append(" OFF") // TODO
                appendLine()
            }

            appendLine("<hr>")
        }
    }
}