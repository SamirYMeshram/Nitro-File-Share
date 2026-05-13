package com.nitrodropnative.core.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

object DeviceInfo {
    fun defaultDisplayName(context: Context): String {
        val androidName = Settings.Global.getString(context.contentResolver, "device_name")
        return androidName?.takeIf { it.isNotBlank() } ?: "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    fun supportsWifiAware(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)

    fun supportsWifiDirect(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)
}
