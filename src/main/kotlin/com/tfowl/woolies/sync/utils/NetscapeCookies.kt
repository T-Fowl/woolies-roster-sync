@file:Suppress("NOTHING_TO_INLINE")

package com.tfowl.woolies.sync.utils

import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.readLines

/**
 * Returns 6th *element* from the list.
 *
 * Throws an [IndexOutOfBoundsException] if the size of this list is less than 6.
 */
private inline operator fun <T> List<T>.component6(): T {
    return get(5)
}

/**
 * Returns 7th *element* from the list.
 *
 * Throws an [IndexOutOfBoundsException] if the size of this list is less than 7.
 */
private inline operator fun <T> List<T>.component7(): T {
    return get(6)
}

private fun String.unescapeHtml(): String {
    val text = this@unescapeHtml
    if (text.isEmpty()) return text

    return buildString(length) {
        var idx = 0
        chars@ while (idx < text.length) {
            val chr = text[idx]

            if (chr == '&') {
                if (text.startsWith("&quot;", startIndex = idx)) {
                    append('"')
                    idx += 6
                    continue@chars
                } else if (text.startsWith("&amp;", startIndex = idx)) {
                    append('&')
                    idx += 5
                    continue@chars
                } else if (text.startsWith("&lt;", startIndex = idx)) {
                    append('<')
                    idx += 4
                    continue@chars
                } else if (text.startsWith("&gt;", startIndex = idx)) {
                    append('>')
                    idx += 4
                    continue@chars
                }
            }

            append(chr)
            idx++
        }
    }
}

data class Cookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val expires: Instant,
)

private fun String.parseNetscapeCookieLine(): Cookie {
    val (domain, _ /* flag */, path, secure, expiration, name, value) = split('\t')
    return Cookie(
        name = name,
        value = value.unescapeHtml(),
        domain = domain,
        path = path,
        secure = secure.lowercase().toBooleanStrict(),
        expires = Instant.ofEpochSecond(expiration.toLong()),
    )
}

internal fun parseCookiesFile(lines: List<String>): List<Cookie> =
    lines
        .filterNot { it.startsWith("#") || it.isBlank() }
        .map(String::parseNetscapeCookieLine)

internal fun Path.readCookies(): List<Cookie> = parseCookiesFile(readLines())