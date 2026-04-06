package com.example.forgeint.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.forgeint.mlmath.GradientDescentCanvasDemo
import com.example.forgeint.presentation.theme.ForgeINTTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {
            val intent = intent
            GeminiApp(intent = intent)
        }
    }
}

@Composable
fun GeminiApp(viewModel: GeminiViewModel = viewModel(), intent: android.content.Intent? = null) {

    val navController = rememberSwipeDismissableNavController()
    val isLiteMode by viewModel.isLiteMode.collectAsStateWithLifecycle()
    val isVoiceDominantMode by viewModel.isVoiceDominantMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val haptics = LocalHapticFeedback.current
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val autoPowerSavingMode by settingsManager.isAutoPowerSavingMode.collectAsStateWithLifecycle(initialValue = false)
    val autoPowerSavingPreviousTheme by settingsManager.autoPowerSavingPreviousTheme.collectAsStateWithLifecycle(initialValue = "")
    val isAutoPowerSavingActive by settingsManager.isAutoPowerSavingActive.collectAsStateWithLifecycle(initialValue = false)
    var batteryPct by remember { mutableIntStateOf(100) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                batteryPct = if (level >= 0 && scale > 0) {
                    ((level * 100f) / scale).toInt()
                } else {
                    batteryPct
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = context.registerReceiver(receiver, filter)
        if (stickyIntent != null) {
            val level = stickyIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = stickyIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                batteryPct = ((level * 100f) / scale).toInt()
            }
        }

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
    val threshold by settingsManager.isAutoPowerSavingModeThreshold.collectAsStateWithLifecycle(initialValue = 20f)
    LaunchedEffect(
        autoPowerSavingMode,
        batteryPct,
        isLiteMode,
        appTheme,
        autoPowerSavingPreviousTheme,
        isAutoPowerSavingActive
    ) {
        if (!autoPowerSavingMode) return@LaunchedEffect

        if (batteryPct <= threshold) {
            if (!isAutoPowerSavingActive) {
                settingsManager.setAutoPowerSavingPreviousTheme(appTheme)
                settingsManager.setAutoPowerSavingActive(true)
            }
            if (!isLiteMode) {
                viewModel.setLiteMode(true)
            }
            if (appTheme != "OLED") {
                viewModel.setTheme("OLED")
            }
        } else if (isAutoPowerSavingActive) {
            if (isLiteMode) {
                viewModel.setLiteMode(false)
            }
            if (autoPowerSavingPreviousTheme.isNotBlank() && appTheme != autoPowerSavingPreviousTheme) {
                viewModel.setTheme(autoPowerSavingPreviousTheme)
            }
            settingsManager.clearAutoPowerSavingPreviousTheme()
            settingsManager.setAutoPowerSavingActive(false)
        }
    }

    LaunchedEffect(intent) {
        intent?.getStringExtra("action_type")?.let {
            viewModel.setPendingAction(it)
        }
    }

    ForgeINTTheme(themeName = appTheme) {
        SwipeDismissableNavHost(navController = navController, startDestination = "history") {

            // ---------------------------------------------------------
            // SCREEN 1: HISTORY (MAIN MENU)
            // ---------------------------------------------------------
            composable("history") {
                val history by viewModel.history.collectAsStateWithLifecycle()
                val pendingAction by viewModel.pendingAction.collectAsStateWithLifecycle()

                val textLauncher = rememberTextInputLauncher { text ->
                    viewModel.startNewChat(text)
                    navController.navigate("chat")
                }

                val voiceLauncher = rememberVoiceLauncher { text ->
                    viewModel.startNewChat(text)
                    navController.navigate("chat")
                }

                val voiceChatLauncher: () -> Unit = remember(navController, viewModel) {
                    {
                        viewModel.startVoiceChat()
                        navController.navigate("voice_chat")
                    }
                }

                val newChatLauncher: () -> Unit = remember(isVoiceDominantMode, navController, viewModel, voiceLauncher, voiceChatLauncher) {
                    {
                        if (isVoiceDominantMode) {
                            voiceChatLauncher.invoke()
                        } else {
                            viewModel.startEmptyChat()
                            navController.navigate("chat")
                        }
                    }
                }
                val onSettingsClick: () -> Unit = remember(navController) { { navController.navigate("settings") } }
                val onLiteChatClick: (Long) -> Unit = remember(navController, viewModel) {
                    { id: Long ->
                        viewModel.openChat(id)
                        convoID.conID.value = id.toInt()
                        navController.navigate("chat")
                    }
                }
                val onLiteBookmarkClick: (Long, Boolean) -> Unit = remember(viewModel) {
                    { id: Long, status: Boolean -> viewModel.toggleBookmark(id, status) }
                }
                val onDetailedChatClick: (Long) -> Unit = remember(navController, viewModel, haptics) {
                    { id: Long ->
                        viewModel.openChat(id)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        convoID.conID.value = id.toInt()
                        navController.navigate("chat")
                    }
                }
                val onDetailedBookmarkClick: (Long, Boolean) -> Unit = remember(viewModel, haptics) {
                    { id: Long, status: Boolean ->
                        viewModel.toggleBookmark(id, status)
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    }
                }
                val onDetailedDeleteClick: (Long) -> Unit = remember(viewModel, haptics) {
                    { id: Long ->
                        viewModel.deleteChat(id)
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    }
                }
                val onNavigateToMemory: () -> Unit = remember(navController, haptics) {
                    {
                        navController.navigate("memory")
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                val onSearchQueryChanged: (String) -> Unit = remember(viewModel) {
                    { query: String -> viewModel.onSearchQueryChanged(query) }
                }

                LaunchedEffect(pendingAction) {
                    if (pendingAction == "voice") {
                        voiceChatLauncher.invoke()
                        viewModel.clearPendingAction()
                    } else if (pendingAction == "text") {
                        textLauncher.invoke()
                        viewModel.clearPendingAction()
                    }
                }

                if (isLiteMode) {
                    LiteHistoryScreen(
                        history = history,
                        onNewChatClick = newChatLauncher,
                        onChatClick = onLiteChatClick,
                        onBookmarkClick = onLiteBookmarkClick,
                        onSettingsClick = onSettingsClick,
                        isVoiceDominant = isVoiceDominantMode
                    )
                } else {
                    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
                    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                    DetailedHistoryScreen(
                        searchResults = searchResults,
                        onNewChatClick = newChatLauncher,
                        onChatClick = onDetailedChatClick,
                        onBookmarkClick = onDetailedBookmarkClick,
                        onSettingsClick = onSettingsClick,
                        onDeleteClick = onDetailedDeleteClick,
                        onNavigateToMemory = onNavigateToMemory,
                        searchQuery = searchQuery,
                        onSearchQueryChanged = onSearchQueryChanged,
                        viewModel = viewModel,
                        isVoiceDominant = isVoiceDominantMode
                    )
                }
            }


            composable("voice_chat") {
                val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                val isWebSearching by viewModel.isWebSearching.collectAsStateWithLifecycle()

                VoiceChatScreen(
                    messages = messages,
                    streamingMessageFlow = viewModel.streamingMessage,
                    isThinkingFlow = viewModel.isThinking,
                    isGenerating = isLoading,
                    isWebSearching = isWebSearching,
                    onVoiceResult = viewModel::submitVoiceInput,
                    onStopResponse = viewModel::stopResponse,
                    onExit = { navController.popBackStack() }
                )
            }

            composable("chat") {
                val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                val isWebSearchEnabled by viewModel.isWebSearchEnabled.collectAsStateWithLifecycle()
                val isWebSearching by viewModel.isWebSearching.collectAsStateWithLifecycle()
                val textLauncher = rememberTextInputLauncher { text ->
                    viewModel.continueChat(text)
                }
                val onDeleteChat: () -> Unit = remember(navController, viewModel, haptics) {
                    {
                        viewModel.deleteChat(id = convoID.conID.value.toLong())
                        navController.popBackStack()
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    }
                }
                val onSwapModel: () -> Unit = remember(navController, haptics) {
                    {
                        navController.navigate("model_select")
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                val onToggleWebSearch: () -> Unit = remember(viewModel, haptics) {
                    {
                        viewModel.toggleWebSearch()
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                val onVoiceResult: (String) -> Unit = remember(viewModel, haptics) {
                    { spokenText: String ->
                        viewModel.continueChat(spokenText)
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    }
                }
                val onBookmarkChat: (Long, Boolean) -> Unit = remember(viewModel, haptics) {
                    { id: Long, status: Boolean ->
                        viewModel.toggleBookmark(id, status)
                        convoID.condBK.value = status
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                val onLoadMoreMessages: () -> Unit = remember(viewModel) { { viewModel.loadNextPage() } }
                val onTryAgain: () -> Unit = remember(viewModel) { { viewModel.continueChat("Try again") } }


                if (isLiteMode) {
                    LiteChatScreen(
                        messages = messages,
                        streamingMessageFlow = viewModel.streamingMessage,
                        isTyping = isLoading,
                        isVoiceDominant = isVoiceDominantMode,
                        isWebSearchEnabled = isWebSearchEnabled,
                        isWebSearching = isWebSearching,
                        onReplyClick = textLauncher,
                        onDeleteClick = onDeleteChat,
                        onSwapModelClick = onSwapModel,
                        onWebsearchClick = onToggleWebSearch,
                        onVoiceResult = onVoiceResult,
                        onStopResponse = viewModel::stopResponse
                    )
                } else {

                    DetailedChatScreen(
                        messages = messages,

                        streamingMessageFlow = viewModel.streamingMessage,
                        isThinkingFlow = viewModel.isThinking,
                        loadingBurstFlow = viewModel.loadingBurst,
                        isGenerating = isLoading,


                        currentConversationId = convoID.conID.value.toLong(),


                        isBookmarked = convoID.condBK.value,
                        isVoiceDominant = isVoiceDominantMode,
                        isWebSearchEnabled = isWebSearchEnabled,
                        isWebSearching = isWebSearching,


                        onReplyClick = textLauncher,
                        onDeleteClick = onDeleteChat,
                        onSwapModelClick = onSwapModel,
                        onBookmarkClick = onBookmarkChat,
                        loadMoreMessages = onLoadMoreMessages,
                        isHistoryLoading = viewModel.isHistoryLoading.collectAsStateWithLifecycle().value,
                        onVoiceResult = onVoiceResult,
                        onTryAgain = onTryAgain,
                        onWebsearchClick = onToggleWebSearch,
                        onStopResponse = viewModel::stopResponse
                    )

                }
            }

            // ---------------------------------------------------------
            // SCREEN 3: SETTINGS
            // ---------------------------------------------------------
            composable("settings") {
                val currentModel by viewModel.currentModelId.collectAsStateWithLifecycle()
                val allPersonas by viewModel.allPersonas.collectAsStateWithLifecycle()
                val onToggleLite: (Boolean) -> Unit = remember(viewModel) { { enabled: Boolean -> viewModel.setLiteMode(enabled); Unit } }
                val onToggleVoiceDominant: (Boolean) -> Unit = remember(viewModel) { { enabled: Boolean -> viewModel.setVoiceDominantMode(enabled); Unit } }
                val onNavigateToModelSettings: () -> Unit = remember(navController) { { navController.navigate("model_settings") } }
                val onNavigateToIP: () -> Unit = remember(navController) { { navController.navigate("ip_select") } }
                val onNavigateToTheme: () -> Unit = remember(navController) { { navController.navigate("theme_select") } }
                val onNavigateToLocalStatus: () -> Unit = remember(navController) { { navController.navigate("local_status") } }
                val onNavigateToExperimental: () -> Unit = remember(navController) { { navController.navigate("experimental_settings") } }
                SettingsScreen(
                    isLiteMode = isLiteMode,
                    isVoiceDominantMode = isVoiceDominantMode,
                    currentModelId = currentModel,
                    allPersonas = allPersonas,
                    onToggleLite = onToggleLite,
                    onToggleVoiceDominant = onToggleVoiceDominant,
                    onNavigateToModelSettings = onNavigateToModelSettings,
                    onNavigateToIP = onNavigateToIP,
                    onNavigateToTheme = onNavigateToTheme,
                    onNavigateToLocalStatus = onNavigateToLocalStatus,
                    onNavigateToExperimental = onNavigateToExperimental,
                )
            }
            composable("remote_ip_select") {
                val remoteHost by viewModel.remoteHostIp.collectAsStateWithLifecycle()
                val remotePort by viewModel.remotePort.collectAsStateWithLifecycle()
                RemoteControlIPSelectionScreen(
                    currentHost = remoteHost,
                    currentPort = remotePort,
                    onHostChange = { viewModel.updateRemoteHostIp(it) },
                    onPortChange = { viewModel.updateRemotePort(it) }
                )
            }
            composable("model_settings") {
                val currentModel by viewModel.currentModelId.collectAsStateWithLifecycle()
                val messageLength by viewModel.messageLength.collectAsStateWithLifecycle()
                val allPersonas by viewModel.allPersonas.collectAsStateWithLifecycle()
                val settingsManager = remember { SettingsManager(context) }
                val selectedPersonaId by settingsManager.selectedPersonaId.collectAsStateWithLifecycle(initialValue = "default")
                val personaName = remember(selectedPersonaId, allPersonas) {
                    allPersonas.find { it.id == selectedPersonaId }?.name ?: Personas.findById(selectedPersonaId).name
                }
                ModelSettingsScreen(
                    currentModelName = formatModelDisplayName(currentModel),
                    currentPersonaName = personaName,
                    currentMessageLength = messageLength,
                    onNavigateToModelSelect = { navController.navigate("model_select") },
                    onNavigateToPersona = { navController.navigate("persona_select") },
                    onNavigateToMessageLength = { navController.navigate("message_length_select") },
                    onNavigateToApiKey = { navController.navigate("api_key") }
                )
            }
            composable("hardware_monitor") {
                val hardwareHost by viewModel.hardwareHostIp.collectAsStateWithLifecycle()
                val hardwarePort by viewModel.hardwarePort.collectAsStateWithLifecycle()
                val localAuthToken by viewModel.localAuthToken.collectAsStateWithLifecycle()
                val isFunnelEnabled by viewModel.isFunnelEnabled.collectAsStateWithLifecycle()

                val cleanHost = hardwareHost.removePrefix("https://").removePrefix("http://").trim('/')
                val isTunnel =
                    isFunnelEnabled ||
                    cleanHost.endsWith(".ts.net", ignoreCase = true) ||
                    cleanHost.contains("cloudflare", ignoreCase = true) ||
                    cleanHost.contains("ngrok", ignoreCase = true) ||
                    cleanHost.contains("loclx", ignoreCase = true)

                val isEmulator = remember {
                    android.os.Build.FINGERPRINT.startsWith("generic") ||
                    android.os.Build.FINGERPRINT.startsWith("unknown") ||
                    android.os.Build.MODEL.contains("google_sdk") ||
                    android.os.Build.MODEL.contains("emulator") ||
                    android.os.Build.MODEL.contains("Android SDK built for x86") ||
                    android.os.Build.MANUFACTURER.contains("Genymotion") ||
                    (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic")) ||
                    "google_sdk" == android.os.Build.PRODUCT
                }

                val effectiveHost = if (isEmulator && !isTunnel) "10.0.2.2" else cleanHost
                val hostWithoutPort = effectiveHost.substringBefore(":")
                val resolvedHardwarePort = hardwarePort.trim().ifBlank { "8080" }

                val baseUrl = if (isTunnel) {
                    "https://$cleanHost/"
                } else {
                    "http://$hostWithoutPort:${resolvedHardwarePort}/"
                }

                val hardwareHeaders = buildMap {
                    if (localAuthToken.isNotBlank()) {
                        put("Authorization", "Bearer ${localAuthToken.trim()}")
                    }
                    if (isTunnel) {
                        put("User-Agent", "ForgeIntApp")
                        put("cf-terminate-connection", "true")
                        if (cleanHost.contains("ngrok", ignoreCase = true)) {
                            put("ngrok-skip-browser-warning", "true")
                        }
                    }
                }

                val context = LocalContext.current
                val phoneCommunicator = remember { PhoneCommunicator(context) }
                
                DisposableEffect(phoneCommunicator) {
                    onDispose { phoneCommunicator.cleanup() }
                }

                val hardwareViewModel = remember(baseUrl, localAuthToken, isTunnel, hardwarePort, isFunnelEnabled) {
                    ForgeHardwareViewModel(
                        HardwareRepository(
                            HardwareApi.create(baseUrl, hardwareHeaders),
                            baseUrl,
                            localAuthToken,
                            phoneCommunicator
                        ),
                        viewModel.connectionMode,
                        onSustainedVramPressure = { telemetry ->
                            viewModel.summarizeCurrentChatAndRollContext(telemetry)
                        },
                        onSystemInstabilityChanged = { shouldUseCloud, telemetry ->
                            viewModel.setTemporaryCloudRouting(shouldUseCloud, telemetry)
                        }
                    )
                }
                G14WatchMonitor(
                    viewModel = hardwareViewModel,
                    onRequestAction = { action ->
                        viewModel.requestPcAction(action)
                        navController.navigate("pc_action_confirm")
                    }
                )
            }
            composable("pc_action_confirm") {
                PcActionConfirmScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }
            composable("theme_select") {
                ThemeSelectionScreen(
                    currentTheme = appTheme,
                    onThemeSelected = { theme ->
                        viewModel.setTheme(theme)
                        navController.popBackStack()
                    }
                )
            }
            composable("api_key") {
                ApiKeySettingsScreen(
                    onApiKeyChanged = { key ->
                        viewModel.updateApiKey(key)
                    },
                    onToggleCustomKey = { enabled ->
                        viewModel.toggleCustomApiKey(enabled)
                    }
                )
            }
            composable("message_length_select") {
                val messageLength by viewModel.messageLength.collectAsStateWithLifecycle()
                MessageLengthSelectionScreen(
                    selectedLength = messageLength,
                    onLengthSelected = { length ->
                        viewModel.setMessageLength(length)
                        navController.popBackStack()
                    }
                )
            }
            composable("memory"){
                val traits by viewModel.allTraits.collectAsStateWithLifecycle()
                MemoryManagementScreen(
                    traits = traits,onDeleteTrait = { id ->
                        viewModel.deleteTrait(id)
                    },
                    onAddManualMemory = { content, type ->
                        viewModel.addManualMemory(content, type)
                    },
                    onClearAll = {
                        viewModel.clearAllTraits()
                    }

                )
            }
            composable("model_select") {
                val currentModel by viewModel.currentModelId.collectAsStateWithLifecycle()
                val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
                val isFetchingAvailableModels by viewModel.isFetchingAvailableModels.collectAsStateWithLifecycle()
                val isLocal by viewModel.isLocalEnabled.collectAsStateWithLifecycle()

                LaunchedEffect(isLocal) {
                    viewModel.fetchAvailableModels()
                }

                ModelSelectionScreen(
                    selectedModelId = currentModel,
                    onModelSelected = { newModel ->
                        viewModel.setModel(newModel)
                        navController.popBackStack()
                    },
                    availableModels = availableModels,
                    isLocalEnabled = isLocal,
                    isLoadingModels = isFetchingAvailableModels
                )
            }
            composable("persona_select") {
                val allPersonas by viewModel.allPersonas.collectAsStateWithLifecycle()
                val isLocalEnabled by viewModel.isLocalEnabled.collectAsStateWithLifecycle()
                val currentModel by viewModel.currentModelId.collectAsStateWithLifecycle()
                val onPersonaSelected: () -> Unit = remember(navController) { { navController.popBackStack() } }
                val onCreatePersonaClick: () -> Unit = remember(navController) { { navController.navigate("create_persona") } }
                val onDeletePersonaClick: (String) -> Unit = remember(viewModel) { { id: String -> viewModel.deletePersona(id) } }
                val isCloudModel = remember(currentModel) {
                    val id = currentModel.trim().lowercase()
                    id.startsWith("google/") ||
                        id.startsWith("openai/") ||
                        id.startsWith("meta-llama/") ||
                        id.startsWith("qwen/") ||
                        id.startsWith("mistralai/") ||
                        id.startsWith("anthropic/") ||
                        id.startsWith("deepseek/") ||
                        id.startsWith("cognitivecomputations/")
                }
                val nsfwPersonasBlocked = !isLocalEnabled || isCloudModel
                PersonaSettings(
                    settingsManager = settingsManager,
                    allPersonas = allPersonas,
                    nsfwPersonasBlocked = nsfwPersonasBlocked,
                    onPersonaSelected = onPersonaSelected,
                    onCreatePersonaClick = onCreatePersonaClick,
                    onDeletePersonaClick = onDeletePersonaClick
                )
            }
            composable("create_persona") {
                val isLocalEnabled by viewModel.isLocalEnabled.collectAsStateWithLifecycle()
                val currentModel by viewModel.currentModelId.collectAsStateWithLifecycle()
                val onCreatePersonaBack: () -> Unit = remember(navController) { { navController.popBackStack() } }
                val onCreatePersonaSave: (String, String, String) -> Unit = remember(viewModel, navController) {
                    { name: String, description: String, prompt: String ->
                        viewModel.createPersona(name, description, prompt)
                        navController.popBackStack()
                    }
                }
                val isCloudModel = remember(currentModel) {
                    val id = currentModel.trim().lowercase()
                    id.startsWith("google/") ||
                        id.startsWith("openai/") ||
                        id.startsWith("meta-llama/") ||
                        id.startsWith("qwen/") ||
                        id.startsWith("mistralai/") ||
                        id.startsWith("anthropic/") ||
                        id.startsWith("deepseek/") ||
                        id.startsWith("cognitivecomputations/")
                }
                CreatePersonaScreen(
                    onBack = onCreatePersonaBack,
                    onSave = onCreatePersonaSave,
                    isNsfwModeLocked = !isLocalEnabled || isCloudModel
                )
            }
            composable("ip_select") {
                val currentHost by viewModel.currentHostIp.collectAsStateWithLifecycle()
                val testResult by viewModel.testResult.collectAsStateWithLifecycle()
                val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()
                val currentPort by viewModel.currentPort.collectAsStateWithLifecycle()
                val isFunnelEnabled by viewModel.isFunnelEnabled.collectAsStateWithLifecycle()
                val localAuthToken by viewModel.localAuthToken.collectAsStateWithLifecycle()
                val currentHardwareHost by viewModel.hardwareHostIp.collectAsStateWithLifecycle()
                val currentHardwarePort by viewModel.hardwarePort.collectAsStateWithLifecycle()
                val isLocalEnabled by viewModel.isLocalEnabled.collectAsStateWithLifecycle()
                val onHostChange: (String) -> Unit = remember(viewModel) { { newIp: String -> viewModel.updateHostIp(newIp); Unit } }
                val onToggleFunnel: (Boolean) -> Unit = remember(viewModel) { { enabled: Boolean -> viewModel.setFunnelEnabled(enabled); Unit } }
                val onTestConnection: () -> Unit = remember(viewModel) { { viewModel.testConnection() } }
                val onBack: () -> Unit = remember(navController) { { navController.popBackStack() } }
                val onToggleLocal: (Boolean) -> Unit = remember(viewModel, currentHost) {
                    { enabled: Boolean ->
                        viewModel.toggleLocalMode(enabled = enabled)
                        Log.d("Address", currentHost)
                        Unit
                    }
                }
                val onPortChange: (String) -> Unit = remember(viewModel) { { newPort: String -> viewModel.updatePort(newPort); Unit } }
                val onAuthTokenChange: (String) -> Unit = remember(viewModel) { { token: String -> viewModel.updateLocalAuthToken(token); Unit } }
                val onHardwareHostChange: (String) -> Unit = remember(viewModel) { { newIp: String -> viewModel.updateHardwareHostIp(newIp); Unit } }
                val onHardwarePortChange: (String) -> Unit = remember(viewModel) { { newPort: String -> viewModel.updateHardwarePort(newPort); Unit } }


                ConnectionSettingsScreen(
                    currentHost = currentHost,
                    isFunnelEnabled = isFunnelEnabled,
                    localAuthToken = localAuthToken,
                    onHostChange = onHostChange,
                    onToggleFunnel = onToggleFunnel,
                    onTestConnection = onTestConnection,
                    testResult = testResult,
                    isTesting = isTesting,
                    onBack = onBack,
                    isLocalEnabled = isLocalEnabled,
                    onToggleLocal = onToggleLocal,
                    currentPort = currentPort,
                    onPortChange = onPortChange,
                    onAuthTokenChange = onAuthTokenChange,
                    currentHardwareHost = currentHardwareHost,
                    currentHardwarePort = currentHardwarePort,
                    onHardwareHostChange = onHardwareHostChange,
                    onHardwarePortChange = onHardwarePortChange

                )
            }
            composable("local_status") {
                LocalServerStatus()
            }
            composable("neural_network") {
                WearNeuralPerceptron()
            }
            composable("system") {
                SystemMonitorSettings(
                    onToggleMemory = {
                      viewModel.toggleMemoryMonitor()
                    },
                    onToggleThermal = {
                        viewModel.toggleThermalMonitor()
                    }

                )
            }
            composable("gradient_descent") {
                GradientDescentCanvasDemo()
            }
            composable("forge_remote") {
                val remoteHost by viewModel.remoteHostIp.collectAsStateWithLifecycle()
                val remotePort by viewModel.remotePort.collectAsStateWithLifecycle()
                RemoteCommandScreen(
                    remoteHostIp = remoteHost,
                    remotePort = remotePort,
                    onNavigateToRemoteSettings = { navController.navigate("remote_ip_select") }
                )
            }
            composable("experimental_settings") {
                ExperimentalSettingsScreen(
                    onNavigateToSystem = { navController.navigate("system") },
                    onNavigateToHardwareMonitor = { navController.navigate("hardware_monitor") },
                    onNavigateToRemoteCommand = { navController.navigate("forge_remote") },
                    onNavigateToGradientDescent = { navController.navigate("gradient_descent") },
                    onNavigateToNeuralNetwork = { navController.navigate("neural_network") }
                )
            }

        }
    }
}
object webEnabled {
    var isWebEnabled = mutableStateOf(false)

}
