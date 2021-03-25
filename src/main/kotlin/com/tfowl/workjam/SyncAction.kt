package com.tfowl.workjam

import com.google.api.client.googleapis.batch.BatchRequest
import com.google.api.services.calendar.model.Event
import com.tfowl.googleapi.CalendarEvents
import com.tfowl.googleapi.pretty
import com.tfowl.googleapi.queue

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

fun CalendarEvents.queue(batch: BatchRequest, action: SyncAction) {
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

fun CalendarEvents.queue(batch: BatchRequest, actions: List<SyncAction>) =
    actions.forEach { queue(batch, it) }
