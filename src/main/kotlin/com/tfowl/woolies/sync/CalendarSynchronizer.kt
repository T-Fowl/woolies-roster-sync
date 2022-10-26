package com.tfowl.woolies.sync

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.tfowl.googleapi.*
import com.tfowl.woolies.sync.utils.ICalManager
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

data class SyncResult(val action: SyncAction, val result: Result<Unit, GoogleJsonError>)

private fun SyncAction.toRequest(view: CalendarView) = when (this) {
    is SyncAction.Create -> view.insert(item)
    is SyncAction.Update -> view.update(original.id, updated)
    is SyncAction.Delete -> view.delete(item.id)
}

class CalendarSynchronizer(
    val service: Calendar,
    val iCalManager: ICalManager,
) {
    private fun computeSyncActions(
        currentEvents: List<Event>,
        requiredEvents: List<Event>
    ): List<SyncAction> {
        val create = requiredEvents.filter { shift -> currentEvents.none { it.iCalUID == shift.iCalUID } }
            .map { SyncAction.Create(it) }

        val update = requiredEvents.mapNotNull { shift ->
            currentEvents.find { it.iCalUID == shift.iCalUID }?.let { existing ->
                SyncAction.Update(existing, shift)
            }
        }

        val delete = currentEvents
            .filter { event -> !event.isCancelled() && requiredEvents.none { it.iCalUID == event.iCalUID } }
            .map { SyncAction.Delete(it) }

        return create + update + delete
    }

    suspend fun sync(calendarId: String, syncPeriodStart: Instant, syncPeriodEnd: Instant, desiredEvents: List<Event>) = coroutineScope {
        val calendar = service.calendarView(calendarId)

        val events = calendar.list()
            .setMaxResults(2500)
            .setTimeMin(syncPeriodStart.toGoogleDateTime())
            .setTimeMax(syncPeriodEnd.toGoogleDateTime())
            .setShowDeleted(true)
            .execute().items
            .filter { iCalManager.isFromThisNamespace(it) }

        // TODO: How to handle this? Remove / Recreate / ignore etc
        events.forEach { e ->
            requireNotNull(e.start.dateTime) { "Unsupported null start datetime for $e" }
        }

        val actions = computeSyncActions(events, desiredEvents)

        /* Avoid Preconditions.checkState(!requestInfos.isEmpty());
         * at com.google.api.client.googleapis.batch.BatchRequest.execute(BatchRequest.java:231) */
        if(actions.isEmpty()) return@coroutineScope

        val actionResults = service.batched {
            actions.map { action ->
                val request = action.toRequest(calendar)

                async(start = CoroutineStart.UNDISPATCHED) {
                    SyncResult(action, request.queueSuspending().map { })
                }
            }
        }.awaitAll()

        actionResults.forEach { (action, result) ->
            when (result) {
                is Ok  -> println("Successfully completed: ${action.pretty()}")
                is Err -> println("Failed to complete: ${action.pretty()} with error: ${result.error}")
            }
        }
    }
}
