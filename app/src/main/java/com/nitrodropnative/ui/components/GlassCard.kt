package com.nitrodropnative.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nitrodropnative.ui.theme.NitroCyan

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.56f)
                    )
                )
            )
            .border(BorderStroke(1.dp, NitroCyan.copy(alpha = 0.16f)), RoundedCornerShape(28.dp))
            .padding(padding)
    ) {
        content()
    }
}
