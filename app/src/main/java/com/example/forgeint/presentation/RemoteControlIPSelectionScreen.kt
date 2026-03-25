package com.example.forgeint.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.ScrollIndicator
import com.example.forgeint.presentation.theme.LocalForgeIntColors
import kotlinx.coroutines.launch

@Composable
fun RemoteControlIPSelectionScreen(
    currentHost: String,
    currentPort: String,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val colors = LocalForgeIntColors.current

    val hostLauncher = rememberTextInputLauncher(onInputReceived = onHostChange)
    val portLauncher = rememberTextInputLauncher(onInputReceived = onPortChange)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        positionIndicator = { ScrollIndicator(state = listState) }
    ) {
        TransformingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .onRotaryScrollEvent {
                    coroutineScope.launch {
                        listState.scrollBy(it.verticalScrollPixels * 2f)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Remote IP Settings",
                    style = MaterialTheme.typography.caption1,
                    color = colors.onPrimary
                )
            }

            item {
                Chip(
                    onClick = { hostLauncher() },
                    label = { Text("Host IP / URL") },
                    secondaryLabel = { Text(currentHost) },
                    icon = { Icon(Icons.Default.Dns, "Host") },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble,
                        endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Chip(
                    onClick = { portLauncher() },
                    label = { Text("Server Port") },
                    secondaryLabel = { Text(currentPort) },
                    icon = { Icon(Icons.Default.SettingsInputComponent, "Port") },
                    colors = ChipDefaults.gradientBackgroundChipColors(
                        startBackgroundColor = colors.userBubble,
                        endBackgroundColor = colors.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
