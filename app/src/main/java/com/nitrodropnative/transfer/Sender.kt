package com.nitrodropnative.transfer

import android.content.Context
import android.net.Uri
import com.nitrodropnative.core.constants.AppConstants
import com.nitrodropnative.storage.FileReader
import com.nitrodropnative.transport.TransportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.math.max

class Sender(private val context: Context) {
    fun sendFile(
        uri: Uri,
        socket: Socket,
        peerName: String,
        transportType: TransportType,
        checksumEnabled: Boolean = true,
        preferredChunkSize: Int = AppConstants.DEFAULT_CHUNK_SIZE
    ): Flow<TransferStats> = channelFlow {
        withContext(Dispatchers.IO) {
            val info = FileReader.info(context, uri)
            val sessionId = UUID.randomUUID().toString()
            val metadata = TransferMetadata(
                sessionId = sessionId,
                fileName = info.name,
                fileSize = info.size,
                mimeType = info.mimeType,
                lastModified = System.currentTimeMillis(),
                checksumType = if (checksumEnabled) "SHA-256" else "NONE",
                chunkSize = preferredChunkSize
            )
            val started = System.currentTimeMillis()
            val tracker = SpeedTracker()
            val stability = StabilityController()
            val bufferPool = BufferPool(preferredChunkSize)
            var transferred = 0L
            var lastEmit = 0L

            socket.use { tuned ->
                tuned.tcpNoDelay = true
                tuned.sendBufferSize = AppConstants.SOCKET_BUFFER_SIZE
                tuned.receiveBufferSize = AppConstants.SOCKET_BUFFER_SIZE
                val inputControl = DataInputStream(tuned.getInputStream())
                val rawOut = tuned.getOutputStream()
                val dataOut = DataOutputStream(BufferedOutputStream(rawOut, AppConstants.SOCKET_BUFFER_SIZE))

                send(stats(metadata, TransferState.Metadata, transferred, tracker, started, transportType, "Sending metadata"))
                TransferProtocol.writeMetadata(dataOut, metadata)
                val rawResumeOffset = TransferProtocol.readResumeOffset(inputControl)
                if (rawResumeOffset < 0L) error("Receiver rejected the transfer")
                val resumeOffset = rawResumeOffset.coerceIn(0L, metadata.fileSize)

                FileReader.open(context, uri).use { fileInput ->
                    val digest = if (checksumEnabled) ChecksumVerifier.newSha256() else null
                    transferred = consumeUntilOffset(fileInput, resumeOffset, digest)
                    val buffer = bufferPool.acquire()
                    try {
                        send(stats(metadata, TransferState.Transferring, transferred, tracker, started, transportType, "Sending to $peerName"))
                        while (transferred < metadata.fileSize) {
                            coroutineContext.ensureActive()
                            val read = fileInput.read(buffer, 0, buffer.size.coerceAtMost((metadata.fileSize - transferred).toIntOrMax()))
                            if (read < 0) break
                            dataOut.write(buffer, 0, read)
                            digest?.update(buffer, 0, read)
                            transferred += read
                            tracker.addBytes(read)
                            val now = System.currentTimeMillis()
                            val currentSpeed = tracker.currentSpeed()
                            val interval = stability.uiIntervalFor(currentSpeed, tracker.stabilityPercent())
                            if (now - lastEmit >= interval) {
                                lastEmit = now
                                send(stats(metadata, TransferState.Transferring, transferred, tracker, started, transportType, "Streaming"))
                            }
                        }
                    } finally {
                        bufferPool.release(buffer)
                    }
                    dataOut.flush()
                    val checksum = digest?.let { ChecksumVerifier.digestHex(it) } ?: ""
                    send(stats(metadata, TransferState.Verifying, transferred, tracker, started, transportType, "Finalizing checksum"))
                    TransferProtocol.writeChecksum(dataOut, checksum)
                    send(stats(metadata, TransferState.Completed, transferred, tracker, started, transportType, "Completed"))
                }
            }
        }
    }

    private fun stats(
        metadata: TransferMetadata,
        state: TransferState,
        transferred: Long,
        tracker: SpeedTracker,
        started: Long,
        transportType: TransportType,
        message: String
    ): TransferStats {
        val current = tracker.currentSpeed()
        val average = tracker.averageSpeed()
        val peak = tracker.peakSpeed()
        val progress = if (metadata.fileSize <= 0L) 0f else (transferred.toFloat() / metadata.fileSize.toFloat()).coerceIn(0f, 1f)
        return TransferStats(
            sessionId = metadata.sessionId,
            fileName = metadata.fileName,
            state = state,
            transferredBytes = transferred,
            totalBytes = metadata.fileSize,
            currentSpeed = current,
            averageSpeed = average,
            peakSpeed = peak,
            etaSeconds = tracker.eta(metadata.fileSize, transferred),
            stabilityPercent = tracker.stabilityPercent(),
            progressPercent = progress,
            transportType = transportType,
            message = message,
            startedAtMs = started,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    private fun consumeUntilOffset(input: java.io.InputStream, offset: Long, digest: MessageDigest?): Long {
        if (offset <= 0) return 0L
        val discard = ByteArray(AppConstants.SMALL_CHUNK_SIZE)
        var consumed = 0L
        while (consumed < offset) {
            val target = max(1L, offset - consumed).toIntOrMax().coerceAtMost(discard.size)
            val read = input.read(discard, 0, target)
            if (read < 0) break
            digest?.update(discard, 0, read)
            consumed += read
        }
        return consumed
    }

    private fun Long.toIntOrMax(): Int = if (this > Int.MAX_VALUE) Int.MAX_VALUE else this.toInt()
}
