package com.nitrodropnative.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nitrodropnative.storage.AppSettings
import com.nitrodropnative.storage.ChunkSizeMode
import com.nitrodropnative.storage.SettingsStore
import com.nitrodropnative.storage.TransportPreference
import com.nitrodropnative.ui.components.GlassCard
import com.nitrodropnative.ui.components.PrimaryActionButton
import com.nitrodropnative.ui.components.StatTile
import com.nitrodropnative.ui.theme.NitroBackground
import com.nitrodropnative.ui.theme.NitroMuted
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    val settings by store.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(NitroBackground).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
        }
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Preferred transport", settings.preferredTransportMode.name)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryActionButton("Auto", modifier = Modifier.weight(1f)) { scope.launch { store.setTransportPreference(TransportPreference.AUTO) } }
                    PrimaryActionButton("LAN", modifier = Modifier.weight(1f)) { scope.launch { store.setTransportPreference(TransportPreference.LAN) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryActionButton("Direct", modifier = Modifier.weight(1f)) { scope.launch { store.setTransportPreference(TransportPreference.WIFI_DIRECT) } }
                    PrimaryActionButton("Aware", modifier = Modifier.weight(1f)) { scope.launch { store.setTransportPreference(TransportPreference.WIFI_AWARE) } }
                }
                Text("Bluetooth is intentionally only a fallback concept for small files; not a high-speed large-file path.", color = NitroMuted)
            }
        }
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingSwitch("Checksum verification", settings.enableChecksumVerification) { scope.launch { store.setChecksumEnabled(it) } }
                SettingSwitch("Wi-Fi performance lock", settings.enableWifiLock) { scope.launch { store.setWifiLockEnabled(it) } }
                StatTile("Chunk size mode", settings.chunkSizeMode.name)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryActionButton("Auto", modifier = Modifier.weight(1f)) { scope.launch { store.setChunkSizeMode(ChunkSizeMode.AUTO) } }
                    PrimaryActionButton("4 MB", modifier = Modifier.weight(1f)) { scope.launch { store.setChunkSizeMode(ChunkSizeMode.FOUR_MB) } }
                    PrimaryActionButton("8 MB", modifier = Modifier.weight(1f)) { scope.launch { store.setChunkSizeMode(ChunkSizeMode.EIGHT_MB) } }
                }
                Text("UI updates are capped around 250 ms during active transfer to avoid Compose recomposition pressure.", color = NitroMuted)
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
