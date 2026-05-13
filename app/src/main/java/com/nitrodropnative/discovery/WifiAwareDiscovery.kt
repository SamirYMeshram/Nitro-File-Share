package com.nitrodropnative.discovery

import android.content.Context
import android.net.wifi.aware.WifiAwareManager
import android.os.Build
import com.nitrodropnative.core.util.DeviceInfo

class WifiAwareDiscovery(private val context: Context) {
    fun isSupported(): Boolean = DeviceInfo.supportsWifiAware(context)

    fun managerOrNull(): WifiAwareManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isSupported()) {
        context.getSystemService(WifiAwareManager::class.java)
    } else null

    fun architectureNote(): String = if (isSupported()) {
        "Wi-Fi Aware available. Add publish/subscribe session wiring when targeting devices that support NAN data paths."
    } else {
        "Wi-Fi Aware unsupported on this device."
    }
}
