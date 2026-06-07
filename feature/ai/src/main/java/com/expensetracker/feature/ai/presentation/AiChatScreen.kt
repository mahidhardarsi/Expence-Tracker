package com.expensetracker.feature.ai.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.domain.model.AnalysisFilter
import com.expensetracker.domain.model.ChatRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    title: String,
    reportId: Long?,
    analysisFilter: AnalysisFilter?,
    onBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel()
) {
    LaunchedEffect(title, reportId, analysisFilter) {
        viewModel.configure(reportId, analysisFilter, title)
    }

    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    var input by remember { mutableStateOf("") }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(state.title, fontWeight = FontWeight.Bold) },
                windowInsets = WindowInsets.statusBars,
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                when (message.role) {
                                    ChatRole.USER -> "You"
                                    ChatRole.MODEL -> "AI"
                                    ChatRole.SYSTEM -> "System"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(message.content.ifBlank { if (message.isStreaming) "..." else "" })
                        }
                    }
                }
            }
            if (!state.errorMessage.isNullOrBlank()) {
                Text(state.errorMessage)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Ask anything about your finances") }
                )
                Button(
                    onClick = {
                        viewModel.sendMessage(input)
                        input = ""
                    },
                    enabled = input.isNotBlank() && !state.isSending
                ) {
                    Text("Send")
                }
            }
        }
    }
}
