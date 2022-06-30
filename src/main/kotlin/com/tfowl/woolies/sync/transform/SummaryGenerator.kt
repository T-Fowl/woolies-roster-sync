package com.tfowl.woolies.sync.transform

import com.tfowl.workjam.client.model.Shift
import com.tfowl.workjam.client.model.startTime
import java.time.LocalTime

fun String.removeSupPrefix(): String = replace(Regex("""^[*]?SUP\s*-\s*"""), "")

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

        if (segmentGroups.keys.any { "supervisor" in it }) {
            return "Supervisor"
        }

        if ("online dispatch" in segmentGroups) {
            val dispatchSegments = segmentGroups["online dispatch"]!!

            if (dispatchSegments.any { segment -> segment.startTime < LocalTime.NOON })
                return "AM Trucks"
            if (dispatchSegments.any { segment -> segment.startTime >= LocalTime.NOON })
                return "PM Trucks"
            return "Trucks"
        }

        /* If the shift isn't set up correctly with dispatch segments: */

        // AM Trucks are usually 4:30 or 5:30, this is for historical shifts where I started earlier
        if (shift.startTime <= LocalTime.of(5, 30))
            return "AM Trucks"

        // Doesn't include starting early, but if I'm already at work doing PM trucks doesn't really matter
        if (shift.startTime == LocalTime.of(13, 0))
            return "PM Trucks"

        return when {
            shift.startTime < LocalTime.NOON  -> "AM"
            shift.startTime >= LocalTime.NOON -> "PM"
            else                              -> shift.event.title?.removeSupPrefix() ?: "Error: Missing Title"
        }
    }
}

