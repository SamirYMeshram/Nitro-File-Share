package com.nitrodropnative.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nitrodropnative.core.util.SpeedFormatter
import com.nitrodropnative.ui.theme.NitroMuted

@Composable
fun SpeedMeter(bytesPerSecond: Long, label: String = "current speed") {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = SpeedFormatter.mbps(bytesPerSecond),
            fontSize = 56.sp,
            lineHeight = 56.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = "MB/s", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(text = label.uppercase(), fontSize = 12.sp, color = NitroMuted)
    }
}
