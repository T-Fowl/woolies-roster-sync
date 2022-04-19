package com.tfowl.woolies.sync.transform

import com.tfowl.workjam.client.model.Segment
import com.tfowl.workjam.client.model.Shift
import java.time.LocalTime

/**
 * /**
 * Responsible for generating the summary/title for [com.google.api.services.calendar.model.Event]s
*/
 */
interface SummaryGenerator {
    fun generate(shift: Shift): String
}

private val Shift.startTime: LocalTime get() = LocalTime.ofInstant(event.startDateTime, event.location.timeZoneID)
private val Shift.endTime: LocalTime get() = LocalTime.ofInstant(event.endDateTime, event.location.timeZoneID)
private val Segment.startTime: LocalTime get() = LocalTime.ofInstant(startDateTime, location.timeZoneID)

data class ShiftRundown(
    val amTrucks: Boolean = false,
    val pmTrucks: Boolean = false,
    val amStart: Boolean = false,
    val pmStart: Boolean = false,
    val amFinish: Boolean = false,
    val pmFinish: Boolean = false
)

object DefaultSummaryGenerator : SummaryGenerator {

    // TODO: There should probably be a way to specify this in a configuration file
    override fun generate(shift: Shift): String {

        var rundown = ShiftRundown(
            amStart = shift.startTime < LocalTime.NOON,
            pmStart = shift.startTime >= LocalTime.NOON,
            amFinish = shift.endTime < LocalTime.NOON,
            pmFinish = shift.endTime >= LocalTime.NOON
        )

        val segmentGroups = shift.segments.groupBy { it.position.name.lowercase() }

        /* If the shift is set up correctly with different segments, including dispatch: */
        if ("online dispatch" in segmentGroups) {
            val dispatchSegments = segmentGroups["online dispatch"]!!

            if (dispatchSegments.any { segment -> segment.startTime < LocalTime.NOON })
                rundown = rundown.copy(amTrucks = true)
            if (dispatchSegments.any { segment -> segment.startTime >= LocalTime.NOON })
                rundown = rundown.copy(pmTrucks = true)
        }

        /* If the shift isn't set up correctly with dispatch segments: */

        // AM Trucks are usually 4:30 or 5:30, this is for historical shifts where I started earlier
        if (shift.startTime <= LocalTime.of(5, 30))
            rundown = rundown.copy(amTrucks = true)

        // Doesn't include starting early, but if I'm already at work doing PM trucks doesn't really matter
        if (shift.startTime == LocalTime.of(13, 0))
            rundown = rundown.copy(pmTrucks = true)


        return when {
            rundown.amTrucks && rundown.pmTrucks -> "AM & PM Trucks"
            rundown.amTrucks                     -> "AM Trucks"
            rundown.amStart && rundown.pmTrucks  -> "AM + PM Trucks"
            rundown.pmTrucks                     -> "PM Trucks"
            // TODO: Should these be changed to just "AM" & "PM" as I am not guaranteed to be only picking
            rundown.amStart                      -> "Picking AM"
            rundown.pmStart                      -> "Picking PM"
            /* idk */
            else                                 -> shift.event.title?.removePrefix("*SUP-")?.removePrefix("SUP-")
                ?: "Error: Missing Title"
        }
    }
}