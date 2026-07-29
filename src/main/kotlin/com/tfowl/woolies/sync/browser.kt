package com.tfowl.woolies.sync

import com.github.michaelbull.result.*
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.AriaRole
import com.microsoft.playwright.options.WaitUntilState

sealed class BrowserError {
    data class CreateWebDriver(val cause: Throwable) : BrowserError()
    data class LaunchBrowser(val cause: Throwable) : BrowserError()
    data class Login(val cause: Throwable) : BrowserError()

    data object TokenNotFound : BrowserError()
}

fun createWebDriver(): Result<Playwright, BrowserError> = runCatching {
    Playwright.create(
        Playwright.CreateOptions().setEnv(
            mapOf("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD" to "true")
        )
    )
}.mapError(BrowserError::CreateWebDriver)

fun connectToBrowser(playwright: Playwright, url: String): Result<Browser, BrowserError> = runCatching {
    playwright.chromium().connect(url)
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

    // Seems to not work, haven't tried with NETWORKIDLE wait state however
    // page.waitForURL(Regex("""https://app(-next)?.workjam.com/home""").pattern)

    page.waitForURL(
        { url -> url in arrayOf("https://app.workjam.com/home", "https://app-next.workjam.com/home") },
        Page.WaitForURLOptions().setWaitUntil(WaitUntilState.NETWORKIDLE)
    )

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