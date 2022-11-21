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

private val ESCAPED_ENTITY_BY_NAME = arrayOf(
    "&Tab;" to '\t',
    "&NewLine;" to '\n',
    "&nbsp;" to ' ',
    "&quot;" to '"',
    "&amp;" to '&',
    "&lt;" to '<',
    "&gt;" to '>',
)

fun String.unescapeHtml(): String {
    if (isEmpty()) return this
    val text = this

    return buildString(length) {
        var i = 0

        chars@ while (i < text.length) {
            val curr = text[i]

            if (curr == '&') {
                // ASCII code is at minimum &#9; long, hence +3
                if (i + 3 < text.length && text[i + 1] == '#') {
                    val end = text.indexOf(';', startIndex = i)

                    if (end > 0) {
                        val code = if (text[i + 2] == 'x') {
                            text.substring(i + 3, end).toIntOrNull(16)
                        } else {
                            text.substring(i + 2, end).toIntOrNull()
                        }

                        if (code != null) {
                            append(code.toChar())
                            i = end + 1
                            continue@chars
                        }
                    }
                } else {
                    // Try the named entities
                    for ((code, escaped) in ESCAPED_ENTITY_BY_NAME) {
                        if (text.startsWith(code, startIndex = i, ignoreCase = true)) {
                            append(escaped)
                            i += code.length
                            continue@chars
                        }
                    }
                }

            }

            // Fallback
            append(curr)
            i++
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

internal fun String.parseNetscapeCookieLine(): Cookie {
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