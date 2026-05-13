package com.nitrodropnative.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nitrodropnative.core.constants.AppConstants
import com.nitrodropnative.ui.components.GlassCard
import com.nitrodropnative.ui.components.PrimaryActionButton
import com.nitrodropnative.ui.theme.NitroBackground
import com.nitrodropnative.ui.theme.NitroMuted
import com.nitrodropnative.viewmodel.ReceiveViewModel

@Composable
fun ReceiveScreen(
    viewModel: ReceiveViewModel,
    onBack: () -> Unit,
    onStartReceive: () -> Unit
) {
    val discovery by viewModel.discoveryState.collectAsStateWithLifecycle()
    DisposableEffect(Unit) {
        viewModel.advertise()
        onDispose { viewModel.stopAdvertising() }
    }
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseValue"
    )

    Column(Modifier.fillMaxSize().background(NitroBackground).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
            Text("Receive files", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
        }

        GlassCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    shape = CircleShape,
                    modifier = Modifier.size(150.dp).scale(pulse)
                ) {}
                Text(viewModel.deviceName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Advertising: ${if (discovery.isAdvertising) "on" else "off"} • Port ${AppConstants.DEFAULT_PORT}", color = NitroMuted)
                Text("When the sender connects, metadata is read first. Production pairing can attach an Accept/Reject dialog before resume offset is sent.", color = NitroMuted)
                PrimaryActionButton("Start Receiver Socket") { onStartReceive() }
            }
        }
    }
}
