package com.mtc.app.ui.screens.capsule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mtc.app.domain.model.Archive
import com.mtc.app.domain.model.Capsule
import com.mtc.app.domain.model.Member
import com.mtc.app.ui.components.EmptyState
import com.mtc.app.ui.components.ErrorState
import com.mtc.app.ui.components.LoadingIndicator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapsuleListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateCapsule: (archiveId: Int, memberId: Int) -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: CapsuleListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDetailDialog by remember { mutableStateOf(false) }
    var showArchiveSelector by remember { mutableStateOf(false) }

    // 每次屏幕回到前台（从创建页返回等）时自动刷新胶囊列表
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadCapsules()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.selectedCapsule) {
        if (uiState.selectedCapsule != null) {
            showDetailDialog = true
        }
    }

    if (showDetailDialog && uiState.selectedCapsule != null) {
        CapsuleDetailDialog(
            capsule = uiState.selectedCapsule!!,
            onDismiss = {
                showDetailDialog = false
                viewModel.clearSelectedCapsule()
            }
        )
    }

    // 档案选择对话框（用于创建胶囊）
    if (showArchiveSelector) {
        if (uiState.archives.isEmpty()) {
            // 加载中或无档案
            LaunchedEffect(Unit) {
                viewModel.loadArchives()
            }
        }
        ArchiveSelectorDialog(
            archives = uiState.archives,
            onDismiss = { showArchiveSelector = false },
            onArchiveSelected = { archiveId, memberId ->
                showArchiveSelector = false
                onNavigateToCreateCapsule(archiveId, memberId)
            },
            onNavigateToHome = {
                showArchiveSelector = false
                onNavigateToHome()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记忆胶囊") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadCapsules() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showArchiveSelector = true }) {
                Icon(Icons.Default.Add, contentDescription = "创建胶囊")
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                LoadingIndicator(modifier = Modifier.padding(padding))
            }
            uiState.error != null && uiState.capsules.isEmpty() -> {
                ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadCapsules() },
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.capsules.isEmpty() -> {
                EmptyState(
                    message = "暂无记忆胶囊\n点击右下角创建第一个胶囊",
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
                    items(uiState.capsules) { capsule ->
                        CapsuleCard(
                            capsule = capsule,
                            onClick = { viewModel.selectCapsule(capsule.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CapsuleCard(
    capsule: Capsule,
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
            Icon(
                imageVector = if (capsule.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = null,
                tint = if (capsule.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = capsule.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "解锁时间: ${capsule.unlockDate.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = capsule.statusDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (capsule.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CapsuleDetailDialog(
    capsule: Capsule,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (capsule.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = if (capsule.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(capsule.title)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row {
                    Text(
                        text = "状态: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = capsule.statusDisplay,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row {
                    Text(
                        text = "解锁时间: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = capsule.unlockDate.take(10),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row {
                    Text(
                        text = "创建时间: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = capsule.createdAt.take(10),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (capsule.content != null) {
                    HorizontalDivider()
                    Text(
                        text = capsule.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (capsule.message != null) {
                    HorizontalDivider()
                    Text(
                        text = capsule.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveSelectorDialog(
    archives: List<com.mtc.app.domain.model.Archive>,
    onDismiss: () -> Unit,
    onArchiveSelected: (archiveId: Int, memberId: Int) -> Unit,
    onNavigateToHome: () -> Unit
) {
    var selectedArchive by remember { mutableStateOf<com.mtc.app.domain.model.Archive?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<com.mtc.app.domain.model.Member?>(null) }
    var isLoadingMembers by remember { mutableStateOf(false) }
    var members by remember { mutableStateOf<List<com.mtc.app.domain.model.Member>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择档案和成员") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (archives.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "暂无档案",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onNavigateToHome) {
                            Text("去创建档案")
                        }
                    }
                } else {
                    // 档案选择
                    ArchiveDropdown(
                        archives = archives,
                        selectedArchive = selectedArchive,
                        onArchiveSelected = { archive ->
                            selectedArchive = archive
                            selectedMember = null
                        }
                    )

                    // 成员选择
                    if (selectedArchive != null) {
                        MemberDropdown(
                            archiveId = selectedArchive!!.id,
                            selectedMember = selectedMember,
                            onMemberSelected = { selectedMember = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedArchive != null && selectedMember != null) {
                        onArchiveSelected(selectedArchive!!.id, selectedMember!!.id)
                    }
                },
                enabled = selectedArchive != null && selectedMember != null
            ) {
                Text("下一步")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveDropdown(
    archives: List<com.mtc.app.domain.model.Archive>,
    selectedArchive: com.mtc.app.domain.model.Archive?,
    onArchiveSelected: (com.mtc.app.domain.model.Archive) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedArchive?.name ?: "选择档案",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            archives.forEach { archive ->
                DropdownMenuItem(
                    text = { Text(archive.name) },
                    onClick = {
                        onArchiveSelected(archive)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDropdown(
    archiveId: Int,
    selectedMember: com.mtc.app.domain.model.Member?,
    onMemberSelected: (com.mtc.app.domain.model.Member) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var members by remember { mutableStateOf<List<com.mtc.app.domain.model.Member>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var viewModel: CapsuleListViewModel? = null

    val localViewModel: CapsuleListViewModel = hiltViewModel()
    viewModel = localViewModel

    LaunchedEffect(archiveId) {
        isLoading = true
        localViewModel.loadMembersForArchive(archiveId) { loadedMembers ->
            members = loadedMembers
            isLoading = false
        }
    }

    if (isLoading) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    } else {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedMember?.name ?: if (members.isEmpty()) "暂无成员" else "选择成员",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = members.isNotEmpty(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (members.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("该档案暂无成员") },
                        onClick = { expanded = false },
                        enabled = false
                    )
                } else {
                    members.forEach { member ->
                        DropdownMenuItem(
                            text = { Text("${member.name} (${member.relationshipType})") },
                            onClick = {
                                onMemberSelected(member)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
