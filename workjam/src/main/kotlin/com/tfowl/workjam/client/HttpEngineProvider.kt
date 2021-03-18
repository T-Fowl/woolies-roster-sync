package com.tfowl.workjam.client

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*

interface HttpEngineProvider {
    fun provide(): HttpClientEngine
}

open class DefaultHttpEngineProvider : HttpEngineProvider {
    override fun provide(): HttpClientEngine = CIO.create()
}