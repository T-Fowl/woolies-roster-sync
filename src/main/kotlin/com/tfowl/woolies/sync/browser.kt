package com.tfowl.woolies.sync

import com.github.michaelbull.result.*
import com.microsoft.playwright.*
import com.microsoft.playwright.options.AriaRole

sealed class BrowserError {
    data class CreateWebDriver(val cause: Throwable) : BrowserError()
    data class LaunchBrowser(val cause: Throwable) : BrowserError()
    data class Login(val cause: Throwable) : BrowserError()

    object TokenNotFound : BrowserError()
}

fun createWebDriver(): Result<Playwright, BrowserError> = runCatching {
    Playwright.create()
}.mapError(BrowserError::CreateWebDriver)

fun launchBrowser(playwright: Playwright, headless: Boolean = true): Result<Browser, BrowserError> = runCatching {
    playwright.firefox().launch(BrowserType.LaunchOptions().setHeadless(headless))
}.mapError(BrowserError::LaunchBrowser)

fun login(browser: Browser, email: String, password: String): Result<Page, BrowserError> = runCatching {
    val page = browser.newPage(Browser.NewPageOptions())

    page.navigate("https://app.workjam.com/")
    page.getByPlaceholder("Username or email or @company").fill(email)
    page.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Continue")).click()

    page.getByPlaceholder("Password").click()
    page.getByPlaceholder("Password").fill(password)

    page.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Sign in")).click()

    page.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("No")).click()

    page.waitForURL("https://app.workjam.com/home")

    page
}.mapError(BrowserError::Login)

fun findTokenCookie(page: Page): Result<String, BrowserError> {
    val token = page.context().cookies().firstOrNull { cookie ->
        "api.workjam.com" in cookie.domain && "token" in cookie.name
    }?.value

    return when (token) {
        null -> Err(BrowserError.TokenNotFound)
        else -> Ok(token)
    }
}