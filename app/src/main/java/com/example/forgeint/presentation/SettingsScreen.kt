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
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivateConnectivity
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.derivedStateOf
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListState
import androidx.wear.compose.material.rememberScalingLazyListState
import androidx.wear.compose.material.PositionIndicator
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
import com.example.weargemini.data.SettingsManager
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.wear.compose.material.CompactChip
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Memory
import com.example.forgeint.presentation.theme.LocalForgeIntColors
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.scrollBy
import androidx.wear.compose.material.items as materialItems


@Composable
fun SettingsScreen(
    isLiteMode: Boolean,
    isVoiceDominantMode: Boolean,
    currentModelId: String,
    allPersonas: List<Persona>,
    onToggleLite: (Boolean) -> Unit,
    onToggleVoiceDominant: (Boolean) -> Unit,
    onNavigateToModelSelect: () -> Unit,
    onNavigateToPersona: () -> Unit,
    onNavigateToIP: () -> Unit,
    onNavigateToLocalModel: () -> Unit,
    onNavigateToNeuralNetwork: () -> Unit,
    onNavigateToSystem: () -> Unit,
    onNavigateToMessageLength: () -> Unit,
    onNavigateToApiKey: () -> Unit,

    onNavigateToTheme: () -> Unit,
    onNavigateToLocalStatus: () -> Unit,
    onNavigateToHardwareMonitor: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val personaId by settingsManager.selectedPersonaId.collectAsState(initial = "default")
    val modelId by settingsManager.selectedModel.collectAsState(initial = "google/gemma-3n-e4b-it:free")
    val connectionTypeID by settingsManager.isLocalEnabled.collectAsState(initial = false)
    val messageLength by settingsManager.messageLength.collectAsState(initial = "Normal")
    val currentTheme by settingsManager.appTheme.collectAsState(initial = "Default")
    val colors = LocalForgeIntColors.current

    val connectionType = if (connectionTypeID) "Local Server" else "Openrouter API"
    val currentModelName = remember(modelId) {
        when (modelId) {
            "google/gemma-3n-e4b-it:free" -> "Gemma E4B"
            "google/gemma-3-27b-it:free" -> "Gemma 3.27B"
            "google/gemma-2-9b-it:free" -> "Gemma 3.9B"
            "google/gemma-3n-e2b-it:free" -> "Gemma E2B"
            "local/gemma-270m" -> "Gemma 270M (local)"
            "qwen/qwen3-4b:free" -> "Qwen 4B"
            "meta-llama/llama-3.3-70b-instruct:free" -> "Llama 3.3 70B"
            "openai/gpt-oss-120b:free" -> "GPT OSS 120B"


            "openai/gpt-oss-20b:free" -> "GPT OSS 20B"
            "cognitivecomputations/dolphin-mistral-24b-venice-edition:free" -> "Dolphin Mistral 24B"
            else -> "Unknown Model"


        }
    }
    val currentPersonaName = remember(personaId, allPersonas) {
        allPersonas.find { it.id == personaId }?.name ?: Personas.findById(personaId).name
    }

    val focusRequester = remember { FocusRequester() }

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

            // 1. Lite Mode Toggle
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

            // 2. Model Selection Button
            item {
                Chip(
                    onClick = onNavigateToModelSelect,
                    label = { Text("AI Model", color = colors.botText) },
                    secondaryLabel = {
                        Text(
                            currentModelName,
                            maxLines = 1,
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
            item {
                Chip(
                    onClick = onNavigateToPersona,
                    label = { Text("AI Persona", color = colors.botText) },
                    secondaryLabel = {
                        Text(
                            currentPersonaName,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.userText
                        )
                    },
                    icon = { Icon(Icons.Default.Person, "Persona", tint = colors.replyIcon) },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble,
                        endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Chip(
                    onClick = onNavigateToMessageLength,
                    label = { Text("Response Length", color = colors.botText) },
                    secondaryLabel = {
                        Text(
                            messageLength,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.userText
                        )
                    },
                    icon = { Icon(Icons.Default.Settings, "Response Length", tint = colors.primary) },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble,
                        endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Chip(
                    onClick = onNavigateToApiKey,
                    label = { Text("API Key", color = colors.botText) },
                    secondaryLabel = {
                        Text(
                            "Manage Custom API Key",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.userText
                        )
                    },
                    icon = { Icon(Icons.Default.PrivateConnectivity, "API Key", tint = colors.settingsIcon) },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble,
                        endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Chip(
                    onClick = onNavigateToIP,
                    label = { Text("Connection Type", color = colors.botText) },
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
            item {
                Chip(
                    onClick = onNavigateToSystem,
                    label = { Text("System Monitor", color = colors.botText) },
                    secondaryLabel = {
                        Text(
                            "Monitor RAM and Thermals",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.userText
                        )
                    },
                    icon = { Icon(Icons.Default.DeviceThermostat, "System Monitor", tint = colors.primary) },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble,
                        endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Chip(
                    onClick = onNavigateToHardwareMonitor,
                    label = { Text("Local Hardware Monitor", color = colors.botText) },
                    secondaryLabel = {
                        Text(
                            "Monitor PC Stats",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.userText
                        )
                    },
                    icon = { Icon(Icons.Default.Memory, "Hardware Monitor", tint = colors.primary) },
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

fun MessageLengthSelectionScreen(
    selectedLength: String,
    onLengthSelected: (String) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val lengths = remember { listOf("Shorter", "Normal", "Longer") }
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
                    "Response Length",
                    style = MaterialTheme.typography.caption1,
                    color = colors.onPrimary
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
fun SystemMonitorSettings(
    onToggleMemory: () -> Unit,
    onToggleThermal: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val memory_monitor by settingsManager.isMemoryMonitorEnabled.collectAsState(false)
    val thermal_monitor by settingsManager.isSystemTelemetryEnabled.collectAsState(false)
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
    val isCustomKeyEnabled by settingsManager.isCustomApiKeyEnabled.collectAsState(false)
    val currentApiKey by settingsManager.apiKey.collectAsState("")
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
    availableModels: List<String> = emptyList(),
    isLocalEnabled: Boolean = false
) {
    val listState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var selectedCompany by remember { mutableStateOf("All") }
    val companies = remember {
        listOf(
            "All", "Google", "Meta", "Alibaba", "OpenAI",
            "Z-AI", "Mistral", "Deepseek", "Nvidia", "Local"
        )
    }

    val defaultModels = remember {
        listOf(
            AiModel("google/gemma-3n-e4b-it:free", "Gemma 3N E4B", "Multimodal: Video, Audio & Mobile efficiency"),
            AiModel("qwen/qwen3-4b:free", "Qwen 4B", "High-speed reasoning & Multilingual coding"),
            AiModel("z-ai/glm-4.5-air:free", "GLM 4.5", "Agentic workflows & Native tool-calling"),
            AiModel("meta-llama/llama-3.1-405b-instruct:free", "Llama 3.1 405B", "Frontier-level intelligence & massive scale reasoning"),
            AiModel("meta-llama/llama-3.3-70b-instruct:free", "Llama 3.3 70B", "Complex instruction following & Reasoning"),
            AiModel("google/gemma-3-27b-it:free", "Gemma 3 27B", "Dense Vision-Language & Deep logic"),
            AiModel("google/gemma-2-9b-it:free", "Gemma 2.9B", "Balanced performance for general tasks"),
            AiModel("google/gemma-3n-e2b-it:free", "Gemma 3N E2B", "Ultra-lightweight multimodal for edge devices"),
            AiModel("openai/gpt-oss-120b:free", "GPT OSS 120B", "Frontier reasoning & Advanced web browsing"),
            AiModel("openai/gpt-oss-20b:free", "GPT OSS 20B", "Fast local reasoning & STEM specialist"),
            AiModel("cognitivecomputations/dolphin-mistral-24b-venice-edition:free", "Dolphin Mistral 24B", "Uncensored roleplay & Multi-turn logic"),
            AiModel("tngtech/deepseek-r1t2-chimera:free", "Deepseek Chimera", "Reasoning-focused merge: Advanced logic, coding & creative roleplay"),
            AiModel("nvidia/llama-3.1-nemotron-70b-instruct:free", "Nemotron 70B", "Extremely helpful response style & high-quality formatting"),
        )
    }

    val activeModels = remember(availableModels, isLocalEnabled) {
        if (isLocalEnabled) {
            if (availableModels.isNotEmpty()) {
                availableModels.map { id ->
                    AiModel(id, id.replace("-", " ").uppercase(), "Local Server Model")
                }
            } else {
                emptyList()
            }
        } else {
            defaultModels
        }
    }
    val colors = LocalForgeIntColors.current
    val filteredModels = remember(selectedCompany, activeModels) {
        if (selectedCompany == "All") activeModels
        else activeModels.filter { model ->
            when (selectedCompany) {
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

            if (activeModels.isEmpty() && isLocalEnabled) {
                item {
                    Text(
                        "No models found.\nCheck connection settings.",
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
