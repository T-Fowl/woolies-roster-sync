package com.tfowl.workjam.client

import io.ktor.client.engine.*
import io.ktor.client.engine.java.*
import io.ktor.http.*

interface HttpEngineProvider {
    fun provide(): HttpClientEngine
    fun defaultUrlBuilder(): URLBuilder
}

open class DefaultHttpEngineProvider(private val host: String = "prod-aus-gcp-woolworths-api.workjam.com") :
    HttpEngineProvider {
    override fun provide(): HttpClientEngine = Java.create()
    override fun defaultUrlBuilder(): URLBuilder {
        return URLBuilder(
            protocol = URLProtocol.HTTPS,
            host = host
        )
    }
}