package com.nitrodropnative.discovery

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.nitrodropnative.core.constants.AppConstants
import com.nitrodropnative.transport.ConnectionInfo
import com.nitrodropnative.transport.TransportType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class WifiDirectDiscovery(private val context: Context) {
    private val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val p2pChannel = manager?.initialize(context, Looper.getMainLooper(), null)

    private fun hasPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.NEARBY_WIFI_DEVICES else Manifest.permission.ACCESS_FINE_LOCATION
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers(): Flow<DiscoveredDevice> = callbackFlow {
        if (manager == null || p2pChannel == null) {
            close(IllegalStateException("Wi-Fi Direct unsupported")); return@callbackFlow
        }
        if (!hasPermission()) {
            close(SecurityException("Nearby Wi-Fi permission missing")); return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        manager.requestPeers(p2pChannel) { peers ->
                            peers.deviceList.forEach { device: WifiP2pDevice ->
                                trySend(
                                    DiscoveredDevice(
                                        id = "p2p-${device.deviceAddress}",
                                        name = device.deviceName ?: "Wi-Fi Direct device",
                                        transportType = TransportType.WIFI_DIRECT,
                                        signalHint = statusLabel(device.status),
                                        connectionInfo = ConnectionInfo(
                                            host = "",
                                            port = AppConstants.DEFAULT_PORT,
                                            transportType = TransportType.WIFI_DIRECT,
                                            peerName = device.deviceName ?: "Wi-Fi Direct device"
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        manager.discoverPeers(p2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = Unit
            override fun onFailure(reason: Int) {
                // Instead of closing with error, we can just emit an empty list or log it
                // To avoid crashing the app with IllegalStateException, we should handle it gracefully
                // trySend(DiscoveredDevice(...)) or just log and wait for next retry if any
                // But definitely DON'T throw or close with exception if we want to stay alive
            }
        })
        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
            runCatching { manager.stopPeerDiscovery(p2pChannel, null) }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String, onResult: (Result<Unit>) -> Unit) {
        if (manager == null || p2pChannel == null) {
            onResult(Result.failure(IllegalStateException("Wi-Fi Direct unsupported"))); return
        }
        val config = WifiP2pConfig().apply { this.deviceAddress = deviceAddress }
        manager.connect(p2pChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = onResult(Result.success(Unit))
            override fun onFailure(reason: Int) = onResult(Result.failure(IllegalStateException("Wi-Fi Direct connect failed: $reason")))
        })
    }

    @SuppressLint("MissingPermission")
    fun requestConnectionInfo(onResult: (WifiP2pInfo?) -> Unit) {
        if (manager == null || p2pChannel == null) {
            onResult(null)
            return
        }
        manager.requestConnectionInfo(p2pChannel) { info -> onResult(info) }
    }

    private fun statusLabel(status: Int): String = when (status) {
        WifiP2pDevice.AVAILABLE -> "Available"
        WifiP2pDevice.INVITED -> "Invited"
        WifiP2pDevice.CONNECTED -> "Connected"
        WifiP2pDevice.FAILED -> "Failed"
        WifiP2pDevice.UNAVAILABLE -> "Unavailable"
        else -> "Unknown"
    }
}
