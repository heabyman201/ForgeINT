
package com.example.forgeint.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.rotate
import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import android.os.SystemClock
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.*
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.exp
import android.app.RemoteInput
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Create
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment

import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.InlineSliderDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListState
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TitleCard
import androidx.wear.compose.material.ToggleButton
import androidx.wear.compose.material.ToggleButtonDefaults
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.input.RemoteInputIntentHelper
import com.example.weargemini.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

import androidx.compose.ui.draw.rotate

import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle

import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

import android.graphics.SweepGradient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.runtime.getValue

import androidx.compose.ui.draw.rotate

import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.compose.material.icons.filled.Mic
import com.example.forgeint.presentation.theme.LocalForgeIntColors
val ColorBlack = Color.Black
val ColorLiteAccent = Color(0xFFDCB17E)
val ColorSettings = Color(0xFF75AEFD)
private val ColorUserBubble = Color(0xFF2B303B)
@Composable
fun Modifier.rotaryScrollable(
    scrollState: ScalingLazyListState,
    focusRequester: FocusRequester
): Modifier {
    val coroutineScope = rememberCoroutineScope()

    return this
        .onRotaryScrollEvent {
            coroutineScope.launch {
                // Adjust the '50f' multiplier to change scrolling speed/sensitivity
                scrollState.scrollBy(it.verticalScrollPixels * 2f)
            }
            true // Consume the event
        }
        .focusRequester(focusRequester)
        .focusable()
}

// ==========================================
// 1. HISTORY SCREENS (Menu)
// ==========================================
val GeminiGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF4285F4), // Google Blue
        Color(0xFF9B72CB), // Purple
        Color(0xFFD96570)  // Pink/Red
    ),
    tileMode = TileMode.Mirror
)
@Composable
fun ChatItemRow(
    chat: Conversation,
    onChatClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onBookmarkClick: (Long, Boolean) -> Unit
) {
    var isDeleting by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val colors = LocalForgeIntColors.current

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isDeleting) 0f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "alpha"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isDeleting) 0.8f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "scale"
    )
    val animatedTranslationY by animateFloatAsState(
        targetValue = if (isDeleting) -100f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "translationY"
    )

    LaunchedEffect(isDeleting) {
        if (isDeleting) {
            delay(500)
            onDeleteClick(chat.id)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
                translationY = animatedTranslationY
            }
    ) {
        TitleCard(
            backgroundPainter = CardDefaults.cardBackgroundPainter(
                startBackgroundColor = if (isDeleting) Color(0xFFB41C1C) else colors.surface,
                endBackgroundColor = if (isDeleting) Color(0xFFB71C1C) else colors.surface
            ),
            contentColor = if (isDeleting) Color.White else colors.onPrimary,
            onClick = { },
            title = {
                Text(
                    text = chat.summary,
                    maxLines = 2,
                    color = colors.userText,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 26.dp)
                )
            },
            time = { },
            content = {
                Text(
                    text = if (isDeleting) "Deleting..." else "Tap to view chat",
                    color = if (isDeleting) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    modifier = Modifier.padding(end = 26.dp)
                )
            }
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp))
                .combinedClickable(
                    onClick = {
                        if (!isDeleting) {
                            onChatClick(chat.id)
                            convoID.conID.value = chat.id.toInt()
                        }
                    },
                    onLongClick = {
                        convoID.conID.value = chat.id.toInt()
                        isDeleting = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
        )

        if (!isDeleting) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
            ) {
                ToggleButton(
                    checked = chat.isBookmarked,
                    onCheckedChange = { onBookmarkClick(chat.id, chat.isBookmarked) },
                    modifier = Modifier.size(24.dp),
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        uncheckedBackgroundColor = Color.Transparent,
                        checkedBackgroundColor = Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = if (chat.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (chat.isBookmarked) Color(0xFFFDD835) else Color.Gray
                    )
                }
            }
        }
    }
}
@Composable
fun DetailedHistoryScreen(
    searchResults: List<Conversation>,
    onNewChatClick: () -> Unit,
    onChatClick: (Long) -> Unit,
    onBookmarkClick: (Long, Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onDeleteClick: (Long) -> Unit,
    onNavigateToMemory: () -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    viewModel: GeminiViewModel // Add ViewModel to access connection mode
) {
    val focusRequester = remember { FocusRequester() }
    val listState = rememberTransformingLazyColumnState()
    val colors = LocalForgeIntColors.current

    var isSearchActive by remember { mutableStateOf(false) }

    val groupedHistory = remember(searchResults) {
        derivedStateOf {
            val now = System.currentTimeMillis()
            searchResults.groupBy { chat ->
                val diff = now - chat.timestamp
                when {
                    diff < 24 * 60 * 60 * 1000L && isSameDay(now, chat.timestamp) -> "Today"
                    diff < 48 * 60 * 60 * 1000L -> "Yesterday"
                    else -> "Past Chats"
                }
            }
        }
    }

    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(isDeleting) {
        if (isDeleting) {
            delay(500)
            onDeleteClick(convoID.conID.value.toLong())
        }
    }

    Scaffold(
        positionIndicator = { ScrollIndicator(state = listState) },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        TransformingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .focusable(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                if (isSearchActive) {
                    CompactSearchInput(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChanged,
                        onClose = {
                            isSearchActive = false
                            onSearchQueryChanged("")
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Network Indicator at the top
                        NetworkIndicator(viewModel = viewModel)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactButton(
                                onClick = { isSearchActive = true },
                                colors = ButtonDefaults.secondaryButtonColors(),
                                modifier = Modifier.padding(end = 2.dp)
                            ) {
                                Icon(Icons.Default.Search, "Search", tint = Color.White)
                            }
                            CompactButton(
                                onClick = onNavigateToMemory,
                                colors = ButtonDefaults.secondaryButtonColors(),
                                modifier = Modifier.padding(end = 2.dp)
                            ){
                                Icon(Icons.Default.Person, "Memory", tint = colors.primary)
                            }
                            CompactButton(
                                onClick = onSettingsClick,
                                colors = ButtonDefaults.secondaryButtonColors(),
                                modifier = Modifier.padding(end = 2.dp)
                            ) {
                                Icon(Icons.Default.Settings, "Settings", tint = colors.settingsIcon)
                            }
                        }
                    }
                }
            }

            if (!isSearchActive || searchQuery.isEmpty()) {
                item {
                    Chip(
                        onClick = onNewChatClick,
                        label = { Text("New Conversation") },
                        icon = { Icon(Icons.Rounded.Add, "Create") },
                        colors = ChipDefaults.gradientBackgroundChipColors(
                            startBackgroundColor = colors.surface,
                            endBackgroundColor = colors.surface.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            groupedHistory.value.forEach { (header, chats) ->
                item {
                    Text(
                        text = header.uppercase(),
                        style = MaterialTheme.typography.caption2,
                        color = if (header == "Today") colors.primary else Color.Gray,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
                    )
                }

                items(chats, key = { it.id }) { chat ->
                    ChatItemRow(
                        chat = chat,
                        onChatClick = {
                            onChatClick(chat.id)
                            convoID.conID.value = chat.id.toInt()
                        },
                        onDeleteClick = {
                            onDeleteClick(chat.id)
                            isDeleting = true
                        },
                        onBookmarkClick = onBookmarkClick
                    )
                }
            }

            if (searchResults.isEmpty()) {
                item {
                    Text(
                        "No Messages.\nStart a new chat.",
                        style = MaterialTheme.typography.caption2,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                    )
                }
            }
        }
    }
}



fun isSameDay(t1: Long, t2: Long): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = t1 }
    val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = t2 }
    return cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR) &&
            cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR)
}


