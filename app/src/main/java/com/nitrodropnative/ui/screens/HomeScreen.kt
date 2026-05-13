package com.nitrodropnative.ui.screens

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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nitrodropnative.core.constants.AppConstants
import com.nitrodropnative.core.util.SpeedFormatter
import com.nitrodropnative.ui.components.GlassCard
import com.nitrodropnative.ui.components.PrimaryActionButton
import com.nitrodropnative.ui.components.StatTile
import com.nitrodropnative.ui.theme.NitroBackground
import com.nitrodropnative.ui.theme.NitroMuted
import com.nitrodropnative.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onWebTransfer: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().background(NitroBackground).verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(AppConstants.APP_NAME, fontSize = 36.sp, fontWeight = FontWeight.Black)
                Text(AppConstants.APP_TAGLINE, color = NitroMuted)
            }
            Row {
                IconButton(onClick = onHistory) { Icon(Icons.Rounded.History, contentDescription = "History") }
                IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
            }
        }
        Spacer(Modifier.height(10.dp))
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Transfer control", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                PrimaryActionButton("Send Files", onClick = onSend)
                PrimaryActionButton("Receive Files", onClick = onReceive)
                PrimaryActionButton("Web Transfer to PC", onClick = onWebTransfer)
            }
        }
        GlassCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatTile("Last speed", SpeedFormatter.format(stats.lastTransferSpeed))
                StatTile("Sent", stats.totalFilesSent.toString())
                StatTile("Fastest", SpeedFormatter.format(stats.fastestRecordedSpeed))
            }
        }
    }
}
