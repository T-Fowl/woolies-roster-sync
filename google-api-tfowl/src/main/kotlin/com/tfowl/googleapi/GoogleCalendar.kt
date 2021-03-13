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
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.tfowl.googlapi.okhttp.OkHttpTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.io.File

const val CLIENT_SECRETS = "client-secrets.json"
const val TOKENS_DIRECTORY = "tokens_dir"
const val APPLICATION_NAME = "APPLICATION_NAME"

@Suppress("SameParameterValue")
private fun getCredentials(
    httpTransport: HttpTransport,
    jsonFactory: JsonFactory,
    scopes: List<String>,
    dataStoreFactory: DataStoreFactory,
): Credential {
    val input = File(CLIENT_SECRETS).reader()
    val secrets = GoogleClientSecrets.load(jsonFactory, input)

    val flow = GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, secrets, scopes)
        .setDataStoreFactory(dataStoreFactory)
        .setAccessType("offline")
        .build()

    val receiver = LocalServerReceiver.Builder().setPort(8888).build()
    return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
}

object GoogleCalendar {
    private val dataStoreFactory: DataStoreFactory = FileDataStoreFactory(File(TOKENS_DIRECTORY))
    private val httpTransport: HttpTransport = OkHttpTransport()
    private val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
    private val scopes = listOf(CalendarScopes.CALENDAR)
    val credentials = getCredentials(httpTransport, jsonFactory, scopes, dataStoreFactory)

    val calendar: Calendar = Calendar.Builder(httpTransport, jsonFactory, credentials)
        .setApplicationName(APPLICATION_NAME)
        .build()
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

//fun GoogleCalendar(
//    dataStoreFactory: DataStoreFactory = FileDataStoreFactory(File(TOKENS_DIRECTORY)),
//    httpTransport: HttpTransport = OkHttpTransport(),
//    jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance(),
//    scopes: List<String> = listOf(CalendarScopes.CALENDAR)
//): Calendar {
//    val credentials = getCredentials(httpTransport, jsonFactory, scopes, dataStoreFactory)
//    return Calendar.Builder(httpTransport, jsonFactory, credentials)
//        .setApplicationName(APPLICATION_NAME)
//        .build()
//}

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