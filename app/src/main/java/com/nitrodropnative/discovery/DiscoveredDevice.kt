package com.nitrodropnative.discovery

import com.nitrodropnative.transport.ConnectionInfo
import com.nitrodropnative.transport.TransportType

data class DiscoveredDevice(
    val id: String,
    val name: String,
    val host: String? = null,
    val port: Int? = null,
    val transportType: TransportType,
    val signalHint: String = "",
    val connectionInfo: ConnectionInfo? = null
)
