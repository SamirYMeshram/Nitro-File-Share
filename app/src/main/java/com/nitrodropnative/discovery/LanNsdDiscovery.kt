package com.nitrodropnative.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.nitrodropnative.core.constants.AppConstants
import com.nitrodropnative.transport.ConnectionInfo
import com.nitrodropnative.transport.TransportType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.InetAddress

class LanNsdDiscovery(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun advertise(deviceName: String, port: Int = AppConstants.DEFAULT_PORT) {
        stopAdvertising()
        val info = NsdServiceInfo().apply {
            serviceName = deviceName
            serviceType = AppConstants.NSD_SERVICE_TYPE
            setPort(port)
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun discover(): Flow<DiscoveredDevice> = callbackFlow {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
                close(IllegalStateException("NSD start failed: $errorCode"))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                close(IllegalStateException("NSD stop failed: $errorCode"))
            }

            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != AppConstants.NSD_SERVICE_TYPE) return
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        val host: InetAddress = resolved.host ?: return
                        val port = resolved.port.takeIf { it > 0 } ?: AppConstants.DEFAULT_PORT
                        trySend(
                            DiscoveredDevice(
                                id = "lan-${host.hostAddress}:$port",
                                name = resolved.serviceName,
                                host = host.hostAddress,
                                port = port,
                                transportType = TransportType.LAN,
                                signalHint = "Same Wi-Fi",
                                connectionInfo = ConnectionInfo(host.hostAddress ?: "", port, TransportType.LAN, resolved.serviceName)
                            )
                        )
                    }
                })
            }
        }
        nsdManager.discoverServices(AppConstants.NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        awaitClose { stopDiscovery() }
    }

    fun stopAdvertising() {
        registrationListener?.let { runCatching { nsdManager.unregisterService(it) } }
        registrationListener = null
    }

    fun stopDiscovery() {
        discoveryListener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
        discoveryListener = null
    }
}
