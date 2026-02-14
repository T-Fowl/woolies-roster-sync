package com.tfowl.woolies.sync.commands.options

import com.auth0.jwt.JWT
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onSuccess
import com.tfowl.woolies.sync.connectToBrowser
import com.tfowl.woolies.sync.createWebDriver
import com.tfowl.woolies.sync.findTokenCookie
import com.tfowl.woolies.sync.login
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Instant

private val LOGGER = LoggerFactory.getLogger("com.tfowl.woolies.sync.commands.options.auth")

sealed class AuthMethod(name: String) : OptionGroup(name)

class BrowserAuthentication : AuthMethod("Options for browser authentication") {
    val email by option("--email", envvar = "WORKJAM_EMAIL")
        .prompt("Enter your email address")

    val password by option("--password", envvar = "WORKJAM_PASSWORD")
        .prompt("Enter your password", hideInput = true)

    val playwrightUrl by option("--playwright-driver-url", envvar = "PLAYWRIGHT_DRIVER_URL").required()
}

class TokenAuthentication : AuthMethod("Options for token authentication") {
    val token by option("--token", envvar = "WORKJAM_TOKEN")
        .convert { JWT.decode(it) }
        .prompt("Enter your token")
        .check("Token must be current") { Instant.now() in (it.notBeforeAsInstant..it.expiresAtAsInstant) }
}

suspend fun AuthMethod.token(): Result<String, Any> = when (this) {
    is TokenAuthentication   -> {
        LOGGER.atDebug().log("Authenticating with a token")
        Ok(token.token)
    }

    is BrowserAuthentication -> withContext(Dispatchers.IO) {
        LOGGER.atDebug().log("Authenticating with a browser")

        binding {
            LOGGER.atDebug().log("Creating web driver")
            createWebDriver().bind().use { driver ->

                LOGGER.atDebug()
                    .addKeyValue("playwrightUrl", playwrightUrl)
                    .log("Connecting to browser")
                val browser = connectToBrowser(driver, playwrightUrl).bind()

                LOGGER.atDebug().log("Logging into workjam")
                val homePage = login(browser, email, password).bind()

                findTokenCookie(homePage).bind()
            }
        }.onSuccess { LOGGER.atDebug().log("Success") }
    }
}