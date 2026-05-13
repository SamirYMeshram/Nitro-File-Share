package com.nitrodropnative.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nitrodropnative.core.util.ByteFormatter
import com.nitrodropnative.core.util.SpeedFormatter
import com.nitrodropnative.core.util.TimeFormatter
import com.nitrodropnative.ui.components.GlassCard
import com.nitrodropnative.ui.components.PrimaryActionButton
import com.nitrodropnative.ui.components.SpeedMeter
import com.nitrodropnative.ui.components.StatTile
import com.nitrodropnative.ui.components.TransferProgressRing
import com.nitrodropnative.ui.theme.NitroBackground
import com.nitrodropnative.ui.theme.NitroMuted
import com.nitrodropnative.viewmodel.WebTransferViewModel
import com.nitrodropnative.webtransfer.WebReceivedFile
import com.nitrodropnative.webtransfer.WebTransferState

@Composable
fun WebTransferScreen(
    viewModel: WebTransferViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val files by viewModel.sharedFiles.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val replaceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.setShareUris(uris)
    }
    val addLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addShareUris(uris)
    }
    val transferredText by remember(stats.transferredBytes, stats.totalBytes) {
        derivedStateOf { "${ByteFormatter.format(stats.transferredBytes)} / ${ByteFormatter.format(stats.totalBytes)}" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NitroBackground)
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
            Text("Web Transfer", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Phone ↔ PC browser mode", fontWeight = FontWeight.Bold)
                Text("Run a local HTTP server on this phone. Open the shown URL on a PC connected to the same Wi‑Fi. No cloud and no PC app required.", color = NitroMuted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryActionButton("Replace files", modifier = Modifier.weight(1f)) { replaceLauncher.launch(arrayOf("*/*")) }
                    PrimaryActionButton(
                        text = if (stats.isRunning) "Restart" else "Start server",
                        modifier = Modifier.weight(1f)
                    ) { viewModel.start() }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryActionButton("Add more files", modifier = Modifier.weight(1f)) { addLauncher.launch(arrayOf("*/*")) }
                    PrimaryActionButton("Clear list", modifier = Modifier.weight(1f), enabled = files.isNotEmpty()) { viewModel.clearSharedFiles() }
                }
                PrimaryActionButton("Stop server", enabled = stats.isRunning) { viewModel.stop() }
                Text("Selected for PC download: ${files.size} file(s). PC upload works even when no phone files are selected.", color = NitroMuted)
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Open on your PC", fontWeight = FontWeight.Bold)
                Text(stats.serverUrl.ifBlank { "Start the server first" }, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text("Password: ${stats.password.ifBlank { "---" }} • Port ${stats.port}", color = NitroMuted)
                PrimaryActionButton("Copy URL", enabled = stats.serverUrl.isNotBlank()) {
                    clipboard.setText(AnnotatedString(stats.serverUrl))
                    Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                }
                Text("Security: the PC browser asks for this 3-digit password once per server session. Stop the server after use.", color = NitroMuted)
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stats.activeFileName.ifBlank { "No active browser transfer" }, fontWeight = FontWeight.Bold)
                SpeedMeter(stats.currentSpeed)
                TransferProgressRing(stats.progressPercent)
                Text("${stats.state.name} • ${stats.message}", color = NitroMuted)
            }
        }

        stats.lastReceivedFile?.let { received ->
            ReceivedFileResultCard(
                file = received,
                onOpen = { openReceivedFile(context, received) },
                onShare = { shareReceivedFile(context, received) },
                onCopyUri = {
                    clipboard.setText(AnnotatedString(received.uri))
                    Toast.makeText(context, "File URI copied", Toast.LENGTH_SHORT).show()
                },
                onDelete = {
                    viewModel.deleteReceivedFile(received) { deleted ->
                        Toast.makeText(
                            context,
                            if (deleted) "Deleted ${received.fileName}" else "Could not delete file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatTile("Average", SpeedFormatter.format(stats.averageSpeed))
                    StatTile("Peak", SpeedFormatter.format(stats.peakSpeed))
                    StatTile("ETA", TimeFormatter.eta(stats.etaSeconds))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatTile("Progress", "${(stats.progressPercent * 100).toInt()}%")
                    StatTile("Bytes", transferredText)
                    StatTile("Stability", "${stats.stabilityPercent}%")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatTile("Downloads", stats.downloadsCompleted.toString())
                    StatTile("Uploads", stats.uploadsCompleted.toString())
                    StatTile("Client", stats.connectedClient.ifBlank { "--" })
                }
            }
        }

        if (stats.recentReceivedFiles.size > 1) {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Recently received", fontWeight = FontWeight.Bold)
                    stats.recentReceivedFiles.drop(1).take(5).forEach { file ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(file.fileName, fontWeight = FontWeight.SemiBold)
                            Text("${file.fileTypeLabel} • ${ByteFormatter.format(file.sizeBytes)} • from ${file.receivedFrom}", color = NitroMuted)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PrimaryActionButton("Open", modifier = Modifier.weight(1f)) { openReceivedFile(context, file) }
                                PrimaryActionButton("Share", modifier = Modifier.weight(1f)) { shareReceivedFile(context, file) }
                            }
                        }
                    }
                }
            }
        }

        if (files.isNotEmpty()) {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Phone files shared to PC", fontWeight = FontWeight.Bold)
                    files.take(8).forEach { file -> Text("${file.name} • ${ByteFormatter.format(file.size)}", color = NitroMuted) }
                    if (files.size > 8) Text("+${files.size - 8} more", color = NitroMuted)
                }
            }
        }

        if (stats.state == WebTransferState.Failed) {
            Spacer(Modifier.height(4.dp))
            Text("If this fails, check that both devices are on the same Wi‑Fi and that no VPN/firewall blocks local network access.", color = NitroMuted)
        }
    }
}

@Composable
private fun ReceivedFileResultCard(
    file: WebReceivedFile,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onCopyUri: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Received successfully", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(file.fileName, fontWeight = FontWeight.Bold)
            Text(
                "${file.fileTypeLabel} • ${ByteFormatter.format(file.sizeBytes)} • Saved to ${file.savedLocation}",
                color = NitroMuted
            )
            Text(
                "From ${file.receivedFrom} • Avg ${SpeedFormatter.format(file.averageSpeed)} • Peak ${SpeedFormatter.format(file.peakSpeed)} • ${TimeFormatter.eta(file.durationSeconds)}",
                color = NitroMuted
            )
            Text("Checksum: ${file.checksumStatus}", color = NitroMuted)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryActionButton(file.primaryActionLabel, modifier = Modifier.weight(1f), onClick = onOpen)
                PrimaryActionButton("Share", modifier = Modifier.weight(1f), onClick = onShare)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryActionButton("Copy URI", modifier = Modifier.weight(1f), onClick = onCopyUri)
                PrimaryActionButton("Delete", modifier = Modifier.weight(1f), onClick = onDelete)
            }
        }
    }
}

private fun openReceivedFile(context: Context, file: WebReceivedFile) {
    val uri = Uri.parse(file.uri)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, file.mimeType.ifBlank { "application/octet-stream" })
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, file.primaryActionLabel))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this file type", Toast.LENGTH_SHORT).show()
    }
}

private fun shareReceivedFile(context: Context, file: WebReceivedFile) {
    val uri = Uri.parse(file.uri)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = file.mimeType.ifBlank { "application/octet-stream" }
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Share ${file.fileName}"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app can share this file", Toast.LENGTH_SHORT).show()
    }
}
