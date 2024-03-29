package com.tfowl.woolies.sync.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.CalendarScopes
import com.tfowl.gcal.GoogleApiServiceConfig
import com.tfowl.woolies.sync.googleClientSecretsOption
import java.io.File
import kotlin.system.exitProcess

class Authorise :
    CliktCommand(name = "authorise", help = "Used when authorising against your google accounts on a remote machine") {

    private val remote by option("--remote", help = "Running this command on a remote headless machine")
        .flag()

    private val doNotOpenBrowser by option(help = "Do not automatically open auth link in default browser")
        .flag("--auth-no-open-browser")

    private val googleClientSecrets by googleClientSecretsOption().required()

    override fun run() {
        val dsf: DataStoreFactory = FileDataStoreFactory(File(DEFAULT_STORAGE_DIR))

        val config = GoogleApiServiceConfig(
            secretsProvider = { googleClientSecrets },
            applicationName = APPLICATION_NAME,
            scopes = listOf(CalendarScopes.CALENDAR),
            dataStoreFactory = dsf
        )

        val input = config.secretsProvider()
        val secrets = GoogleClientSecrets.load(config.jsonFactory, input)

        val flow = GoogleAuthorizationCodeFlow.Builder(config.httpTransport, config.jsonFactory, secrets, config.scopes)
            .setDataStoreFactory(config.dataStoreFactory)
            .setAccessType("offline")
            .build()

        if (remote) {
            println("Please enter the code received on your machine:")
            val code = readln()

            flow.createAndStoreCredential(
                flow.newTokenRequest(code).setRedirectUri("http://localhost:8888/Callback").execute(),
                "user"
            )
            println("Done")
        } else {
            val receiver = LocalServerReceiver.Builder().setPort(8888).build()

            val url = flow.newAuthorizationUrl().setRedirectUri(receiver.redirectUri).build()

            if (doNotOpenBrowser) {
                println("Please open the following address in your browser:")
                println("  $url")
            } else {
                AuthorizationCodeInstalledApp.DefaultBrowser().browse(url)
            }

            val code = receiver.waitForCode()
            println("Copy and paste the following code into the headless machine:\n    $code")
        }

        exitProcess(0)
    }
}