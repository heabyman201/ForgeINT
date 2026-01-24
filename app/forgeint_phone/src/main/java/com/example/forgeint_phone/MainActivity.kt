package com.example.forgeint_phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.forgeint_phone.presentation.GeminiViewModel
import com.example.forgeint_phone.ui.theme.ForgeINTTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.forgeint_phone.presentation.ChatScreen
import com.example.forgeint_phone.presentation.HistoryScreen
import com.example.forgeint_phone.presentation.ModelSelectionScreen
import com.example.forgeint_phone.presentation.PersonaSelectionScreen
import com.example.forgeint_phone.presentation.SettingsScreen
import com.example.forgeint_phone.presentation.MessageLengthSelectionScreen
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
    val isLocalEnabled by viewModel.isLocalEnabled.collectAsStateWithLifecycle()
    val currentHost by viewModel.currentHostIp.collectAsStateWithLifecycle()
    val currentPort by viewModel.currentPort.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()
    val messageLength by viewModel.messageLength.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()

    AnimatedNavHost(
        navController = navController, 
        startDestination = "history",
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable(
            "history",
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -300 }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -300 }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))
            }
        ) {
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
        composable(
            "chat",
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 300 }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 300 }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
            }
        ) {
            val onSendMessage: (String) -> Unit = {
                if (viewModel.currentChat.value == null) {
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
                }
            )
        }
        composable(
            "settings",
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 300 }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 300 }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
            }
        ) {
            SettingsScreen(
                isLiteMode = isLiteMode,
                onToggleLiteMode = viewModel::setLiteMode,
                currentModelId = currentModelId,
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
        composable(
            "message_length_select",
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 300 }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 300 }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
            }
        ) {
            MessageLengthSelectionScreen(
                selectedLength = messageLength,
                onLengthSelected = {
                    viewModel.setMessageLength(it)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "model_select",
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 300 }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 300 }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
            }
        ) {
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
        composable(
            "persona_select",
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 300 }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 300 }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
            }
        ) {
            PersonaSelectionScreen(
                selectedPersonaId = selectedPersonaId,
                onPersonaSelected = {
                    viewModel.setPersona(it)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
