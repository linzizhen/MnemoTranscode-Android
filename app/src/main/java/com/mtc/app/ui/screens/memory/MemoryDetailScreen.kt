package com.mtc.app.ui.screens.memory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mtc.app.ui.components.ErrorState
import com.mtc.app.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDetailScreen(
    memoryId: Int,
    onNavigateBack: () -> Unit,
    viewModel: MemoryDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(memoryId) {
        viewModel.loadMemory(memoryId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除记忆") },
            text = { Text("确定要删除这段记忆吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMemory(memoryId) { onNavigateBack() }
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.memory?.title ?: "记忆详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                LoadingIndicator(modifier = Modifier.padding(padding))
            }
            uiState.error != null -> {
                ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadMemory(memoryId) },
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.memory != null -> {
                val memory = uiState.memory!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 标题和情感
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = memory.title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f)
                        )
                        if (memory.emotionLabel != null) {
                            Text(
                                text = memory.emotionIcon,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }

                    // 时间地点信息
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (!memory.timestamp.isNullOrBlank()) {
                                Row {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = memory.timestamp,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            if (!memory.location.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = memory.location,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    // 情感标签
                    if (memory.emotionLabel != null) {
                        Row {
                            Text(
                                text = "情感：",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "${memory.emotionIcon} ${memory.emotionDisplay}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider()

                    // 记忆内容
                    Text(
                        text = memory.contentText,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    HorizontalDivider()

                    // 元信息
                    Text(
                        text = "创建于 ${memory.createdAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
