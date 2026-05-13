package com.nitrodropnative.webtransfer

import android.content.Context
import com.nitrodropnative.core.constants.AppConstants
import com.nitrodropnative.service.WifiPerformanceLock
import com.nitrodropnative.storage.SelectedFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebTransferManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val wifiLock = WifiPerformanceLock(appContext)
    private val registry = WebSharedFileRegistry()

    private var server: LocalHttpServer? = null

    private val _stats = MutableStateFlow(WebTransferStats())
    val stats: StateFlow<WebTransferStats> = _stats.asStateFlow()

    private val _sharedFiles = MutableStateFlow<List<SelectedFile>>(emptyList())
    val sharedFiles: StateFlow<List<SelectedFile>> = _sharedFiles.asStateFlow()

    fun replaceSharedFiles(files: List<SelectedFile>) {
        val updated = registry.replace(files)
        _sharedFiles.value = updated.map { it.toSelectedFile() }
        publishShareListChanged(updated.size)
    }

    fun addSharedFiles(files: List<SelectedFile>) {
        val updated = registry.add(files)
        _sharedFiles.value = updated.map { it.toSelectedFile() }
        publishShareListChanged(updated.size)
    }

    fun removeSharedFile(id: String) {
        val updated = registry.remove(id)
        _sharedFiles.value = updated.map { it.toSelectedFile() }
        publishShareListChanged(updated.size)
    }

    fun clearSharedFiles() {
        registry.clear()
        _sharedFiles.value = emptyList()
        publishShareListChanged(0)
    }

    fun start() {
        stop()
        scope.launch {
            runCatching {
                val token = WebTransferSecurity.newToken()
                val password = WebTransferSecurity.newPassword()
                val ip = WebTransferNetwork.bestLocalIp(appContext)
                val httpServer = LocalHttpServer(
                    context = appContext,
                    registry = registry,
                    sessionToken = token,
                    password = password,
                    preferredPort = AppConstants.WEB_TRANSFER_PORT
                ) { next -> _stats.value = preserveSession(next) }
                server = httpServer
                wifiLock.acquire()
                val actualPort = httpServer.start(ip)
                val url = "http://$ip:$actualPort/"
                _stats.value = WebTransferStats(
                    state = WebTransferState.Running,
                    isRunning = true,
                    serverUrl = url,
                    token = token,
                    password = password,
                    deviceIp = ip,
                    port = actualPort,
                    message = if (WebTransferNetwork.isOnWifi(appContext)) {
                        "Open this address on your PC and enter the 3-digit password"
                    } else {
                        "Not connected to Wi‑Fi. Connect both devices to the same Wi‑Fi network."
                    }
                )
            }.onFailure { error ->
                wifiLock.release()
                _stats.value = WebTransferStats(
                    state = WebTransferState.Failed,
                    isRunning = false,
                    message = error.message ?: "Unable to start Web Transfer"
                )
            }
        }
    }

    fun stop() {
        server?.stop()
        server = null
        wifiLock.release()
        val old = _stats.value
        if (old.isRunning) {
            _stats.value = old.copy(
                state = WebTransferState.Stopped,
                isRunning = false,
                token = "",
                password = "",
                message = "Web Transfer stopped"
            )
        }
    }

    fun removeReceivedFile(uri: String) {
        val old = _stats.value
        val updated = old.recentReceivedFiles.filterNot { it.uri == uri }
        _stats.value = old.copy(
            recentReceivedFiles = updated,
            lastReceivedFile = updated.firstOrNull()
        )
    }

    private fun publishShareListChanged(count: Int) {
        val old = _stats.value
        if (old.isRunning) {
            _stats.value = old.copy(
                state = WebTransferState.Running,
                message = "Shared file list updated: $count file(s) available"
            )
        }
    }

    private fun preserveSession(next: WebTransferStats): WebTransferStats {
        val previous = _stats.value
        return next.copy(
            serverUrl = next.serverUrl.ifBlank { previous.serverUrl },
            token = next.token.ifBlank { previous.token },
            password = next.password.ifBlank { previous.password },
            deviceIp = next.deviceIp.ifBlank { previous.deviceIp },
            port = if (next.port > 0) next.port else previous.port,
            lastReceivedFile = next.lastReceivedFile ?: previous.lastReceivedFile,
            recentReceivedFiles = if (next.recentReceivedFiles.isNotEmpty()) next.recentReceivedFiles else previous.recentReceivedFiles
        )
    }
}
