package com.nitrodropnative.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nitrodropnative.transport.ConnectionInfo
import com.nitrodropnative.ui.components.DeviceCard
import com.nitrodropnative.ui.components.GlassCard
import com.nitrodropnative.ui.components.PrimaryActionButton
import com.nitrodropnative.ui.theme.NitroBackground
import com.nitrodropnative.ui.theme.NitroMuted
import com.nitrodropnative.viewmodel.DiscoveryViewModel
import com.nitrodropnative.viewmodel.SendViewModel

@Composable
fun DeviceDiscoveryScreen(
    viewModel: DiscoveryViewModel,
    sendViewModel: SendViewModel,
    onBack: () -> Unit,
    onStartTransfer: (Uri, ConnectionInfo) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val files by sendViewModel.selectedFiles.collectAsStateWithLifecycle()
    DisposableEffect(Unit) {
        viewModel.start()
        onDispose { viewModel.stop() }
    }
    Column(Modifier.fillMaxSize().background(NitroBackground).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
            Text("Nearby receivers", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
        }
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (state.isScanning) "Scanning LAN / Wi-Fi Direct" else "Scanner stopped", fontWeight = FontWeight.Bold)
                Text(state.message.ifBlank { "LAN NSD is fully wired; Wi-Fi Direct peer discovery is available where permissions and hardware support it." }, color = NitroMuted)
                PrimaryActionButton("Refresh") { viewModel.start() }
            }
        }
        if (state.devices.isEmpty()) {
            GlassCard { Text("No device found yet. Both devices must be on the same Wi-Fi for LAN mode, or enable Wi-Fi Direct on supported devices.", color = NitroMuted) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.devices) { device ->
                    DeviceCard(device) {
                        val file = files.firstOrNull()
                        val connection = device.connectionInfo
                        if (file != null && connection != null && connection.host.isNotBlank()) onStartTransfer(file.uri, connection)
                    }
                }
            }
        }
    }
}
