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
    val myShift: Shift,
    val title: String,
    val storeRoles: List<RoleSummary>,
) {
    companion object {
        fun create(
            myShift: Shift,
            title: String,
            storeDailyShifts: List<Shift>,
        ): DescribableShift {
            val roster = mutableMapOf<String, MutableList<DepartmentShift>>()

            storeDailyShifts.forEach { shift ->

                /**
                 * Group segments together when they're in the same department
                 * e.g. [Produce, Online, Meal, Online] --> [[Produce,], [Online, Meal, Online]]
                 *      [Produce, Meal, Online]         --> [[Produce, Meal], [Online,]]
                 */
                val segmentGroups = shift.segments.fold(emptyList<List<Segment>>()) { groups, segment ->
                    if (groups.isEmpty())
                        return@fold listOf(listOf(segment))
                    else {
                        val currentGroup = groups.last()
                        val lastSegment = currentGroup.last()
                        val secondLastSegment = currentGroup.getOrNull(currentGroup.lastIndex - 1)

                        /**
                         * 1. Last segment is same position as this segment (in case)
                         *    e.g. [Online, Online] --> [[Online,]]
                         * 2. This segment is a break (tack onto the previous segment regardless as I cannot know which one it technically belongs to)
                         *    e.g. [Online, Meal, Produce] --> [[Online, Meal], [Produce,]]
                         * 3. If the last segment is a break and the one before it is the same position as this one
                         *    e.g. [Online, Meal, Produce] --> [[Online, Meal], [Produce,]]
                         */
                        if (lastSegment.isSamePositionAs(segment) || segment.isBreak ||
                            (lastSegment.isBreak && true == secondLastSegment?.isSamePositionAs(segment))
                        ) {
                            return@fold groups.dropLast(1).plusElement(currentGroup + segment)
                        } else {
                            return@fold groups.plusElement(listOf(segment))
                        }
                    }
                }

                segmentGroups.forEach { group ->
                    val position = group.first().position.name.removeSupPrefix()
                    val startTime = group.first().startTime
                    val endTime = group.last().endTime

                    roster.getOrPut(position) { mutableListOf() }.addAll(
                        shift.assignees.map { assignee ->
                            DepartmentShift(
                                assignee,
                                startTime,
                                endTime
                            )
                        }
                    )
                }
            }

            val shiftComparator: Comparator<DepartmentShift> =
                compareBy { s: DepartmentShift -> s.startTime }
                    .thenComparing { s: DepartmentShift -> s.endTime }
                    .thenComparing { s: DepartmentShift -> s.lastName }

            val roleSummaries = roster.toSortedMap().map { (position, shifts) ->
                RoleSummary(
                    position,
                    shifts.sortedWith(shiftComparator)
                )
            }

            return DescribableShift(myShift, title, roleSummaries)
        }
    }
}

internal data class RoleSummary(
    val position: String,
    val coworkers: List<DepartmentShift>,
)

internal data class DepartmentShift(
    val firstName: String,
    val lastName: String,
    val avatarUrl: String? = null,
    val startTime: LocalTime,
    val endTime: LocalTime,
) {
    constructor(assignee: Assignee, startTime: LocalTime, endTime: LocalTime) : this(
        assignee.profile.firstName,
        assignee.profile.lastName,
        assignee.profile.avatarURL?.replace("/image/upload", "/image/upload/f_auto,q_auto,w_64,h_64,c_thumb,g_face"),
        startTime, endTime
    )

    val fullName: String get() = "$firstName $lastName"
}

internal object DefaultDescriptionGenerator : DescriptionGenerator {
    override fun generate(describableShift: DescribableShift): String = buildString {
        appendLine("<h1>Segments</h1>")
        describableShift.myShift.segments.forEach { segment ->
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
        describableShift.storeRoles.forEach { sp ->
            appendLine("<b>${sp.position}</b>")

            sp.coworkers.forEach { cw ->
                append("\t${cw.startTime} - ${cw.endTime} ${cw.fullName}")
                appendLine()
            }

            appendLine("<hr>")
        }
    }
}

val Segment.isBreak: Boolean
    get() = this.type == SegmentType.BREAK_MEAL || this.type == SegmentType.BREAK_REST

fun Segment.isSamePositionAs(other: Segment): Boolean =
    this.position.name.removeSupPrefix() == other.position.name.removeSupPrefix()