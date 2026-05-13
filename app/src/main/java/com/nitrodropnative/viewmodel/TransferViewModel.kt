package com.nitrodropnative.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nitrodropnative.core.constants.AppConstants
import com.nitrodropnative.service.TransferForegroundService
import com.nitrodropnative.service.TransferNotificationManager
import com.nitrodropnative.transfer.TransferEngine
import com.nitrodropnative.transfer.TransferMetadata
import com.nitrodropnative.transfer.TransferState
import com.nitrodropnative.transfer.TransferStats
import com.nitrodropnative.transport.ConnectionInfo
import com.nitrodropnative.transport.TransportType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class TransferViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = TransferEngine(application)
    private val notificationManager = TransferNotificationManager(application)
    private val _stats = MutableStateFlow(TransferStats())
    val stats: StateFlow<TransferStats> = _stats.asStateFlow()

    private val _incomingRequest = MutableStateFlow<TransferMetadata?>(null)
    val incomingRequest: StateFlow<TransferMetadata?> = _incomingRequest.asStateFlow()

    private var incomingDecision: CompletableDeferred<Boolean>? = null
    private var job: Job? = null

    fun startSend(uri: Uri, host: String, peerName: String = host) {
        cancelActiveOnly()
        job = viewModelScope.launch {
            runCatching {
                TransferForegroundService.start(getApplication(), "Sending file")
                val connection = ConnectionInfo(host = host, port = AppConstants.DEFAULT_PORT, transportType = TransportType.LAN, peerName = peerName)
                engine.sendFile(uri, connection).collect { stats ->
                    _stats.value = stats
                    if (stats.state == TransferState.Transferring) notificationManager.notify(stats)
                    if (stats.state in terminalStates) TransferForegroundService.stop(getApplication())
                }
            }.onFailure { error ->
                _stats.value = _stats.value.copy(state = TransferState.Failed, message = error.message ?: "Send failed")
                TransferForegroundService.stop(getApplication())
            }
        }
    }

    fun startReceive() {
        cancelActiveOnly()
        job = viewModelScope.launch {
            runCatching {
                TransferForegroundService.start(getApplication(), "Waiting for receiver")
                _stats.value = TransferStats(state = TransferState.Waiting, message = "Listening on port ${AppConstants.DEFAULT_PORT}")
                val server = engine.openServer(AppConstants.DEFAULT_PORT)
                engine.receiveOnce(
                    serverSocket = server,
                    transportType = TransportType.LAN,
                    acceptIncoming = { metadata ->
                        val decision = CompletableDeferred<Boolean>()
                        incomingDecision = decision
                        _incomingRequest.value = metadata
                        val accepted = decision.await()
                        _incomingRequest.value = null
                        incomingDecision = null
                        accepted
                    }
                ).collect { stats ->
                    _stats.value = stats
                    if (stats.state == TransferState.Transferring) notificationManager.notify(stats)
                    if (stats.state in terminalStates) TransferForegroundService.stop(getApplication())
                }
            }.onFailure { error ->
                _stats.value = _stats.value.copy(state = TransferState.Failed, message = error.message ?: "Receive failed")
                TransferForegroundService.stop(getApplication())
            }
        }
    }

    fun acceptIncoming() {
        incomingDecision?.complete(true)
    }

    fun rejectIncoming() {
        incomingDecision?.complete(false)
    }

    fun pause() {
        _stats.value = _stats.value.copy(state = TransferState.Paused, message = "Paused locally. Resume support keeps partial files on receiver.")
        job?.cancel()
        TransferForegroundService.stop(getApplication())
    }

    fun cancel() {
        _stats.value = _stats.value.copy(state = TransferState.Cancelled, message = "Cancelled")
        cancelActiveOnly()
        TransferForegroundService.stop(getApplication())
    }

    private fun cancelActiveOnly() {
        incomingDecision?.cancel()
        incomingDecision = null
        _incomingRequest.value = null
        job?.cancel()
        job = null
    }

    private val terminalStates = setOf(TransferState.Completed, TransferState.Cancelled, TransferState.Failed)
}
