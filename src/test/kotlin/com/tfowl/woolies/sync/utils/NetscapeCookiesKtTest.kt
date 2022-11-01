package com.tfowl.woolies.sync.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

class NetscapeCookiesKtTest {

    @Test
    fun parseNetscapeCookieLine() {
        assertEquals(
            Cookie(
                name = "name",
                value = "value",
                domain = "example.com",
                path = "/path",
                secure = false,
                expires = Instant.ofEpochSecond(0)
            ),
            "example.com\tFALSE\t/path\tFALSE\t0\tname\tvalue".parseNetscapeCookieLine()
        )
    }

    @Test
    fun parseNetscapeCookieFile() {

        // Comments, empty and blank lines
        assertEquals(emptyList<Cookie>(), parseCookiesFile(listOf("# Hello", "# World", "# ", "", " ")))

        // Standard
        assertEquals(
            listOf(
                Cookie(
                    name = "a",
                    value = "b",
                    domain = "example.com",
                    path = "/path",
                    secure = false,
                    expires = Instant.ofEpochSecond(0)
                ),
                Cookie(
                    name = "c",
                    value = "d",
                    domain = "example.com",
                    path = "/path",
                    secure = false,
                    expires = Instant.ofEpochSecond(0)
                )
            ), parseCookiesFile(
                listOf(
                    "# Cookie #1",
                    "example.com\tFALSE\t/path\tFALSE\t0\ta\tb",
                    "# Cookie #2",
                    "example.com\tFALSE\t/path\tFALSE\t0\tc\td"
                )
            )
        )
    }

    @Nested
    inner class UnescapeHtml {

        @Test
        fun identity() {
            // Don't unescape non-escaped characters
            assertEquals(
                "The quick brown fox jumps over the lazy dog",
                "The quick brown fox jumps over the lazy dog".unescapeHtml()
            )
        }

        @Test
        fun named() {

            // Required named entities
            assertEquals("\t\n \"&<>", "&Tab;&NewLine;&nbsp;&quot;&amp;&lt;&gt;".unescapeHtml())
        }

        @Test
        fun numeric() {
            // Numeric entities
            assertEquals(
                "Hello World",
                "&#72;&#101;&#108;&#108;&#111;&#32;&#87;&#111;&#114;&#108;&#100;".unescapeHtml()
            )
        }

        @Test
        fun hex() {
            // Hex entities
            assertEquals(
                "Hello World",
                "&#x48;&#x65;&#x6c;&#x6c;&#x6f;&#x20;&#x57;&#x6f;&#x72;&#x6c;&#x64;".unescapeHtml()
            )
        }

        @Test
        fun combined() {
            // Combined
            assertEquals("Hello World!", "&#72;&#x65;l&#x6c;&#x6f;&nbsp;&#x57;&#111;rld!".unescapeHtml())
        }
    }
}
