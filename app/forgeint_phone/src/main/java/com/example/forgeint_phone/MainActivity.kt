package com.example.forgeint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeint.presentation.GeminiViewModel
import com.example.forgeint.ui.theme.ForgeINTTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.forgeint.presentation.ChatScreen
import com.example.forgeint.presentation.HistoryScreen
import com.example.forgeint.presentation.ModelSelectionScreen
import com.example.forgeint.presentation.PersonaSelectionScreen
import com.example.forgeint.presentation.SettingsScreen
import com.example.forgeint.presentation.MessageLengthSelectionScreen
import com.example.forgeint.presentation.CreatePersonaScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

@OptIn(ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ForgeINTTheme {
                GeminiApp()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GeminiApp(viewModel: GeminiViewModel = viewModel()) {
    val navController = rememberAnimatedNavController()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val streamingMessage by viewModel.streamingMessage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLiteMode by viewModel.isLiteMode.collectAsStateWithLifecycle()
    val currentModelId by viewModel.currentModelId.collectAsStateWithLifecycle()
    val selectedPersonaId by viewModel.selectedPersonaId.collectAsStateWithLifecycle()
    val allPersonas by viewModel.allPersonas.collectAsStateWithLifecycle()
    val isLocalEnabled by viewModel.isLocalEnabled.collectAsStateWithLifecycle()
    val currentHost by viewModel.currentHostIp.collectAsStateWithLifecycle()
    val currentPort by viewModel.currentPort.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()
    val messageLength by viewModel.messageLength.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedNavHost(
            navController = navController,
            startDestination = "history",
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable("history") {
                HistoryScreen(
                    history = history,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    searchResults = searchResults,
                    onNewChatClick = {
                        viewModel.prepareForNewChat()
                        navController.navigate("chat")
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
                val onSendMessage: (String) -> Unit = {
                    if (viewModel.currentConversationId.value == null) {
                        viewModel.startNewChat(it)
                    } else {
                        viewModel.continueChat(it)
                    }
                }
                ChatScreen(
                    messages = activeMessages,
                    streamingMessage = streamingMessage,
                    isThinkingFlow = viewModel.isThinking,
                    isLoading = isLoading,
                    onSendMessage = onSendMessage,
                    onDeleteChat = {
                        viewModel.currentChat.value?.conversation?.id?.let {
                            viewModel.deleteChat(it)
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

