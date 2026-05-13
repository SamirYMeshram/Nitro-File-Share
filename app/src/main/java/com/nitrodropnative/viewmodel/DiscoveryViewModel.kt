package com.nitrodropnative.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nitrodropnative.discovery.DeviceDiscoveryManager
import com.nitrodropnative.discovery.DiscoveryState
import kotlinx.coroutines.flow.StateFlow

class DiscoveryViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = DeviceDiscoveryManager(application)
    val state: StateFlow<DiscoveryState> = manager.state

    fun start() = manager.startDiscovery(includeWifiDirect = true)
    fun stop() = manager.stopDiscovery()

    override fun onCleared() {
        manager.stopDiscovery()
        super.onCleared()
    }
}
