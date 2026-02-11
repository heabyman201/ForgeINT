package com.example.forgeint.presentation

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
import com.example.weargemini.data.SettingsManager
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val isLiteMode by viewModel.isLiteMode.collectAsState()
    val isVoiceDominantMode by viewModel.isVoiceDominantMode.collectAsState()
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val haptics = LocalHapticFeedback.current
    val appTheme by viewModel.appTheme.collectAsState()

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
                val history by viewModel.history.collectAsState()
                val pendingAction by viewModel.pendingAction.collectAsState()

                // Input launcher for "New Chat"
                val textLauncher = rememberTextInputLauncher { text ->
                    viewModel.startNewChat(text)
                    navController.navigate("chat")
                }

                val voiceLauncher = rememberVoiceLauncher { text ->
                    viewModel.startNewChat(text)
                    navController.navigate("chat")
                }

                val newChatLauncher = {
                    if (isVoiceDominantMode) {
                        voiceLauncher.invoke()
                    } else {
                        textLauncher.invoke()
                    }
                }

                LaunchedEffect(pendingAction) {
                    if (pendingAction == "voice") {
                        voiceLauncher.invoke()
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
                        onChatClick = { id ->
                            viewModel.openChat(id)
                            convoID.conID.value = id.toInt()

                            navController.navigate("chat")
                        },
                        onBookmarkClick = { id, status -> viewModel.toggleBookmark(id, status) },
                        onSettingsClick = { navController.navigate("settings") },
                        viewModel = viewModel
                    )
                } else {
                    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
                    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                    DetailedHistoryScreen(
                        searchResults = searchResults,
                        onNewChatClick = newChatLauncher,
                        onChatClick = { id ->
                            viewModel.openChat(id)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            convoID.conID.value = id.toInt()
                            navController.navigate("chat")
                        },
                        onBookmarkClick = { id, status -> viewModel.toggleBookmark(id, status);
                            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)},
                        onSettingsClick = { navController.navigate("settings")
                                          },
                        onDeleteClick = { id ->
                            viewModel.deleteChat(id)
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        },
                        onNavigateToMemory = {
                            navController.navigate("memory")
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        searchQuery = searchQuery,
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        viewModel = viewModel
                    )
                }
            }


            composable("chat") {
                val messages by viewModel.activeMessages.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
                val textLauncher = rememberTextInputLauncher { text ->
                    viewModel.continueChat(text)
                }


                if (isLiteMode) {
                    LiteChatScreen(
                        messages = messages,
                        streamingMessageFlow = viewModel.streamingMessage,
                        isTyping = isLoading,
                        isVoiceDominant = isVoiceDominantMode,
                        onReplyClick = textLauncher,
                        onDeleteClick = {
                            viewModel.deleteChat(id = convoID.conID.value.toLong())
                            navController.popBackStack()
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)

                        },
                        onSwapModelClick = { navController.navigate("model_select") },
                        onVoiceResult = { spokenText ->
                            viewModel.continueChat(spokenText)
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        }
                    )
                } else {

                    DetailedChatScreen(
                        messages = messages,

                        streamingMessageFlow = viewModel.streamingMessage,
                        isThinkingFlow = viewModel.isThinking,


                        currentConversationId = convoID.conID.value.toLong(),


                        isBookmarked = convoID.condBK.value,
                        isVoiceDominant = isVoiceDominantMode,


                        onReplyClick = textLauncher,
                        onDeleteClick = {
                            viewModel.deleteChat(id = convoID.conID.value.toLong())
                            navController.popBackStack()
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        },
                        isLoading = isLoading,
                        onSwapModelClick = {
                            navController.navigate("model_select")
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onBookmarkClick = { id, status ->
                            viewModel.toggleBookmark(id, status)

                            convoID.condBK.value = status
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        loadMoreMessages = {
                            viewModel.loadNextPage()
                        },
                        isHistoryLoading = viewModel.isHistoryLoading.collectAsStateWithLifecycle().value,
                        onVoiceResult = { spokenText ->
                            viewModel.continueChat(spokenText)
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        },
                        onTryAgain = {
                            viewModel.continueChat("Try again")
                        },
                        onWebsearchClick = {
                            viewModel.toggleWebSearch()
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            webEnabled.isWebEnabled.value = viewModel.isWebSearchEnabled.value
                            Log.d("WebSearch", viewModel.isWebSearchEnabled.value.toString())
                        }
                    )

                }
            }

            // ---------------------------------------------------------
            // SCREEN 3: SETTINGS
            // ---------------------------------------------------------
            composable("settings") {
                val currentModel by viewModel.currentModelId.collectAsState()
                val allPersonas by viewModel.allPersonas.collectAsStateWithLifecycle()
                SettingsScreen(
                    isLiteMode = isLiteMode,
                    isVoiceDominantMode = isVoiceDominantMode,
                    currentModelId = currentModel,
                    allPersonas = allPersonas,

                    onToggleLite = { viewModel.setLiteMode(it) },
                    onToggleVoiceDominant = { viewModel.setVoiceDominantMode(it) },
                    onNavigateToModelSelect = { navController.navigate("model_select") },
                    onNavigateToPersona = { navController.navigate("persona_select") },
                    onNavigateToIP = { navController.navigate("ip_select") },
                    onNavigateToLocalModel = { navController.navigate("local_model") },
                    onNavigateToNeuralNetwork = { navController.navigate("neural_network") },
                    onNavigateToSystem = { navController.navigate("system") },
                    onNavigateToMessageLength = { navController.navigate("message_length_select") },
                    onNavigateToApiKey = { navController.navigate("api_key") },
                    onNavigateToTheme = { navController.navigate("theme_select") },
                    onNavigateToLocalStatus = { navController.navigate("local_status") },
                    onNavigateToHardwareMonitor = { navController.navigate("hardware_monitor") }
                )
            }
            composable("hardware_monitor") {
                val host by viewModel.currentHostIp.collectAsState()
                
                val cleanHost = host.removePrefix("https://").removePrefix("http://").trim('/')
                val isTunnel = cleanHost.contains("cloudflare") || cleanHost.contains("ngrok") || cleanHost.contains("loclx")

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

                val baseUrl = if (isTunnel) {
                    "https://$cleanHost/"
                } else {
                    "http://$hostWithoutPort:8080/"
                }

                val context = LocalContext.current
                val phoneCommunicator = remember { PhoneCommunicator(context) }
                
                DisposableEffect(phoneCommunicator) {
                    onDispose { phoneCommunicator.cleanup() }
                }

                val hardwareViewModel = remember(baseUrl) {
                    ForgeHardwareViewModel(
                        HardwareRepository(HardwareApi.create(baseUrl), baseUrl, phoneCommunicator),
                        viewModel.connectionMode
                    )
                }
                G14WatchMonitor(viewModel = hardwareViewModel)
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
                val messageLength by viewModel.messageLength.collectAsState()
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
                val currentModel by viewModel.currentModelId.collectAsState()
                val availableModels by viewModel.availableModels.collectAsState()
                val isLocal by viewModel.isLocalEnabled.collectAsState()

                ModelSelectionScreen(
                    selectedModelId = currentModel,
                    onModelSelected = { newModel ->
                        viewModel.setModel(newModel)
                        navController.popBackStack()
                    },
                    availableModels = availableModels,
                    isLocalEnabled = isLocal
                )
            }
            composable("persona_select") {
                val allPersonas by viewModel.allPersonas.collectAsStateWithLifecycle()
                PersonaSettings(
                    settingsManager = settingsManager,
                    allPersonas = allPersonas,
                    onPersonaSelected = {
                        navController.popBackStack()
                    },
                    onCreatePersonaClick = {
                        navController.navigate("create_persona")
                    },
                    onDeletePersonaClick = { id ->
                        viewModel.deletePersona(id)
                    }
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
            composable("ip_select") {
                val currentHost by viewModel.currentHostIp.collectAsState()
                val testResult by viewModel.testResult.collectAsState()
                val isTesting by viewModel.isTesting.collectAsState()
                val currentPort by viewModel.currentPort.collectAsState()


                ConnectionSettingsScreen(
                    currentHost = currentHost,
                    onHostChange = { newIp ->
                        viewModel.updateHostIp(newIp)
                    },
                    onTestConnection = {

                        viewModel.testConnection()
                    },
                    testResult = testResult,
                    isTesting = isTesting,
                    onBack = {
                        navController.popBackStack()
                    },
                    isLocalEnabled = viewModel.isLocalEnabled.collectAsState().value,
                    onToggleLocal = {
                        viewModel.toggleLocalMode(
                            enabled = it
                        )
                        Log.d("Address", currentHost)
                    },
                    currentPort = currentPort,
                    onPortChange = { newPort ->
                        viewModel.updatePort(newPort)
                    }

                )
            }

            composable("local_model") {
                LocalModelScreen(viewModel = viewModel)
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

        }
    }
}
object webEnabled {
    var isWebEnabled = mutableStateOf(false)

}
