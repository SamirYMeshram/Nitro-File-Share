package com.nitrodropnative.transport

import com.nitrodropnative.core.constants.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

open class TcpSocketTransport(
    override val type: TransportType = TransportType.LAN
) : Transport {
    override suspend fun connect(connectionInfo: ConnectionInfo): Socket = withContext(Dispatchers.IO) {
        Socket().apply {
            tcpNoDelay = true
            keepAlive = true
            sendBufferSize = AppConstants.SOCKET_BUFFER_SIZE
            receiveBufferSize = AppConstants.SOCKET_BUFFER_SIZE
            connect(InetSocketAddress(connectionInfo.host, connectionInfo.port), 12_000)
            soTimeout = 30_000
        }
    }

    override suspend fun openServer(port: Int): ServerSocket = withContext(Dispatchers.IO) {
        ServerSocket().apply {
            reuseAddress = true
            receiveBufferSize = AppConstants.SOCKET_BUFFER_SIZE
            bind(InetSocketAddress(port))
            soTimeout = 0
        }
    }

    protected fun tune(socket: Socket): Socket = socket.apply {
        tcpNoDelay = true
        keepAlive = true
        sendBufferSize = AppConstants.SOCKET_BUFFER_SIZE
        receiveBufferSize = AppConstants.SOCKET_BUFFER_SIZE
        soTimeout = 30_000
    }
}
