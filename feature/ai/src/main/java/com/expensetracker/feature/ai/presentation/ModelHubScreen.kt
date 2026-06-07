package com.expensetracker.feature.ai.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.ModelState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelHubScreen(
    onBack: () -> Unit,
    viewModel: ModelHubViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("AI Models", fontWeight = FontWeight.Bold) },
                windowInsets = WindowInsets.statusBars,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Download and manage on-device AI models. These models run entirely on your phone for maximum privacy and do not require any login.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(state.models, key = { it.model.id }) { card ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(card.model.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(card.model.description, style = MaterialTheme.typography.bodyMedium)
                        Text("~${card.model.sizeBytes / 1_000_000_000.0} GB • ${if (card.model.supportsVision) "Text + Image" else "Text only"}")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(renderState(card.state), style = MaterialTheme.typography.bodySmall)
                            if (card.isActive && card.state is ModelState.Ready) {
                                val accelerator = card.state.accelerator
                                val isGpu = accelerator == "GPU"
                                AssistChip(
                                    onClick = {},
                                    label = { Text(accelerator, style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Memory,
                                            contentDescription = null,
                                            modifier = Modifier.padding(0.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (isGpu)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.secondaryContainer,
                                        labelColor = if (isGpu)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSecondaryContainer,
                                        leadingIconContentColor = if (isGpu)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }
                        if (card.state is ModelState.Downloading) {
                            LinearProgressIndicator(
                                progress = { card.state.progress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (card.state) {
                                ModelState.NotDownloaded,
                                is ModelState.Error -> Button(
                                    onClick = { viewModel.startDownload(card.model.id) }
                                ) { Text("Download") }
                                ModelState.Queued,
                                is ModelState.Downloading -> OutlinedButton(onClick = { viewModel.cancelDownload(card.model.id) }) { Text("Cancel") }
                                ModelState.Downloaded,
                                ModelState.Loading,
                                is ModelState.Ready -> Button(onClick = { viewModel.useModel(card.model.id) }) {
                                    Text(if (card.isActive) "Loaded" else "Use This Model")
                                }
                            }
                            if (card.state != ModelState.NotDownloaded) {
                                OutlinedButton(onClick = { viewModel.deleteModel(card.model.id) }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun renderState(state: ModelState): String = when (state) {
    ModelState.NotDownloaded -> "Not downloaded"
    ModelState.Queued -> "Queued"
    is ModelState.Downloading -> "Downloading ${state.progress}%"
    ModelState.Downloaded -> "Downloaded"
    ModelState.Loading -> "Loading"
    is ModelState.Ready -> "Ready • ${state.accelerator}"
    is ModelState.Error -> state.message
}
