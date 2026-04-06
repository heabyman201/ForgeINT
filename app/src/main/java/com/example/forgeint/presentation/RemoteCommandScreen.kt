package com.example.forgeint.presentation

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.forgeint.ForgeResponse
import com.example.forgeint.RemoteImageOptions
import com.example.forgeint.presentation.theme.LocalForgeIntColors
import kotlinx.coroutines.launch

private fun formatByteCount(bytes: Int?): String {
    val safeBytes = bytes ?: return ""
    return when {
        safeBytes >= 1024 * 1024 -> String.format("%.2f MB", safeBytes / (1024f * 1024f))
        safeBytes >= 1024 -> String.format("%.1f KB", safeBytes / 1024f)
        else -> "$safeBytes B"
    }
}

private fun screenshotSummary(response: ForgeResponse): String {
    val dimensionSummary = if (response.imageWidth != null && response.imageHeight != null) {
        "${response.imageWidth}x${response.imageHeight}"
    } else {
        "Image ready"
    }
    val compressedSize = formatByteCount(response.compressedByteCount)
    val originalSize = formatByteCount(response.originalByteCount)

    return when {
        response.imageWasCompressed && compressedSize.isNotBlank() && originalSize.isNotBlank() ->
            "$dimensionSummary - $compressedSize from $originalSize"
        compressedSize.isNotBlank() -> "$dimensionSummary - $compressedSize"
        else -> dimensionSummary
    }
}

private fun clampPan(offset: Float, containerSize: Int, scale: Float): Float {
    val maxOffset = ((containerSize * (scale - 1f)) / 2f).coerceAtLeast(0f)
    return offset.coerceIn(-maxOffset, maxOffset)
}

@Composable
private fun FullscreenScreenshotOverlay(
    screenshotImage: androidx.compose.ui.graphics.ImageBitmap,
    summary: String,
    onDismiss: () -> Unit
) {
    var zoomScale by remember { mutableStateOf(1f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
    ) {
        Image(
            bitmap = screenshotImage,
            contentDescription = "Expanded remote screenshot preview",
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 20.dp)
                .onSizeChanged { containerSize = it }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val updatedScale = (zoomScale * zoom).coerceIn(1f, 6f)
                        if (updatedScale == 1f) {
                            zoomScale = 1f
                            panX = 0f
                            panY = 0f
                        } else {
                            zoomScale = updatedScale
                            panX = clampPan(panX + pan.x, containerSize.width, updatedScale)
                            panY = clampPan(panY + pan.y, containerSize.height, updatedScale)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (zoomScale > 1f) {
                                zoomScale = 1f
                                panX = 0f
                                panY = 0f
                            } else {
                                zoomScale = 2.25f
                            }
                        }
                    )
                }
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panX
                    translationY = panY
                },
            contentScale = ContentScale.Fit
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close screenshot",
                tint = Color.White
            )
        }

        Text(
            text = "$summary\nPinch to zoom, drag to pan, double tap to reset",
            style = MaterialTheme.typography.caption3,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}

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
    val screenshotImageOptions = remember {
        RemoteImageOptions.watchPreview(maxDimensionPx = 1080, jpegQuality = 82)
    }
    val forgeClient = remember(remoteHostIp, remotePort) { 
        ForgeClient(host = remoteHostIp, port = remotePort.toIntOrNull() ?: 1235) 
    }
    var statusText by remember { mutableStateOf("Ready") }
    var isExecuting by remember { mutableStateOf(false) }
    var latestScreenshot by remember { mutableStateOf<ForgeResponse?>(null) }
    var isFullscreenPreviewOpen by remember { mutableStateOf(false) }
    val screenshotPreview = remember(latestScreenshot?.bytes) {
        latestScreenshot?.bytes?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val executeCommand: (String, String) -> Unit = { command, payloadJson ->
        if (!isExecuting) {
            coroutineScope.launch {
                isExecuting = true
                statusText = "Running $command..."
                val imageOptions = if (command == "screenshot") screenshotImageOptions else null
                runCatching {
                    if (payloadJson.isBlank()) {
                        forgeClient.execute(command = command, imageOptions = imageOptions)
                    } else {
                        forgeClient.executeJson(
                            command = command,
                            payloadJson = payloadJson,
                            imageOptions = imageOptions
                        )
                    }
                }.onSuccess { response ->
                    if (response.success) {
                        if (command == "screenshot") {
                            latestScreenshot = response.takeIf { it.bytes != null }
                            statusText = if (response.bytes != null) {
                                screenshotSummary(response)
                            } else {
                                "No image returned"
                            }
                        } else {
                            statusText = "Success"
                        }
                    } else {
                        statusText = "Error: ${response.code}"
                    }
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
                val statusColor = when {
                    statusText.startsWith("Error") || statusText == "Failed" || statusText == "No image returned" ->
                        Color(0xFFFF8A80)
                    statusText.contains("KB") || statusText.contains("MB") || statusText == "Success" ->
                        colors.primary
                    else -> colors.userText
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.caption2,
                    color = statusColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            item {
                val screenshotResponse = latestScreenshot
                if (screenshotPreview != null && screenshotResponse != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Latest Screenshot",
                            style = MaterialTheme.typography.caption2,
                            color = colors.botText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.primary.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                                .clickable { isFullscreenPreviewOpen = true }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = screenshotPreview,
                                contentDescription = "Remote screenshot preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${screenshotSummary(screenshotResponse)}\nTap image to expand",
                            style = MaterialTheme.typography.caption3,
                            color = colors.userText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
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

    val fullscreenScreenshot = latestScreenshot
    if (isFullscreenPreviewOpen && screenshotPreview != null && fullscreenScreenshot != null) {
        Dialog(
            onDismissRequest = { isFullscreenPreviewOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FullscreenScreenshotOverlay(
                screenshotImage = screenshotPreview,
                summary = screenshotSummary(fullscreenScreenshot),
                onDismiss = { isFullscreenPreviewOpen = false }
            )
        }
    }
}
