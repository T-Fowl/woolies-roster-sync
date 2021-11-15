package com.tfowl.woolies.sync.utils

import com.tfowl.workjam.client.model.Event
import com.google.api.services.calendar.model.Event as GoogleEvent

class ICalManager(private val suffix: String) {

    fun generate(event: Event): String = "${event.id}$suffix"

    fun isFromThisNamespace(event: GoogleEvent) = event.iCalUID?.endsWith(suffix) == true
}