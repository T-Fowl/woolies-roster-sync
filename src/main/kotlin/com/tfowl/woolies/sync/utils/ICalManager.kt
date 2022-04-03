package com.tfowl.woolies.sync.utils

import com.tfowl.workjam.client.model.ScheduleEvent
import com.google.api.services.calendar.model.Event as GoogleEvent

class ICalManager(private val suffix: String) {

    fun generate(event: ScheduleEvent): String = "${event.id}$suffix"

    fun isFromThisNamespace(event: GoogleEvent) = event.iCalUID?.endsWith(suffix) == true
}