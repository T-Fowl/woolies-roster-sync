package com.tfowl.woolies.sync

import com.tfowl.workjam.client.WorkjamClient
import com.tfowl.workjam.client.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate

sealed class OutputEvent {
    abstract val event: ScheduleEvent

    companion object {
        suspend fun create(
            workjam: WorkjamClient,
            company: String,
            event: ScheduleEvent,
        ): OutputEvent? {
            return when (event.type) {
                ScheduleEventType.SHIFT                 -> OutputShift.create(
                    workjam,
                    company,
                    workjam.shift(company, event.location.id, event.id)
                )

                ScheduleEventType.AVAILABILITY_TIME_OFF -> OutputAvailability(
                    workjam.availability(
                        company,
                        workjam.userId,
                        event.id
                    )
                )

                else                                    -> null
            }
        }
    }
}

data class OutputShift(
    val shift: Shift,
    val store: Store,
    val storeShifts: List<Shift>,
) : OutputEvent() {
    override val event: ScheduleEvent
        get() = shift.event

    companion object {
        suspend fun create(workjam: WorkjamClient, company: String, shift: Shift): OutputEvent {
            val event = shift.event
            val location = event.location
            val zone = location.timeZoneID


            return coroutineScope {
                val storeAsync = async(Dispatchers.IO) {
                    workjam.employers(workjam.userId).companies.firstNotNullOf { company ->
                        company.stores.find { store -> store.externalID == shift.event.location.externalID }
                    }
                }

                val storeRosterAsync = async(Dispatchers.IO) {
                    workjam.shifts(
                        company,
                        shift.event.location.id,
                        startDateTime = LocalDate.ofInstant(shift.event.startDateTime, zone).atStartOfDay(zone)
                            .toOffsetDateTime(),
                        endDateTime = LocalDate.ofInstant(shift.event.startDateTime, zone).plusDays(1)
                            .atStartOfDay(zone)
                            .toOffsetDateTime()
                    )
                }

                OutputShift(shift, storeAsync.await(), storeRosterAsync.await())
            }
        }
    }
}

data class OutputAvailability(val availability: Availability) : OutputEvent() {
    override val event: ScheduleEvent
        get() = availability.event

    companion object {
        suspend fun create(workjam: WorkjamClient, availability: Availability): OutputEvent {
            return OutputAvailability(availability)
        }
    }
}