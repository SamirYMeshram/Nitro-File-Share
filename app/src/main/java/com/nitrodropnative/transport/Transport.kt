package com.nitrodropnative.transport

import java.net.ServerSocket
import java.net.Socket

interface Transport {
    val type: TransportType
    suspend fun connect(connectionInfo: ConnectionInfo): Socket
    suspend fun openServer(port: Int): ServerSocket
}
