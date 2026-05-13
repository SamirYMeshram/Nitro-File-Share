package com.nitrodropnative.service

import android.content.Context
import android.net.wifi.WifiManager

class WifiPerformanceLock(context: Context) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val lock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "NitroDrop:TransferWifiLock")

    fun acquire() {
        if (!lock.isHeld) lock.acquire()
    }

    fun release() {
        if (lock.isHeld) lock.release()
    }
}
