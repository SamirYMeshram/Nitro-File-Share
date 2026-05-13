package com.nitrodropnative.core.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManager {
    const val REQUEST_CODE_STARTUP = 9001

    fun runtimePermissions(): Array<String> = buildList {
        if (Build.VERSION.SDK_INT >= 33) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }.toTypedArray()

    fun currentState(context: Context): PermissionState {
        val missing = runtimePermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        val notificationGranted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        return PermissionState(missingPermissions = missing, notificationPermissionGranted = notificationGranted)
    }

    fun requestStartupPermissions(activity: Activity) {
        val missing = currentState(activity).missingPermissions
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, missing.toTypedArray(), REQUEST_CODE_STARTUP)
        }
    }
}
