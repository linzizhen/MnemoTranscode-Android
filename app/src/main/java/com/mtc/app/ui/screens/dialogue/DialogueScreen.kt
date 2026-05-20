package com.mtc.app.ui.screens.dialogue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogueScreen(
    memberId: Int,
    archiveId: Int,
    onNavigateBack: () -> Unit,
    viewModel: DialogueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(archiveId to memberId) {
        viewModel.loadMember(archiveId, memberId)
    }

    // 自动滚动到最新消息
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // 清除历史对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除对话历史") },
            text = { Text("确定要清除所有对话历史吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("清除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 记忆预览对话框
    if (uiState.showMemoryPreview) {
        MemoryPreviewDialog(
            memories = uiState.extractedMemories,
            isSaving = uiState.isSavingAsMemory,
            onDismiss = { viewModel.closeMemoryPreview() },
            onSaveAll = { viewModel.saveExtractedMemories() }
        )
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. 顶部导航栏
            TopAppBar(
                title = {
                    Text(
                        text = "与 ${uiState.member?.name ?: "..."} 的对话",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "清除历史")
                    }
                }
            )

            // 2. 主体区域（全宽）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // ========== 主交互区 ==========
                LeftPanel(
                    modifier = Modifier.fillMaxSize(),
                    messages = uiState.messages,
                    currentMessage = uiState.currentMessage,
                    onMessageChange = { viewModel.updateMessage(it) },
                    onSend = { viewModel.sendMessage() },
                    onExtractMemory = { viewModel.extractMemories() },
                    onSaveToMemory = { viewModel.saveExtractedMemories() },
                    isSending = uiState.isSending,
                    isExtractingMemory = uiState.isExtractingMemory,
                    extractedCount = uiState.extractedMemories.size,
                    error = uiState.error,
                    onClearError = { viewModel.clearError() },
                    listState = listState,
                    memberName = uiState.member?.name,
                    memberIsAlive = uiState.member?.isAlive == true,
                    modelName = uiState.currentModelName,
                    modelProvider = uiState.currentModelProvider
                )
            }

            // 3. 底部：安卓系统导航栏占位
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(0.dp)
            )
        }
    }
}

/**
 * 左侧主交互区
 */
@Composable
fun LeftPanel(
    modifier: Modifier = Modifier,
    messages: List<com.mtc.app.domain.model.DialogueMessage>,
    currentMessage: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onExtractMemory: () -> Unit,
    onSaveToMemory: () -> Unit,
    isSending: Boolean,
    isExtractingMemory: Boolean,
    extractedCount: Int,
    error: String?,
    onClearError: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    memberName: String?,
    memberIsAlive: Boolean,
    modelName: String?,
    modelProvider: String?
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 模型状态模块（齿轮图标 + 模型名 + 兼容标识 + 勾选状态）
        ModelStatusCard(
            modifier = Modifier.fillMaxWidth(),
            modelName = modelName,
            provider = modelProvider
        )

        // 2. 对话消息流区域（垂直滚动）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (messages.isEmpty()) {
                // 欢迎信息
                EmptyChatPlaceholder(memberName = memberName)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages.size) { index ->
                        val message = messages[index]
                        ChatBubble(
                            message = message,
                            isUser = message.role == "user",
                            memberStatusIcon = if (!message.isUser) {
                                if (memberIsAlive) Icons.Default.Face else Icons.Default.Star
                            } else null
                        )
                    }

                    // 正在发送中的提示
                    if (isSending) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "正在思考...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. 多行文本输入框（占位符「输入你想说的话…」）
        OutlinedTextField(
            value = currentMessage,
            onValueChange = onMessageChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 80.dp),
            placeholder = { Text("输入你想说的话...") },
            enabled = !isSending,
            maxLines = 4,
            shape = RoundedCornerShape(12.dp)
        )

        // 4. 全宽「发送」按钮
        Button(
            onClick = onSend,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isSending && currentMessage.isNotBlank(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("发送中...")
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("发送", style = MaterialTheme.typography.titleSmall)
            }
        }

        // 5. 水平双按钮：「提炼记忆」、「写入记忆」
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onExtractMemory,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                enabled = !isExtractingMemory && !isSending && messages.isNotEmpty(),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isExtractingMemory) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("提炼记忆", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = onSaveToMemory,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                enabled = extractedCount > 0 && !isSending,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("写入记忆", style = MaterialTheme.typography.labelMedium)
            }
        }

        // 提炼结果提示
        AnimatedVisibility(visible = extractedCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "已提炼 $extractedCount 条记忆",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // 错误提示
        AnimatedVisibility(visible = error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClearError,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 6. 底部说明文字
        Text(
            text = "AI 会根据记忆中的故事来回应你",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 模型状态卡片
 */
@Composable
fun ModelStatusCard(
    modifier: Modifier = Modifier,
    modelName: String?,
    provider: String?
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "设置",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = modelName ?: "模型未配置",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = provider ?: "请在设置中配置 AI 模型",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            if (modelName != null) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "已配置",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "未配置",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 空聊天占位符
 */
@Composable
fun EmptyChatPlaceholder(memberName: String?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (memberName != null) "💭" else "🤖",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (memberName != null) "与 ${memberName} 开始对话" else "开始对话",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AI 会用记忆中的故事回应你",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 聊天气泡
 */
@Composable
fun ChatBubble(
    message: com.mtc.app.domain.model.DialogueMessage,
    isUser: Boolean,
    memberStatusIcon: androidx.compose.ui.graphics.vector.ImageVector?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            // AI 头像
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    memberStatusIcon?.let {
                        Icon(it, contentDescription = null, modifier = Modifier.size(20.dp))
                    } ?: Text("🤖", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // 用户头像
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("👤", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * 右侧边栏
 */
@Composable
fun RightSidebar(
    modifier: Modifier = Modifier,
    memberName: String?,
    messageCount: Int,
    isAlive: Boolean
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 顶部头像图标
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isAlive) {
                        Icon(
                            Icons.Default.Face,
                            contentDescription = "头像",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "头像",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 2. 垂直排列的功能图标（文字正向显示）
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 对话图标
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "对话",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "对话",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 记忆图标
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = "记忆",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "记忆",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 关系网图标
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Hub,
                        contentDescription = "关系网",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "关系网",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 3. 带数字角标的消息提示块
            Box {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = messageCount.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "消息",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // 角标
                if (messageCount > 0) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd),
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = minOf(messageCount, 99).toString(),
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }
    }
}

/**
 * 记忆预览对话框
 */
@Composable
fun MemoryPreviewDialog(
    memories: List<ExtractedMemory>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSaveAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("提炼的记忆")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 200.dp)
            ) {
                Text(
                    text = "从对话中提炼出 ${memories.size} 条记忆：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memories.size) { index ->
                        val memory = memories[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${index + 1}. ${memory.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = memory.content.take(80) + if (memory.content.length > 80) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSaveAll,
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isSaving) "保存中..." else "全部保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("取消")
            }
        }
    )
}
