package com.mtc.app.ui.screens.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mtc.app.ui.components.EmptyState
import com.mtc.app.ui.components.ErrorState
import com.mtc.app.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    archiveId: Int,
    onNavigateBack: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showArchiveSelector by remember { mutableStateOf(false) }

    // 当 archiveId 传入时，自动选择该档案
    LaunchedEffect(archiveId) {
        viewModel.selectArchive(archiveId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("时间线") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadArchives() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 档案选择器
            if (uiState.archives.isNotEmpty()) {
                ArchiveSelectorBar(
                    archives = uiState.archives,
                    selectedArchiveId = uiState.selectedArchiveId,
                    onSelectArchive = { viewModel.selectArchive(it) }
                )
            }

            when {
                uiState.isLoading -> {
                    LoadingIndicator()
                }
                uiState.error != null && uiState.timeline.isEmpty() -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { uiState.selectedArchiveId?.let { viewModel.selectArchive(it) } }
                    )
                }
                uiState.timeline.isEmpty() -> {
                    EmptyState(
                        message = "暂无时间线数据\n请先添加记忆",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(uiState.timeline.size) { index ->
                            val entry = uiState.timeline[index]
                            val isLast = index == uiState.timeline.lastIndex

                            TimelineEntryItem(
                                entry = entry,
                                isLast = isLast
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveSelectorBar(
    archives: List<com.mtc.app.domain.model.Archive>,
    selectedArchiveId: Int?,
    onSelectArchive: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedArchive = archives.find { it.id == selectedArchiveId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = selectedArchive?.name ?: "选择档案",
            onValueChange = {},
            readOnly = true,
            label = { Text("档案") },
            leadingIcon = {
                selectedArchive?.let {
                    Text(it.archiveTypeIcon, style = MaterialTheme.typography.titleMedium)
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            archives.forEach { archive ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(archive.archiveTypeIcon)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(archive.name)
                        }
                    },
                    onClick = {
                        onSelectArchive(archive.id)
                        expanded = false
                    },
                    trailingIcon = {
                        if (archive.id == selectedArchiveId) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TimelineEntryItem(
    entry: com.mtc.app.domain.model.TimelineEntry,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 时间线连接线
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(60.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(80.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
            }
        }

        // 内容
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = 16.dp)
                .clickable { },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (entry.year != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${entry.year}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
