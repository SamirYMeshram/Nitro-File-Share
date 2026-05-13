package com.nitrodropnative.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nitrodropnative.core.util.DeviceInfo
import com.nitrodropnative.discovery.DeviceDiscoveryManager
import com.nitrodropnative.discovery.DiscoveryState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReceiveViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = DeviceDiscoveryManager(application)
    val discoveryState: StateFlow<DiscoveryState> = manager.state
    val deviceName: String = DeviceInfo.defaultDisplayName(application)

    fun advertise() {
        viewModelScope.launch { manager.advertise(deviceName) }
    }

    fun stopAdvertising() { manager.stopAdvertising() }

    override fun onCleared() {
        manager.stopAdvertising()
        super.onCleared()
    }
}
