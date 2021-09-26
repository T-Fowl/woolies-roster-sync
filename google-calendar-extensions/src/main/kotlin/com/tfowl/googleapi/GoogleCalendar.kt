package com.tfowl.googleapi

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.batch.BatchRequest
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.apache.v2.ApacheHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.io.File

const val DEFAULT_TOKENS_DIR = "tokens_dir"

data class GoogleApiServiceConfig(
    val secrets: File,
    val applicationName: String,
    val scopes: List<String>,
    val httpTransport: HttpTransport = ApacheHttpTransport(),
    val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance(),
    val dataStoreFactory: DataStoreFactory = FileDataStoreFactory(File(DEFAULT_TOKENS_DIR))
)

object GoogleCalendar {
    private fun getCredentials(
        config: GoogleApiServiceConfig
    ): Credential {
        val input = config.secrets.reader()
        val secrets = GoogleClientSecrets.load(config.jsonFactory, input)

        val flow = GoogleAuthorizationCodeFlow.Builder(config.httpTransport, config.jsonFactory, secrets, config.scopes)
            .setDataStoreFactory(config.dataStoreFactory)
            .setAccessType("offline")
            .build()

        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    fun create(
        config: GoogleApiServiceConfig,
    ): Calendar {
        val credentials = getCredentials(config)
        return Calendar.Builder(config.httpTransport, config.jsonFactory, credentials)
            .setApplicationName(config.applicationName)
            .build()
    }
}

fun Calendar.calendarEvents(id: String) = CalendarEvents(this, id)

class CalendarEvents(
    private val api: Calendar,
    private val calendarId: String
) {
    fun list() = api.events().list(calendarId)
    fun insert(event: Event) = api.events().insert(calendarId, event)
    fun update(id: String, content: Event) = api.events().update(calendarId, id, content)
    fun delete(id: String) = api.events().delete(calendarId, id)
    fun get(id: String) = api.events().get(calendarId, id)
    fun patch(id: String, content: Event) = api.events().patch(calendarId, id, content)
}

internal class GoogleJsonException(val error: GoogleJsonError) : RuntimeException()

internal fun <T> AbstractGoogleJsonClientRequest<T>.queueDeferred(batch: BatchRequest): Deferred<T> {
    val cp = CompletableDeferred<T>()

    queue(batch, object : JsonBatchCallback<T>() {
        override fun onSuccess(t: T, responseHeaders: HttpHeaders) {
            cp.complete(t)
        }

        override fun onFailure(e: GoogleJsonError, responseHeaders: HttpHeaders) {
            cp.completeExceptionally(GoogleJsonException(e))
        }
    })

    return cp
}

fun <T> AbstractGoogleJsonClientRequest<T>.queue(
    batch: BatchRequest,
    success: (T) -> Unit = {},
    failure: (GoogleJsonError) -> Unit = {},
) {
    queue(batch, object : JsonBatchCallback<T>() {
        override fun onSuccess(t: T, responseHeaders: HttpHeaders?) {
            success(t)
        }

        override fun onFailure(e: GoogleJsonError, responseHeaders: HttpHeaders?) {
            failure(e)
        }
    })
}

class BatchRequestContext(
    val batch: BatchRequest
)

inline fun <R> Calendar.batched(block: BatchRequestContext.() -> R): R {
    val ctx = BatchRequestContext(batch())
    val result = ctx.block()
    ctx.batch.execute()
    return result
}