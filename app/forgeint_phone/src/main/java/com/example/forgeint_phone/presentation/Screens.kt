@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.forgeint_phone.presentation
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.forgeint_phone.data.Conversation
import com.example.forgeint_phone.data.Message
import com.example.forgeint_phone.domain.Persona
import com.example.forgeint_phone.domain.Personas
import kotlinx.coroutines.flow.StateFlow

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
        topBar = {
            TopAppBar(
                title = { Text("Conversations") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewChatClick
            ) {
                Icon(Icons.Default.Add, "New Chat")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                label = { Text("Search...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val itemsToDisplay = if (searchQuery.isNotBlank()) searchResults else history

            if (itemsToDisplay.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No conversations yet.")
                }
            } else {
                 LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(itemsToDisplay, key = { it.id }) { chat ->
                        ChatItemCard(
                            chat = chat,
                            onChatClick = onChatClick,
                            onDeleteClick = onDeleteClick,
                            onBookmarkClick = onBookmarkClick
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatItemCard(
    chat: Conversation,
    onChatClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onBookmarkClick: (Long, Boolean) -> Unit
) {
    Card(
        onClick = { onChatClick(chat.id) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = chat.summary,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Row {
                 IconButton(onClick = { onBookmarkClick(chat.id, chat.isBookmarked) }) {
                    Icon(
                        if (chat.isBookmarked) Icons.Filled.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark"
                    )
                }
                IconButton(onClick = { onDeleteClick(chat.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
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
    onSendMessage: (String) -> Unit,
    onDeleteChat: () -> Unit
) {
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val isThinking by isThinkingFlow.collectAsStateWithLifecycle()

    LaunchedEffect(messages.size, streamingMessage, isThinking) {
        if (messages.isNotEmpty() || streamingMessage?.isNotEmpty() == true || isThinking) {
            listState.animateScrollToItem(messages.size) // +1 if thinking/streaming but size is index-based
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                actions = {
                    IconButton(onClick = onDeleteChat) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Chat")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("Type a message...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
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
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
            }

            if (isThinking) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reasoning...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (streamingMessage != null) {
                item {
                    MessageBubble(message = Message(text = streamingMessage, isUser = false, conversationId = 0L))
                }
            }

            if (isLoading && streamingMessage == null) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}


@Composable
fun MessageBubble(message: Message) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    } else {
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(shape)
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(text = message.text)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isLiteMode: Boolean,
    onToggleLiteMode: (Boolean) -> Unit,
    currentModelId: String,
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
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text("Display", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    ListItem(
                        headlineContent = { Text("Lite Mode") },
                        supportingContent = { Text("A simpler UI for faster interactions.") },
                        trailingContent = {
                            Switch(
                                checked = isLiteMode,
                                onCheckedChange = onToggleLiteMode
                            )
                        }
                    )
                }
            }
            item {
                Divider()
            }
            item {
                Column {
                    Text("Response", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    ListItem(
                        headlineContent = { Text("Response Length") },
                        supportingContent = { Text(messageLength) },
                        modifier = Modifier.clickable(onClick = onNavigateToMessageLength)
                    )
                }
            }
            item {
                Divider()
            }
            item {
                Column {
                    Text("Model", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    ListItem(
                        headlineContent = { Text("Current Model") },
                        supportingContent = { Text(currentModelId) },
                        modifier = Modifier.clickable(onClick = onNavigateToModelSelect)
                    )
                }
            }
            item {
                Divider()
            }
            item {
                Column {
                    Text("Persona", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    ListItem(
                        headlineContent = { Text("Current Persona") },
                        supportingContent = { Text(Personas.findById(currentModelId).name) },
                        modifier = Modifier.clickable(onClick = onNavigateToPersona)
                    )
                }
            }
            item {
                Divider()
            }
            item {
                Column {
                    Text("Local Inference", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    ListItem(
                        headlineContent = { Text("Use Local Server") },
                        supportingContent = { Text(if (isLocalEnabled) "Using local server" else "Using cloud server") },
                        trailingContent = {
                            Switch(
                                checked = isLocalEnabled,
                                onCheckedChange = onToggleLocal
                            )
                        }
                    )
                    if (isLocalEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = currentHost,
                            onValueChange = onHostChange,
                            label = { Text("Host Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = currentPort,
                            onValueChange = onPortChange,
                            label = { Text("Port") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onTestConnection,
                            enabled = !isTesting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Text("Test Connection")
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
            }
        }
    }
}

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

    val activeModels = if (isLocalEnabled) {
        if (availableModels.isNotEmpty()) {
            availableModels
        } else {
            emptyList()
        }
    } else {
        defaultModels
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isLocalEnabled) "Local Models" else "Cloud Models") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.padding(it)
        ) {
            if (activeModels.isEmpty() && isLocalEnabled) {
                item {
                    Text(
                        "No models found. Check connection.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            items(activeModels) { modelId ->
                ListItem(
                    headlineContent = { Text(modelId) },
                    modifier = Modifier.clickable {
                        onModelSelected(modelId)
                    },
                    trailingContent = {
                        if (selectedModelId == modelId) {
                            Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun PersonaSelectionScreen(
    selectedPersonaId: String,
    onPersonaSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select a Persona") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.padding(it)
        ) {
            items(Personas.list) { persona ->
                ListItem(
                    headlineContent = { Text(persona.name) },
                    supportingContent = { Text(persona.description) },
                    modifier = Modifier.clickable {
                        onPersonaSelected(persona.id)
                    },
                    trailingContent = {
                        if (selectedPersonaId == persona.id) {
                            Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                )
            }
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
        topBar = {
            TopAppBar(
                title = { Text("Select Response Length") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.padding(it)
        ) {
            items(lengths) { length ->
                ListItem(
                    headlineContent = { Text(length) },
                    modifier = Modifier.clickable {
                        onLengthSelected(length)
                    },
                    trailingContent = {
                        if (selectedLength == length) {
                            Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                )
            }
        }
    }
}
