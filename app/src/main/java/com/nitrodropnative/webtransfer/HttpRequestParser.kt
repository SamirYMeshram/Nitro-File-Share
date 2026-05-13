package com.nitrodropnative.webtransfer

import java.io.BufferedInputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

internal data class HttpRequest(
    val method: String,
    val path: String,
    val query: Map<String, String>,
    val headers: Map<String, String>,
    val input: BufferedInputStream,
    val remoteAddress: String
) {
    fun header(name: String): String? = headers[name.lowercase(Locale.US)]
}

internal object HttpRequestParser {
    private const val MAX_LINE_LENGTH = 16 * 1024
    private const val MAX_HEADERS = 96

    fun parse(input: BufferedInputStream, remoteAddress: String): HttpRequest? {
        val requestLine = readAsciiLine(input).takeIf { it.isNotBlank() } ?: return null
        val requestParts = requestLine.split(' ', limit = 3)
        if (requestParts.size < 2) return null
        val method = requestParts[0].uppercase(Locale.US)
        val rawTarget = requestParts[1]
        val path = rawTarget.substringBefore('?')
        val query = parseQuery(rawTarget.substringAfter('?', ""))
        val headers = LinkedHashMap<String, String>()
        repeat(MAX_HEADERS) {
            val line = readAsciiLine(input)
            if (line.isBlank()) return HttpRequest(method, path, query, headers, input, remoteAddress)
            val separator = line.indexOf(':')
            if (separator > 0) {
                val key = line.substring(0, separator).trim().lowercase(Locale.US)
                val value = line.substring(separator + 1).trim()
                headers[key] = value
            }
        }
        return HttpRequest(method, path, query, headers, input, remoteAddress)
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split('&')
            .filter { it.isNotBlank() }
            .associate { part ->
                val key = part.substringBefore('=')
                val value = part.substringAfter('=', "")
                decode(key) to decode(value)
            }
    }

    fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    private fun readAsciiLine(input: BufferedInputStream): String {
        val buffer = StringBuilder(128)
        var count = 0
        while (true) {
            val b = input.read()
            if (b == -1) break
            if (b == '\n'.code) break
            if (b != '\r'.code) buffer.append(b.toChar())
            count++
            require(count <= MAX_LINE_LENGTH) { "HTTP line too long" }
        }
        return buffer.toString()
    }
}