// --- Helper Composable for the Input ---
@Composable
fun CompactSearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF202124), CircleShape)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = MaterialTheme.typography.body2.copy(color = Color.White),
            singleLine = true,

            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            "Search...",
                            color = Color.Gray,
                            style = MaterialTheme.typography.caption2
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Close,
                "Close",
                tint = Color.Red,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
@Composable
fun LiteHistoryScreen(
    history: List<Conversation>,
    onNewChatClick: () -> Unit,
    onChatClick: (Long) -> Unit,
    onBookmarkClick: (Long, Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: GeminiViewModel
) {
    val listState = rememberTransformingLazyColumnState()
    val focusRequester = remember { FocusRequester() }
    val colors = LocalForgeIntColors.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Scaffold(
        positionIndicator = { ScrollIndicator(state = listState) },


    ) {
        TransformingLazyColumn(
            modifier = Modifier.fillMaxSize().background(colors.background),

            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Network Indicator + Settings Icon
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(0.2f), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(onClick = onSettingsClick)
                        )
                    }
                }
            }

            // Manual "Chip" (Row + Box)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .clickable(onClick = onNewChatClick)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Create, null, tint = colors.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("New Chat", color = Color.White)
                }
            }

            // Manual List Items
            items(
                items = history,
                key = { it.id },
                contentType = { "history_item" }
            ) { chat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface)
                        .clickable { onChatClick(chat.id); convoID.conID.value = chat.id.toInt() }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.summary,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Simple clickable region for bookmark
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onBookmarkClick(chat.id, chat.isBookmarked) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (chat.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = if (chat.isBookmarked) Color(0xFFFDD835) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. CHAT SCREENS (Conversation)
// ==========================================
fun String.toDisplayName(): String {
    return this.split("/")
        .lastOrNull()
        ?.replace("-it", "")
        ?.replace(":free", "")
        ?.replace("-", " ")
        ?.uppercase()
        ?: "Unknown Model"
}
@Composable
fun DetailedChatScreen(
    messages: List<Message>,
    streamingMessageFlow: StateFlow<String?>,
    isThinkingFlow: StateFlow<Boolean>,
    onReplyClick: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onSwapModelClick: () -> Unit,
    isLoading: Boolean,
    onBookmarkClick: (Long, Boolean) -> Unit,
    loadMoreMessages: () -> Unit,
    isHistoryLoading: Boolean,
    currentConversationId: Long,
    isBookmarked: Boolean,
    onTryAgain: () -> Unit,
    onWebsearchClick: () -> Unit,
    isVoiceDominant: Boolean = false
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val rawModelName by settingsManager.selectedModel.collectAsStateWithLifecycle(initialValue = "")
    val currentFullText by streamingMessageFlow.collectAsStateWithLifecycle(initialValue = null)
    val hasStreamingText by remember(currentFullText) {
        derivedStateOf { !currentFullText.isNullOrEmpty() }
    }
    val colors = LocalForgeIntColors.current
    var isVoiceActive by remember { mutableStateOf(false) }
    val sentenceDelimiters = remember { charArrayOf('.', '!', '?', '\n') }

    val showBezelLoadingOverlay = isLoading && !hasStreamingText
    val coroutineScope = rememberCoroutineScope()
    val isThinking by isThinkingFlow.collectAsStateWithLifecycle()
    
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isVoiceActive = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                onVoiceResult(spokenText)
            }
        }
    }

    val triggerVoiceInput = {
        isVoiceActive = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Assistant")
        }
        voiceLauncher.launch(intent)
    }

    // --- TTS Logic ---
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    val isTtsReady = remember { mutableStateOf(false) }
    var lastReadIndex by remember { mutableIntStateOf(0) }

    // Keep TTS fully disabled unless voice-dominant mode is active to save battery.
    DisposableEffect(context, isVoiceDominant) {
        if (!isVoiceDominant) {
            tts.value?.stop()
            tts.value?.shutdown()
            tts.value = null
            isTtsReady.value = false
            onDispose { }
        } else {
            val speech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady.value = true
                }
            }
            tts.value = speech
            onDispose {
                speech.stop()
                speech.shutdown()
                tts.value = null
                isTtsReady.value = false
            }
        }
    }

    // Handle TTS Progress and Auto-Mic
    LaunchedEffect(isTtsReady.value, isVoiceDominant) {
        if (!isVoiceDominant || !isTtsReady.value) return@LaunchedEffect
        tts.value?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId == "final_segment" && isVoiceDominant) {
                    coroutineScope.launch {
                        delay(500)
                        triggerVoiceInput()
                    }
                }
            }
            override fun onError(utteranceId: String?) {}
        })
    }

    fun findLastDelimiterIndex(text: String, startIndex: Int, delimiters: CharArray): Int {
        if (startIndex >= text.length) return -1
        val unread = text.substring(startIndex)
        var lastLocalIndex = -1
        delimiters.forEach { delimiter ->
            val idx = unread.lastIndexOf(delimiter)
            if (idx > lastLocalIndex) lastLocalIndex = idx
        }
        return if (lastLocalIndex >= 0) startIndex + lastLocalIndex else -1
    }

    // Read segments during streaming
    LaunchedEffect(currentFullText, isTtsReady.value, isVoiceDominant) {
        if (!isVoiceDominant || !isTtsReady.value || currentFullText.isNullOrEmpty()) {
            if (currentFullText == null && lastReadIndex > 0) {
                lastReadIndex = 0
            }
            return@LaunchedEffect
        }

        val textToScan = currentFullText!!
        val lastPunct = findLastDelimiterIndex(textToScan, lastReadIndex, sentenceDelimiters)

        if (lastPunct >= lastReadIndex) {
            val segment = textToScan.substring(lastReadIndex, lastPunct + 1).trim()
            if (segment.isNotEmpty()) {
                tts.value?.speak(segment, TextToSpeech.QUEUE_ADD, null, "segment_$lastReadIndex")
                lastReadIndex = lastPunct + 1
            }
        }
    }

    val isStreamingObserved by remember(currentFullText) {
        derivedStateOf { !currentFullText.isNullOrEmpty() }
    }
    val prevIsStreaming = remember { mutableStateOf(false) }
    
    // Read the final part when streaming finishes
    LaunchedEffect(isStreamingObserved, isVoiceDominant, isTtsReady.value) {
        if (!isVoiceDominant || !isTtsReady.value) {
            prevIsStreaming.value = isStreamingObserved
            return@LaunchedEffect
        }

        if (prevIsStreaming.value && !isStreamingObserved) {
            // Streaming just finished, speak the last message's remaining text
            val lastMsg = messages.lastOrNull()?.text ?: ""
            val remainingText = if (lastMsg.length > lastReadIndex) lastMsg.substring(lastReadIndex).trim() else ""
            if (remainingText.isNotEmpty()) {
                tts.value?.speak(remainingText, TextToSpeech.QUEUE_ADD, null, "final_segment")
            } else {
                // Nothing left to speak, but we need to trigger mic if we didn't just speak
                triggerVoiceInput()
            }
        }
        prevIsStreaming.value = isStreamingObserved
    }

    val isStreaming by remember(currentFullText) {
        derivedStateOf { !currentFullText.isNullOrEmpty() }
    }

    val displayName = remember(rawModelName) {
        when (rawModelName) {
            "google/gemma-3n-e4b-it:free" -> "Gemma E4B"
            "google/gemma-3n-e2b-it:free" -> "Gemma E2B"
            "google/gemma-3-27b-it:free" -> "Gemma 3.27B"
            "google/gemma-2-9b-it:free" -> "Gemma 2.9B"
            "openai/gpt-oss-20b:free" -> "GPT OSS 20B"
            "meta-llama/llama-3.3-70b-instruct:free" -> "Llama 3.3 70B"
            "cognitivecomputations/dolphin-mistral-24b-venice-edition:free" -> "Dolphin Mistral 24B"
            else -> "AI Assistant"
        }
    }

    val userShape = remember { RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) }
    val geminiShape = remember { RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp) }
    
    // Dynamic Colors
    val userTextColor = colors.userText
    val geminiTextColor = colors.botText

    val reachedTop by remember {
        derivedStateOf {
            val firstItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            firstItem?.index == 0 && firstItem.offset >= 0
        }
    }
    var lastLoadMoreAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(reachedTop, isHistoryLoading) {
        if (reachedTop && !isHistoryLoading) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastLoadMoreAt > 800L) {
                lastLoadMoreAt = now
                loadMoreMessages()
            }
        }
    }

    val historyOffset = if (isHistoryLoading) 1 else 0
    val bookmarkOffset = 0
    val messagesOffset = messages.size
    val streamingIndex = historyOffset + bookmarkOffset + messagesOffset
    val replyChipIndex = if (isVoiceDominant) streamingIndex + 1 else streamingIndex + 1

    // Avoid unnecessary scroll animations to reduce work and battery drain.
    var lastAutoScrollTarget by remember { mutableIntStateOf(-1) }
    LaunchedEffect(messages.size, isStreaming, isThinking) {
        if (messages.isNotEmpty()) {
            val target = if (isStreaming || isThinking) {
                replyChipIndex
            } else {
                (streamingIndex - 1).coerceAtLeast(0)
            }

            if (target != lastAutoScrollTarget) {
                if (isStreaming || isThinking) {
                    listState.scrollToItem(target)
                } else {
                    listState.animateScrollToItem(target)
                }
                lastAutoScrollTarget = target
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val showRam = settingsManager.isMemoryMonitorEnabled.collectAsStateWithLifecycle(initialValue = false)
    val showTelemetry = settingsManager.isSystemTelemetryEnabled.collectAsStateWithLifecycle(initialValue = false)

    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .rotaryScrollable(listState, focusRequester)
                .focusable(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isHistoryLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        OrbitalThinkingIndicator()
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                MessageCard(
                    msg = msg,
                    userShape = userShape,
                    geminiShape = geminiShape,
                    userTextColor = userTextColor,
                    geminiTextColor = geminiTextColor,
                    displayName = displayName
                )
            }

            if (isThinking) {
                item {
                    CompactChip(
                        onClick = { },
                        label = { Text("Reasoning...", style = MaterialTheme.typography.caption2) },
                        icon = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                indicatorColor = colors.primary
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors(
                            backgroundColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.LightGray
                        ),
                    )
                }
            }

            item {
                StreamingMessageCard(
                    streamingFlow = streamingMessageFlow,
                    userTextColor = userTextColor,
                    geminiTextColor = geminiTextColor,
                    isUser = false,
                    displayName = displayName,
                    onTryAgain = onTryAgain
                )
            }

            if (showRam.value) {
                item { MemoryMonitorOverlay() }
            }
            if (showTelemetry.value) {
                item { PerformanceMonitorOverlay() }
                item { SystemTelemetryOverlay() }
            }

            if (isVoiceDominant) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isVoiceActive) {
                            VoicePulseEffect(
                                color = colors.primary,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                                .clickable { triggerVoiceInput() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Chip(
                            onClick = onReplyClick,
                            label = { Text("Keyboard", style = MaterialTheme.typography.caption2, maxLines = 1) },
                            icon = { Icon(Icons.Default.Keyboard, null, modifier = Modifier.size(16.dp)) },
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Chip(
                            onClick = onSwapModelClick,
                            label = { Text("Model", style = MaterialTheme.typography.caption2, maxLines = 1) },
                            icon = { Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp)) },
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(43.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF1B1B1B))
                                .border(
                                    border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .clickable(onClick = onReplyClick)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Type a reply...",
                                style = MaterialTheme.typography.caption2,
                                color = Color(0xFFAFAFAF),
                                maxLines = 1
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4285F4))
                                .clickable {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to $displayName")
                                    }
                                    voiceLauncher.launch(intent)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Reply",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(14.dp))
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Chip(
                            onClick = onSwapModelClick,
                            label = { Text("Model", style = MaterialTheme.typography.caption3) },
                            icon = { Icon(Icons.Default.AutoAwesome, "Model", modifier = Modifier.size(14.dp)) },
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Chip(
                            onClick = onDeleteClick,
                            label = { Text("Delete", style = MaterialTheme.typography.caption3) },
                            icon = { Icon(Icons.Default.Delete, "Delete Chat", modifier = Modifier.size(14.dp)) },
                            colors = ChipDefaults.secondaryChipColors(
                                backgroundColor = Color(0xFF4A1111),
                                contentColor = Color(0xFFFFB4B4)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (isVoiceDominant) {
                item {
                    Chip(
                        onClick = onDeleteClick,
                        label = { Text("Delete Chat") },
                        icon = { Icon(Icons.Default.Delete, "Delete Chat") },
                        colors = ChipDefaults.gradientBackgroundChipColors(
                            startBackgroundColor = Color.Red,
                            endBackgroundColor = Color.DarkGray,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
                    )
                }
            }
        }
    }
    if (showBezelLoadingOverlay) {
        GeminiBezelLoadingIndicator()
    }
}

@Composable
fun VoicePulseEffect(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VoicePulse")
    val pulse1 = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse1"
    )
    val alpha1 = infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Alpha1"
    )

    val pulse2 = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse2"
    )
    val alpha2 = infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Alpha2"
    )

    Canvas(modifier = modifier) {
        val baseRadius = size.minDimension / 2f
        
        drawCircle(
            color = color,
            radius = baseRadius * pulse1.value,
            alpha = alpha1.value,
            style = Stroke(width = 2.dp.toPx())
        )
        
        drawCircle(
            color = color,
            radius = baseRadius * pulse2.value,
            alpha = alpha2.value,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun OrbitalThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing))
    )

    Canvas(modifier = Modifier.size(24.dp).padding(2.dp)) {
        drawArc(
            color = Color.Cyan,
            startAngle = angle,
            sweepAngle = 60f,
            useCenter = false,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}
@Composable
fun GeminiBezelLoadingIndicator() {
    val transition = rememberInfiniteTransition(label = "GeminiBezelSpin")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "Rotation"
    )

    val colors = LocalForgeIntColors.current
    val background = colors.background.toArgb()
    val primary = colors.primary.toArgb()
    val colorArray = intArrayOf(background, primary, background)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .rotate(rotation)
    ) {
        val strokeWidth = 4.dp.toPx()
        // Sticky Logic: Inset by half the stroke width so the outer edge hits the bezel
        val stickyPadding = strokeWidth / 2f

        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                strokeCap = android.graphics.Paint.Cap.ROUND
                shader = SweepGradient(
                    size.width / 2f,
                    size.height / 2f,
                    colorArray,
                    null
                )
            }

            // Core Pass (The sharp line - cheap and efficient)
            // Removed BlurMaskFilter for battery optimization
            canvas.nativeCanvas.drawArc(
                stickyPadding, stickyPadding,
                size.width - stickyPadding, size.height - stickyPadding,
                0f, 290f, false, paint
            )
        }
    }
}
@Composable
fun StreamingMessageCard(
    streamingFlow: StateFlow<String?>,
    isUser: Boolean,
    userTextColor: Color,
    geminiTextColor: Color,
    displayName: String,
    onTryAgain: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf<Size>(Size.Zero) }
    val colors = LocalForgeIntColors.current

    val rawText by streamingFlow.collectAsStateWithLifecycle()

    val hasText by remember(rawText) {
        derivedStateOf { !rawText.isNullOrEmpty() }
    }

    val hasError by remember(rawText) {
        derivedStateOf {
            val text = rawText ?: ""
            text.contains("Error", ignoreCase = true) ||
                    text.contains("HTTP", ignoreCase = true) ||
                    text.contains("Exception", ignoreCase = true) ||
                    text.contains("failed", ignoreCase = true) ||
                    text.contains("ConnectException", ignoreCase = true) ||
                    text.contains("Timeout", ignoreCase = true)
        }
    }

    val formattedText by remember(rawText, userTextColor) {
        derivedStateOf {
            val text = rawText ?: ""
            RichTextFormatter.format(text + " ▋", userTextColor, fastMode = true)
        }
    }

    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isUser) colors.userBubble else colors.botBubble
    val textColor = if (isUser) userTextColor else geminiTextColor
    val senderName = if (isUser) "You" else "$displayName (Typing...)"
    val senderColor = if (isUser) userTextColor else colors.primary

    if (hasText) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            val senderTextLayout = remember(senderName, senderColor, canvasSize.width) {
                textMeasurer.measure(
                    text = AnnotatedString(senderName),
                    style = TextStyle(
                        color = senderColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Normal
                    ),
                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = (canvasSize.width * 0.9f).toInt())
                )
            }

            val messageTextLayout = remember(formattedText, canvasSize.width) {
                textMeasurer.measure(
                    text = formattedText,
                    style = TextStyle(
                        color = textColor,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Normal
                    ),
                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = (canvasSize.width * 0.9f).toInt()),
                    overflow = TextOverflow.Clip,
                    softWrap = true
                )
            }

            val bubbleWidth = remember(senderTextLayout.size.width, messageTextLayout.size.width, canvasSize.width) {
                maxOf(senderTextLayout.size.width, messageTextLayout.size.width) + 32.dp.value
            }
            val bubbleHeight = remember(senderTextLayout.size.height, messageTextLayout.size.height) {
                senderTextLayout.size.height + messageTextLayout.size.height + 24.dp.value
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(with(LocalDensity.current) { bubbleHeight.toDp() })
            ) {
                canvasSize = size
                val cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())

                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(bubbleColor, bubbleColor)),
                    topLeft = Offset(if (isUser) size.width - bubbleWidth else 0f, 0f),
                    size = Size(bubbleWidth, bubbleHeight),
                    cornerRadius = cornerRadius,
                )

                drawText(
                    textLayoutResult = senderTextLayout,
                    topLeft = Offset(
                        x = if (isUser) size.width - bubbleWidth + 16.dp.toPx() else 16.dp.toPx(),
                        y = 8.dp.toPx()
                    )
                )

                drawText(
                    textLayoutResult = messageTextLayout,
                    topLeft = Offset(
                        x = if (isUser) size.width - bubbleWidth + 16.dp.toPx() else 16.dp.toPx(),
                        y = senderTextLayout.size.height + 12.dp.toPx()
                    )
                )
            }

            if (hasError) {
                CompactChip(
                    onClick = onTryAgain,
                    label = { Text("Retry") },
                    icon = { Icon(Icons.Default.Refresh, "Retry") },
                    colors = ChipDefaults.primaryChipColors(
                        backgroundColor = Color(0xFFB71C1C)
                    ),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
@Composable
fun PerformanceMonitorOverlay() {
    var cpuUsage by remember { mutableStateOf("CPU: --%") }
    // FPS monitoring removed to save battery
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        var lastCpuTime = android.os.Process.getElapsedCpuTime()
        var lastAppTime = SystemClock.uptimeMillis()

        while (true) {
            delay(10000) // Poll every 10 seconds instead of 3
            val currentCpuTime = android.os.Process.getElapsedCpuTime()
            val currentAppTime = SystemClock.uptimeMillis()
            val cpuDelta = currentCpuTime - lastCpuTime
            val timeDelta = currentAppTime - lastAppTime

            if (timeDelta > 0) {
                val usage = (cpuDelta * 100) / timeDelta
                cpuUsage = "CPU: $usage%"
            }

            lastCpuTime = currentCpuTime
            lastAppTime = currentAppTime
        }
    }

    Column(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(4.dp)
    ) {
        Text(
            text = cpuUsage,
            style = MaterialTheme.typography.caption2,
            color = Color(0xFFFFA500)
        )
    }
}
@Composable
fun SystemTelemetryOverlay() {
    val context = LocalContext.current
    val batteryManager = remember { context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }
    
    var telemetryData by remember { mutableStateOf("Initializing HUD...") }
    var tempColor by remember { mutableStateOf(Color.Cyan) }

    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val tempInt = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                    val tempCelsius = tempInt / 10f
                    
                    val pct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    val currentMicroAmps = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    val currentMilliAmps = currentMicroAmps / 1000

                    telemetryData = "BAT: $pct% | DRAW: ${currentMilliAmps}mA\nTEMP: ${tempCelsius}°C"
                    
                    tempColor = when {
                        tempCelsius < 35 -> Color.Green
                        tempCelsius < 40 -> Color.Yellow
                        else -> Color.Red
                    }
                }
            }
        }

        val filter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Column(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(6.dp)
    ) {
        Text(
            text = telemetryData,
            style = MaterialTheme.typography.caption3,
            color = tempColor
        )
    }
}
@Composable
fun MemoryMonitorOverlay() {
    var memoryStats by remember { mutableStateOf("Calculating...") }

    LaunchedEffect(Unit) {
        while (true) {
            val runtime = Runtime.getRuntime()
            val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val maxMem = runtime.maxMemory() / 1024 / 1024
            memoryStats = "RAM: ${usedMem}MB / ${maxMem}MB"
            delay(10000)
        }
    }

    Text(
        text = memoryStats,
        style = MaterialTheme.typography.caption2,
        color = Color.Green,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(4.dp)
    )
}
@Composable
private fun StreamingCanvasBubble(
    streamingFlow: StateFlow<String?>,
    bubbleColor: Color,
    textColor: Color,
    senderName: String,
    senderColor: Color,
    isUser: Boolean,
    onTryAgain: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf<Size>(Size.Zero) }
    var hasError by remember { mutableStateOf(false) }
    val formattedText by produceState<AnnotatedString>(initialValue = AnnotatedString(""), key1 = streamingFlow) {
        withContext(Dispatchers.Default) {
            streamingFlow
                .filterNotNull()
//                .conflate()
                .collect { text ->

                    val isError = text.contains("Error", ignoreCase = true) ||
                            text.contains("HTTP", ignoreCase = true) ||
                            text.contains("Exception", ignoreCase = true) ||
                            text.contains("failed", ignoreCase = true)
                    if (isError) android.util.Log.d("StreamDebug", "Keyword Match Found!")
                    val formatted = RichTextFormatter.format(text + " ▋", textColor, fastMode = true)

                    withContext(Dispatchers.Main) {
                        value = formatted
                        hasError = isError
                    }
                }

        }
    }

    val senderTextLayout = remember(senderName, senderColor, canvasSize.width) {
        textMeasurer.measure(
            text = AnnotatedString(senderName),
            style = TextStyle(
                color = senderColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal
            ),
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = (canvasSize.width * 0.9f).toInt())
        )
    }

    val messageTextLayout = remember(formattedText, canvasSize.width) {
        textMeasurer.measure(
            text = formattedText,
            style = TextStyle(
                color = textColor,
                fontSize = 14.sp,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal
            ),
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = (canvasSize.width * 0.9f).toInt()),
            overflow = TextOverflow.Clip,
            softWrap = true
        )
    }

    val bubbleWidth = remember(senderTextLayout.size.width, messageTextLayout.size.width, canvasSize.width) {
        maxOf(senderTextLayout.size.width, messageTextLayout.size.width) + 32.dp.value
    }
    val bubbleHeight = remember(senderTextLayout.size.height, messageTextLayout.size.height) {
        senderTextLayout.size.height + messageTextLayout.size.height + 24.dp.value
    }



    val currentText by streamingFlow.collectAsStateWithLifecycle()


    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(with(LocalDensity.current) { bubbleHeight.toDp() })
        ) {
            canvasSize = size
            val cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())

            drawRoundRect(
                brush = Brush.verticalGradient(listOf(bubbleColor, bubbleColor)),
                topLeft = Offset(if (isUser) size.width - bubbleWidth else 0f, 0f),
                size = Size(bubbleWidth, bubbleHeight),
                cornerRadius = cornerRadius,
            )

            drawText(
                textLayoutResult = senderTextLayout,
                topLeft = Offset(
                    x = if (isUser) size.width - bubbleWidth + 16.dp.toPx() else 16.dp.toPx(),
                    y = 8.dp.toPx()
                )
            )

            drawText(
                textLayoutResult = messageTextLayout,
                topLeft = Offset(
                    x = if (isUser) size.width - bubbleWidth + 16.dp.toPx() else 16.dp.toPx(),
                    y = senderTextLayout.size.height + 12.dp.toPx()
                )
            )
        }

        if (hasError) {
            Button(
                onClick = {

                    println("Retry clicked")
                    onTryAgain()
                },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(0.6f)
                    .height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Red.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "Retry",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                )
            }
        }
    }
}
@Composable
fun MessageCard(
    msg: Message,
    userShape: RoundedCornerShape,
    geminiShape: RoundedCornerShape,
    userTextColor: Color,
    geminiTextColor: Color,
    displayName: String
) {
    val isUser = msg.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val colors = LocalForgeIntColors.current
    val bubbleColor = if (isUser) colors.userBubble else colors.botBubble
    val shape = if (isUser) userShape else geminiShape

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(
            onClick = {},
            backgroundPainter = CardDefaults.cardBackgroundPainter(
                startBackgroundColor = bubbleColor,
                endBackgroundColor = bubbleColor
            ),
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = shape
        ) {
            Column {
                val formattedText = remember(msg.text) {
                    RichTextFormatter.format(
                        text = msg.text,
                        primaryColor = if (isUser) userTextColor else geminiTextColor
                    )
                }

                Text(
                    text = if (isUser) "You" else displayName,
                    style = MaterialTheme.typography.caption2,
                    color = if (isUser) userTextColor else colors.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(formattedText, style = MaterialTheme.typography.body2)
            }
        }
    }
}

