@file:UseSerializers(DurationSerialiser::class, LocalDateSerialiser::class)

package com.tfowl.workjam.client.model

import com.tfowl.workjam.client.model.serialisers.DurationSerialiser
import com.tfowl.workjam.client.model.serialisers.LocalDateSerialiser
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@Serializable
data class PeriodicTimecard(
    val approvalStatus: String,
    val endDay: LocalDate,
    val startDay: LocalDate,
    val paidHours: Double,
    val paidHoursDuration: Duration,
    val status: String,
    val timecards: List<Timecard>,
    val payPeriodType: String,
)

@Serializable
data class Timecard(
    val status: String,
    @Contextual
    val startDateTime: Instant,
    val paidHours: Double,
    val paidHoursDuration: Duration,
    val punchEntries: List<PunchEntry>,
    val earningsItem: List<EarningsItem>,
)

@Serializable
data class EarningsItem(
    @Contextual
    val dateTime: Instant,
    val duration: Duration,
    val payCode: String,
)

@Serializable
data class PunchEntry(
    @Contextual
    val dateTime: Instant,
    val type: String,
    val location: PunchEntryLocation,
    val position: PunchEntryPosition,
)

@Serializable
data class PunchEntryPosition(
    val id: String,
    val name: String,
)

@Serializable
data class PunchEntryLocation(
    val id: String,
    val name: String,
    val type: String,
)