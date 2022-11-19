package com.tfowl.googleapi

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver
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
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.io.File

data class GoogleApiServiceConfig(
    val secrets: File,
    val applicationName: String,
    val scopes: List<String>,
    val dataStoreFactory: DataStoreFactory,
    val httpTransport: HttpTransport = ApacheHttpTransport(),
    val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance(),
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

        val receiver: VerificationCodeReceiver = LocalServerReceiver.Builder().setPort(8888).build()
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

fun Calendar.calendarView(id: String) = CalendarView(this, id)

class CalendarView(
    private val api: Calendar,
    private val calendarId: String
) {
    fun batch(): BatchRequest = api.batch()
    fun list() = api.events().list(calendarId)
    fun insert(event: Event) = api.events().insert(calendarId, event)
    fun update(id: String, content: Event) = api.events().update(calendarId, id, content)
    fun delete(id: String) = api.events().delete(calendarId, id)
    fun get(id: String) = api.events().get(calendarId, id)
    fun patch(id: String, content: Event) = api.events().patch(calendarId, id, content)
}

suspend fun <T> AbstractGoogleJsonClientRequest<T>.queueSuspending(batch: BatchRequest): Result<T, GoogleJsonError> =
    queueAsync(batch).await()

fun <T> AbstractGoogleJsonClientRequest<T>.queueAsync(batch: BatchRequest): Deferred<Result<T, GoogleJsonError>> {
    val cp = CompletableDeferred<Result<T, GoogleJsonError>>()

    queue(batch, object : JsonBatchCallback<T>() {
        override fun onSuccess(t: T, responseHeaders: HttpHeaders?) {
            cp.complete(Ok(t))
        }

        override fun onFailure(e: GoogleJsonError, responseHeaders: HttpHeaders?) {
            cp.complete(Err(e))
        }
    })

    return cp
}

class BatchRequestContext(private val batch: BatchRequest) {
    fun <T> AbstractGoogleJsonClientRequest<T>.queue(callback: JsonBatchCallback<T>) =
        queue(batch, callback)

    fun <T> AbstractGoogleJsonClientRequest<T>.queueAsync(): Deferred<Result<T, GoogleJsonError>> =
        queueAsync(batch)

    suspend fun <T> AbstractGoogleJsonClientRequest<T>.queueSuspending(): Result<T, GoogleJsonError> =
        queueSuspending(batch)
}

inline fun <R> BatchRequest.use(block: BatchRequestContext.() -> R): R {
    val ctx = BatchRequestContext(this)
    val result = ctx.block()
    execute()
    return result
}