@Composable
fun LiteChatScreen(
    messages: List<Message>,
    streamingMessageFlow: StateFlow<String?>,
    isTyping: Boolean = false,
    isVoiceDominant: Boolean = false,
    onReplyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSwapModelClick: () -> Unit,
    onVoiceResult: (String) -> Unit = {}
) {
    val listState = rememberScalingLazyListState()
    val colors = LocalForgeIntColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isVoiceActive by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isVoiceActive = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                onVoiceResult(spokenText)
            }
        }
    }

    val triggerVoiceInput = {
        isVoiceActive = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak...")
        }
        voiceLauncher.launch(intent)
    }

    // --- TTS Logic ---
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    val isTtsReady = remember { mutableStateOf(false) }
    var lastReadIndex by remember { mutableIntStateOf(0) }
    val currentFullText by streamingMessageFlow.collectAsStateWithLifecycle()

    DisposableEffect(context) {
        val speech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady.value = true
            }
        }
        tts.value = speech
        onDispose {
            speech.stop()
            speech.shutdown()
        }
    }

    LaunchedEffect(isTtsReady.value) {
        if (isTtsReady.value) {
            tts.value?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "final_segment" && isVoiceDominant) {
                        coroutineScope.launch {
                            delay(500)
                            triggerVoiceInput()
                        }
                    }
                }
                override fun onError(utteranceId: String?) {}
            })
        }
    }

    LaunchedEffect(currentFullText, isTtsReady.value) {
        if (isVoiceDominant && isTtsReady.value && !currentFullText.isNullOrEmpty()) {
            val textToScan = currentFullText!!
            val delimiters = listOf('.', '!', '?', '\n')
            var lastPunct = -1
            for (d in delimiters) {
                val idx = textToScan.lastIndexOf(d)
                if (idx > lastPunct) lastPunct = idx
            }

            if (lastPunct >= lastReadIndex) {
                val segment = textToScan.substring(lastReadIndex, lastPunct + 1).trim()
                if (segment.isNotEmpty()) {
                    tts.value?.speak(segment, TextToSpeech.QUEUE_ADD, null, "segment_$lastReadIndex")
                    lastReadIndex = lastPunct + 1
                }
            }
        } else if (currentFullText == null && lastReadIndex > 0) {
            lastReadIndex = 0
        }
    }

    val isStreamingObserved by streamingMessageFlow.map { !it.isNullOrEmpty() }.collectAsStateWithLifecycle(initialValue = false)
    val prevIsStreaming = remember { mutableStateOf(false) }
    
    LaunchedEffect(isStreamingObserved) {
        if (prevIsStreaming.value && !isStreamingObserved && isVoiceDominant && isTtsReady.value) {
            val lastMsg = messages.lastOrNull()?.text ?: ""
            val remainingText = if (lastMsg.length > lastReadIndex) lastMsg.substring(lastReadIndex).trim() else ""
            if (remainingText.isNotEmpty()) {
                tts.value?.speak(remainingText, TextToSpeech.QUEUE_ADD, null, "final_segment")
            } else {
                triggerVoiceInput()
            }
        }
        prevIsStreaming.value = isStreamingObserved
    }

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty() || isTyping) {
            listState.animateScrollToItem(if (isTyping) messages.size + 1 else messages.size)
        }
    }
    val focusRequester = remember { FocusRequester() }


    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
        // No TimeText, No Vignette
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().background(colors.background)
                .rotaryScrollable(
                    listState,
                    focusRequester
                ),
            state = listState,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = messages,
                key = { it.id }
            ) { msg ->
                val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                val bg = if (msg.isUser) colors.userBubble else colors.botBubble
                val txtColor = if (msg.isUser) colors.userText else colors.botText

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
                    // Simple Box + Text. No Card overhead.
                    Text(
                        text = msg.text,
                        color = txtColor,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .clip(RoundedCornerShape(
                                topStart = 12.dp, topEnd = 12.dp,
                                bottomStart = if (msg.isUser) 12.dp else 2.dp,
                                bottomEnd = if (msg.isUser) 2.dp else 12.dp
                            ))
                            .background(bg)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }

            if (isTyping) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp, 12.dp, 12.dp, 2.dp))
                                .background(colors.botBubble)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "dots")
                            val dotAlphas = (0..2).map { index ->
                                infiniteTransition.animateFloat(
                                    initialValue = 0.2f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = keyframes {
                                            durationMillis = 600
                                            0.2f at index * 100
                                            1f at (index * 100 + 300)
                                            0.2f at 600
                                        },
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "dot$index"
                                )
                            }
                            dotAlphas.forEach { alpha ->
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.botText.copy(alpha = alpha.value))
                                )
                            }
                        }
                    }
                }
            }

            if (isVoiceDominant) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isVoiceActive) {
                            VoicePulseEffect(
                                color = colors.primary,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                                .clickable { triggerVoiceInput() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(CircleShape)
                                .background(colors.replyIcon)
                                .clickable(onClick = onReplyClick),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Keyboard, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Keyboard", color = Color.Black, style = MaterialTheme.typography.caption2, maxLines = 1)
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                                .clickable(onClick = onSwapModelClick),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Model", color = Color.Black, style = MaterialTheme.typography.caption2, maxLines = 1)
                        }
                    }
                }
            } else {
                item {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(colors.replyIcon)
                            .clickable(onClick = onReplyClick),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Keyboard, null, tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("Reply", color = Color.Black, style = MaterialTheme.typography.button)
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                            .clickable(onClick = onSwapModelClick),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("Change Model", color = Color.Black, style = MaterialTheme.typography.button)
                    }
                }
            }

            item {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .clickable(onClick = onDeleteClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete Chat", color = Color.Black, style = MaterialTheme.typography.button)
                }
            }
        }
    }
}
@Composable
fun ConnectionSettingsScreen(
    currentHost: String,
    currentPort: String,
    isLocalEnabled: Boolean,
    onToggleLocal: (Boolean) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    testResult: String?,
    isTesting: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalForgeIntColors.current

    // Launcher for IP Address
    val ipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = RemoteInput.getResultsFromIntent(result.data)
        val newIp = results?.getCharSequence("ip_input")?.toString()
        if (newIp != null) {
            onHostChange(newIp)
        }
    }

    // Launcher for Port Number
    val portLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = RemoteInput.getResultsFromIntent(result.data)
        val newPort = results?.getCharSequence("port_input")?.toString()
        if (newPort != null) {
            onPortChange(newPort)
        }
    }

    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .rotaryScrollable(listState, focusRequester),
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ListHeader {
                    Text("Connection", color = colors.settingsIcon)
                }
            }

            // 1. Master Toggle for Local Mode
            item {
                ToggleChip(
                    checked = isLocalEnabled,
                    onCheckedChange = onToggleLocal,
                    label = { Text("Use Local Server") },
                    secondaryLabel = {
                        Text(if (isLocalEnabled) "LM Studio / Ollama" else "Using Cloud (OpenRouter)")
                    },
                    toggleControl = {
                        Switch(
                            checked = isLocalEnabled,
                            onCheckedChange = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Only show Settings if Local Mode is ON
            if (isLocalEnabled) {
                // IP Address Chip
                item {
                    Chip(
                        onClick = {
                            val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                            val remoteInputs = listOf(
                                RemoteInput.Builder("ip_input")
                                    .setLabel("Enter IP Address")
                                    .build()
                            )
                            RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                            ipLauncher.launch(intent)
                        },
                        label = { Text("Server Address") },
                        secondaryLabel = {
                            val displayHost = currentHost
                                .removePrefix("https://")
                                .removePrefix("http://")
                                .removeSuffix("/")
                            Text(displayHost, color = colors.primary)
                        },
                        icon = { Icon(Icons.Default.Web, null) },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }




                item {
                    Button(
                        onClick = onTestConnection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isTesting) Color.Gray else Color(0xFF4CAF50)
                        ),
                        enabled = !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,

                            )
                        } else {
                            Text("Test Connection", color = ColorBlack)
                        }
                    }
                }

                testResult?.let { result ->
                    item {
                        val isSuccess = result.contains("OK") || result.contains("Connected")
                        LaunchedEffect(result) {
                            haptic.performHapticFeedback(
                                if (isSuccess) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                            )
                        }

                        Text(
                            text = if (isSuccess) "Success: $result" else "Failed: $result",
                            style = MaterialTheme.typography.caption2,
                            color = if (isSuccess) Color.Green else Color.Red,
                            modifier = Modifier.padding(horizontal = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            item {
                CompactChip(
                    onClick = onBack,
                    label = { Text("Done") },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
@Composable
fun LocalServerStatus(){

    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }

    ) {

    }
}


@Composable
fun WearNeuralPerceptron() {
    val listState = rememberScalingLazyListState()
    var x1 by remember { mutableFloatStateOf(0.5f) }
    var x2 by remember { mutableFloatStateOf(0.8f) }

    val w1 = 0.5f
    val w2 = -0.2f
    var bias by remember { mutableFloatStateOf(0.1f) }

    val z = (x1 * w1) + (x2 * w2) + bias
    val activation = 1.0f / (1.0f + exp(-z))

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "NEURAL_CORE",
                    color = Color(0xFF00FF41),
                    style = MaterialTheme.typography.caption2,
                    fontFamily = FontFamily.Monospace
                )
            }

            item {
                TitleCard(
                    onClick = { },
                    title = { Text("Output: %.2f".format(activation)) },
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = Color(0xFF00FF41).copy(alpha = 0.2f),
                        endBackgroundColor = Color.Black
                    )
                ) {
                    Text("z: %.4f".format(z), fontSize = 10.sp, color = Color.Gray)
                }
            }

            item {

                NeuralSlider(
                    value = x1,
                    onValueChange = { x1 = it },
                    label = "X1"

                )
            }

            item {
                NeuralSlider(
                    value = x2,
                    onValueChange = { x2 = it },
                    label = "X2"

                )
            }

            item {
                NeuralSlider(
                    value = bias,
                    onValueChange = { bias = it },
                    label = "BIAS"

                )
            }
        }
    }
}
@Composable
fun NeuralSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.caption2, color = Color.Gray)
        InlineSlider(
            value = value,
            onValueChange = onValueChange,
            increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
            decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
            valueRange = 0f..1f,
            steps = 9, // Creates 0.1 increments
            segmented = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
@Composable
fun MemoryManagementScreen(
    traits: List<UserTrait>,
    onDeleteTrait: (Int) -> Unit, // Pass the ID
    onAddManualMemory: (String, MemoryType) -> Unit,
    onClearAll: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val colors = LocalForgeIntColors.current

    val addLongTermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = RemoteInput.getResultsFromIntent(result.data)
        val text = results?.getCharSequence("manual_ltm_input")?.toString()?.trim()
        if (!text.isNullOrBlank()) {
            onAddManualMemory(text, MemoryType.LONG_TERM)
        }
    }

    val addShortTermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = RemoteInput.getResultsFromIntent(result.data)
        val text = results?.getCharSequence("manual_stm_input")?.toString()?.trim()
        if (!text.isNullOrBlank()) {
            onAddManualMemory(text, MemoryType.SHORT_TERM)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val ltm = remember(traits) { traits.filter { it.type == MemoryType.LONG_TERM } }
    val stm = remember(traits) { traits.filter { it.type == MemoryType.SHORT_TERM } }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .rotaryScrollable(listState, focusRequester)
                .focusable(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ListHeader {
                    Text(
                        "Manual Add",
                        color = colors.primary,
                        style = MaterialTheme.typography.caption2,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Chip(
                    onClick = {
                        val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                        val remoteInputs = listOf(
                            RemoteInput.Builder("manual_ltm_input")
                                .setLabel("Long-term fact to save")
                                .setAllowFreeFormInput(true)
                                .build()
                        )
                        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                        addLongTermLauncher.launch(intent)
                    },
                    label = { Text("Add Long-Term") },
                    secondaryLabel = { Text("Save exact fact") },
                    icon = { Icon(Icons.Rounded.Add, "Add LTM") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Chip(
                    onClick = {
                        val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                        val remoteInputs = listOf(
                            RemoteInput.Builder("manual_stm_input")
                                .setLabel("Short-term context to save")
                                .setAllowFreeFormInput(true)
                                .build()
                        )
                        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                        addShortTermLauncher.launch(intent)
                    },
                    label = { Text("Add Short-Term") },
                    secondaryLabel = { Text("Save temporary context") },
                    icon = { Icon(Icons.Rounded.Add, "Add STM") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // --- Long-Term Memory Section ---
            item {
                ListHeader {
                    Text(
                        "Long-Term Memory",
                        color = colors.primary,
                        style = MaterialTheme.typography.caption2,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (ltm.isEmpty()) {
                item {
                    Text(
                        "No long-term facts stored.",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(ltm, key = { it.id }) { trait ->
                    MemoryTraitRow(
                        trait = trait,
                        onDelete = { onDeleteTrait(trait.id) }
                    )
                }
            }

            // --- Short-Term Memory Section ---
            item {
                ListHeader {
                    Text(
                        "Short-Term Memory",
                        color = ColorLiteAccent,
                        style = MaterialTheme.typography.caption2,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }

            if (stm.isEmpty()) {
                item {
                    Text(
                        "No short-term context stored.",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(stm, key = { it.id }) { trait ->
                    MemoryTraitRow(
                        trait = trait,
                        onDelete = { onDeleteTrait(trait.id) }
                    )
                }
            }

            if (traits.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    Chip(
                        onClick = onClearAll,
                        label = { Text("Clear All Memories") },
                        icon = { Icon(Icons.Default.Delete, "Wipe") },
                        colors = ChipDefaults.gradientBackgroundChipColors(
                            startBackgroundColor = Color(0xFFB71C1C),
                            endBackgroundColor = colors.background
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryTraitRow(
    trait: UserTrait,
    onDelete: () -> Unit
) {
    var isDeleting by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val colors = LocalForgeIntColors.current

    LaunchedEffect(isDeleting) {
        if (isDeleting) {
            delay(500)
            onDelete()
        }
    }

    val animatedAlpha by animateFloatAsState(if (isDeleting) 0f else 1f, label = "alpha")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = animatedAlpha }
    ) {
        Card(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            backgroundPainter = CardDefaults.cardBackgroundPainter(
                startBackgroundColor = colors.surface,
                endBackgroundColor = colors.surface
            ),
            contentColor = colors.onPrimary
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = trait.content,
                    style = MaterialTheme.typography.body2,
                    color = colors.userText,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                IconButton(
                    onClick = {
                        isDeleting = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Forget",
                        tint = Color.Gray
                    )
                }
            }
        }
    }


}


object convoID {
    var conID = mutableStateOf(0)
    var condBK = mutableStateOf(false)
}
