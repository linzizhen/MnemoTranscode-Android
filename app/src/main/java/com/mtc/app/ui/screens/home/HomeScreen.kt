package com.mtc.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mtc.app.domain.model.Archive
import com.mtc.app.domain.model.Member
import com.mtc.app.ui.components.EmptyState
import com.mtc.app.ui.components.ErrorState
import com.mtc.app.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToArchive: (Int) -> Unit,
    onNavigateToCreateArchive: () -> Unit,
    onNavigateToArchiveList: () -> Unit,
    onNavigateToDialogue: (Int, Int) -> Unit,
    onNavigateToTimeline: (Int) -> Unit,
    onNavigateToStorybook: (Int) -> Unit,
    onNavigateToCapsules: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialogueSelector by remember { mutableStateOf(false) }
    var selectedArchive by remember { mutableStateOf<Archive?>(null) }
    var selectedMember by remember { mutableStateOf<Member?>(null) }
    var expandedArchive by remember { mutableStateOf(false) }
    var expandedMember by remember { mutableStateOf(false) }

    // 找到第一个档案用于时间线和故事书
    val firstArchiveId = uiState.archives.firstOrNull()?.id

    // AI 对话选择对话框
    if (showDialogueSelector) {
        AlertDialog(
            onDismissRequest = { showDialogueSelector = false },
            title = { Text("选择对话对象") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 档案选择
                    var archiveExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = archiveExpanded,
                        onExpandedChange = { archiveExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedArchive?.name ?: "选择档案",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = archiveExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = archiveExpanded,
                            onDismissRequest = { archiveExpanded = false }
                        ) {
                            uiState.archives.forEach { archive ->
                                DropdownMenuItem(
                                    text = { Text(archive.name) },
                                    onClick = {
                                        selectedArchive = archive
                                        selectedMember = null // 重置成员选择
                                        archiveExpanded = false
                                        // 加载成员列表
                                        viewModel.loadMembersForArchive(archive.id)
                                    }
                                )
                            }
                        }
                    }

                    // 成员选择
                    if (uiState.membersForDialogue.isNotEmpty()) {
                        var memberExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = memberExpanded,
                            onExpandedChange = { memberExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedMember?.name ?: "选择成员",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = memberExpanded,
                                onDismissRequest = { memberExpanded = false }
                            ) {
                                uiState.membersForDialogue.forEach { member ->
                                    DropdownMenuItem(
                                        text = { Text("${member.name} (${member.relationshipType})") },
                                        onClick = {
                                            selectedMember = member
                                            memberExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedArchive != null && selectedMember != null) {
                            showDialogueSelector = false
                            onNavigateToDialogue(selectedArchive!!.id, selectedMember!!.id)
                        }
                    },
                    enabled = selectedArchive != null && selectedMember != null
                ) {
                    Text("开始对话")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogueSelector = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MTC") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateArchive) {
                Icon(Icons.Default.Add, contentDescription = "创建档案")
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                LoadingIndicator(modifier = Modifier.padding(padding))
            }
            uiState.error != null -> {
                ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.archives.isEmpty() -> {
                EmptyState(
                    message = "暂无档案\n点击右下角按钮创建第一个档案",
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 欢迎信息
                    item {
                        WelcomeCard(username = uiState.user?.username ?: "用户")
                    }

                    // 快速操作入口（与网页一致）
                    item {
                        QuickActionsGrid(
                            onNavigateToArchiveList = onNavigateToArchiveList,
                            onNavigateToDialogue = {
                                if (uiState.archives.isNotEmpty()) {
                                    showDialogueSelector = true
                                }
                            },
                            onNavigateToTimeline = { firstArchiveId?.let { onNavigateToTimeline(it) } },
                            onNavigateToStorybook = { firstArchiveId?.let { onNavigateToStorybook(it) } }
                        )
                    }

                    // 统计信息
                    item {
                        val totalMemories = uiState.archives.sumOf { it.memoryCount }
                        val totalMembers = uiState.archives.sumOf { it.memberCount }
                        StatsCard(
                            archiveCount = uiState.archives.size,
                            totalMemories = totalMemories,
                            totalMembers = totalMembers
                        )
                    }

                    // 档案类型分布（与网页一致）
                    item {
                        ArchiveTypeDistribution(
                            archives = uiState.archives
                        )
                    }

                    // 档案列表标题
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "最近访问",
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(onClick = onNavigateToArchiveList) {
                                Text("查看全部")
                            }
                        }
                    }

                    // 档案列表
                    items(uiState.archives.take(3)) { archive ->
                        ArchiveCard(
                            archive = archive,
                            onClick = { onNavigateToArchive(archive.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeCard(username: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "你好，$username",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "用 AI 守护每一段珍贵的记忆",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun StatsCard(
    archiveCount: Int,
    totalMemories: Int,
    totalMembers: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatItem(
            modifier = Modifier.weight(1f),
            value = archiveCount.toString(),
            label = "档案"
        )
        StatItem(
            modifier = Modifier.weight(1f),
            value = totalMembers.toString(),
            label = "成员"
        )
        StatItem(
            modifier = Modifier.weight(1f),
            value = totalMemories.toString(),
            label = "记忆"
        )
    }
}

@Composable
fun StatItem(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionsGrid(
    onNavigateToArchiveList: () -> Unit,
    onNavigateToDialogue: () -> Unit,
    onNavigateToTimeline: () -> Unit,
    onNavigateToStorybook: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 档案库
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Folder,
                title = "档案库",
                subtitle = "管理记忆档案",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = onNavigateToArchiveList
            )
            // AI 对话
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.Chat,
                title = "AI 对话",
                subtitle = "与记忆对话",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = onNavigateToDialogue
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 时间线
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Timeline,
                title = "时间线",
                subtitle = "记忆编年史",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                onClick = onNavigateToTimeline
            )
            // 故事书
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AutoStories,
                title = "故事书",
                subtitle = "生成回忆录",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                onClick = onNavigateToStorybook
            )
        }
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ArchiveCard(
    archive: Archive,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类型图标
            Text(
                text = archive.archiveTypeIcon,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = archive.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!archive.description.isNullOrBlank()) {
                    Text(
                        text = archive.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "${archive.memberCount} 成员",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${archive.memoryCount} 记忆",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = archive.archiveTypeDisplay,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 档案类型分布卡片（与网页一致）
 */
@Composable
fun ArchiveTypeDistribution(archives: List<Archive>) {
    // 使用 remember 缓存统计结果，避免每次重组都重新计算
    val typeStats = remember(archives) {
        listOf(
            "family" to "家族记忆",
            "lover" to "恋人记忆",
            "friend" to "挚友记忆",
            "relative" to "至亲记忆",
            "celebrity" to "伟人记忆",
            "nation" to "国家历史"
        ).map { (type, label) ->
            type to archives.count { it.archiveType == type }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "档案分布",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                typeStats.forEach { (type, count) ->
                    ArchiveTypeItem(
                        icon = when (type) {
                            "family" -> "👨‍👩‍👧‍👦"
                            "lover" -> "💑"
                            "friend" -> "🤝"
                            "relative" -> "❤️"
                            "celebrity" -> "⭐"
                            "nation" -> "🏛️"
                            else -> "📁"
                        },
                        label = when (type) {
                            "family" -> "家族"
                            "lover" -> "恋人"
                            "friend" -> "挚友"
                            "relative" -> "至亲"
                            "celebrity" -> "伟人"
                            "nation" -> "国家"
                            else -> type
                        },
                        count = count
                    )
                }
            }
        }
    }
}

@Composable
fun ArchiveTypeItem(
    icon: String,
    label: String,
    count: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
