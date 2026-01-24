package com.example.forgeint.presentation

import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.AutoCenteringParams
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import com.example.weargemini.data.SettingsManager
import kotlinx.coroutines.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.*


import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.*
import kotlinx.coroutines.launch

@Composable
fun PersonaSettings(
    settingsManager: SettingsManager,
    onPersonaSelected: () -> Unit
) {
    val currentPersonaId by settingsManager.selectedPersonaId.collectAsState(initial = "default")
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    val haptics = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }

    // Category mapping for personas
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Logic", "Dev", "Lifestyle", "Chaos")

    val personas = remember { Personas.list }

    val filteredPersonas = remember(selectedCategory) {
        if (selectedCategory == "All") personas
        else personas.filter { persona ->
            when (selectedCategory) {
                "Logic" -> persona.id in listOf("philosopher", "historian")
                "Dev" -> persona.id in listOf("code_helper", "tech_architect")
                "Lifestyle" -> persona.id in listOf("fitness_coach", "medical_consultant", "travel_guide", "performance_psychologist")
                "Chaos" -> persona.id == "shitposter"
                else -> true
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = Modifier.background(Color.Black),
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .rotaryScrollable(listState, focusRequester),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            autoCentering = AutoCenteringParams(itemIndex = 1)
        ) {
            item {
                Text(
                    text = "Select Persona",
                    style = MaterialTheme.typography.caption1,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Filter Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isFilterSelected = selectedCategory == category

                        Chip(
                            onClick = {
                                selectedCategory = category
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            label = {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.caption2.copy(
                                        fontWeight = if (isFilterSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = ChipDefaults.gradientBackgroundChipColors(
                                startBackgroundColor = if (isFilterSelected) Color(0xFF081A36) else Color(0xFF1C1B1F),
                                endBackgroundColor = if (isFilterSelected) Color(0xFF5C4D00) else Color(0xFF1C1B1F),
                                contentColor = if (isFilterSelected) Color.White else Color(0xFFCAC4D0)
                            ),
                            modifier = (Modifier as Modifier)
                                .height(32.dp)
                                .then(
                                    if (!isFilterSelected) {
                                        Modifier.border(
                                            width = 1.dp,
                                            color = Color(0xFF49454F),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                    } else Modifier
                                ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            items(
                items = filteredPersonas,
                key = { it.id }
            ) { persona ->
                val isSelected = remember(currentPersonaId) { persona.id == currentPersonaId }

                ToggleChip(
                    checked = isSelected,
                    onCheckedChange = {
                        coroutineScope.launch {
                            settingsManager.setSelectedPersona(persona.id)
                            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            onPersonaSelected()
                        }
                    },
                    label = { Text(persona.name) },
                    secondaryLabel = {
                        Text(
                            text = persona.description,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    toggleControl = {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.6f)
                        )
                    },
                    colors = ToggleChipDefaults.toggleChipColors(
                        checkedStartBackgroundColor = Color(0xFF0D47A1),
                        checkedEndBackgroundColor = Color(0xFFFFD700),
                        uncheckedStartBackgroundColor = Color.Black,
                        uncheckedEndBackgroundColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}