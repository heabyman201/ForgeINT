package com.example.forgeint

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeint.presentation.GeminiViewModel
import com.example.forgeint.ui.theme.ForgeINTTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.forgeint.presentation.ChatScreen
import com.example.forgeint.presentation.HistoryScreen
import com.example.forgeint.presentation.ModelSelectionScreen
import com.example.forgeint.presentation.PersonaSelectionScreen
import com.example.forgeint.presentation.SettingsScreen
import com.example.forgeint.presentation.MessageLengthSelectionScreen
import com.example.forgeint.presentation.CreatePersonaScreen
import com.example.forgeint.presentation.ThemeSelectionScreen
import com.example.forgeint.presentation.LocalModelScreen
import com.example.forgeint.presentation.MemoryManagementScreen
import com.example.forgeint.presentation.HardwareMonitorScreen
import com.example.forgeint.presentation.ApiKeySettingsScreen
import com.example.forgeint.presentation.ChatAttachment
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeminiApp(intent = intent)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GeminiApp(viewModel: GeminiViewModel = viewModel(), intent: Intent? = null) {
    val navController = rememberAnimatedNavController()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val streamingMessage by viewModel.streamingMessage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val loadingBurst by viewModel.loadingBurst.collectAsStateWithLifecycle()
    val isLiteMode by viewModel.isLiteMode.collectAsStateWithLifecycle()
    val isVoiceDominantMode by viewModel.isVoiceDominantMode.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val currentModelId by viewModel.currentModelId.collectAsStateWithLifecycle()
    val selectedPersonaId by viewModel.selectedPersonaId.collectAsStateWithLifecycle()
    val allPersonas by viewModel.allPersonas.collectAsStateWithLifecycle()
    val isLocalEnabled by viewModel.isLocalEnabled.collectAsStateWithLifecycle()
    val memoryMonitorEnabled by viewModel.memory_monitor.collectAsStateWithLifecycle()
    val thermalMonitorEnabled by viewModel.thermal_monitor.collectAsStateWithLifecycle()
    val currentHost by viewModel.currentHostIp.collectAsStateWithLifecycle()
    val currentPort by viewModel.currentPort.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()
    val messageLength by viewModel.messageLength.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val isCustomApiKeyEnabled by viewModel.isCustomApiKeyEnabled.collectAsStateWithLifecycle()
    val isFunnelEnabled by viewModel.isFunnelEnabled.collectAsStateWithLifecycle()
    val localAuthToken by viewModel.localAuthToken.collectAsStateWithLifecycle()
    val pendingAction by viewModel.pendingAction.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val localModelStatus by viewModel.localModelStatus.collectAsStateWithLifecycle()
    val modelDownloadProgress by viewModel.modelDownloadProgress.collectAsStateWithLifecycle()
    val traits by viewModel.allTraits.collectAsStateWithLifecycle()
    val voiceLauncher = rememberPhoneVoiceLauncher { spoken ->
        if (viewModel.currentConversationId.value == null) {
            viewModel.startNewChat(spoken)
        } else {
            viewModel.continueChat(spoken)
        }
        navController.navigate("chat")
    }

    LaunchedEffect(intent) {
        intent?.getStringExtra("action_type")?.let(viewModel::setPendingAction)
    }
    LaunchedEffect(pendingAction) {
        when (pendingAction) {
            "voice" -> {
                voiceLauncher()
                viewModel.clearPendingAction()
            }
            "text" -> {
                viewModel.prepareForNewChat()
                navController.navigate("chat")
                viewModel.clearPendingAction()
            }
        }
    }

    ForgeINTTheme(themeName = appTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val blurRadius = remember { Animatable(0f) }
            LaunchedEffect(backStackEntry?.destination?.route) {
                blurRadius.snapTo(10f)
                blurRadius.animateTo(0f, animationSpec = tween(300))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AnimatedNavHost(
                    navController = navController,
                    startDestination = "history",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .blur(blurRadius.value.dp),
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth / 3 },
                            animationSpec = tween(320)
                        ) + fadeIn(animationSpec = tween(220))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 6 },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(180))
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = tween(320)
                        ) + fadeIn(animationSpec = tween(220))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth / 6 },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(180))
                    }
                ) {
                composable("history") {
                    HistoryScreen(
                        history = history,
                        searchQuery = searchQuery,
                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                        searchResults = searchResults,
                        onNewChatClick = {
                            if (isVoiceDominantMode) {
                                voiceLauncher()
                            } else {
                                viewModel.prepareForNewChat()
                                navController.navigate("chat")
                            }
                        },
                        onChatClick = {
                            viewModel.openChat(it)
                            navController.navigate("chat")
                        },
                        onBookmarkClick = viewModel::toggleBookmark,
                        onDeleteClick = viewModel::deleteChat,
                        onSettingsClick = { navController.navigate("settings") }
                    )
                }
                composable("chat") {
                    val onSendMessage: (String, List<ChatAttachment>) -> Unit = { text, attachments ->
                        if (viewModel.currentConversationId.value == null) {
                            viewModel.startNewChat(text, attachments)
                        } else {
                            viewModel.continueChat(text, attachments)
                        }
                    }
                    ChatScreen(
                        messages = activeMessages,
                        streamingMessage = streamingMessage,
                        isThinkingFlow = viewModel.isThinking,
                        isLoading = isLoading,
                        loadingBurst = loadingBurst,
                        isVoiceDominantMode = isVoiceDominantMode,
                        onSendMessage = onSendMessage,
                        onStopResponse = viewModel::stopResponse,
                        onVoiceInput = { voiceLauncher() },
                        onDeleteChat = {
                            viewModel.currentConversationId.value?.let { chatId ->
                                viewModel.deleteChat(chatId)
                            }
                            navController.popBackStack()
                        },
                        onModelChangeClick = {
                            navController.navigate("model_select")
                        },
                        currentModelName = currentModelId,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        isLiteMode = isLiteMode,
                        onToggleLiteMode = viewModel::setLiteMode,
                        isVoiceDominantMode = isVoiceDominantMode,
                        onToggleVoiceDominantMode = viewModel::setVoiceDominantMode,
                        currentModelId = currentModelId,
                        selectedPersonaId = selectedPersonaId,
                        allPersonas = allPersonas,
                        onNavigateToModelSelect = { navController.navigate("model_select") },
                        onNavigateToPersona = { navController.navigate("persona_select") },
                        isLocalEnabled = isLocalEnabled,
                        onToggleLocal = viewModel::toggleLocalMode,
                        currentHost = currentHost,
                        onHostChange = viewModel::updateHostIp,
                        currentPort = currentPort,
                        onPortChange = viewModel::updatePort,
                        onTestConnection = viewModel::testConnection,
                        testResult = testResult,
                        isTesting = isTesting,
                        messageLength = messageLength,
                        onNavigateToMessageLength = { navController.navigate("message_length_select") },
                        appTheme = appTheme,
                        onNavigateToTheme = { navController.navigate("theme_select") },
                        apiKey = apiKey,
                        isCustomApiKeyEnabled = isCustomApiKeyEnabled,
                        onNavigateToApiKey = { navController.navigate("api_key") },
                        memoryMonitor = memoryMonitorEnabled,
                        thermalMonitor = thermalMonitorEnabled,
                        onToggleMemoryMonitor = { viewModel.toggleMemoryMonitor() },
                        onToggleThermalMonitor = { viewModel.toggleThermalMonitor() },
                        onNavigateToMemory = { navController.navigate("memory") },
                        onNavigateToHardwareMonitor = { navController.navigate("hardware_monitor") },
                        onNavigateToLocalModel = { navController.navigate("local_model") },
                        isFunnelEnabled = isFunnelEnabled,
                        onFunnelEnabledChange = viewModel::setFunnelEnabled,
                        localAuthToken = localAuthToken,
                        onLocalAuthTokenChange = viewModel::updateLocalAuthToken,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("api_key") {
                    ApiKeySettingsScreen(
                        apiKey = apiKey,
                        customApiKeyEnabled = isCustomApiKeyEnabled,
                        onApiKeyChanged = viewModel::updateApiKey,
                        onToggleCustomKey = viewModel::toggleCustomApiKey,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("message_length_select") {
                    MessageLengthSelectionScreen(
                        selectedLength = messageLength,
                        onLengthSelected = {
                            viewModel.setMessageLength(it)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("theme_select") {
                    ThemeSelectionScreen(
                        currentTheme = appTheme,
                        onThemeSelected = {
                            viewModel.setTheme(it)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("local_model") {
                    LocalModelScreen(
                        status = localModelStatus,
                        progress = modelDownloadProgress,
                        onDownload = viewModel::downloadLocalModel,
                        onDelete = viewModel::deleteLocalModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("memory") {
                    MemoryManagementScreen(
                        traits = traits,
                        onDeleteTrait = viewModel::deleteTrait,
                        onAddManualMemory = viewModel::addManualMemory,
                        onClearAll = viewModel::clearAllTraits,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("hardware_monitor") {
                    HardwareMonitorScreen(
                        currentHost = currentHost,
                        isFunnelEnabled = isFunnelEnabled,
                        localAuthToken = localAuthToken,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("model_select") {
                    ModelSelectionScreen(
                        selectedModelId = currentModelId,
                        onModelSelected = {
                            viewModel.setModel(it)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() },
                        availableModels = availableModels,
                        isLocalEnabled = isLocalEnabled
                    )
                }
                composable("persona_select") {
                    PersonaSelectionScreen(
                        selectedPersonaId = selectedPersonaId,
                        allPersonas = allPersonas,
                        onPersonaSelected = {
                            viewModel.setPersona(it)
                            navController.popBackStack()
                        },
                        onCreatePersonaClick = {
                             navController.navigate("create_persona")
                        },
                        onDeletePersonaClick = {
                            viewModel.deletePersona(it)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("create_persona") {
                    CreatePersonaScreen(
                        onBack = { navController.popBackStack() },
                        onSave = { name, description, prompt ->
                            viewModel.createPersona(name, description, prompt)
                            navController.popBackStack()
                        }
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun rememberPhoneVoiceLauncher(onResult: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) onResult(spoken)
        }
    }

    return remember {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message")
            }
            launcher.launch(intent)
        }
    }
}

