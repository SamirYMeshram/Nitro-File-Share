package com.nitrodropnative.discovery

data class DiscoveryState(
    val isScanning: Boolean = false,
    val isAdvertising: Boolean = false,
    val devices: List<DiscoveredDevice> = emptyList(),
    val error: String? = null,
    val message: String = ""
)
