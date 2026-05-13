package com.nitrodropnative.discovery

import android.content.Context
import com.nitrodropnative.core.constants.AppConstants
import com.nitrodropnative.core.util.DeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class DeviceDiscoveryManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val lan = LanNsdDiscovery(context)
    private val wifiDirect = WifiDirectDiscovery(context)
    private val aware = WifiAwareDiscovery(context)
    private val _state = MutableStateFlow(DiscoveryState())
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()
    private var scanJob: Job? = null

    fun advertise(deviceName: String = DeviceInfo.defaultDisplayName(context), port: Int = AppConstants.DEFAULT_PORT) {
        lan.advertise(deviceName, port)
        _state.value = _state.value.copy(isAdvertising = true, message = "Advertising on LAN as $deviceName")
    }

    fun stopAdvertising() {
        lan.stopAdvertising()
        _state.value = _state.value.copy(isAdvertising = false)
    }

    fun startDiscovery(includeWifiDirect: Boolean = true) {
        stopDiscovery()
        _state.value = DiscoveryState(isScanning = true, message = aware.architectureNote())
        val devices = linkedMapOf<String, DiscoveredDevice>()
        scanJob = scope.launch {
            launch {
                lan.discover().collect { device ->
                    devices[device.id] = device
                    _state.value = _state.value.copy(devices = devices.values.toList(), isScanning = true)
                }
            }
            if (includeWifiDirect) {
                launch {
                    wifiDirect.discoverPeers().collect { device ->
                        devices[device.id] = device
                        _state.value = _state.value.copy(devices = devices.values.toList(), isScanning = true)
                    }
                }
            }
        }
    }

    fun stopDiscovery() {
        scanJob?.cancel()
        scanJob = null
        lan.stopDiscovery()
        _state.value = _state.value.copy(isScanning = false)
    }
}
