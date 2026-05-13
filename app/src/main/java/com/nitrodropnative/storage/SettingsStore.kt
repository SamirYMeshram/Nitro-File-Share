package com.nitrodropnative.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nitrodropnative.core.constants.AppConstants
import com.nitrodropnative.core.util.DeviceInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class TransportPreference { AUTO, LAN, WIFI_DIRECT, WIFI_AWARE }
enum class ChunkSizeMode { AUTO, ONE_MB, FOUR_MB, EIGHT_MB }

data class AppSettings(
    val preferredTransportMode: TransportPreference = TransportPreference.AUTO,
    val defaultSaveLocation: String = AppConstants.DOWNLOADS_FOLDER,
    val enableChecksumVerification: Boolean = true,
    val enableSpeedStabilityMode: Boolean = true,
    val enableWifiLock: Boolean = true,
    val uiUpdateRateMs: Int = AppConstants.UI_UPDATE_INTERVAL_MS.toInt(),
    val chunkSizeMode: ChunkSizeMode = ChunkSizeMode.AUTO,
    val deviceDisplayName: String = "NitroDrop Device"
)

private val Context.dataStore by preferencesDataStore("nitrodrop_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val transport = stringPreferencesKey("preferred_transport")
        val saveLocation = stringPreferencesKey("save_location")
        val checksum = booleanPreferencesKey("checksum")
        val stability = booleanPreferencesKey("stability")
        val wifiLock = booleanPreferencesKey("wifi_lock")
        val uiRate = intPreferencesKey("ui_rate")
        val chunkMode = stringPreferencesKey("chunk_mode")
        val displayName = stringPreferencesKey("display_name")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            preferredTransportMode = prefs[Keys.transport]?.let { runCatching { TransportPreference.valueOf(it) }.getOrNull() } ?: TransportPreference.AUTO,
            defaultSaveLocation = prefs[Keys.saveLocation] ?: AppConstants.DOWNLOADS_FOLDER,
            enableChecksumVerification = prefs[Keys.checksum] ?: true,
            enableSpeedStabilityMode = prefs[Keys.stability] ?: true,
            enableWifiLock = prefs[Keys.wifiLock] ?: true,
            uiUpdateRateMs = prefs[Keys.uiRate] ?: AppConstants.UI_UPDATE_INTERVAL_MS.toInt(),
            chunkSizeMode = prefs[Keys.chunkMode]?.let { runCatching { ChunkSizeMode.valueOf(it) }.getOrNull() } ?: ChunkSizeMode.AUTO,
            deviceDisplayName = prefs[Keys.displayName] ?: DeviceInfo.defaultDisplayName(context)
        )
    }

    suspend fun setTransportPreference(value: TransportPreference) {
        context.dataStore.edit { it[Keys.transport] = value.name }
    }

    suspend fun setChecksumEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.checksum] = value }
    }

    suspend fun setWifiLockEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.wifiLock] = value }
    }

    suspend fun setUiRate(value: Int) {
        context.dataStore.edit { it[Keys.uiRate] = value }
    }

    suspend fun setChunkSizeMode(value: ChunkSizeMode) {
        context.dataStore.edit { it[Keys.chunkMode] = value.name }
    }

    suspend fun setDeviceDisplayName(value: String) {
        context.dataStore.edit { it[Keys.displayName] = value }
    }
}
