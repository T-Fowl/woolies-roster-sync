package com.tfowl.woolies.sync.transform

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

object DefaultSummaryGenerator : SummaryGenerator {
    override fun generate(shift: Shift): String {
        val start = LocalTime.ofInstant(shift.event.startDateTime, shift.event.location.timeZoneID)
//        val end = LocalTime.ofInstant(shift.event.endDateTime, shift.event.location.timeZoneID)

        /* Trucks */
        when (start) {
            LocalTime.of(4, 30), LocalTime.of(5, 30) -> return "AM Trucks"
            LocalTime.of(13, 0)                      -> return "PM Trucks"
        }

        /* General Picking */
        when (start.hour) {
            in 0..13  -> return "Picking AM"
            in 16..23 -> return "Picking PM"
        }

        /* idk */
        return shift.event.title?.removePrefix("*SUP-")?.removePrefix("SUP-") ?: "Error: Missing Title"
    }
}