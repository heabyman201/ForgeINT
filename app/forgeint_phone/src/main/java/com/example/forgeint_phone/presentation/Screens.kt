@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.forgeint.presentation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.forgeint.data.Conversation
import com.example.forgeint.data.Message
import com.example.forgeint.data.UserTrait
import com.example.forgeint.domain.Personas
import com.example.forgeint.ui.theme.appThemes
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<Conversation>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    searchResults: List<Conversation>,
    onNewChatClick: () -> Unit,
    onChatClick: (Long) -> Unit,
    onBookmarkClick: (Long, Boolean) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewChatClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.Add, "New Chat")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Conversations",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings, 
                        contentDescription = "Settings", 
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Search Field (Pill Shape)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search history...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )

            val itemsToDisplay = if (searchQuery.isNotBlank()) searchResults else history

            if (itemsToDisplay.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No conversations yet", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(itemsToDisplay, key = { it.id }) { chat ->
                        ChatItemCard(
                            chat = chat,
                            onChatClick = onChatClick,
                            onDeleteClick = onDeleteClick,
                            onBookmarkClick = onBookmarkClick,

                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItemCard(
    chat: Conversation,
    onChatClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onBookmarkClick: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var appeared by remember(chat.id) { mutableStateOf(false) }
    LaunchedEffect(chat.id) { appeared = true }
    val cardAlpha by animateFloatAsState(if (appeared) 1f else 0f, animationSpec = tween(220), label = "cardAlpha")
    val cardScale by animateFloatAsState(if (appeared) 1f else 0.97f, animationSpec = tween(260), label = "cardScale")

    val gradientTransition = rememberInfiniteTransition(label = "bookmarkGradient")
    val gradientShift by gradientTransition.animateFloat(
        initialValue = -260f,
        targetValue = 460f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientShift"
    )

    val bookmarkPulse = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Card(
        onClick = { onChatClick(chat.id) },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = cardAlpha
                scaleX = cardScale
                scaleY = cardScale
            }
    ) {
        Box(
            modifier = Modifier.background(
                if (chat.isBookmarked) {
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        ),
                        start = Offset(gradientShift, 0f),
                        end = Offset(gradientShift + 340f, 260f)
                    )
                } else {
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)
                    )
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chat.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (chat.isBookmarked) "Bookmarked" else "Tap to view",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (chat.isBookmarked) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                IconButton(onClick = {
                    scope.launch {
                        bookmarkPulse.snapTo(1f)
                        bookmarkPulse.animateTo(1.22f, tween(120))
                        bookmarkPulse.animateTo(1f, tween(180))
                    }
                    onBookmarkClick(chat.id, chat.isBookmarked)
                }) {
                    Icon(
                        if (chat.isBookmarked) Icons.Filled.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (chat.isBookmarked) Color(0xFFFDD835) else Color.Gray,
                        modifier = Modifier.graphicsLayer {
                            scaleX = bookmarkPulse.value
                            scaleY = bookmarkPulse.value
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<Message>,
    streamingMessage: String?,
    isThinkingFlow: StateFlow<Boolean>,
    isLoading: Boolean,
    loadingBurst: Boolean,
    isVoiceDominantMode: Boolean,
    onSendMessage: (String, List<ChatAttachment>) -> Unit,
    onStopResponse: () -> Unit,
    onVoiceInput: () -> Unit,
    onDeleteChat: () -> Unit,
    onModelChangeClick: () -> Unit,
    currentModelName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val attachments = remember { mutableStateListOf<ChatAttachment>() }
    val isThinking by isThinkingFlow.collectAsStateWithLifecycle()
    val estimatedLines = remember(inputText) {
        val newlineLines = inputText.count { it == '\n' } + 1
        val wrapLines = (inputText.length / 36) + 1
        maxOf(newlineLines, wrapLines).coerceIn(1, 6)
    }
    val inputCornerRadius by animateDpAsState(
        targetValue = when {
            estimatedLines <= 1 -> 28.dp
            estimatedLines <= 3 -> 22.dp
            else -> 16.dp
        },
        animationSpec = tween(180),
        label = "inputCornerRadius"
    )
    val shouldStickToBottom by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems <= 1) {
                true
            } else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= totalItems - 2
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                readChatAttachment(context, uri, preferImage = true)?.let(attachments::add)
            }
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                readChatAttachment(context, uri, preferImage = false)?.let(attachments::add)
            }
        }
    }

    LaunchedEffect(messages.size, streamingMessage?.length, isThinking, loadingBurst) {
        if (messages.isNotEmpty() || streamingMessage?.isNotEmpty() == true || isThinking) {
            val extraItems = (if (isThinking || loadingBurst) 1 else 0) + (if (streamingMessage != null) 1 else 0)
            val targetIndex = (messages.size + extraItems - 1).coerceAtLeast(0)
            if (shouldStickToBottom) {
                listState.scrollToItem(targetIndex)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = currentModelName.split("/").last().split(":").first(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = onModelChangeClick) {
                        Icon(Icons.Default.Tune, "Change Model", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDeleteChat) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(inputCornerRadius))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (attachments.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            attachments.forEachIndexed { index, attachment ->
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            attachment.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (attachment.imageDataUrl != null) Icons.Default.Image else Icons.Default.AttachFile,
                                            contentDescription = null
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove attachment",
                                            modifier = Modifier.clickable { attachments.removeAt(index) }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { filePicker.launch(arrayOf("*/*")) },
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.AttachFile, "Attach file", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { imagePicker.launch("image/*") },
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Image, "Attach photo", tint = MaterialTheme.colorScheme.primary)
                        }
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Message...", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(inputCornerRadius),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            minLines = 1,
                            maxLines = 6
                        )
                        IconButton(
                            onClick = onVoiceInput,
                            enabled = !isLoading
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                "Voice",
                                tint = if (isVoiceDominantMode) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                        IconButton(
                            onClick = {
                                if (isLoading) {
                                    onStopResponse()
                                } else if (inputText.isNotBlank() || attachments.isNotEmpty()) {
                                    onSendMessage(inputText.trim(), attachments.toList())
                                    inputText = ""
                                    attachments.clear()
                                }
                            },
                            enabled = isLoading || inputText.isNotBlank() || attachments.isNotEmpty()
                        ) {
                            Icon(
                                if (isLoading) Icons.Default.Stop else Icons.Default.Send,
                                if (isLoading) "Stop" else "Send",
                                tint = if (isLoading) {
                                    Color(0xFFE53935)
                                } else if (inputText.isNotBlank() || attachments.isNotEmpty()) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    Color.Gray
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
            }

            if (isThinking || loadingBurst) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val pulse = rememberInfiniteTransition(label = "thinkingPulse")
                        val scale by pulse.animateFloat(
                            initialValue = 0.85f,
                            targetValue = 1.12f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 850),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "thinkingScale"
                        )
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (streamingMessage != null) {
                item {
                    MessageBubble(message = Message(text = streamingMessage, isUser = false, conversationId = 0L))
                }
            }
        }
    }
}

