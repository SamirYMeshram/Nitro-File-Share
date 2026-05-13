package com.nitrodropnative.webtransfer

import android.content.Context
import android.util.Log
import com.nitrodropnative.core.constants.AppConstants
import com.nitrodropnative.storage.FileReader
import com.nitrodropnative.storage.MediaStoreSaver
import com.nitrodropnative.transfer.SpeedTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.BindException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min

internal class LocalHttpServer(
    private val context: Context,
    private val registry: WebSharedFileRegistry,
    private val sessionToken: String,
    private val password: String,
    private val preferredPort: Int,
    private val onStats: (WebTransferStats) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var downloadsCompleted = 0
    private var uploadsCompleted = 0
    private var advertisedDeviceIp: String = ""
    private var advertisedUrl: String = ""
    private val uploadSessions = ConcurrentHashMap<String, WebUploadSession>()
    private val receivedFiles = ArrayDeque<WebReceivedFile>()

    suspend fun start(deviceIp: String): Int = withContext(Dispatchers.IO) {
        val server = bindServer(preferredPort)
        serverSocket = server
        advertisedDeviceIp = deviceIp
        advertisedUrl = "http://$deviceIp:${server.localPort}/"
        acceptJob = scope.launch { acceptLoop(server, deviceIp) }
        server.localPort
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        acceptJob?.cancel()
        serverSocket = null
    }

    private fun bindServer(port: Int): ServerSocket {
        fun create(bindPort: Int): ServerSocket = ServerSocket().apply {
            reuseAddress = true
            receiveBufferSize = AppConstants.WEB_SOCKET_BUFFER_SIZE
            bind(InetSocketAddress("0.0.0.0", bindPort), 96)
        }
        return try {
            create(port)
        } catch (busy: BindException) {
            Log.w(TAG, "Preferred web-transfer port $port is busy; falling back to an available port", busy)
            create(0)
        }
    }

    private suspend fun acceptLoop(server: ServerSocket, deviceIp: String) {
        while (!server.isClosed) {
            coroutineContext.ensureActive()
            val socket = try {
                server.accept()
            } catch (_: SocketException) {
                break
            } catch (t: Throwable) {
                Log.e(TAG, "Accept loop failed", t)
                continue
            }
            scope.launch { handle(socket, deviceIp) }
        }
    }

    private suspend fun handle(socket: Socket, deviceIp: String) = withContext(Dispatchers.IO) {
        socket.use { client ->
            runCatching {
                client.tcpNoDelay = true
                client.receiveBufferSize = AppConstants.WEB_SOCKET_BUFFER_SIZE
                client.sendBufferSize = AppConstants.WEB_SOCKET_BUFFER_SIZE
            }
            val remote = client.inetAddress?.hostAddress.orEmpty()
            val input = BufferedInputStream(client.getInputStream(), 128 * 1024)
            val output = client.getOutputStream()
            val request = runCatching { HttpRequestParser.parse(input, remote) }
                .onFailure { Log.w(TAG, "Bad HTTP request from $remote", it) }
                .getOrNull()
            if (request == null) {
                safeText(output, "400 Bad Request", "Bad request")
                return@withContext
            }

            try {
                when {
                    request.method == "GET" && (request.path == "/" || request.path == "/p") -> servePage(request, output, deviceIp)
                    request.method == "POST" && request.path == "/auth" -> handleAuth(request, output)
                    request.method == "GET" && request.path == "/api/files" -> serveFiles(request, output)
                    request.method == "GET" && request.path == "/api/status" -> serveStatus(request, output)
                    request.method == "GET" && request.path.startsWith("/d/") -> serveDownload(request, output)
                    request.method == "GET" && request.path.startsWith("/api/download/") -> serveLegacyDownload(request, output)
                    request.method == "PUT" && (request.path == "/u" || request.path == "/api/upload") -> handleUpload(request, output)
                    request.method == "GET" && request.path == "/api/upload/status" -> serveUploadStatus(request, output)
                    else -> safeText(output, "404 Not Found", "Not found")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Unhandled web-transfer request error: ${request.method} ${request.path} from $remote", t)
                onStats(baseStats(WebTransferState.Running, true, deviceIp, remote).copy(message = "Server error: ${(t.message ?: "request failed").take(140)}"))
                safeJson(output, "500 Internal Server Error", JSONObject().put("error", "Request failed").toString())
            }
        }
    }

    private fun servePage(request: HttpRequest, output: OutputStream, deviceIp: String) {
        if (!authorized(request)) {
            HttpResponseWriter.text(
                out = output,
                status = "200 OK",
                body = WebPageAssets.passwordHtml(),
                contentType = "text/html; charset=utf-8",
                extraHeaders = mapOf("Cache-Control" to "no-store")
            )
            return
        }
        HttpResponseWriter.text(
            out = output,
            status = "200 OK",
            body = WebPageAssets.indexHtml(),
            contentType = "text/html; charset=utf-8",
            extraHeaders = mapOf("Cache-Control" to "no-store")
        )
        onStats(baseStats(WebTransferState.Running, true, deviceIp, request.remoteAddress).copy(message = "PC connected: ${request.remoteAddress}"))
    }

    private fun handleAuth(request: HttpRequest, output: OutputStream) {
        val length = request.header("content-length")?.toIntOrNull()?.coerceIn(0, 4096) ?: 0
        val body = readBodyText(request.input, length)
        val form = parseForm(body)
        val supplied = form["password"].orEmpty()
        if (WebTransferSecurity.constantTimeEquals(supplied, password)) {
            HttpResponseWriter.redirect(
                out = output,
                location = "/",
                extraHeaders = mapOf(
                    "Set-Cookie" to "nd_session=$sessionToken; Path=/; Max-Age=86400; HttpOnly; SameSite=Strict"
                )
            )
        } else {
            HttpResponseWriter.text(
                out = output,
                status = "401 Unauthorized",
                body = WebPageAssets.passwordHtml("Wrong password. Check the 3-digit code on your phone."),
                contentType = "text/html; charset=utf-8",
                extraHeaders = mapOf("Cache-Control" to "no-store")
            )
        }
    }

    private fun serveFiles(request: HttpRequest, output: OutputStream) {
        if (!authorized(request)) {
            safeJson(output, "401 Unauthorized", JSONObject().put("error", "Password required").toString())
            return
        }
        val items = JSONArray().apply {
            registry.snapshot().forEach { file ->
                put(
                    JSONObject()
                        .put("id", file.id)
                        .put("name", file.name)
                        .put("size", file.size)
                        .put("mimeType", file.mimeType)
                )
            }
        }
        HttpResponseWriter.json(
            output,
            "200 OK",
            JSONObject()
                .put("files", items)
                .put("downloadsCompleted", downloadsCompleted)
                .put("uploadsCompleted", uploadsCompleted)
                .put("updatedAt", System.currentTimeMillis())
                .toString()
        )
    }

    private fun serveStatus(request: HttpRequest, output: OutputStream) {
        if (!authorized(request)) {
            safeJson(output, "401 Unauthorized", JSONObject().put("error", "Password required").toString())
            return
        }
        HttpResponseWriter.json(
            output,
            "200 OK",
            JSONObject()
                .put("running", true)
                .put("files", registry.snapshot().size)
                .put("downloadsCompleted", downloadsCompleted)
                .put("uploadsCompleted", uploadsCompleted)
                .toString()
        )
    }

    private suspend fun serveLegacyDownload(request: HttpRequest, output: OutputStream) {
        if (!authorized(request)) {
            safeText(output, "401 Unauthorized", "Password required")
            return
        }
        val index = request.path.substringAfterLast('/').toIntOrNull()
        val selected = index?.let { registry.snapshot().getOrNull(it) }
        if (selected == null) {
            safeText(output, "404 Not Found", "File not found")
            return
        }
        streamFile(request, output, selected)
    }

    private suspend fun serveDownload(request: HttpRequest, output: OutputStream) {
        if (!authorized(request)) {
            safeText(output, "401 Unauthorized", "Password required")
            return
        }
        val id = request.path.removePrefix("/d/").substringBefore('/').trim()
        val selected = registry.get(id)
        if (selected == null) {
            safeText(output, "404 Not Found", "File not found")
            return
        }
        streamFile(request, output, selected)
    }

    private suspend fun streamFile(request: HttpRequest, output: OutputStream, selected: WebSharedFile) {
        val fileSize = selected.size.coerceAtLeast(0L)
        val range = parseRange(request.header("range"), fileSize)
        if (request.header("range") != null && range == null && fileSize > 0L) {
            HttpResponseWriter.headers(
                out = output,
                status = "416 Range Not Satisfiable",
                contentType = "text/plain; charset=utf-8",
                contentLength = 0L,
                extraHeaders = mapOf("Content-Range" to "bytes */$fileSize", "Cache-Control" to "no-store")
            )
            return
        }

        val start = range?.first ?: 0L
        val endInclusive = range?.second ?: (fileSize - 1L).coerceAtLeast(0L)
        val bytesToSend = if (fileSize <= 0L) 0L else (endInclusive - start + 1L).coerceAtLeast(0L)
        val status = if (range != null) "206 Partial Content" else "200 OK"
        val headers = linkedMapOf(
            "Content-Disposition" to contentDisposition(selected.name),
            "Accept-Ranges" to "bytes",
            "Cache-Control" to "no-store"
        )
        if (range != null) headers["Content-Range"] = "bytes $start-$endInclusive/$fileSize"

        try {
            HttpResponseWriter.headers(output, status, selected.mimeType.ifBlank { "application/octet-stream" }, bytesToSend, headers)
        } catch (io: IOException) {
            Log.w(TAG, "Client disconnected before download headers: ${selected.name}", io)
            return
        }

        val tracker = SpeedTracker()
        var sent = 0L
        var lastEmit = 0L
        try {
            FileReader.open(context, selected.uri).use { input ->
                skipFully(input, start)
                val buffer = ByteArray(AppConstants.DEFAULT_CHUNK_SIZE)
                while (sent < bytesToSend) {
                    coroutineContext.ensureActive()
                    val target = min(buffer.size.toLong(), bytesToSend - sent).toInt()
                    val read = input.read(buffer, 0, target)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    sent += read
                    tracker.addBytes(read)
                    val now = System.currentTimeMillis()
                    if (now - lastEmit >= AppConstants.UI_UPDATE_INTERVAL_MS) {
                        lastEmit = now
                        onStats(transferStats(
                            state = WebTransferState.Downloading,
                            fileName = selected.name,
                            transferred = sent,
                            total = bytesToSend,
                            tracker = tracker,
                            client = request.remoteAddress,
                            message = "Downloading to PC"
                        ))
                    }
                }
                output.flush()
            }
            downloadsCompleted += 1
            onStats(transferStats(
                state = WebTransferState.Completed,
                fileName = selected.name,
                transferred = sent,
                total = bytesToSend,
                tracker = tracker,
                client = request.remoteAddress,
                message = "Download completed"
            ).copy(downloadsCompleted = downloadsCompleted, uploadsCompleted = uploadsCompleted))
        } catch (clientGone: SocketException) {
            Log.i(TAG, "Download cancelled by browser/client: ${selected.name} after $sent bytes")
            onStats(baseStats(WebTransferState.Running, true, advertisedDeviceIp, request.remoteAddress).copy(message = "Download cancelled by browser"))
        } catch (clientGone: IOException) {
            Log.i(TAG, "Download I/O ended for ${selected.name} after $sent bytes: ${clientGone.message}")
            onStats(baseStats(WebTransferState.Running, true, advertisedDeviceIp, request.remoteAddress).copy(message = "Download interrupted; server still running"))
        } catch (t: Throwable) {
            Log.e(TAG, "Download failed: ${selected.name}", t)
            onStats(baseStats(WebTransferState.Running, true, advertisedDeviceIp, request.remoteAddress).copy(message = "Download failed: ${(t.message ?: "file error").take(120)}"))
        }
    }

    private suspend fun handleUpload(request: HttpRequest, output: OutputStream) {
        if (!authorized(request)) {
            safeJson(output, "401 Unauthorized", JSONObject().put("error", "Password required").toString())
            return
        }
        val fileName = WebTransferSecurity.safeFileName(request.query["fileName"].orEmpty())
        val sessionId = WebTransferSecurity.safeFileName(request.query["sessionId"].orEmpty()).ifBlank { "web_${System.currentTimeMillis()}" }
        val fileSize = request.query["fileSize"]?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        val offset = request.query["offset"]?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        val contentLength = request.header("content-length")?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        if (fileName.isBlank() || contentLength <= 0L) {
            safeJson(output, "400 Bad Request", JSONObject().put("error", "Missing fileName or body").toString())
            return
        }

        val dir = File(context.getExternalFilesDir(null), "web_uploads").apply { mkdirs() }
        val partial = File(dir, "${sessionId}_$fileName${AppConstants.PARTIAL_EXTENSION}")
        val tracker = SpeedTracker()
        val uploadSession = uploadSessions.computeIfAbsent(sessionId) {
            WebUploadSession(
                id = sessionId,
                fileName = fileName,
                fileSize = fileSize,
                startedAtMillis = System.currentTimeMillis()
            )
        }
        var received = 0L
        var lastEmit = 0L

        try {
            RandomAccessFile(partial, "rw").use { raf ->
                raf.seek(offset)
                val buffer = ByteArray(AppConstants.DEFAULT_CHUNK_SIZE)
                var remaining = contentLength
                while (remaining > 0L) {
                    coroutineContext.ensureActive()
                    val target = min(buffer.size.toLong(), remaining).toInt()
                    val read = request.input.read(buffer, 0, target)
                    if (read < 0) throw IOException("Upload connection closed before chunk completed")
                    raf.write(buffer, 0, read)
                    remaining -= read
                    received += read
                    val absoluteReceived = offset + received
                    tracker.addBytes(read)
                    uploadSession.receivedBytes = max(uploadSession.receivedBytes, absoluteReceived)
                    uploadSession.peakSpeed = max(uploadSession.peakSpeed, tracker.peakSpeed())

                    val now = System.currentTimeMillis()
                    if (now - lastEmit >= AppConstants.UI_UPDATE_INTERVAL_MS) {
                        lastEmit = now
                        onStats(transferStats(
                            state = WebTransferState.Uploading,
                            fileName = fileName,
                            transferred = absoluteReceived,
                            total = max(fileSize, offset + contentLength),
                            tracker = tracker,
                            client = request.remoteAddress,
                            message = "Uploading from PC"
                        ))
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Upload chunk failed: $fileName", t)
            safeJson(output, "500 Internal Server Error", JSONObject().put("error", "Upload failed").toString())
            onStats(baseStats(WebTransferState.Running, true, advertisedDeviceIp, request.remoteAddress).copy(message = "Upload failed; server still running"))
            return
        }

        val totalReceived = offset + received
        uploadSession.receivedBytes = max(uploadSession.receivedBytes, totalReceived)
        uploadSession.peakSpeed = max(uploadSession.peakSpeed, tracker.peakSpeed())

        var completed = false
        var savedUri = ""
        var receivedFile: WebReceivedFile? = null
        val mimeType = guessMime(fileName)
        if (fileSize > 0L && totalReceived >= fileSize) {
            val completedPrivate = File(dir, fileName)
            runCatching { if (completedPrivate.exists()) completedPrivate.delete() }
            if (partial.renameTo(completedPrivate)) {
                savedUri = runCatching { MediaStoreSaver.saveToDownloads(context, completedPrivate, fileName, mimeType).toString() }
                    .onFailure { Log.e(TAG, "Failed saving upload to MediaStore: $fileName", it) }
                    .getOrDefault("")
                completedPrivate.delete()
                if (savedUri.isNotBlank()) {
                    uploadsCompleted += 1
                    completed = true

                    val now = System.currentTimeMillis()
                    val durationMillis = max(1L, now - uploadSession.startedAtMillis)
                    val averageSpeed = if (durationMillis > 0L) (totalReceived * 1000L) / durationMillis else 0L
                    receivedFile = WebReceivedFile(
                        id = uploadSession.id,
                        fileName = fileName,
                        sizeBytes = totalReceived,
                        mimeType = mimeType,
                        uri = savedUri,
                        savedLocation = "Downloads/NitroDrop",
                        receivedFrom = request.remoteAddress,
                        averageSpeed = averageSpeed,
                        peakSpeed = max(uploadSession.peakSpeed, tracker.peakSpeed()),
                        durationSeconds = max(1L, durationMillis / 1000L),
                        timestampMillis = now
                    )
                    uploadSessions.remove(sessionId)
                }
            }
        }
        val state = if (completed) WebTransferState.Completed else WebTransferState.Uploading
        val recent = receivedFile?.let { rememberReceivedFile(it) }.orEmpty()
        onStats(transferStats(
            state = state,
            fileName = fileName,
            transferred = totalReceived,
            total = max(fileSize, totalReceived),
            tracker = tracker,
            client = request.remoteAddress,
            message = if (completed) "Upload saved to Downloads/NitroDrop" else "Chunk received"
        ).copy(
            downloadsCompleted = downloadsCompleted,
            uploadsCompleted = uploadsCompleted,
            lastReceivedFile = receivedFile,
            recentReceivedFiles = recent
        ))

        safeJson(
            output,
            "200 OK",
            JSONObject()
                .put("receivedBytes", totalReceived)
                .put("complete", completed)
                .put("savedUri", savedUri)
                .toString()
        )
    }

    private fun serveUploadStatus(request: HttpRequest, output: OutputStream) {
        if (!authorized(request)) {
            safeJson(output, "401 Unauthorized", JSONObject().put("error", "Password required").toString())
            return
        }
        val sessionId = WebTransferSecurity.safeFileName(request.query["sessionId"].orEmpty())
        val fileName = WebTransferSecurity.safeFileName(request.query["fileName"].orEmpty())
        val file = File(File(context.getExternalFilesDir(null), "web_uploads"), "${sessionId}_$fileName${AppConstants.PARTIAL_EXTENSION}")
        safeJson(output, "200 OK", JSONObject().put("receivedBytes", if (file.exists()) file.length() else 0L).toString())
    }

    private fun authorized(request: HttpRequest): Boolean {
        val cookieToken = request.cookies()[SESSION_COOKIE].orEmpty()
        val legacyQueryToken = request.query["token"].orEmpty()
        return WebTransferSecurity.constantTimeEquals(cookieToken, sessionToken) ||
            legacyQueryToken.isNotBlank() && WebTransferSecurity.constantTimeEquals(legacyQueryToken, sessionToken)
    }

    private fun HttpRequest.cookies(): Map<String, String> {
        val raw = header("cookie").orEmpty()
        if (raw.isBlank()) return emptyMap()
        return raw.split(';')
            .mapNotNull { part ->
                val trimmed = part.trim()
                val index = trimmed.indexOf('=')
                if (index <= 0) null else trimmed.substring(0, index) to trimmed.substring(index + 1)
            }
            .toMap()
    }

    private fun baseStats(state: WebTransferState, running: Boolean, deviceIp: String, client: String): WebTransferStats {
        return WebTransferStats(
            state = state,
            isRunning = running,
            serverUrl = advertisedUrl.ifBlank { "http://$deviceIp:$preferredPort/" },
            token = sessionToken,
            password = password,
            deviceIp = deviceIp,
            port = serverSocket?.localPort ?: preferredPort,
            connectedClient = client,
            downloadsCompleted = downloadsCompleted,
            uploadsCompleted = uploadsCompleted
        )
    }

    private fun transferStats(
        state: WebTransferState,
        fileName: String,
        transferred: Long,
        total: Long,
        tracker: SpeedTracker,
        client: String,
        message: String
    ): WebTransferStats {
        val progress = if (total <= 0L) 0f else (transferred.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        return WebTransferStats(
            state = state,
            isRunning = true,
            serverUrl = advertisedUrl,
            token = sessionToken,
            password = password,
            deviceIp = advertisedDeviceIp,
            port = serverSocket?.localPort ?: preferredPort,
            activeFileName = fileName,
            transferredBytes = transferred,
            totalBytes = total,
            currentSpeed = tracker.currentSpeed(),
            averageSpeed = tracker.averageSpeed(),
            peakSpeed = tracker.peakSpeed(),
            etaSeconds = tracker.eta(total, transferred),
            progressPercent = progress,
            stabilityPercent = tracker.stabilityPercent(),
            connectedClient = client,
            downloadsCompleted = downloadsCompleted,
            uploadsCompleted = uploadsCompleted,
            message = message
        )
    }

    private fun rememberReceivedFile(file: WebReceivedFile): List<WebReceivedFile> = synchronized(receivedFiles) {
        val iterator = receivedFiles.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().uri == file.uri) iterator.remove()
        }
        receivedFiles.addFirst(file)
        while (receivedFiles.size > 12) receivedFiles.removeLast()
        receivedFiles.toList()
    }

    private fun parseRange(value: String?, fileSize: Long): Pair<Long, Long>? {
        if (value.isNullOrBlank() || !value.startsWith("bytes=")) return null
        if (fileSize <= 0L) return null
        val spec = value.removePrefix("bytes=").substringBefore(',').trim()
        if (spec.startsWith("-")) {
            val suffixLength = spec.substringAfter('-').toLongOrNull() ?: return null
            if (suffixLength <= 0L) return null
            val start = (fileSize - suffixLength).coerceAtLeast(0L)
            return start to (fileSize - 1L)
        }
        val startText = spec.substringBefore('-')
        val endText = spec.substringAfter('-', "")
        val start = startText.toLongOrNull() ?: return null
        if (start !in 0L until fileSize) return null
        val end = endText.toLongOrNull()?.coerceIn(start, fileSize - 1L) ?: (fileSize - 1L)
        return start to end
    }

    private fun skipFully(input: InputStream, offset: Long) {
        var remaining = offset
        val fallback = ByteArray(64 * 1024)
        while (remaining > 0L) {
            val skipped = input.skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
            } else {
                val read = input.read(fallback, 0, min(fallback.size.toLong(), remaining).toInt())
                if (read < 0) break
                remaining -= read
            }
        }
    }

    private fun contentDisposition(name: String): String {
        val safeAscii = name.replace(Regex("[\\r\\n\"]"), "_").ifBlank { "nitrodrop_file" }
        val encoded = URLEncoder.encode(name, StandardCharsets.UTF_8.name()).replace("+", "%20")
        return "attachment; filename=\"$safeAscii\"; filename*=UTF-8''$encoded"
    }

    private fun readBodyText(input: BufferedInputStream, length: Int): String {
        if (length <= 0) return ""
        val bytes = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(bytes, offset, length - offset)
            if (read < 0) break
            offset += read
        }
        return bytes.copyOf(offset).toString(StandardCharsets.UTF_8)
    }

    private fun parseForm(body: String): Map<String, String> {
        if (body.isBlank()) return emptyMap()
        return body.split('&')
            .filter { it.isNotBlank() }
            .associate { part ->
                val key = HttpRequestParser.decode(part.substringBefore('='))
                val value = HttpRequestParser.decode(part.substringAfter('=', ""))
                key to value
            }
    }

    private fun safeText(output: OutputStream, status: String, body: String) {
        runCatching { HttpResponseWriter.text(output, status, body) }
            .onFailure { Log.w(TAG, "Unable to write HTTP text response: $status", it) }
    }

    private fun safeJson(output: OutputStream, status: String, body: String) {
        runCatching { HttpResponseWriter.json(output, status, body) }
            .onFailure { Log.w(TAG, "Unable to write HTTP JSON response: $status", it) }
    }

    private fun guessMime(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }

    private companion object {
        private const val TAG = "NitroDropWebServer"
        private const val SESSION_COOKIE = "nd_session"
    }
}

private data class WebUploadSession(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val startedAtMillis: Long,
    @Volatile var receivedBytes: Long = 0L,
    @Volatile var peakSpeed: Long = 0L
)
