package com.nitrodropnative.webtransfer

import com.nitrodropnative.core.constants.AppConstants

enum class WebTransferState {
    Idle,
    Running,
    Downloading,
    Uploading,
    Completed,
    Failed,
    Stopped
}

data class WebTransferStats(
    val state: WebTransferState = WebTransferState.Idle,
    val isRunning: Boolean = false,
    val serverUrl: String = "",
    val token: String = "",
    val password: String = "",
    val deviceIp: String = "",
    val port: Int = AppConstants.WEB_TRANSFER_PORT,
    val activeFileName: String = "",
    val transferredBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val currentSpeed: Long = 0L,
    val averageSpeed: Long = 0L,
    val peakSpeed: Long = 0L,
    val etaSeconds: Long = -1L,
    val progressPercent: Float = 0f,
    val stabilityPercent: Int = 100,
    val connectedClient: String = "",
    val downloadsCompleted: Int = 0,
    val uploadsCompleted: Int = 0,
    val lastReceivedFile: WebReceivedFile? = null,
    val recentReceivedFiles: List<WebReceivedFile> = emptyList(),
    val message: String = "Web Transfer is idle"
)
