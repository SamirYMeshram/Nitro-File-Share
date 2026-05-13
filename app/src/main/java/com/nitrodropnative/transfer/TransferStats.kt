package com.nitrodropnative.transfer

import com.nitrodropnative.transport.TransportType

data class TransferStats(
    val sessionId: String = "",
    val fileName: String = "",
    val state: TransferState = TransferState.Idle,
    val transferredBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val currentSpeed: Long = 0L,
    val averageSpeed: Long = 0L,
    val peakSpeed: Long = 0L,
    val etaSeconds: Long = Long.MAX_VALUE,
    val stabilityPercent: Int = 100,
    val progressPercent: Float = 0f,
    val transportType: TransportType = TransportType.LAN,
    val message: String = "",
    val startedAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)
