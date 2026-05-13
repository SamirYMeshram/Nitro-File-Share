package com.nitrodropnative.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nitrodropnative.core.util.ByteFormatter
import com.nitrodropnative.core.util.SpeedFormatter
import com.nitrodropnative.core.util.TimeFormatter
import com.nitrodropnative.ui.components.GlassCard
import com.nitrodropnative.ui.theme.NitroBackground
import com.nitrodropnative.ui.theme.NitroMuted
import com.nitrodropnative.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val history by viewModel.history.collectAsStateWithLifecycle(initialValue = emptyList())
    Column(Modifier.fillMaxSize().background(NitroBackground).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
            Text("History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
        }
        if (history.isEmpty()) {
            GlassCard { Text("No completed transfers yet.", color = NitroMuted) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history) { item ->
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.fileName, fontWeight = FontWeight.Bold)
                            Text("${item.direction} • ${item.transportType} • ${item.status}", color = NitroMuted)
                            Text("${ByteFormatter.format(item.fileSize)} • Avg ${SpeedFormatter.format(item.averageSpeed)} • Peak ${SpeedFormatter.format(item.peakSpeed)} • ${TimeFormatter.duration(item.duration)}")
                        }
                    }
                }
            }
        }
    }
}
