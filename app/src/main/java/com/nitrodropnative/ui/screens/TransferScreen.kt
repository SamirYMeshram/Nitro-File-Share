package com.nitrodropnative.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nitrodropnative.core.util.ByteFormatter
import com.nitrodropnative.core.util.SpeedFormatter
import com.nitrodropnative.core.util.TimeFormatter
import com.nitrodropnative.transfer.TransferState
import com.nitrodropnative.ui.components.GlassCard
import com.nitrodropnative.ui.components.PrimaryActionButton
import com.nitrodropnative.ui.components.SpeedMeter
import com.nitrodropnative.ui.components.StatTile
import com.nitrodropnative.ui.components.TransferProgressRing
import com.nitrodropnative.ui.theme.NitroBackground
import com.nitrodropnative.ui.theme.NitroMuted
import com.nitrodropnative.viewmodel.TransferViewModel

@Composable
fun TransferScreen(viewModel: TransferViewModel, onDone: () -> Unit) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val incoming by viewModel.incomingRequest.collectAsStateWithLifecycle()
    val transferredText by remember(stats.transferredBytes, stats.totalBytes) {
        derivedStateOf { "${ByteFormatter.format(stats.transferredBytes)} / ${ByteFormatter.format(stats.totalBytes)}" }
    }

    Column(Modifier.fillMaxSize().background(NitroBackground).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Transfer dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        GlassCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stats.fileName.ifBlank { "Waiting for transfer" }, fontWeight = FontWeight.Bold)
                SpeedMeter(stats.currentSpeed)
                TransferProgressRing(stats.progressPercent)
                Text(stats.state.name + if (stats.message.isNotBlank()) " • ${stats.message}" else "", color = NitroMuted)
            }
        }

        incoming?.let { meta ->
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Incoming file request", fontWeight = FontWeight.Bold)
                    Text("${meta.fileName} • ${ByteFormatter.format(meta.fileSize)}", color = NitroMuted)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryActionButton("Accept", modifier = Modifier.weight(1f)) { viewModel.acceptIncoming() }
                        PrimaryActionButton("Reject", modifier = Modifier.weight(1f)) { viewModel.rejectIncoming() }
                    }
                }
            }
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
                    StatTile("Transport", stats.transportType.label)
                    StatTile("Session", stats.sessionId.take(8).ifBlank { "--" })
                    StatTile("State", stats.state.name)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryActionButton("Pause", modifier = Modifier.weight(1f), enabled = stats.state == TransferState.Transferring) { viewModel.pause() }
            PrimaryActionButton("Cancel", modifier = Modifier.weight(1f)) { viewModel.cancel() }
        }
        if (stats.state in setOf(TransferState.Completed, TransferState.Failed, TransferState.Cancelled)) {
            PrimaryActionButton("Finish") { onDone() }
        }
    }
}
