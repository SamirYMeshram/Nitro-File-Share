package com.nitrodropnative.webtransfer

import java.io.OutputStream
import java.nio.charset.StandardCharsets

internal object HttpResponseWriter {
    fun text(
        out: OutputStream,
        status: String,
        body: String,
        contentType: String = "text/plain; charset=utf-8",
        extraHeaders: Map<String, String> = emptyMap()
    ) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        headers(out, status, contentType, bytes.size.toLong(), extraHeaders)
        out.write(bytes)
        out.flush()
    }

    fun json(out: OutputStream, status: String, body: String) {
        text(out, status, body, "application/json; charset=utf-8", mapOf("Cache-Control" to "no-store"))
    }

    fun redirect(out: OutputStream, location: String, extraHeaders: Map<String, String> = emptyMap()) {
        val body = "Redirecting to $location"
        text(
            out = out,
            status = "303 See Other",
            body = body,
            contentType = "text/plain; charset=utf-8",
            extraHeaders = extraHeaders + mapOf("Location" to location, "Cache-Control" to "no-store")
        )
    }

    fun headers(
        out: OutputStream,
        status: String,
        contentType: String,
        contentLength: Long,
        extraHeaders: Map<String, String> = emptyMap()
    ) {
        val builder = StringBuilder()
            .append("HTTP/1.1 ").append(status).append("\r\n")
            .append("Content-Type: ").append(contentType).append("\r\n")
            .append("Content-Length: ").append(contentLength).append("\r\n")
            .append("Connection: close\r\n")
            .append("X-Content-Type-Options: nosniff\r\n")
            .append("Referrer-Policy: no-referrer\r\n")
        extraHeaders.forEach { (key, value) -> builder.append(key).append(": ").append(value).append("\r\n") }
        builder.append("\r\n")
        out.write(builder.toString().toByteArray(StandardCharsets.UTF_8))
        out.flush()
    }
}
