package com.nitrodropnative.transfer

import android.content.Context
import android.net.Uri
import com.nitrodropnative.core.constants.AppConstants
import com.nitrodropnative.storage.AppDatabase
import com.nitrodropnative.storage.TransferHistoryEntity
import com.nitrodropnative.transport.ConnectionInfo
import com.nitrodropnative.transport.TransportSelector
import com.nitrodropnative.transport.TransportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import java.net.Socket

class TransferEngine(private val context: Context) {
    private val selector = TransportSelector(context)
    private val historyDao = AppDatabase.get(context).transferHistoryDao()

    fun sendFile(
        uri: Uri,
        connectionInfo: ConnectionInfo,
        checksumEnabled: Boolean = true,
        chunkSize: Int = AppConstants.DEFAULT_CHUNK_SIZE
    ): Flow<TransferStats> {
        val transport = selector.transportFor(connectionInfo.transportType)
        var lastStats: TransferStats? = null
        return kotlinx.coroutines.flow.flow {
            val socket = transport.connect(connectionInfo)
            Sender(context).sendFile(uri, socket, connectionInfo.peerName, connectionInfo.transportType, checksumEnabled, chunkSize)
                .collect { emit(it) }
        }.onEach { stats ->
            lastStats = stats
            if (stats.state == TransferState.Metadata) saveStart(stats, "SEND", connectionInfo.peerName)
            if (stats.state in terminalStates) saveTerminal(stats, "SEND", connectionInfo.peerName, null)
        }.onCompletion { cause ->
            if (cause != null) {
                lastStats?.copy(state = TransferState.Failed, message = cause.message ?: "Transfer failed")
                    ?.let { saveTerminal(it, "SEND", connectionInfo.peerName, null) }
            }
        }
    }

    fun receiveOnce(
        serverSocket: ServerSocket,
        transportType: TransportType = TransportType.LAN,
        checksumEnabled: Boolean = true,
        acceptIncoming: suspend (TransferMetadata) -> Boolean = { true }
    ): Flow<TransferStats> {
        var lastStats: TransferStats? = null
        return Receiver(context).receiveOnce(serverSocket, transportType, checksumEnabled, acceptIncoming)
            .onEach { stats ->
                lastStats = stats
                if (stats.state == TransferState.Metadata) saveStart(stats, "RECEIVE", "")
                if (stats.state in terminalStates) saveTerminal(stats, "RECEIVE", "", stats.message)
            }.onCompletion { cause ->
                if (cause != null) {
                    lastStats?.copy(state = TransferState.Failed, message = cause.message ?: "Transfer failed")
                        ?.let { saveTerminal(it, "RECEIVE", "", null) }
                }
            }
    }

    suspend fun openServer(port: Int = AppConstants.DEFAULT_PORT): ServerSocket = withContext(Dispatchers.IO) {
        selector.transportFor(TransportType.LAN).openServer(port)
    }

    private suspend fun saveStart(stats: TransferStats, direction: String, peer: String) {
        historyDao.upsert(
            TransferHistoryEntity(
                id = stats.sessionId,
                fileName = stats.fileName,
                fileSize = stats.totalBytes,
                direction = direction,
                receiverName = if (direction == "SEND") peer else "",
                senderName = if (direction == "RECEIVE") peer else "",
                transportType = stats.transportType.name,
                averageSpeed = 0L,
                peakSpeed = 0L,
                duration = 0L,
                status = stats.state.name,
                timestamp = System.currentTimeMillis(),
                savedPath = null
            )
        )
    }

    private suspend fun saveTerminal(stats: TransferStats, direction: String, peer: String, savedPath: String?) {
        val duration = (stats.updatedAtMs - stats.startedAtMs).coerceAtLeast(0L)
        historyDao.upsert(
            TransferHistoryEntity(
                id = stats.sessionId,
                fileName = stats.fileName,
                fileSize = stats.totalBytes,
                direction = direction,
                receiverName = if (direction == "SEND") peer else "",
                senderName = if (direction == "RECEIVE") peer else "",
                transportType = stats.transportType.name,
                averageSpeed = stats.averageSpeed,
                peakSpeed = stats.peakSpeed,
                duration = duration,
                status = stats.state.name,
                timestamp = System.currentTimeMillis(),
                savedPath = savedPath
            )
        )
    }

    companion object {
        private val terminalStates = setOf(TransferState.Completed, TransferState.Cancelled, TransferState.Failed)
    }
}
