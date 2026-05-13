package com.nitrodropnative.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nitrodropnative.ui.theme.NitroMuted

@Composable
fun StatTile(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, color = NitroMuted, style = MaterialTheme.typography.labelMedium)
        Text(text = value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}
