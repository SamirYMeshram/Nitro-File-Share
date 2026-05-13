package com.nitrodropnative.transport

import android.content.Context
import com.nitrodropnative.core.util.DeviceInfo
import com.nitrodropnative.storage.TransportPreference

class TransportSelector(private val context: Context) {
    fun choose(preference: TransportPreference, candidates: List<ConnectionInfo>): ConnectionInfo? {
        val ordered = when (preference) {
            TransportPreference.LAN -> listOf(TransportType.LAN)
            TransportPreference.WIFI_DIRECT -> listOf(TransportType.WIFI_DIRECT)
            TransportPreference.WIFI_AWARE -> listOf(TransportType.WIFI_AWARE)
            TransportPreference.AUTO -> listOf(TransportType.LAN, TransportType.WIFI_DIRECT, TransportType.WIFI_AWARE)
        }
        return ordered.asSequence()
            .filter { type -> type != TransportType.WIFI_AWARE || DeviceInfo.supportsWifiAware(context) }
            .flatMap { type -> candidates.asSequence().filter { it.transportType == type } }
            .firstOrNull()
    }

    fun transportFor(type: TransportType): Transport = when (type) {
        TransportType.LAN -> LanSocketTransport()
        TransportType.WIFI_DIRECT -> WifiDirectTransport()
        TransportType.WIFI_AWARE -> WifiAwareTransport()
        TransportType.BLUETOOTH -> LanSocketTransport() // Bluetooth is intentionally not a primary large-file path.
    }
}
