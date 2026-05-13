package com.nitrodropnative.webtransfer

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WebTransferNetwork {
    fun bestLocalIp(context: Context): String {
        val fromInterfaces = interfaceIpv4()
        if (fromInterfaces.isNotBlank()) return fromInterfaces
        return wifiManagerIp(context).ifBlank { "127.0.0.1" }
    }

    fun isOnWifi(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun interfaceIpv4(): String {
        return runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .map { it.hostAddress.orEmpty() }
                .firstOrNull { ip ->
                    ip.startsWith("192.168.") || ip.startsWith("10.") || ip.matches(Regex("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*"))
                }.orEmpty()
        }.getOrDefault("")
    }

    @Suppress("DEPRECATION")
    private fun wifiManagerIp(context: Context): String {
        return runCatching {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val raw = wifi.connectionInfo.ipAddress
            if (raw == 0) return@runCatching ""
            val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(raw).array()
            bytes.joinToString(".") { (it.toInt() and 0xff).toString() }
        }.getOrDefault("")
    }
}
