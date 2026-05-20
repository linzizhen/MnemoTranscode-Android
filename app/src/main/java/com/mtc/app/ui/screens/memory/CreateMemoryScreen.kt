package com.mtc.app.ui.screens.memory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mtc.app.ui.components.ErrorState
import com.mtc.app.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMemoryScreen(
    memberId: Int,
    onNavigateBack: () -> Unit,
    onMemoryCreated: () -> Unit,
    viewModel: CreateMemoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var title by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    var timestamp by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedEmotion by remember { mutableStateOf<String?>(null) }
    var emotionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onMemoryCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加记忆") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("记忆标题 *") },
                placeholder = { Text("例如：父亲的第一次远行") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = contentText,
                onValueChange = { contentText = it },
                label = { Text("记忆内容 *") },
                placeholder = { Text("详细描述这段记忆...") },
                minLines = 5,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = timestamp,
                    onValueChange = { timestamp = it },
                    label = { Text("发生时间") },
                    placeholder = { Text("例如：1998年春天") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("发生地点") },
                    placeholder = { Text("例如：深圳") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // 情感标签选择
            ExposedDropdownMenuBox(
                expanded = emotionExpanded,
                onExpandedChange = { emotionExpanded = !emotionExpanded }
            ) {
                OutlinedTextField(
                    value = viewModel.emotionLabels.find { it.first == selectedEmotion }?.second ?: "选择情感标签",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("情感标签") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = emotionExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = emotionExpanded,
                    onDismissRequest = { emotionExpanded = false }
                ) {
                    viewModel.emotionLabels.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedEmotion = value
                                emotionExpanded = false
                            }
                        )
                    }
                }
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.createMemory(
                        memberId = memberId,
                        title = title,
                        contentText = contentText,
                        timestamp = timestamp.ifBlank { null },
                        location = location.ifBlank { null },
                        emotionLabel = selectedEmotion
                    )
                },
                enabled = !uiState.isLoading && title.isNotBlank() && contentText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("保存记忆")
                }
            }
        }
    }
}
