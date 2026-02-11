@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.forgeint.presentation

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.forgeint.data.Conversation
import com.example.forgeint.data.Message
import com.example.forgeint.domain.Personas
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
                            onBookmarkClick = onBookmarkClick
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
    onBookmarkClick: (Long, Boolean) -> Unit
) {
    Card(
        onClick = { onChatClick(chat.id) },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
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
                    text = "Tap to view",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
            
            IconButton(onClick = { onBookmarkClick(chat.id, chat.isBookmarked) }) {
                Icon(
                    if (chat.isBookmarked) Icons.Filled.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (chat.isBookmarked) Color(0xFFFDD835) else Color.Gray
                )
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
    onDeleteChat: () -> Unit,
    onModelChangeClick: () -> Unit,
    currentModelName: String,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val isThinking by isThinkingFlow.collectAsStateWithLifecycle()

    LaunchedEffect(messages.size, streamingMessage, isThinking) {
        if (messages.isNotEmpty() || streamingMessage?.isNotEmpty() == true || isThinking) {
            listState.animateScrollToItem(messages.size)
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
            // Pill Shape Input Field
            Surface(
                color = Color.Transparent,
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Message...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 4
                    )
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading
                    ) {
                        Icon(
                            Icons.Default.Send, 
                            "Send", 
                            tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.tertiary else Color.Gray
                        )
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

            if (isThinking) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
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

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
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
    onBack: () -> Unit
) {
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
                                value = currentHost,
                                onValueChange = onHostChange,
                                label = { Text("Host Address") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = currentPort,
                                onValueChange = onPortChange,
                                label = { Text("Port") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
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
