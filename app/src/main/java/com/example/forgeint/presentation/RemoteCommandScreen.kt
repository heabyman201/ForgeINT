package com.example.forgeint.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.ScrollIndicator
import com.example.forgeint.ForgeClient
import com.example.forgeint.presentation.theme.LocalForgeIntColors
import kotlinx.coroutines.launch

@Composable
private fun CircularActionButton(
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconTint: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.35f))
                .border(1.dp, iconTint.copy(alpha = if (enabled) 0.35f else 0.15f), CircleShape)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) iconTint else iconTint.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.caption3,
            color = if (enabled) Color.White else Color.LightGray
        )
    }
}

@Composable
private fun HoldToConfirmActionButton(
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconTint: Color = Color.White,
    enabled: Boolean = true,
    onConfirm: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.35f))
                .border(1.dp, iconTint.copy(alpha = if (enabled) 0.35f else 0.15f), CircleShape)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            isHolding = true
                            val job = coroutineScope.launch {
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 5000)
                                )
                                // If animation finishes, it means 5 seconds have passed
                                onConfirm()
                            }
                            // Wait for the user to release their finger
                            tryAwaitRelease()
                            
                            // Cancel animation on release and reset
                            job.cancel()
                            isHolding = false
                            coroutineScope.launch {
                                progress.snapTo(0f)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) iconTint else iconTint.copy(alpha = 0.5f)
            )
            
            if (isHolding) {
                CircularProgressIndicator(
                    progress = progress.value,
                    modifier = Modifier.fillMaxSize(),
                    startAngle = 270f,
                    endAngle = 270f + 360f,
                    strokeWidth = 4.dp,
                    indicatorColor = Color.White,
                    trackColor = Color.Transparent
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isHolding) "Hold..." else label,
            style = MaterialTheme.typography.caption3,
            color = if (enabled) Color.White else Color.LightGray
        )
    }
}

@Composable
fun RemoteCommandScreen(
    remoteHostIp: String,
    remotePort: String,
    onNavigateToRemoteSettings: () -> Unit
) {
    val colors = LocalForgeIntColors.current
    val listState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val forgeClient = remember(remoteHostIp, remotePort) { 
        ForgeClient(host = remoteHostIp, port = remotePort.toIntOrNull() ?: 1235) 
    }
    var statusText by remember { mutableStateOf("Ready") }
    var isExecuting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val executeCommand: (String, String) -> Unit = { command, payloadJson ->
        if (!isExecuting) {
            coroutineScope.launch {
                isExecuting = true
                statusText = "Running $command..."
                runCatching {
                    if (payloadJson.isBlank()) {
                        forgeClient.execute(command = command)
                    } else {
                        forgeClient.executeJson(
                            command = command,
                            payloadJson = payloadJson
                        )
                    }
                }.onSuccess { response ->
                    statusText = if (response.success) "Success" else "Error: ${response.code}"
                }.onFailure {
                    statusText = "Failed"
                }
                isExecuting = false
            }
        }
    }

    Scaffold(
        positionIndicator = { ScrollIndicator(state = listState) }
    ) {
        TransformingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .onRotaryScrollEvent {
                    coroutineScope.launch {
                        listState.scrollBy(it.verticalScrollPixels * 2f)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Remote Control",
                    style = MaterialTheme.typography.title3,
                    color = colors.botText,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            item {
                Chip(
                    onClick = onNavigateToRemoteSettings,
                    label = { Text("Remote IP / Port") },
                    secondaryLabel = { Text("${remoteHostIp.trim()}:$remotePort") },
                    icon = { Icon(Icons.Default.Settings, "Remote settings") },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble,
                        endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.caption2,
                    color = if (statusText == "Success") colors.primary else if (statusText == "Failed") Color(0xFFFF8A80) else colors.userText,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Power Presets (Top)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HoldToConfirmActionButton(
                        label = "Sleep",
                        icon = Icons.Default.Brightness3,
                        backgroundColor = Color(0xFF1E88E5),
                        enabled = !isExecuting,
                        onConfirm = { executeCommand("sleep", "") }
                    )
                    HoldToConfirmActionButton(
                        label = "Shutdown",
                        icon = Icons.Default.PowerSettingsNew,
                        backgroundColor = Color(0xFFD32F2F),
                        enabled = !isExecuting,
                        onConfirm = { executeCommand("shutdown", "") }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HoldToConfirmActionButton(
                        label = "Hibernate",
                        icon = Icons.Default.AcUnit,
                        backgroundColor = Color(0xFF673AB7),
                        enabled = !isExecuting,
                        onConfirm = { executeCommand("hibernate", "") }
                    )
                    HoldToConfirmActionButton(
                        label = "Lock",
                        icon = Icons.Default.Lock,
                        backgroundColor = Color(0xFF607D8B),
                        enabled = !isExecuting,
                        onConfirm = { executeCommand("lock", "") }
                    )
                }
            }

            // Other Utilities
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularActionButton(
                        label = "Screenshot",
                        icon = Icons.Default.CameraAlt,
                        backgroundColor = colors.primary.copy(alpha = 0.8f),
                        enabled = !isExecuting,
                        onClick = { executeCommand("screenshot", "") }
                    )
                    CircularActionButton(
                        label = "Speak",
                        icon = Icons.Default.RecordVoiceOver,
                        backgroundColor = Color(0xFF9C27B0),
                        enabled = !isExecuting,
                        onClick = { executeCommand("text_to_speech", """{"text":"Hello from ForgeINT"}""") }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularActionButton(
                        label = "Notify",
                        icon = Icons.Default.Notifications,
                        backgroundColor = Color(0xFFFF9800),
                        enabled = !isExecuting,
                        onClick = { executeCommand("show_notification", """{"title":"Forge","message":"Hello from ForgeINT"}""") }
                    )
                    CircularActionButton(
                        label = "Command",
                        icon = Icons.Default.Terminal,
                        backgroundColor = Color(0xFF4CAF50),
                        enabled = !isExecuting,
                        onClick = { executeCommand("run_cmd", """{"cmd":"whoami","timeout":15}""") }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularActionButton(
                        label = "Vol +",
                        icon = Icons.Default.VolumeUp,
                        backgroundColor = Color(0xFF009688),
                        enabled = !isExecuting,
                        onClick = { executeCommand("volume_up", "") }
                    )
                    CircularActionButton(
                        label = "Vol -",
                        icon = Icons.Default.VolumeDown,
                        backgroundColor = Color(0xFF00796B),
                        enabled = !isExecuting,
                        onClick = { executeCommand("volume_down", "") }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularActionButton(
                        label = "Bright +",
                        icon = Icons.Default.BrightnessHigh,
                        backgroundColor = Color(0xFFFFC107),
                        enabled = !isExecuting,
                        onClick = { executeCommand("brightness_up", "") }
                    )
                    CircularActionButton(
                        label = "Bright -",
                        icon = Icons.Default.BrightnessLow,
                        backgroundColor = Color(0xFFFFA000),
                        enabled = !isExecuting,
                        onClick = { executeCommand("brightness_down", "") }
                    )
                }
            }
        }
    }
}
