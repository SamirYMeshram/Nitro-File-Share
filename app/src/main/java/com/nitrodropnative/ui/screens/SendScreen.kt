package com.nitrodropnative.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nitrodropnative.core.util.ByteFormatter
import com.nitrodropnative.ui.components.GlassCard
import com.nitrodropnative.ui.components.PrimaryActionButton
import com.nitrodropnative.ui.theme.NitroBackground
import com.nitrodropnative.ui.theme.NitroMuted
import com.nitrodropnative.viewmodel.SendViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    viewModel: SendViewModel,
    onBack: () -> Unit,
    onDiscover: () -> Unit,
    onStartTransfer: (Uri, String) -> Unit
) {
    val selected by viewModel.selectedFiles.collectAsStateWithLifecycle()
    val ip by viewModel.receiverIp.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.setUris(uris)
    }

    Column(
        Modifier.fillMaxSize().background(NitroBackground).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
            Text("Send files", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select files", fontWeight = FontWeight.Bold)
                PrimaryActionButton("Open Android File Picker") { launcher.launch(arrayOf("*/*")) }
                Text("Storage Access Framework is used, so broad storage permission is avoided.", color = NitroMuted)
            }
        }

        if (selected.isNotEmpty()) {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selected (${selected.size})", fontWeight = FontWeight.Bold)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        items(selected) { file ->
                            Text("${file.name} • ${ByteFormatter.format(file.size)}", color = NitroMuted)
                        }
                    }
                }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Receiver", fontWeight = FontWeight.Bold)
                PrimaryActionButton("Discover receivers") { onDiscover() }
                OutlinedTextField(
                    value = ip,
                    onValueChange = viewModel::setReceiverIp,
                    label = { Text("Manual receiver IP on same Wi-Fi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("For first test: open Receive on device B, then enter device B IP here on device A.", color = NitroMuted)
                PrimaryActionButton(
                    text = "Start LAN TCP Transfer",
                    enabled = selected.isNotEmpty() && ip.isNotBlank(),
                    onClick = { selected.firstOrNull()?.let { onStartTransfer(it.uri, ip) } }
                )
                if (selected.size > 1) Text("Phase 1 sends the first selected file. Multi-file queue is prepared in architecture.", color = NitroMuted)
            }
        }
    }
}
