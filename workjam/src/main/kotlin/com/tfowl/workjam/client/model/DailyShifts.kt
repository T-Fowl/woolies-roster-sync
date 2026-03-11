package com.tfowl.workjam.client.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class DailyShift(
    @Contextual
    val startDateTime: Instant,
    val shiftEvent: ScheduleEvent,
    val timecardSummaries: List<TimecardSummary>,
)

@Serializable
data class TimecardSummary(
    val status: String,
    val punchEntries: List<PunchEntry>,
    val employee: Employee,
) {
    @Serializable
    data class PunchEntry(
        val id: String,
        val employeeId: String,
        @Contextual val dateTime: Instant,
    )
}

