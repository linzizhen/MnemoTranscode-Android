package com.mtc.app.ui.screens.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mtc.app.domain.model.Archive
import com.mtc.app.ui.components.EmptyState
import com.mtc.app.ui.components.ErrorState
import com.mtc.app.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveListScreen(
    onNavigateToArchive: (Int) -> Unit,
    onNavigateToCreateArchive: () -> Unit,
    viewModel: ArchiveListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的档案") },
                actions = {
                    IconButton(onClick = { viewModel.loadArchives() }) {
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
                    onRetry = { viewModel.loadArchives() },
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.archives.isEmpty() -> {
                EmptyState(
                    message = "暂无档案",
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
                    items(uiState.archives) { archive ->
                        ArchiveListItem(
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
fun ArchiveListItem(
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

            AssistChip(
                onClick = { },
                label = { Text(archive.archiveTypeDisplay) }
            )
        }
    }
}
