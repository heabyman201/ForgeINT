package com.example.forgeint.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition

@Composable
fun LocalModelScreen(viewModel: GeminiViewModel) {
    val modelStatus by viewModel.localModelStatus.collectAsState()
    val downloadProgress by viewModel.modelDownloadProgress.collectAsState()

    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("On-Device Model", style = MaterialTheme.typography.title3)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Gemma 2B ( quantized )",
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (modelStatus) {
                LocalModelStatus.NotPresent -> {
                    Text(
                        "Download the model to use it for inference locally on your device.",
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Chip(
                        onClick = { viewModel.downloadLocalModel() },
                        label = { Text("Download (900 MB)") },
                        icon = { Icon(Icons.Default.Download, "Download") },
                        colors = ChipDefaults.primaryChipColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                LocalModelStatus.Downloading -> {
                    if (downloadProgress < 0f) {
                        CircularProgressIndicator() // Indeterminate
                        Text("Downloading...", textAlign = TextAlign.Center)
                    } else {
                        CircularProgressIndicator(progress = downloadProgress)
                        Text("Downloading... ${(downloadProgress * 100).toInt()}%", textAlign = TextAlign.Center)
                    }
                }
                LocalModelStatus.Present -> {
                    Text("Model is ready to use.", style = MaterialTheme.typography.body2, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.deleteLocalModel() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Delete Model")
                    }
                }
            }
        }
    }
}
