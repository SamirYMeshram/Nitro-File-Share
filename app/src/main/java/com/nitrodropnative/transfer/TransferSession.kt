package com.nitrodropnative.transfer

import com.nitrodropnative.transport.TransportType
import java.util.UUID

data class TransferSession(
    val id: String = UUID.randomUUID().toString(),
    val direction: TransferDirection = TransferDirection.SEND,
    val state: TransferState = TransferState.Idle,
    val fileName: String = "",
    val totalBytes: Long = 0L,
    val transferredBytes: Long = 0L,
    val transportType: TransportType = TransportType.LAN,
    val peerName: String = "",
    val error: String? = null
)
