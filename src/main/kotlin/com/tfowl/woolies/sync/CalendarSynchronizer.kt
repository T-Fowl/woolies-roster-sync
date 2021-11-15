package com.tfowl.woolies.sync

import com.google.api.client.googleapis.batch.BatchRequest
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.tfowl.googleapi.*
import java.time.Instant

sealed class SyncAction {
    abstract fun pretty(): String

    data class Create(val item: Event) : SyncAction() {
        override fun pretty() = "Create(${item.pretty()})"
    }

    data class Update(val original: Event, val updated: Event) : SyncAction() {
        override fun pretty() = "Update(${original.pretty()} to ${updated.pretty()})"
    }

    data class Delete(val item: Event) : SyncAction() {
        override fun pretty() = "Delete(${item.pretty()})"
    }
}

fun CalendarView.queue(batch: BatchRequest, action: SyncAction) {
    val request = when (action) {
        is SyncAction.Create -> insert(action.item)
        is SyncAction.Update -> update(action.original.id, action.updated)
        is SyncAction.Delete -> delete(action.item.id)
    }

    request.queue(batch,
        success = { println("Successfully executed ${action.pretty()}") },
        failure = { println("Failed to execute ${action.pretty()} because $it") }
    )
}

fun CalendarView.queue(batch: BatchRequest, actions: List<SyncAction>) =
    actions.forEach { queue(batch, it) }

class CalendarSynchronizer(
    val service: Calendar,
    val iCalManager: ICalManager,
) {
    private fun createGoogleSyncActions(
        currents: List<Event>,
        targets: List<Event>
    ): List<SyncAction> {
        val create = targets.filter { shift -> currents.none { it.iCalUID == shift.iCalUID } }
            .map { SyncAction.Create(it) }

        val update = targets.mapNotNull { shift ->
            currents.find { it.iCalUID == shift.iCalUID }?.let { existing ->
                SyncAction.Update(existing, shift)
            }
        }

        val delete = currents
            .filter { event -> !event.isCancelled() && targets.none { it.iCalUID == event.iCalUID } }
            .map { SyncAction.Delete(it) }

        return create + update + delete
    }

    fun sync(calendarId: String, syncStart: Instant, syncEnd: Instant, desired: List<Event>) {
        val timetable = service.calendarView(calendarId)

        val events = timetable.list()
            .setMaxResults(2500)
            .setTimeMin(syncStart.toGoogleDateTime())
            .setTimeMax(syncEnd.toGoogleDateTime())
            .setShowDeleted(true)
            .execute().items
            .filter { iCalManager.isFromThisNamespace(it) }

        // TODO: How to handle this? Remove / Recreate / ignore etc
        events.forEach { e ->
            requireNotNull(e.start.dateTime) { "Unsupported null start datetime for $e" }
        }

        val actions = createGoogleSyncActions(events, desired)
        service.batched {
            timetable.queue(batch, actions)
        }
    }
}
