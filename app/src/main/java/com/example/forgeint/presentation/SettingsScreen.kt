package com.example.forgeint.presentation
import Persona
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BroadcastOnPersonal
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.Alignment
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.RadioButton
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Science
import com.example.forgeint.presentation.theme.LocalForgeIntColors
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.scrollBy
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SliderDefaults

fun formatModelDisplayName(modelId: String): String {
    val rawName = modelId.substringAfter('/', "").substringBefore(':')
    if (rawName.isBlank()) return modelId.ifBlank { "Unknown model" }

    val hiddenTokens = setOf("instruct", "chat", "preview", "latest", "it")
    val tokens = rawName
        .split('-', '_')
        .filter { it.isNotBlank() && it.lowercase() !in hiddenTokens }

    if (tokens.isEmpty()) return modelId

    return tokens.joinToString(" ") { token ->
        when {
            token.all { it.isDigit() || it == '.' } -> token
            token.length <= 3 -> token.uppercase()
            else -> token.replaceFirstChar { c ->
                if (c.isLowerCase()) c.titlecase() else c.toString()
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val colors = LocalForgeIntColors.current
    Text(
        text = title,
        style = MaterialTheme.typography.caption1,
        color = colors.settingsIcon,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}


@Composable
fun SettingsScreen(
    isLiteMode: Boolean,
    isVoiceDominantMode: Boolean,
    currentModelId: String,
    allPersonas: List<Persona>,
    onToggleLite: (Boolean) -> Unit,
    onToggleVoiceDominant: (Boolean) -> Unit,
    onNavigateToModelSettings: () -> Unit,
    onNavigateToIP: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToLocalStatus: () -> Unit,
    onNavigateToExperimental: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val personaId by settingsManager.selectedPersonaId.collectAsStateWithLifecycle(initialValue = "default")
    val modelId by settingsManager.selectedModel.collectAsStateWithLifecycle(initialValue = SettingsManager.DEFAULT_CLOUD_MODEL_ID)
    val connectionTypeID by settingsManager.isLocalEnabled.collectAsStateWithLifecycle(initialValue = false)
    val isFunnelEnabled by settingsManager.isFunnelEnabled.collectAsStateWithLifecycle(initialValue = false)
    val messageLength by settingsManager.messageLength.collectAsStateWithLifecycle(initialValue = "Normal")
    val currentTheme by settingsManager.appTheme.collectAsStateWithLifecycle(initialValue = "Default")
    val isAutoPowerSavingMode by settingsManager.isAutoPowerSavingMode.collectAsStateWithLifecycle(initialValue = false)
    val colors = LocalForgeIntColors.current

    val connectionType = when {
        connectionTypeID && isFunnelEnabled -> "Local Server (Funnel)"
        connectionTypeID -> "Local Server"
        else -> "Openrouter API"
    }
    val currentModelName = remember(modelId) { formatModelDisplayName(modelId) }
    val currentPersonaName = remember(personaId, allPersonas) {
        allPersonas.find { it.id == personaId }?.name ?: Personas.findById(personaId).name
    }

    val focusRequester = remember { FocusRequester() }
    val autoPowerThreshold by settingsManager.isAutoPowerSavingModeThreshold.collectAsStateWithLifecycle(initialValue = 20f)
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Settings", style = MaterialTheme.typography.title3, color = colors.botText)
            }

            item { SettingsSectionHeader("Power & Behavior") }
            item {
                ToggleChip(
                    checked = isLiteMode,
                    onCheckedChange = onToggleLite,
                    label = { Text("Power Saving Mode") },
                    secondaryLabel = { Text(if (isLiteMode) "On" else "Off") },
                    toggleControl = { Switch(checked = isLiteMode, onCheckedChange = null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ToggleChipDefaults.toggleChipColors(
                        checkedStartBackgroundColor = Color(0xFF081A36),
                        checkedEndBackgroundColor = Color(0xFF5C4D00),
                        uncheckedStartBackgroundColor = colors.surface,
                        uncheckedEndBackgroundColor = colors.surface

                    )
                )

            }

            item {

                ToggleChip(
                    checked = isAutoPowerSavingMode,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            settingsManager.setAutoPowerSavingMode(enabled)
                        }
                    },
                    label = { Text("Auto Power Saving") },
                    secondaryLabel = {
                        Text(
                            if (isAutoPowerSavingMode) {
                                "Below $autoPowerThreshold%: enable power saving + OLED"
                            } else {
                                "Trigger battery saver automatically"
                            }
                        )
                    },
                    toggleControl = {
                        Switch(checked = isAutoPowerSavingMode, onCheckedChange = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ToggleChipDefaults.toggleChipColors(
                        checkedStartBackgroundColor = Color(0xFF102300),
                        checkedEndBackgroundColor = Color(0xFF3E5200),
                        uncheckedStartBackgroundColor = colors.surface,
                        uncheckedEndBackgroundColor = colors.surface
                    )
                )

            }
            item {
                Slider(
                    value = autoPowerThreshold,
                    onValueChange = { newValue ->
                        coroutineScope.launch {
                            settingsManager.setAutoPowerSavingModeThreshold(newValue)
                        }
                    },
                    steps = 9,
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.sliderColors(
                        selectedBarColor =  Color(0xFF0060FF),
                        unselectedBarColor = Color(0xFF000000),
                        selectedBarSeparatorColor = colors.surface



                    )
                )
            }
            // Voice-Dominant Mode Toggle
            item {
                ToggleChip(
                    checked = isVoiceDominantMode,
                    onCheckedChange = onToggleVoiceDominant,
                    label = { Text("Voice-Dominant Mode") },
                    secondaryLabel = { Text(if (isVoiceDominantMode) "On" else "Off") },
                    toggleControl = { Switch(checked = isVoiceDominantMode, onCheckedChange = null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ToggleChipDefaults.toggleChipColors(
                        checkedStartBackgroundColor = Color(0xFF1A3608),
                        checkedEndBackgroundColor = Color(0xFF005C4D),
                        uncheckedStartBackgroundColor = colors.surface,
                        uncheckedEndBackgroundColor = colors.surface
                    )
                )
            }

            item { SettingsSectionHeader("Core AI") }
            item {
                Chip(
                    onClick = onNavigateToModelSettings,
                    label = { Text("Model Settings", color = colors.botText) },
                    secondaryLabel = {
                        Text(
                            "$currentModelName • $currentPersonaName • $messageLength",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.userText
                        )
                    },
                    icon = { Icon(Icons.Default.AutoAwesome, "Model", tint = colors.primary) },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble,
                        endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { SettingsSectionHeader("Appearance") }
            item {
                Chip(
                    onClick = onNavigateToTheme,
                    label = { Text("App Theme", color = colors.botText) },
                    secondaryLabel = {
                        Text(
                            currentTheme,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.userText
                        )
                    },
                    icon = { Icon(Icons.Default.AccountTree, "Theme", tint = colors.settingsIcon) },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble,
                        endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { SettingsSectionHeader("Connection") }
            item {
                Chip(
                    onClick = onNavigateToIP,
                    label = { Text("Connection Settings", color = colors.botText) },
                    secondaryLabel = {
                        Text(
                            connectionType,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.userText
                        )
                    },
                    icon = { Icon(Icons.Default.WifiFind, "Model", tint = colors.replyIcon) },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble,
                        endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
//            item {
//                Chip(
//                    onClick = onNavigateToLocalStatus,
//                    label = { Text("Local Server Status", color = colors.botText) },
//                    secondaryLabel = {
//                        Text(
//                            "Check server health and availability",
//                            maxLines = 1,
//                            overflow = TextOverflow.Ellipsis,
//                            color = colors.userText
//                        )
//                    },
//                    icon = { Icon(Icons.Default.Settings, "Local status", tint = colors.settingsIcon) },
//                    colors = ChipDefaults.gradientBackgroundChipColors(
//                        startBackgroundColor = colors.userBubble,
//                        endBackgroundColor = colors.surface
//                    ),
//                    modifier = Modifier.fillMaxWidth()
//                )
//            }
            item { SettingsSectionHeader("Advanced") }
            item {
                Chip(
                    onClick = onNavigateToExperimental,
                    label = { Text("Experimental Settings", color = colors.botText) },
                    secondaryLabel = {
                        Text(
                            "Hardware monitor, remote control & demos",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.userText
                        )
                    },
                    icon = { Icon(Icons.Default.Science, "Experimental", tint = colors.replyIcon) },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble,
                        endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }





        }
    }
}
@Composable
fun ExperimentalSettingsScreen(
    onNavigateToSystem: () -> Unit,
    onNavigateToHardwareMonitor: () -> Unit,
    onNavigateToRemoteCommand: () -> Unit,
    onNavigateToGradientDescent: () -> Unit,
    onNavigateToNeuralNetwork: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val colors = LocalForgeIntColors.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(positionIndicator = { ScrollIndicator(state = listState) }) {
        TransformingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .onRotaryScrollEvent {
                    coroutineScope.launch { listState.scrollBy(it.verticalScrollPixels * 2f) }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Experimental", style = MaterialTheme.typography.title3, color = colors.botText)
            }
            item { SettingsSectionHeader("Device Monitoring") }
            item {
                Chip(
                    onClick = onNavigateToSystem,
                    label = { Text("System Monitor", color = colors.botText) },
                    secondaryLabel = {
                        Text("Monitor RAM and Thermals", maxLines = 2, overflow = TextOverflow.Ellipsis, color = colors.userText)
                    },
                    icon = { Icon(Icons.Default.DeviceThermostat, "System Monitor", tint = colors.primary) },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble, endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Chip(
                    onClick = onNavigateToHardwareMonitor,
                    label = { Text("Local Hardware Monitor", color = colors.botText) },
                    secondaryLabel = {
                        Text("Monitor PC Stats", maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.userText)
                    },
                    icon = { Icon(Icons.Default.Memory, "Hardware Monitor", tint = colors.primary) },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble, endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { SettingsSectionHeader("Remote Control") }
            item {
                Chip(
                    onClick = onNavigateToRemoteCommand,
                    label = { Text("Remote Control", color = colors.botText) },
                    secondaryLabel = {
                        Text("Send remote commands to the host", maxLines = 2, overflow = TextOverflow.Ellipsis, color = colors.userText)
                    },
                    icon = { Icon(Icons.Default.Computer, "Remote Control", tint = colors.replyIcon) },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble, endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { SettingsSectionHeader("Demos") }
            item {
                Chip(
                    onClick = onNavigateToGradientDescent,
                    label = { Text("Gradient Descent", color = colors.botText) },
                    secondaryLabel = {
                        Text("Canvas Math Demo", maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.userText)
                    },
                    icon = { Icon(Icons.Default.ShowChart, "Gradient Descent", tint = colors.primary) },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble, endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Chip(
                    onClick = onNavigateToNeuralNetwork,
                    label = { Text("Neural Network", color = colors.botText) },
                    secondaryLabel = {
                        Text("Experimental math demo", maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.userText)
                    },
                    icon = { Icon(Icons.Default.BroadcastOnPersonal, "Neural Network", tint = colors.replyIcon) },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble, endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun MessageLengthSelectionScreen(
    selectedLength: String,
    onLengthSelected: (String) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val lengths = remember { listOf("Shorter", "Normal", "Longer") }
    val colors = LocalForgeIntColors.current
    val selectedStartColor = colors.primary.copy(alpha = 0.24f)
    val selectedEndColor = colors.surface.copy(alpha = 0.96f)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Response Length",
                    style = MaterialTheme.typography.caption1,
                    color = colors.botText
                )
            }

            items(lengths, key = { it }) { length ->
                val isSelected = (length == selectedLength)

                ToggleChip(
                    checked = isSelected,
                    onCheckedChange = { onLengthSelected(length) },
                    label = { Text(length) },
                    toggleControl = {
                        RadioButton(selected = isSelected)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ToggleChipDefaults.toggleChipColors(
                        checkedStartBackgroundColor = selectedStartColor,
                        checkedEndBackgroundColor = selectedEndColor,
                        uncheckedStartBackgroundColor = colors.userBubble,
                        uncheckedEndBackgroundColor = colors.surface
                    )
                )
            }
        }
    }
}

@Composable
fun SystemMonitorSettings(
    onToggleMemory: () -> Unit,
    onToggleThermal: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val memory_monitor by settingsManager.isMemoryMonitorEnabled.collectAsStateWithLifecycle(initialValue = false)
    val thermal_monitor by settingsManager.isSystemTelemetryEnabled.collectAsStateWithLifecycle(initialValue = false)
    val colors = LocalForgeIntColors.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Performance Monitors",
                    style = MaterialTheme.typography.caption1,
                    color = colors.userText
                )
            }

            item {
                ToggleChip(
                    checked = memory_monitor,
                    onCheckedChange = { onToggleMemory() },
                    label = { Text("Memory Monitor") },
                    toggleControl = { RadioButton(selected = memory_monitor) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ToggleChipDefaults.toggleChipColors(
                        checkedStartBackgroundColor = Color(0xFF081A36),
                        checkedEndBackgroundColor = Color(0xFF5C4D00),
                        uncheckedStartBackgroundColor = colors.surface,
                        uncheckedEndBackgroundColor = colors.surface
                    )
                )
            }

            item {
                ToggleChip(
                    checked = thermal_monitor,
                    onCheckedChange = { onToggleThermal() },
                    label = { Text("Thermal Monitor") },
                    toggleControl = { RadioButton(selected = thermal_monitor) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ToggleChipDefaults.toggleChipColors(
                        checkedStartBackgroundColor = Color(0xFF081A36),
                        checkedEndBackgroundColor = Color(0xFF5C4D00),
                        uncheckedStartBackgroundColor = colors.surface,
                        uncheckedEndBackgroundColor = colors.surface
                    )
                )
            }
        }
    }
}
@Composable

fun ApiKeySettingsScreen(
    onApiKeyChanged: (String) -> Unit,
    onToggleCustomKey: (Boolean) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val isCustomKeyEnabled by settingsManager.isCustomApiKeyEnabled.collectAsStateWithLifecycle(initialValue = false)
    val currentApiKey by settingsManager.apiKey.collectAsStateWithLifecycle(initialValue = "")
    val colors = LocalForgeIntColors.current

    val launcher = rememberTextInputLauncher(onInputReceived = onApiKeyChanged)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "API Key Settings",
                    style = MaterialTheme.typography.caption1,
                    color = colors.onPrimary
                )
            }

            item {
                ToggleChip(
                    checked = isCustomKeyEnabled,
                    onCheckedChange = onToggleCustomKey,
                    label = { Text("Use Custom API Key") },
                    toggleControl = { Switch(checked = isCustomKeyEnabled, onCheckedChange = null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ToggleChipDefaults.toggleChipColors(
                        checkedStartBackgroundColor = Color(0xFF081A36),
                        checkedEndBackgroundColor = Color(0xFF5C4D00),
                        uncheckedStartBackgroundColor = colors.surface,
                        uncheckedEndBackgroundColor = colors.surface
                    )
                )
            }

            if (isCustomKeyEnabled) {
                item {
                    Chip(
                        onClick = { launcher() },
                        label = { Text("Edit API Key") },
                        secondaryLabel = {
                            Text(
                                if (currentApiKey.isNotEmpty()) "Key Set (Tap to change)" else "No Key Set",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.LightGray
                            )
                        },
                        icon = { Icon(Icons.Default.SaveAlt, "Edit Key") },
                        colors = ChipDefaults.gradientBackgroundChipColors(
                            startBackgroundColor = Color(0xFF335C77),
                            endBackgroundColor = colors.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (currentApiKey.isNotEmpty()) {
                    item {
                        Text(
                            text = "Current Key: ${currentApiKey.take(4)}...${currentApiKey.takeLast(4)}",
                            style = MaterialTheme.typography.caption2,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun ModelSelectionScreen(
    selectedModelId: String,
    onModelSelected: (String) -> Unit,
    availableModels: List<AvailableModelOption> = emptyList(),
    isLocalEnabled: Boolean = false,
    isLoadingModels: Boolean = false
) {
    val listState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var selectedCompany by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    val companies = remember {
        listOf(
            "All", "Google", "Meta", "Alibaba", "OpenAI",
            "Z-AI", "Mistral", "Deepseek", "Nvidia", "Local"
        )
    }

    val activeModels = remember(availableModels, isLocalEnabled) {
        if (isLocalEnabled) {
            availableModels.map { model ->
                AiModel(model.id, model.name, model.description)
            }
        } else {
            availableModels.map { model ->
                AiModel(model.id, model.name, model.description)
            }
        }
    }
    val colors = LocalForgeIntColors.current
    val filteredModels = remember(selectedCompany, activeModels, searchQuery) {
        activeModels
            .filter { model ->
                selectedCompany == "All" || when (selectedCompany) {
                    "Google" -> model.id.startsWith("google")
                    "Meta" -> model.id.startsWith("meta")
                    "Alibaba" -> model.id.startsWith("qwen")
                    "OpenAI" -> model.id.startsWith("openai")
                    "Z-AI" -> model.id.startsWith("z-ai")
                    "Mistral" -> model.id.contains("mistral")
                    "Deepseek" -> model.id.startsWith("tngtech")
                    "Nvidia" -> model.id.startsWith("nvidia")
                    "Local" -> true
                    else -> true
                }
            }
            .filter { model ->
                searchQuery.isBlank() ||
                    model.name.contains(searchQuery, ignoreCase = true) ||
                    model.id.contains(searchQuery, ignoreCase = true) ||
                    model.specialty.contains(searchQuery, ignoreCase = true)
            }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        positionIndicator = { ScrollIndicator(state = listState) },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        TransformingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onRotaryScrollEvent {
                    coroutineScope.launch {
                        listState.scrollBy(it.verticalScrollPixels * 2f)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    if (isLocalEnabled) "Local Models" else "Cloud Models",
                    style = MaterialTheme.typography.caption1,
                    color = Color.Gray
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(20.dp))
                        .border(1.dp, colors.primary.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search models",
                        tint = colors.primary
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.body2.copy(color = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isBlank()) {
                                Text(
                                    text = if (isLocalEnabled) "Search local models" else "Search free OpenRouter models",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.body2
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotBlank()) {
                        Chip(
                            onClick = { searchQuery = "" },
                            label = { Text("Clear") },
                            icon = { Icon(Icons.Default.Close, contentDescription = "Clear search") },
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier.height(34.dp)
                        )
                    }
                }
            }

            if (!isLocalEnabled) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        companies.forEach { company ->
                            val isSelected = selectedCompany == company

                            Chip(
                                onClick = { selectedCompany = company },
                                label = {
                                    Text(
                                        text = company,
                                        style = MaterialTheme.typography.caption2.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        maxLines = 1
                                    )
                                },
                                colors = ChipDefaults.gradientBackgroundChipColors(
                                    startBackgroundColor = if (isSelected) colors.primary else Color(0xFF1C1B1F),
                                    endBackgroundColor = if (isSelected) colors.background else Color(0xFF1C1B1F),
                                    contentColor = if (isSelected) Color.White else Color(0xFFCAC4D0)
                                ),
                                modifier = Modifier
                                    .height(32.dp)
                                    .then(
                                        if (!isSelected) {
                                            Modifier.border(
                                                width = 1.dp,
                                                color = Color(0xFF49454F),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                        } else Modifier
                                    ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }

            if (isLoadingModels) {
                item {
                    Text(
                        if (isLocalEnabled) "Loading local models..." else "Fetching free OpenRouter models...",
                        style = MaterialTheme.typography.body2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (activeModels.isEmpty() && !isLoadingModels) {
                item {
                    Text(
                        if (isLocalEnabled) {
                            "No models found.\nCheck connection settings."
                        } else {
                            "No free OpenRouter models found."
                        },
                        style = MaterialTheme.typography.body2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (filteredModels.isEmpty() && activeModels.isNotEmpty()) {
                item {
                    Text(
                        if (searchQuery.isBlank()) {
                            "No models match the selected filter."
                        } else {
                            "No models match \"$searchQuery\"."
                        },
                        style = MaterialTheme.typography.body2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            items(filteredModels, key = { it.id }) { model ->
                val isSelected = (model.id == selectedModelId)

                ToggleChip(
                    checked = isSelected,
                    onCheckedChange = { onModelSelected(model.id) },
                    label = {
                        Text(
                            text = model.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = model.specialty,
                            style = MaterialTheme.typography.caption2,
                            color = if (isSelected) Color.LightGray else Color.Gray,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    toggleControl = {
                        RadioButton(selected = isSelected)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ToggleChipDefaults.toggleChipColors(
                        checkedStartBackgroundColor = colors.botBubble,
                        checkedEndBackgroundColor = colors.background,
                        uncheckedStartBackgroundColor = Color.Black,
                        uncheckedEndBackgroundColor = Color.Black
                    )
                )
            }
        }
    }
}

@Composable
fun ThemeSelectionScreen(
    currentTheme: String,
    onThemeSelected: (String) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val themes = remember {
        listOf(
            "Default", "Cyberpunk", "Crimson", "Sunset", "Dark Purple",
            "Midnight Blue", "Forest Deep", "Slate Gray", "Royal Gold",
            "Neon Violet", "Nature", "Minimal", "OLED", "Matrix",
            "Solarized", "Cotton Candy", "High Contrast",
            "Ocean Breeze", "Volcanic Ash", "Mint Chocolate", "Electric Indigo",
            "Desert Sand", "Cosmic Nebula", "Autumn Leaves", "Glacier White",
            "Abyssal Depths", "Retro 80s"
        )
    }

    val colors = LocalForgeIntColors.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "App Theme",
                    style = MaterialTheme.typography.caption1,
                    color = colors.botText
                )
            }

            items(themes, key = { it }) { themeName ->
                val isSelected = (themeName == currentTheme)
                val colorPreview = when (themeName) {
                    "Default" -> Color(0xFFDCB17E)
                    "Cyberpunk" -> Color(0xFF00FF9D)
                    "Nature" -> Color(0xFF81C784)
                    "Minimal" -> Color.White
                    "OLED" -> Color(0xFFFFD700)
                    "Matrix" -> Color(0xFF00FF41)
                    "Solarized" -> Color(0xFF268BD2)
                    "Cotton Candy" -> Color(0xFFFFB7B2)
                    "High Contrast" -> Color.White
                    "Crimson" -> Color(0xFFDC143C)
                    "Sunset" -> Color(0xFFFF7E5F)
                    "Dark Purple" -> Color(0xFF9D46FF)
                    "Midnight Blue" -> Color(0xFF4C8BF5)
                    "Forest Deep" -> Color(0xFF2E7D32)
                    "Slate Gray" -> Color(0xFF90A4AE)
                    "Royal Gold" -> Color(0xFFFFD700)
                    "Neon Violet" -> Color(0xFFD500F9)
                    "Ocean Breeze" -> Color(0xFF64FFDA)
                    "Volcanic Ash" -> Color(0xFFFF3D00)
                    "Mint Chocolate" -> Color(0xFF98FF98)
                    "Electric Indigo" -> Color(0xFF6600FF)
                    "Desert Sand" -> Color(0xFFE4A010)
                    "Cosmic Nebula" -> Color(0xFFFF007F)
                    "Autumn Leaves" -> Color(0xFFFF5722)
                    "Glacier White" -> Color(0xFFE0F7FA)
                    "Abyssal Depths" -> Color(0xFF00FFCC)
                    "Retro 80s" -> Color(0xFFFF00FF)
                    else -> Color.Gray
                }

                ToggleChip(
                    checked = isSelected,
                    onCheckedChange = { onThemeSelected(themeName) },
                    label = {
                        Text(
                            text = themeName,
                            color = colors.botText
                        )
                    },
                    toggleControl = {
                        RadioButton(selected = isSelected)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ToggleChipDefaults.toggleChipColors(
                        checkedStartBackgroundColor = colorPreview.copy(alpha = 0.2f),
                        checkedEndBackgroundColor = colors.background,
                        uncheckedStartBackgroundColor = colors.surface,
                        uncheckedEndBackgroundColor = colors.surface,
                        checkedContentColor = colorPreview,
                        uncheckedContentColor = colors.botText,
                        checkedToggleControlColor = colorPreview,
                        uncheckedToggleControlColor = colors.botText.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}




data class AiModel(
    val id: String,
    val name: String,
    val specialty: String
)


data class ModelQuota(
                val modelId: String,
                val dailyLimit: Int
            )

            val modelQuotas = listOf(
                ModelQuota("gemini-2.5-flash", 1500),
                ModelQuota("gemini-2.5-flash-lite", 1500),
                ModelQuota("gemini-2.5-pro", 25),
                ModelQuota("gemini-3-flash-preview", 1500)
            )


