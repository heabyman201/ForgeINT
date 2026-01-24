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

            GeminiApp()
        }
    }
}

@Composable
fun GeminiApp(viewModel: GeminiViewModel = viewModel()) {

    val navController = rememberSwipeDismissableNavController()
    val isLiteMode by viewModel.isLiteMode.collectAsState()
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val haptics = LocalHapticFeedback.current
    val appTheme by viewModel.appTheme.collectAsState()

    ForgeINTTheme(themeName = appTheme) {
        SwipeDismissableNavHost(navController = navController, startDestination = "history") {

            // ---------------------------------------------------------
            // SCREEN 1: HISTORY (MAIN MENU)
            // ---------------------------------------------------------
            composable("history") {
                val history by viewModel.history.collectAsState()

                // Input launcher for "New Chat"
                val textLauncher = rememberTextInputLauncher { text ->
                    viewModel.startNewChat(text)
                    navController.navigate("chat")
                }

                if (isLiteMode) {
                    LiteHistoryScreen(
                        history = history,
                        onNewChatClick = textLauncher,
                        onChatClick = { id ->
                            viewModel.openChat(id)
                            convoID.conID.value = id.toInt()

                            navController.navigate("chat")
                        },
                        onBookmarkClick = { id, status -> viewModel.toggleBookmark(id, status) },
                        onSettingsClick = { navController.navigate("settings") }
                    )
                } else {
                    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
                    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                    DetailedHistoryScreen(
                        searchResults = searchResults,
                        onNewChatClick = textLauncher,
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
                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) }

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
                        isTyping = isLoading,
                        onReplyClick = textLauncher,
                        onDeleteClick = {
                            viewModel.deleteChat(id = convoID.conID.value.toLong())
                            navController.popBackStack()
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)

                        },
                        onSwapModelClick = { navController.navigate("model_select") }
                    )
                } else {

                    DetailedChatScreen(
                        messages = messages,

                        streamingMessageFlow = viewModel.streamingMessage,
                        isThinkingFlow = viewModel.isThinking,


                        currentConversationId = convoID.conID.value.toLong(),


                        isBookmarked = convoID.condBK.value,


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
                SettingsScreen(
                    isLiteMode = isLiteMode,
                    currentModelId = currentModel,

                    onToggleLite = { viewModel.setLiteMode(it) },
                    onNavigateToModelSelect = { navController.navigate("model_select") },
                    onNavigateToPersona = { navController.navigate("persona_select") },
                    onNavigateToIP = { navController.navigate("ip_select") },
                    onNavigateToLocalModel = { navController.navigate("local_model") },
                    onNavigateToNeuralNetwork = { navController.navigate("neural_network") },
                    onNavigateToSystem = { navController.navigate("system") },
                    onNavigateToMessageLength = { navController.navigate("message_length_select") },
                    onNavigateToApiKey = { navController.navigate("api_key") },
                    onNavigateToTheme = { navController.navigate("theme_select") }
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
                    traits = traits,onDeleteTrait = { traitKey ->
                        viewModel.deleteTrait(traitKey)
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
                PersonaSettings(
                    settingsManager = settingsManager,
                    onPersonaSelected = {
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