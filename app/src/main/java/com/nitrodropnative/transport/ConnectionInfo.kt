package com.nitrodropnative.transport

import com.nitrodropnative.core.constants.AppConstants

data class ConnectionInfo(
    val host: String,
    val port: Int = AppConstants.DEFAULT_PORT,
    val transportType: TransportType = TransportType.LAN,
    val peerName: String = "Android device",
    val isGroupOwner: Boolean = false
)
