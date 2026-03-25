package com.example.forgeint.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun NetworkIndicator(
    viewModel: GeminiViewModel,
    modifier: Modifier = Modifier
) {
    val connectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()

    val icon: ImageVector
    val label: String
    val color: Color

    when (connectionMode) {
        ConnectionMode.Standalone -> {
            icon = Icons.Default.Wifi
            label = "Wi-Fi"
            color = Color(0xFF4CAF50) // Green
        }
        ConnectionMode.Phone -> {
            icon = Icons.Default.Smartphone
            label = "Phone"
            color = Color(0xFF2196F3) // Blue
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.2f))
            .clickable { viewModel.toggleConnectionMode() }
            .animateContentSize(
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.caption2,
            color = color
        )
    }
}

