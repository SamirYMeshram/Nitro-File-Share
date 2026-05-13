package com.nitrodropnative.transfer

import android.content.Context
import com.nitrodropnative.core.constants.AppConstants
import com.nitrodropnative.storage.FileWriter
import com.nitrodropnative.storage.MediaStoreSaver
import com.nitrodropnative.transport.TransportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import kotlin.coroutines.coroutineContext

class Receiver(private val context: Context) {
    fun receiveOnce(
        serverSocket: ServerSocket,
        transportType: TransportType,
        checksumEnabled: Boolean = true,
        acceptIncoming: suspend (TransferMetadata) -> Boolean = { true }
    ): Flow<TransferStats> = channelFlow {
        val socket = withContext(Dispatchers.IO) { serverSocket.accept() }
        receiveFromSocket(socket, transportType, checksumEnabled, acceptIncoming).collect { send(it) }
    }

    fun receiveFromSocket(
        socket: Socket,
        transportType: TransportType,
        checksumEnabled: Boolean = true,
        acceptIncoming: suspend (TransferMetadata) -> Boolean = { true }
    ): Flow<TransferStats> = channelFlow {
        withContext(Dispatchers.IO) {
            val tracker = SpeedTracker()
            val resume = ResumeManager(context)
            var transferred = 0L
            var lastEmit = 0L
            val started = System.currentTimeMillis()

            socket.use { tuned ->
                tuned.tcpNoDelay = true
                tuned.receiveBufferSize = AppConstants.SOCKET_BUFFER_SIZE
                tuned.sendBufferSize = AppConstants.SOCKET_BUFFER_SIZE

                val dataIn = DataInputStream(BufferedInputStream(tuned.getInputStream(), AppConstants.SOCKET_BUFFER_SIZE))
                val dataOut = DataOutputStream(tuned.getOutputStream())
                val meta = TransferProtocol.readMetadata(dataIn)

                send(stats(meta, TransferState.Metadata, 0L, tracker, started, transportType, "Incoming ${meta.fileName}"))
                if (!acceptIncoming(meta)) {
                    TransferProtocol.writeResumeOffset(dataOut, -1L)
                    send(stats(meta, TransferState.Cancelled, 0L, tracker, started, transportType, "Rejected"))
                    return@withContext
                }

                val partial = resume.partialFile(meta)
                transferred = resume.receivedBytes(meta)
                TransferProtocol.writeResumeOffset(dataOut, transferred)

                val digest = if (checksumEnabled && meta.checksumType == "SHA-256") ChecksumVerifier.newSha256() else null
                if (digest != null && transferred > 0 && partial.exists()) updateDigestWithExistingBytes(partial, transferred, digest)

                val buffer = ByteArray(meta.chunkSize.coerceIn(AppConstants.SMALL_CHUNK_SIZE, AppConstants.LARGE_CHUNK_SIZE))
                FileWriter.randomAccess(partial, transferred).use { raf ->
                    send(stats(meta, TransferState.Transferring, transferred, tracker, started, transportType, "Receiving"))
                    while (transferred < meta.fileSize) {
                        coroutineContext.ensureActive()
                        val remaining = (meta.fileSize - transferred).toIntOrMax().coerceAtMost(buffer.size)
                        val read = dataIn.read(buffer, 0, remaining)
                        if (read < 0) error("Connection closed before transfer completed")
                        raf.write(buffer, 0, read)
                        digest?.update(buffer, 0, read)
                        transferred += read
                        tracker.addBytes(read)
                        val now = System.currentTimeMillis()
                        if (now - lastEmit >= AppConstants.UI_UPDATE_INTERVAL_MS) {
                            lastEmit = now
                            send(stats(meta, TransferState.Transferring, transferred, tracker, started, transportType, "Receiving"))
                        }
                    }
                }

                send(stats(meta, TransferState.Verifying, transferred, tracker, started, transportType, "Verifying"))
                val senderChecksum = TransferProtocol.readChecksum(dataIn)
                val receiverChecksum = digest?.let { ChecksumVerifier.digestHex(it) } ?: ""
                if (checksumEnabled && senderChecksum.isNotBlank() && senderChecksum != receiverChecksum) {
                    send(stats(meta, TransferState.Failed, transferred, tracker, started, transportType, "Checksum mismatch"))
                    return@withContext
                }

                val finalPrivate = resume.finalPrivateFile(meta)
                if (finalPrivate.exists()) finalPrivate.delete()
                partial.renameTo(finalPrivate)
                MediaStoreSaver.saveToDownloads(context, finalPrivate, meta.fileName, meta.mimeType).toString()
                send(stats(meta, TransferState.Completed, transferred, tracker, started, transportType, "Saved to Downloads/NitroDrop"))
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
        val progress = if (metadata.fileSize <= 0) 0f else (transferred.toFloat() / metadata.fileSize.toFloat()).coerceIn(0f, 1f)
        return TransferStats(
            sessionId = metadata.sessionId,
            fileName = metadata.fileName,
            state = state,
            transferredBytes = transferred,
            totalBytes = metadata.fileSize,
            currentSpeed = tracker.currentSpeed(),
            averageSpeed = tracker.averageSpeed(),
            peakSpeed = tracker.peakSpeed(),
            etaSeconds = tracker.eta(metadata.fileSize, transferred),
            stabilityPercent = tracker.stabilityPercent(),
            progressPercent = progress,
            transportType = transportType,
            message = message,
            startedAtMs = started,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    private fun updateDigestWithExistingBytes(file: File, limit: Long, digest: java.security.MessageDigest) {
        file.inputStream().use { input ->
            val buffer = ByteArray(AppConstants.SMALL_CHUNK_SIZE)
            var remaining = limit
            while (remaining > 0) {
                val read = input.read(buffer, 0, remaining.toIntOrMax().coerceAtMost(buffer.size))
                if (read < 0) break
                digest.update(buffer, 0, read)
                remaining -= read
            }
        }
    }

    private fun Long.toIntOrMax(): Int = if (this > Int.MAX_VALUE) Int.MAX_VALUE else this.toInt()
}