private suspend fun readChatAttachment(
    context: Context,
    uri: Uri,
    preferImage: Boolean
): ChatAttachment? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
    val label = queryDisplayName(context, uri) ?: "attachment"
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
    if (bytes.isEmpty()) return@withContext null

    if (preferImage || mimeType.startsWith("image/")) {
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return@withContext ChatAttachment(
            label = label,
            mimeType = mimeType,
            imageDataUrl = "data:$mimeType;base64,$base64"
        )
    }

    val textContent = when {
        mimeType.startsWith("text/") ||
            mimeType.contains("json") ||
            mimeType.contains("xml") ||
            mimeType.contains("csv") -> bytes.toString(Charsets.UTF_8).take(12_000)
        else -> "Attached file: $label (${formatBytes(bytes.size.toLong())}). Binary preview is not supported, but the model can use this metadata."
    }
    return@withContext ChatAttachment(
        label = label,
        mimeType = mimeType,
        textContent = textContent
    )
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
    return String.format("%.1f MB", bytes / (1024f * 1024f))
}

@Composable
fun MessageBubble(message: Message) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    
    // Watch-style shapes: Rounded corners, slight "speech bubble" feel
    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    var appeared by remember(message.id, message.text) { mutableStateOf(false) }
    LaunchedEffect(message.id, message.text) { appeared = true }
    val animatedAlpha by animateFloatAsState(targetValue = if (appeared) 1f else 0f, animationSpec = tween(220), label = "bubbleAlpha")
    val animatedScale by animateFloatAsState(targetValue = if (appeared) 1f else 0.96f, animationSpec = tween(240), label = "bubbleScale")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
            },
        contentAlignment = alignment
    ) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            if (!isUser) {
                Text(
                    text = "AI Assistant",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                )
            } else {
                 Text(
                    text = "You",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 12.dp, bottom = 4.dp)
                )
            }
            
            Surface(
                shape = shape,
                color = bubbleColor,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                val annotatedText = remember(message.text) {
                    RichTextFormatter.format(message.text, textColor)
                }
                
                Text(
                    text = annotatedText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isLiteMode: Boolean,
    onToggleLiteMode: (Boolean) -> Unit,
    isVoiceDominantMode: Boolean,
    onToggleVoiceDominantMode: (Boolean) -> Unit,
    currentModelId: String,
    selectedPersonaId: String,
    allPersonas: List<com.example.forgeint.domain.Persona>,
    onNavigateToModelSelect: () -> Unit,
    onNavigateToPersona: () -> Unit,
    isLocalEnabled: Boolean,
    onToggleLocal: (Boolean) -> Unit,
    currentHost: String,
    onHostChange: (String) -> Unit,
    currentPort: String,
    onPortChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    testResult: String?,
    isTesting: Boolean,
    messageLength: String,
    onNavigateToMessageLength: () -> Unit,
    appTheme: String,
    onNavigateToTheme: () -> Unit,
    apiKey: String,
    isCustomApiKeyEnabled: Boolean,
    onNavigateToApiKey: () -> Unit,
    memoryMonitor: Boolean,
    thermalMonitor: Boolean,
    onToggleMemoryMonitor: () -> Unit,
    onToggleThermalMonitor: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToHardwareMonitor: () -> Unit,
    onNavigateToLocalModel: () -> Unit,
    isFunnelEnabled: Boolean,
    onFunnelEnabledChange: (Boolean) -> Unit,
    localAuthToken: String,
    onLocalAuthTokenChange: (String) -> Unit,
    onBack: () -> Unit
) {
    var hostDraft by rememberSaveable { mutableStateOf(currentHost.removePrefix("https://").removePrefix("http://").trim('/')) }
    var portDraft by rememberSaveable { mutableStateOf(currentPort) }

    LaunchedEffect(currentHost) {
        val cleaned = currentHost.removePrefix("https://").removePrefix("http://").trim('/')
        if (cleaned != hostDraft) hostDraft = cleaned
    }
    LaunchedEffect(currentPort) {
        if (currentPort != portDraft) portDraft = currentPort
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection("Display") {
                    SettingsRow(
                        title = "Lite Mode",
                        subtitle = "Simpler UI",
                        control = { Switch(checked = isLiteMode, onCheckedChange = onToggleLiteMode) }
                    )
                    SettingsRow(
                        title = "Voice Dominant",
                        subtitle = "Start chats with microphone by default",
                        control = { Switch(checked = isVoiceDominantMode, onCheckedChange = onToggleVoiceDominantMode) }
                    )
                    SettingsRow(
                        title = "Theme",
                        subtitle = appTheme,
                        onClick = onNavigateToTheme
                    )
                }
            }
            item {
                SettingsSection("Model Config") {
                    SettingsRow(
                        title = "Current Model",
                        subtitle = currentModelId,
                        onClick = onNavigateToModelSelect
                    )
                    SettingsRow(
                        title = "Response Length",
                        subtitle = messageLength,
                        onClick = onNavigateToMessageLength
                    )
                     SettingsRow(
                        title = "Persona",
                        subtitle = allPersonas.find { it.id == selectedPersonaId }?.name ?: "Unknown",
                        onClick = onNavigateToPersona
                    )
                    SettingsRow(
                        title = "API Key",
                        subtitle = if (isCustomApiKeyEnabled && apiKey.isNotBlank()) "Custom key enabled" else "Using built-in key",
                        onClick = onNavigateToApiKey
                    )
                }
            }
            item {
                SettingsSection("Local Inference") {
                    SettingsRow(
                        title = "Use Local Server",
                        subtitle = if (isLocalEnabled) "Using local server" else "Using cloud server",
                        control = { Switch(checked = isLocalEnabled, onCheckedChange = onToggleLocal) }
                    )
                    if (isLocalEnabled) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            OutlinedTextField(
                                value = hostDraft,
                                onValueChange = { hostDraft = it },
                                label = { Text("Host Address") },
                                placeholder = { Text("100.x.y.z or device.tailnet.ts.net") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Text(
                                text = "Hint: for Tailscale, use a 100.x tailnet IP or a .ts.net hostname.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = portDraft,
                                onValueChange = { if (!isFunnelEnabled) portDraft = it.filter(Char::isDigit).take(5) },
                                label = { Text("Port") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isFunnelEnabled
                            )
                            AnimatedVisibility(visible = isFunnelEnabled) {
                                Text(
                                    text = "Port is locked in funnel mode. Endpoint uses HTTPS tunnel routing.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tunnel/Funnel Mode", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = isFunnelEnabled,
                                    onCheckedChange = onFunnelEnabledChange
                                )
                            }
                            OutlinedTextField(
                                value = localAuthToken,
                                onValueChange = onLocalAuthTokenChange,
                                label = { Text("Local Auth Token") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onHostChange(hostDraft)
                                    if (!isFunnelEnabled) onPortChange(portDraft)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("Apply Endpoint", color = MaterialTheme.colorScheme.onTertiary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onTestConnection,
                                enabled = !isTesting,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Test Connection", color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            testResult?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (it.contains("Success")) Color.Green else Color.Red,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                    SettingsRow(
                        title = "Manage Local Model",
                        subtitle = "Download or remove on-device GGUF model",
                        onClick = onNavigateToLocalModel
                    )
                }
            }
            item {
                SettingsSection("Memory + Monitoring") {
                    SettingsRow(
                        title = "Memory System",
                        subtitle = if (memoryMonitor) "Enabled" else "Disabled",
                        control = { Switch(checked = memoryMonitor, onCheckedChange = { onToggleMemoryMonitor() }) }
                    )
                    SettingsRow(
                        title = "System Telemetry",
                        subtitle = if (thermalMonitor) "Enabled" else "Disabled",
                        control = { Switch(checked = thermalMonitor, onCheckedChange = { onToggleThermalMonitor() }) }
                    )
                    SettingsRow(
                        title = "Memory Manager",
                        subtitle = "Review, add, and clear stored memory traits",
                        onClick = onNavigateToMemory
                    )
                    SettingsRow(
                        title = "Hardware Monitor",
                        subtitle = "CPU, RAM, storage, battery, thermal snapshot",
                        onClick = onNavigateToHardwareMonitor
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    control: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
        if (control != null) {
            control()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}
// Placeholder for other screens (ModelSelection, PersonaSelection, MessageLength) 
// They would follow the same SettingsScreen pattern.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionScreen(
    selectedModelId: String,
    onModelSelected: (String) -> Unit,
    onBack: () -> Unit,
    availableModels: List<String> = emptyList(),
    isLocalEnabled: Boolean = false
) {
    val defaultModels = listOf(
        "google/gemma-3n-e4b-it:free",
        "google/gemma-3-27b-it:free",
        "cognitivecomputations/dolphin-mistral-24b-venice-edition:free"
    )

    val activeModels = if (isLocalEnabled && availableModels.isNotEmpty()) availableModels else defaultModels

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isLocalEnabled) "Local Models" else "Cloud Models", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            item {
                Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        activeModels.forEach { modelId ->
                            SettingsRow(
                                title = modelId,
                                onClick = { onModelSelected(modelId) },
                                control = {
                                    if (selectedModelId == modelId) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaSelectionScreen(
    selectedPersonaId: String,
    allPersonas: List<com.example.forgeint.domain.Persona>,
    onPersonaSelected: (String) -> Unit,
    onCreatePersonaClick: () -> Unit,
    onDeletePersonaClick: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Select Persona", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreatePersonaClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.Add, "Create")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            item {
                Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        allPersonas.forEach { persona ->
                            val isDefault = Personas.list.any { it.id == persona.id }
                            SettingsRow(
                                title = persona.name,
                                subtitle = persona.description,
                                onClick = { onPersonaSelected(persona.id) },
                                control = {
                                    Row {
                                        if (selectedPersonaId == persona.id) {
                                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                                        }
                                        if (!isDefault) {
                                            IconButton(onClick = { onDeletePersonaClick(persona.id) }) {
                                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            // Spacer for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageLengthSelectionScreen(
    selectedLength: String,
    onLengthSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val lengths = listOf("Shorter", "Normal", "Longer")
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Response Length", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            item {
                Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        lengths.forEach { length ->
                            SettingsRow(
                                title = length,
                                onClick = { onLengthSelected(length) },
                                control = {
                                    if (selectedLength == length) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionScreen(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val themes = remember { appThemes() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("App Theme", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            item {
                Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        themes.forEach { theme ->
                            SettingsRow(
                                title = theme,
                                onClick = { onThemeSelected(theme) },
                                control = {
                                    if (currentTheme == theme) {
                                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeySettingsScreen(
    apiKey: String,
    customApiKeyEnabled: Boolean,
    onApiKeyChanged: (String) -> Unit,
    onToggleCustomKey: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var draft by remember(apiKey) { mutableStateOf(apiKey) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("API Key", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Use Custom OpenRouter Key", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = customApiKeyEnabled, onCheckedChange = onToggleCustomKey)
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = {
                            draft = it
                            onApiKeyChanged(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("sk-or-v1-...") },
                        enabled = customApiKeyEnabled,
                        singleLine = true
                    )
                    Text(
                        text = if (customApiKeyEnabled) "Custom key active" else "Built-in key active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalModelScreen(
    status: LocalModelStatus,
    progress: Float,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Local LLM", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val statusText = when (status) {
                        LocalModelStatus.NotPresent -> "Not downloaded"
                        LocalModelStatus.Downloading -> "Downloading model..."
                        LocalModelStatus.Present -> "Ready on device"
                    }
                    Text("Gemma 2 2B IQ2 XXS", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (status == LocalModelStatus.Downloading) {
                        if (progress in 0f..1f) {
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("Downloading...", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onDownload,
                            enabled = status != LocalModelStatus.Downloading
                        ) { Text(if (status == LocalModelStatus.Present) "Re-download" else "Download") }
                        OutlinedButton(
                            onClick = onDelete,
                            enabled = status != LocalModelStatus.Downloading && status == LocalModelStatus.Present
                        ) { Text("Delete") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryManagementScreen(
    traits: List<UserTrait>,
    onDeleteTrait: (String) -> Unit,
    onAddManualMemory: (String, String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    var memoryDraft by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("long_term") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Memory System", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) }
                },
                actions = {
                    TextButton(onClick = onClearAll) { Text("Clear All") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = memoryDraft,
                            onValueChange = { memoryDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Add memory manually") },
                            placeholder = { Text("Example: User prefers quick answers") }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedType == "long_term",
                                onClick = { selectedType = "long_term" },
                                label = { Text("Long Term") }
                            )
                            FilterChip(
                                selected = selectedType == "short_term",
                                onClick = { selectedType = "short_term" },
                                label = { Text("Short Term") }
                            )
                        }
                        Button(
                            onClick = {
                                onAddManualMemory(memoryDraft, selectedType)
                                memoryDraft = ""
                            },
                            enabled = memoryDraft.isNotBlank()
                        ) {
                            Text("Save Memory")
                        }
                    }
                }
            }
            items(traits, key = { it.traitKey }) { trait ->
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(trait.traitKey, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(trait.traitValue, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(trait.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDeleteTrait(trait.traitKey) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete memory", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareMonitorScreen(
    currentHost: String,
    isFunnelEnabled: Boolean,
    localAuthToken: String,
    onBack: () -> Unit
) {
    val remoteStats by produceState(
        initialValue = LibreHardwareStats(endpointLabel = "connecting", errorMessage = "Connecting..."),
        currentHost,
        isFunnelEnabled,
        localAuthToken
    ) {
        while (true) {
            value = fetchLibreHardwareStats(
                host = currentHost,
                isFunnelEnabled = isFunnelEnabled,
                localAuthToken = localAuthToken
            )
            kotlinx.coroutines.delay(2200)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Hardware Monitor", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { HardwareStatTile("Endpoint", remoteStats.endpointLabel) }
            if (remoteStats.errorMessage != null) {
                item { HardwareStatTile("Status", remoteStats.errorMessage!!) }
            } else {
                item { HardwareStatTile("Status", "Connected") }
                val cpuThermalPct = parseThermalPercent(remoteStats.cpuTemp)
                val cpuThermalColor = thermalProgressColor(cpuThermalPct)
                val ramUsagePct = parseRamPercent(remoteStats.ramUsed, remoteStats.ramTotal)
                item { HardwareStatTile("CPU Temp", remoteStats.cpuTemp) }
                item { HardwareStatTile("CPU Power", remoteStats.cpuPower) }
                item {
                    HardwareProgressTile(
                        title = "CPU Thermal Load",
                        value = "${(cpuThermalPct * 100).toInt()}%",
                        progress = cpuThermalPct,
                        progressColor = cpuThermalColor
                    )
                }
                item { HardwareStatTile("dGPU Temp", remoteStats.dgpuTemp) }
                item { HardwareStatTile("dGPU Power", remoteStats.dgpuPower) }
                item { HardwareStatTile("RAM Used", remoteStats.ramUsed) }
                item { HardwareStatTile("RAM Total", remoteStats.ramTotal) }
                item {
                    HardwareProgressTile(
                        title = "RAM Usage",
                        value = "${(ramUsagePct * 100).toInt()}%",
                        progress = ramUsagePct
                    )
                }
                if (remoteStats.batteryWattage != "N/A") {
                    item { HardwareStatTile("Battery Wattage", remoteStats.batteryWattage) }
                }
            }
        }
    }
}

@Composable
private fun HardwareStatTile(title: String, value: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}


@Composable
private fun HardwareProgressTile(
    title: String,
    value: String,
    progress: Float,
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = progressColor
            )
        }
    }
}

private data class LibreHardwareStats(
    val endpointLabel: String,
    val cpuTemp: String = "N/A",
    val cpuPower: String = "N/A",
    val dgpuTemp: String = "N/A",
    val dgpuPower: String = "N/A",
    val ramUsed: String = "N/A",
    val ramTotal: String = "N/A",
    val batteryWattage: String = "N/A",
    val errorMessage: String? = null
)

private data class HardwareNode(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("Text") val Text: String = "",
    @SerializedName("Children") val Children: List<HardwareNode>? = null,
    @SerializedName("Min") val Min: String? = null,
    @SerializedName("Value") val Value: String? = null,
    @SerializedName("Max") val Max: String? = null,
    @SerializedName("ImageURL") val ImageURL: String? = null,
    @SerializedName("SensorId") val SensorId: String? = null
)

private data class HardwareResponse(
    @SerializedName("Children") val Children: List<HardwareNode>? = null
)

private suspend fun fetchLibreHardwareStats(
    host: String,
    isFunnelEnabled: Boolean,
    localAuthToken: String
): LibreHardwareStats = withContext(Dispatchers.IO) {
    val cleanHost = host.removePrefix("https://").removePrefix("http://").trim('/')
    if (cleanHost.isBlank()) {
        return@withContext LibreHardwareStats(
            endpointLabel = "not configured",
            errorMessage = "Set host in Settings first."
        )
    }

    val isTunnel = isFunnelEnabled ||
        cleanHost.contains("cloudflare") ||
        cleanHost.contains("ngrok") ||
        cleanHost.contains("loclx") ||
        cleanHost.endsWith(".ts.net", ignoreCase = true)

    val base = if (isTunnel) {
        "https://$cleanHost"
    } else {
        "http://$cleanHost:8080"
    }
    val endpoint = "$base/data.json"

    val requestBuilder = Request.Builder().url(endpoint)
    if (isTunnel) {
        requestBuilder.header("User-Agent", "ForgeIntApp")
        requestBuilder.header("cf-terminate-connection", "true")
        if (cleanHost.contains("ngrok", ignoreCase = true)) {
            requestBuilder.header("ngrok-skip-browser-warning", "true")
        }
    }
    if (localAuthToken.isNotBlank()) {
        requestBuilder.header("Authorization", "Bearer ${localAuthToken.trim()}")
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()

    return@withContext try {
        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                return@use LibreHardwareStats(endpointLabel = endpoint, errorMessage = "HTTP ${response.code}")
            }
            val raw = response.body?.string().orEmpty()
            if (raw.isBlank()) return@use LibreHardwareStats(endpointLabel = endpoint, errorMessage = "Empty response body.")

            val parsed = Gson().fromJson(raw, HardwareResponse::class.java)
            val children = parsed.Children ?: emptyList()
            if (children.isEmpty()) {
                return@use LibreHardwareStats(endpointLabel = endpoint, errorMessage = "No Libre Hardware nodes found.")
            }
            parseLibreNodes(endpoint, children)
        }
    } catch (e: Exception) {
        LibreHardwareStats(
            endpointLabel = endpoint,
            errorMessage = "${e.javaClass.simpleName}: ${e.message ?: "Unknown error"}"
        )
    }
}

private fun parseLibreNodes(endpoint: String, nodes: List<HardwareNode>): LibreHardwareStats {
    var cpuTemp = "N/A"
    var cpuPower = "N/A"
    var cpuTempPriority = -1
    var cpuPowerPriority = -1
    var dgpuTemp = "N/A"
    var dgpuPower = "N/A"
    var batt = "N/A"
    var ram = "N/A"
    var ramTotal = "N/A"
    var ramUsedMb: Float? = null
    var ramAvailMb: Float? = null

    fun traverse(node: HardwareNode, parentType: String = "", parentPath: String = "") {
        var currentType = parentType
        val textLower = node.Text.lowercase()
        val imageLower = node.ImageURL?.lowercase() ?: ""
        val sensorIdLower = node.SensorId?.lowercase() ?: ""
        val currentPath = if (parentPath.isEmpty()) textLower else "$parentPath/$textLower"

        if (imageLower.contains("nvidia") || textLower.contains("nvidia") || textLower.contains("geforce") || textLower.contains("rtx") || textLower.contains("gtx")) {
            currentType = "dGPU"
        } else if (textLower.contains("radeon") || textLower.contains("intel uhd") || textLower.contains("iris") || (textLower.contains("graphics") && parentType == "")) {
            currentType = "iGPU"
        } else if (imageLower.contains("cpu") || textLower.contains("ryzen") || textLower.contains("intel core") || textLower.contains("processor") || textLower.contains("cpu")) {
            currentType = "CPU"
        } else if (textLower.contains("memory") && parentType == "" && !textLower.contains("gpu") && !textLower.contains("graphics")) {
            currentType = "RAM"
        } else if (textLower.contains("battery")) {
            currentType = "Battery"
        }

        val value = node.Value ?: "N/A"
        val valueLower = value.lowercase()
        val hasTempUnit = value.contains("ï¿½") || value.contains("Â°") || valueLower.contains("c") || valueLower.contains("deg")
        val hasPowerUnit = valueLower.contains("w") || valueLower.contains("watt")
        if (value != "N/A" && value.any { it.isDigit() }) {
            val hasTempUnitNormalized = valueLower.contains("c") || valueLower.contains("deg")
            val hasPowerUnitNormalized = valueLower.contains("w") || valueLower.contains("watt")
            val isCpuNamedSensor =
                textLower.contains("cpu") ||
                    textLower.contains("ryzen") ||
                    textLower.contains("tdie") ||
                    textLower.contains("tctl") ||
                    textLower.contains("ccd")
            val isGpuNamedSensor =
                textLower.contains("gpu") ||
                    textLower.contains("nvidia") ||
                    textLower.contains("geforce") ||
                    textLower.contains("rtx") ||
                    textLower.contains("gtx")
            val isCpuPath = currentPath.contains("cpu") || currentPath.contains("ryzen") || currentPath.contains("processor")
            val isGpuPath = currentPath.contains("gpu") || currentPath.contains("nvidia") || currentPath.contains("geforce") || currentPath.contains("rtx") || currentPath.contains("gtx")
            val isCpuLikeSensor = (currentType == "CPU" || isCpuNamedSensor || isCpuPath) && !isGpuNamedSensor && !isGpuPath
            val isPerCorePower = textLower.contains("core #") ||
                textLower.contains("core ") ||
                textLower.contains("cores #") ||
                sensorIdLower.contains("core #") ||
                sensorIdLower.contains("/core/")
            val isPackageLikePower = textLower.contains("package") ||
                textLower.contains("total package") ||
                textLower.contains("cpu package") ||
                textLower.contains("ppt") ||
                sensorIdLower.contains("/package") ||
                sensorIdLower.contains("/ppt")

            if (isCpuLikeSensor) {
                if ((textLower.contains("temperature") || textLower.contains("tdie") || textLower.contains("tctl") || textLower.contains("package") || textLower.contains("core max") || textLower.contains("ccd")) &&
                    (hasTempUnitNormalized || hasTempUnit) &&
                    !textLower.contains("distance")
                ) {
                    val tempPriority = when {
                        textLower.contains("package") || textLower.contains("tdie") || textLower.contains("tctl") -> 4
                        textLower.contains("core max") || textLower.contains("average") -> 3
                        textLower.contains("ccd") -> 2
                        else -> 1
                    }
                    if (tempPriority >= cpuTempPriority) {
                        cpuTemp = value
                        cpuTempPriority = tempPriority
                    }
                }
                if (
                    (hasPowerUnitNormalized || hasPowerUnit) &&
                    (
                        textLower.contains("power") ||
                            textLower.contains("ppt") ||
                            textLower.contains("cores") ||
                            textLower.contains("soc") ||
                            (textLower.contains("package") && (currentType == "CPU" || isCpuNamedSensor || isCpuPath)) ||
                            sensorIdLower.contains("/power")
                        ) &&
                    !isGpuNamedSensor &&
                    !isGpuPath
                ) {
                    val powerPriority = when {
                        isPackageLikePower -> 5
                        textLower.contains("soc") -> 3
                        isPerCorePower -> 1
                        sensorIdLower.contains("/power") -> 2
                        else -> 1
                    }
                    if (powerPriority >= cpuPowerPriority) {
                        cpuPower = value
                        cpuPowerPriority = powerPriority
                    }
                }
            }

            when (currentType) {
                "CPU" -> {
                    if ((value.contains("ï¿½C") || value.contains("Â°C") || value.contains("C") || textLower.contains("temperature") || node.ImageURL?.contains("temperature") == true) &&
                        !textLower.contains("distance")
                    ) {
                        if (cpuTemp == "N/A" || textLower.contains("package") || textLower.contains("tdie")) {
                            cpuTemp = value
                        }
                    }
                    if (value.contains("W") || hasPowerUnit || hasPowerUnitNormalized || textLower.contains("power") || sensorIdLower.contains("/power")) {
                        if (cpuPower == "N/A" || textLower.contains("package") || textLower.contains("total")) {
                            cpuPower = value
                        }
                    }
                }
                "dGPU" -> {
                    if ((value.contains("ï¿½C") || value.contains("Â°C") || node.ImageURL?.contains("temperature") == true) && (dgpuTemp == "N/A" || textLower.contains("core"))) {
                        dgpuTemp = value
                    }
                    if ((value.contains("W") || hasPowerUnit || hasPowerUnitNormalized || textLower.contains("power")) && (dgpuPower == "N/A" || textLower.contains("tgp") || textLower.contains("total"))) {
                        dgpuPower = value
                    }
                }
                "Battery" -> {
                    if (value.contains("W") || hasPowerUnit || hasPowerUnitNormalized || textLower.contains("rate")) batt = value
                }
                "RAM" -> {
                    if (textLower.contains("used") && (value.contains("GB") || value.contains("MB"))) {
                        ram = value
                        val usedMb = parseMemoryToMb(value)
                        if (usedMb > 0f) ramUsedMb = usedMb
                    }
                    if ((textLower.contains("available") || textLower.contains("free")) && (value.contains("GB") || value.contains("MB"))) {
                        val availMb = parseMemoryToMb(value)
                        if (availMb > 0f) ramAvailMb = availMb
                    }
                    if ((textLower.contains("total") || textLower.contains("capacity")) && (value.contains("GB") || value.contains("MB"))) {
                        ramTotal = value
                    }
                }
            }
        }

        node.Children?.forEach { traverse(it, currentType, currentPath) }
    }

    nodes.forEach { traverse(it) }
    if (ramTotal == "N/A" && ramUsedMb != null && ramAvailMb != null) {
        ramTotal = formatMemoryFromMb(ramUsedMb!! + ramAvailMb!!)
    }

    return LibreHardwareStats(
        endpointLabel = endpoint,
        cpuTemp = cpuTemp,
        cpuPower = cpuPower,
        dgpuTemp = dgpuTemp,
        dgpuPower = dgpuPower,
        ramUsed = ram,
        ramTotal = ramTotal,
        batteryWattage = batt
    )
}

private fun parseValue(valueString: String): Float {
    return valueString.filter { it.isDigit() || it == '.' }.toFloatOrNull() ?: 0f
}


private fun parseThermalPercent(tempString: String): Float {
    val celsius = parseValue(tempString)
    return (celsius / 100f).coerceIn(0f, 1f)
}

private fun thermalProgressColor(progress: Float): Color {
    return when {
        progress < 0.60f -> Color(0xFF4CAF50)
        progress < 0.80f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

private fun parseRamPercent(used: String, total: String): Float {
    val usedMb = parseMemoryToMb(used)
    val totalMb = parseMemoryToMb(total)
    if (usedMb <= 0f || totalMb <= 0f) return 0f
    return (usedMb / totalMb).coerceIn(0f, 1f)
}

private fun parseMemoryToMb(valueString: String): Float {
    val value = parseValue(valueString)
    return when {
        valueString.contains("GB", ignoreCase = true) -> value * 1024f
        valueString.contains("MB", ignoreCase = true) -> value
        else -> 0f
    }
}

private fun formatMemoryFromMb(valueMb: Float): String {
    return if (valueMb >= 1024f) String.format("%.1f GB", valueMb / 1024f) else String.format("%.0f MB", valueMb)
}
