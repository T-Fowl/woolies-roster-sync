package com.tfowl.woolies.sync.transform

import com.tfowl.workjam.client.model.Shift
import com.tfowl.workjam.client.model.startTime
import java.time.LocalTime

fun String.removeSupPrefix(): String = replace(Regex("""^[*]?SUP\s*-\s*"""), "")

private fun LocalTime.isTrucksStartingTime(): Boolean =
    (this == LocalTime.of(4, 30)) || (this == LocalTime.of(5, 30)) || (this == LocalTime.of(13, 0))

/**
 * /**
 * Responsible for generating the summary/title for [com.google.api.services.calendar.model.Event]s
*/
 */
interface SummaryGenerator {
    fun generate(shift: Shift): String
}

object DefaultSummaryGenerator : SummaryGenerator {

    // TODO: There should probably be a way to specify this in a configuration file
    override fun generate(shift: Shift): String {
        /* If the shift is set up correctly with different segments, including dispatch: */
        val segmentGroups = shift.segments.groupBy { it.position.name.removeSupPrefix().lowercase() }

        return buildString {
            when {
                shift.startTime < LocalTime.NOON  -> append("AM ")
                shift.startTime >= LocalTime.NOON -> append("PM ")
            }

            if (shift.startTime == LocalTime.of(3, 0))
                append("Opening ")

            if (segmentGroups.keys.any { "supervisor" in it }) {
                append("Supervisor ")
            }

            if ("online dispatch" in segmentGroups) {
                append("Trucks ")
            } else if (segmentGroups.keys.any { "online" in it } && shift.startTime.isTrucksStartingTime()) {
                /* If the shift isn't set up correctly with dispatch segments: */
                append("Trucks ")
            }
        }.trim()
    }
}

