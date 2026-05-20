package com.mtc.app.ui.screens.member

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mtc.app.domain.model.MediaAsset
import com.mtc.app.domain.model.Memory
import com.mtc.app.domain.repository.MediaRepository
import com.mtc.app.data.model.ChatImportConfig
import com.mtc.app.data.model.ParseMode
import com.mtc.app.ui.components.ErrorState
import com.mtc.app.ui.components.LoadingIndicator
import com.mtc.app.ui.components.MediaSelfContainedThumbnail
import com.mtc.app.ui.components.MediaThumbnailImage
import com.mtc.app.ui.components.MemberAvatar
import com.mtc.app.ui.components.MemoryRelationGraph
import com.mtc.app.ui.components.AuthMediaImage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    archiveId: Int,
    memberId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToMemory: (Int) -> Unit,
    onNavigateToCreateMemory: () -> Unit,
    onNavigateToDialogue: () -> Unit,
    viewModel: MemberDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val mediaDownloadUrls by viewModel.mediaDownloadUrls.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteMemoryDialog by remember { mutableStateOf<Int?>(null) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var showAvatarUploadDialog by remember { mutableStateOf(false) }
    var showMediaUploadDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var uploadDialogPurpose by remember { mutableStateOf("") } // "avatar" or "media"
    val context = LocalContext.current

    // 头像上传专用选择器
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            selectedFileName = getFileName(context, it)
            uploadDialogPurpose = "avatar"
            showAvatarUploadDialog = true
        }
    }

    // 媒体上传专用选择器
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            selectedFileName = getFileName(context, it)
            uploadDialogPurpose = "media"
            showMediaUploadDialog = true
        }
    }

    // 头像上传成功时
    LaunchedEffect(uploadState.isSuccess, uploadDialogPurpose) {
        if (uploadState.isSuccess && uploadDialogPurpose == "avatar") {
            showAvatarUploadDialog = false
            selectedImageUri = null
            selectedFileName = null
            uploadDialogPurpose = ""
            viewModel.clearUploadState()
        }
    }

    // 媒体上传成功时
    LaunchedEffect(uploadState.isSuccess, uploadDialogPurpose) {
        if (uploadState.isSuccess && uploadDialogPurpose == "media") {
            showMediaUploadDialog = false
            selectedImageUri = null
            selectedFileName = null
            uploadDialogPurpose = ""
            viewModel.clearUploadState()
            uiState.member?.let { viewModel.loadMedia(it.id) }
        }
    }

    // 头像上传确认对话框
    if (showAvatarUploadDialog && selectedImageUri != null && uiState.member != null) {
        MemberAvatarUploadDialog(
            fileName = selectedFileName ?: "未知文件",
            isUploading = uploadState.isUploading,
            progress = uploadState.progress,
            error = uploadState.error,
            onConfirm = {
                selectedImageUri?.let { uri ->
                    val member = uiState.member!!
                    uploadDialogPurpose = "avatar"
                    viewModel.uploadMemberAvatar(
                        context = context,
                        uri = uri,
                        filename = selectedFileName ?: "avatar.jpg",
                        archiveId = archiveId,
                        memberId = member.id
                    )
                }
            },
            onDismiss = {
                showAvatarUploadDialog = false
                selectedImageUri = null
                selectedFileName = null
                uploadDialogPurpose = ""
            }
        )
    }

    // 媒体上传确认对话框
    if (showMediaUploadDialog && selectedImageUri != null && uiState.member != null) {
        MediaUploadDialog(
            fileName = selectedFileName ?: "未知文件",
            isUploading = uploadState.isUploading,
            progress = uploadState.progress,
            error = uploadState.error,
            onConfirm = {
                selectedImageUri?.let { uri ->
                    val member = uiState.member!!
                    uploadDialogPurpose = "media"
                    viewModel.uploadMedia(
                        context = context,
                        uri = uri,
                        filename = selectedFileName ?: "media.jpg",
                        memberId = member.id
                    )
                }
            },
            onDismiss = {
                showMediaUploadDialog = false
                selectedImageUri = null
                selectedFileName = null
                uploadDialogPurpose = ""
            }
        )
    }

    // 导入聊天记录弹窗
    if (showImportDialog && uiState.member != null) {
        MemberImportDialog(
            onDismiss = { showImportDialog = false },
            onImport = { text, config, navigateToProgress ->
                if (text.isNotBlank()) {
                    isImporting = true
                    viewModel.importChatFromText(
                        rawText = text,
                        config = config,
                        archiveId = archiveId,
                        memberId = memberId,
                        navigateToProgress = navigateToProgress,
                        onSuccess = {
                            isImporting = false
                            android.widget.Toast.makeText(
                                context,
                                "成功导入 ${it} 条记忆",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            showImportDialog = false
                            viewModel.loadMemories(memberId)
                        },
                        onError = { error ->
                            isImporting = false
                            android.widget.Toast.makeText(
                                context,
                                "导入失败: $error",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            },
            archiveId = archiveId,
            memberId = memberId,
            isImporting = isImporting
        )
    }

    LaunchedEffect(archiveId to memberId) {
        viewModel.loadMember(archiveId, memberId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除成员") },
            text = { Text("确定要删除这个成员吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMember(archiveId, memberId) { onNavigateBack() }
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

    // 删除记忆对话框
    showDeleteMemoryDialog?.let { memoryId ->
        AlertDialog(
            onDismissRequest = { showDeleteMemoryDialog = null },
            title = { Text("删除记忆") },
            text = { Text("确定要删除这段记忆吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMemory(memoryId)
                        showDeleteMemoryDialog = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMemoryDialog = null }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.member?.name ?: "成员详情") },
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
        },
        floatingActionButton = {
            Column {
                SmallFloatingActionButton(
                    onClick = onNavigateToDialogue,
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "对话")
                }
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(onClick = onNavigateToCreateMemory) {
                    Icon(Icons.Default.Add, contentDescription = "添加记忆")
                }
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
                    onRetry = { viewModel.loadMember(archiveId, memberId) },
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.member != null -> {
                val member = uiState.member!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 成员信息卡片
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MemberAvatar(
                                        avatarUrl = member.avatarUrl,
                                        name = member.name,
                                        isAlive = member.isAlive,
                                        size = 64.dp,
                                        onClick = { }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = member.relationshipType,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    TextButton(
                                        onClick = { avatarPickerLauncher.launch("image/*") }
                                    ) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("更换头像", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                // 导入记忆按钮（从档案详情移至此处）
                                AssistChip(
                                    onClick = { showImportDialog = true },
                                    label = { Text("导入记忆", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.FileUpload,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = member.statusDisplay,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    if (member.lifespan.isNotBlank()) {
                                        Text(
                                            text = " · ${member.lifespan}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                if (!member.bio.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = member.bio,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                    )
                                }

                                if (member.emotionTags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        member.emotionTags.take(3).forEach { tag ->
                                            AssistChip(
                                                onClick = { },
                                                label = { Text(tag) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 统计卡片
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(modifier = Modifier.weight(1f)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${uiState.memories.size}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "记忆",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 记忆关系网络卡片
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Hub,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "记忆神经网络",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (uiState.isLoadingGraph) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else if (uiState.graphNodes.isNotEmpty()) {
                                        Text(
                                            text = "${uiState.graphNodes.size} 节点",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                when {
                                    uiState.isLoadingGraph -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator()
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "正在加载关系网络...",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    uiState.graphError != null -> {
                                        var showDetails by remember { mutableStateOf(false) }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.CloudOff,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "关系网络暂时无法加载",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                TextButton(
                                                    onClick = { showDetails = !showDetails }
                                                ) {
                                                    Text(
                                                        text = if (showDetails) "收起详情" else "查看详情",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                                if (showDetails) {
                                                    Text(
                                                        text = uiState.graphError ?: "未知错误",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                OutlinedButton(
                                                    onClick = { viewModel.loadMnemoGraph(archiveId, memberId) },
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Refresh,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("重试", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                    uiState.graphNodes.isEmpty() -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.School,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(32.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "暂无关系网络",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "添加记忆后将自动生成",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        MemoryRelationGraph(
                                            nodes = uiState.graphNodes,
                                            edges = uiState.graphEdges,
                                            selectedNodeId = selectedNodeId,
                                            onNodeSelected = { nodeId ->
                                                selectedNodeId = nodeId
                                            },
                                            onNodeDragEnd = { _, _, _ -> },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 媒体库卡片（与关系图同层）
                    item {
                        MediaGalleryCard(
                            mediaAssets = uiState.mediaAssets,
                            downloadUrls = mediaDownloadUrls,
                            isLoading = uiState.isLoadingMedia,
                            memberName = uiState.member?.name ?: "成员",
                            onUploadClick = { mediaPickerLauncher.launch("image/*") },
                            onFetchUrl = { assetId -> viewModel.fetchMediaUrl(assetId) },
                            onDeleteAsset = { asset -> viewModel.deleteMediaAsset(asset) }
                        )
                    }

                    // 记忆列表卡片
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Memory,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "记忆列表",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (uiState.isLoadingMemories) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = "${uiState.memories.size} 条",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                when {
                                    uiState.isLoadingMemories -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                    uiState.memories.isEmpty() -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(60.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "暂无记忆，点击下方按钮添加",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    else -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            uiState.memories.take(10).forEach { memory ->
                                                MemoryListItem(
                                                    memory = memory,
                                                    onClick = { onNavigateToMemory(memory.id) },
                                                    onDeleteClick = { showDeleteMemoryDialog = memory.id }
                                                )
                                            }
                                            if (uiState.memories.size > 10) {
                                                TextButton(
                                                    onClick = { /* TODO: 导航到完整记忆列表 */ },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("查看全部 ${uiState.memories.size} 条记忆")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 底部按钮
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateToDialogue,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("与 TA 对话")
                            }

                            OutlinedButton(
                                onClick = onNavigateToCreateMemory,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("添加记忆")
                            }
                        }
                    }

                    // 底部留白
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryListItem(
    memory: Memory,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = memory.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    memory.emotionLabel?.let { emotion ->
                        Text(
                            text = getEmotionEmoji(emotion),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = memory.contentText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (memory.timestamp != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = memory.timestamp.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun getEmotionEmoji(emotion: String): String = when (emotion) {
    "joy" -> "😊"
    "love" -> "❤️"
    "sadness" -> "😢"
    "anger" -> "😠"
    "fear" -> "😨"
    "surprise" -> "😮"
    "nostalgia" -> "🥹"
    "gratitude" -> "🙏"
    "regret" -> "😔"
    "peaceful" -> "😌"
    else -> "📝"
}

private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}

/**
 * 导入聊天记录弹窗（全功能版）
 * 包含文件导入区、解析模式设置、原文编辑区、底部操作按钮
 * 内容可垂直滑动，底部按钮固定
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberImportDialog(
    onDismiss: () -> Unit,
    onImport: (String, ChatImportConfig, Boolean) -> Unit,
    archiveId: Int,
    memberId: Int,
    isImporting: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var rawText by remember { mutableStateOf("") }
    var parseMode by remember { mutableStateOf(ParseMode.AUTO) }
    var useAiRefine by remember { mutableStateOf(true) }
    var buildRelationNetwork by remember { mutableStateOf(false) }
    var parseModeExpanded by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    // 文件选择器（内部管理）
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val text = String(bytes, Charsets.UTF_8)
                    rawText = text
                    selectedFileName = it.lastPathSegment ?: "聊天记录.txt"
                    importError = null
                }
            } catch (e: Exception) {
                importError = "读取文件失败: ${e.message}"
            }
        }
    }

    val charCount = rawText.length
    val isTextValid = rawText.isNotBlank() && rawText.length <= 500_000

    Dialog(onDismissRequest = { if (!isImporting) onDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ===== 顶部标题栏（固定） =====
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(48.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "导入聊天记录",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (selectedFileName != null) "已选: $selectedFileName"
                                       else "支持 txt 格式",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            enabled = !isImporting
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // ===== 可滑动内容区 =====
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // ===== 1. 文件导入区 =====
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fileLauncher.launch("text/plain") }
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (selectedFileName != null) "已选择: $selectedFileName"
                                       else "将聊天记录 .txt 拖到此处",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { fileLauncher.launch("text/plain") }) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("选择 txt 文件")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "支持单篇最大 500,000 字 · 兼容微信/QQ导出格式",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // ===== 2. 解析模式设置区 =====
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "解析模式",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // 下拉选择
                            ExposedDropdownMenuBox(
                                expanded = parseModeExpanded,
                                onExpandedChange = { if (!isImporting) parseModeExpanded = !parseModeExpanded }
                            ) {
                                OutlinedTextField(
                                    value = when (parseMode) {
                                        ParseMode.AUTO -> "自动识别（推荐）"
                                        ParseMode.WECHAT -> "微信风格（日期/昵称行）"
                                        ParseMode.PLAIN -> "纯文本：按空行分段"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = !isImporting,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parseModeExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = parseModeExpanded,
                                    onDismissRequest = { parseModeExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("自动识别（推荐）") },
                                        onClick = { parseMode = ParseMode.AUTO; parseModeExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("微信风格（日期/昵称行）") },
                                        onClick = { parseMode = ParseMode.WECHAT; parseModeExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("纯文本：按空行分段") },
                                        onClick = { parseMode = ParseMode.PLAIN; parseModeExpanded = false }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            // 功能开关 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "使用AI精炼记忆正文",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "多批处理，需配置LLM",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = useAiRefine,
                                    onCheckedChange = { if (!isImporting) useAiRefine = it },
                                    enabled = !isImporting
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 功能开关 2
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "导入后构建记忆关系网",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "基于时间链+LLM联结生成关联关系",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = buildRelationNetwork,
                                    onCheckedChange = { if (!isImporting) buildRelationNetwork = it },
                                    enabled = !isImporting
                                )
                            }
                        }
                    }

                    // ===== 3. 聊天原文编辑区 =====
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "聊天原文（可编辑）",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "$charCount / 500,000",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (charCount > 500_000) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = rawText,
                                onValueChange = {
                                    if (it.length <= 500_000) {
                                        rawText = it
                                        if (selectedFileName != null) selectedFileName = null
                                    }
                                },
                                placeholder = { Text("粘贴全文，或从上方载入txt…") },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp),
                                minLines = 8,
                                enabled = !isImporting
                            )
                        }
                    }

                    // ===== 错误提示 =====
                    importError?.let { err ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }

                // ===== 4. 底部操作按钮（固定） =====
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            enabled = !isImporting
                        ) {
                            Text("取消")
                        }

                        OutlinedButton(
                            onClick = {
                                if (!isTextValid) {
                                    importError = "请输入聊天内容（不超过50万字）"
                                    return@OutlinedButton
                                }
                                importError = null
                                onImport(rawText, ChatImportConfig(parseMode, useAiRefine, buildRelationNetwork), false)
                            },
                            modifier = Modifier.weight(1.2f),
                            enabled = !isImporting && isTextValid
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("单次请求导入")
                            }
                        }

                        Button(
                            onClick = {
                                if (!isTextValid) {
                                    importError = "请输入聊天内容（不超过50万字）"
                                    return@Button
                                }
                                importError = null
                                onImport(rawText, ChatImportConfig(parseMode, useAiRefine, buildRelationNetwork), true)
                            },
                            modifier = Modifier.weight(1.4f),
                            enabled = !isImporting && isTextValid
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI导入进度页")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberAvatarUploadDialog(
    fileName: String,
    isUploading: Boolean,
    progress: Float,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("更换头像") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "选中的文件: $fileName",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isUploading) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "上传中... ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isUploading
            ) {
                Text("确认上传")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("取消")
            }
        }
    )
}

/**
 * 成员详情页的媒体库卡片
 * 显示该成员关联的照片/视频等媒体
 * URL 完全自管理：优先用 presigned URL，否则用直连 /media/{objectKey} 兜底
 */
@Composable
private fun MediaGalleryCard(
    mediaAssets: List<MediaAsset>,
    downloadUrls: Map<Int, String>,
    isLoading: Boolean,
    memberName: String,
    onUploadClick: () -> Unit,
    onFetchUrl: (Int) -> Unit,
    onDeleteAsset: (MediaAsset) -> Unit
) {
    val filters = remember { listOf("all" to "全部", "image" to "照片", "video" to "视频", "audio" to "语音") }
    var selectedFilter by remember { mutableStateOf("all") }

    val filteredAssets = remember(mediaAssets, selectedFilter) {
        when (selectedFilter) {
            "image" -> mediaAssets.filter { it.isImage }
            "video" -> mediaAssets.filter { it.isVideo }
            "audio" -> mediaAssets.filter { it.isAudio }
            else -> mediaAssets
        }
    }

    var selectedAsset by remember { mutableStateOf<MediaAsset?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "媒体库",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                TextButton(onClick = onUploadClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("上传")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading && mediaAssets.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                filteredAssets.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "暂无媒体",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onUploadClick) {
                                Text("上传第一张")
                            }
                        }
                    }
                }
                else -> {
                    val rowCount = filteredAssets.size / 3 + if (filteredAssets.size % 3 > 0) 1 else 0
                    val rowHeight = 84.dp
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight.times(rowCount.coerceAtLeast(1)))
                    ) {
                        items(
                            items = filteredAssets,
                            key = { asset -> "${asset.id}_${asset.objectKey}" }
                        ) { asset ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                MediaSelfContainedThumbnail(
                                    mediaId = asset.id,
                                    modifier = Modifier.fillMaxSize(),
                                    onClick = { selectedAsset = asset }
                                )
                                // 删除按钮
                                IconButton(
                                    onClick = { onDeleteAsset(asset) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 点击查看大图
    selectedAsset?.let { asset ->
        AlertDialog(
            onDismissRequest = { selectedAsset = null },
            title = { Text("媒体详情") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (asset.isImage) {
                        AuthMediaImage(
                            mediaId = asset.id,
                            downloadUrl = downloadUrls[asset.id],
                            contentDescription = "${memberName}的媒体",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    asset.isVideo -> Icons.Default.PlayCircle
                                    asset.isAudio -> Icons.Default.Mic
                                    else -> Icons.Default.InsertDriveFile
                                },
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DetailRow("文件名", asset.objectKey.substringAfterLast("/"))
                    DetailRow("用途", asset.purposeDisplay)
                    DetailRow("大小", asset.sizeDisplay)
                    DetailRow("上传时间", asset.createdAt.take(10))
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAsset = null }) {
                    Text("关闭")
                }
            }
        )
    }
}

/** Hilt EntryPoint 供私有 Composable 获取 Repository */
@EntryPoint
@InstallIn(SingletonComponent::class)
private interface MemberMediaEntryPoint {
    fun mediaRepository(): MediaRepository
}

/**
 * 详情行组件
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * 媒体上传确认对话框
 */
@Composable
private fun MediaUploadDialog(
    fileName: String,
    isUploading: Boolean,
    progress: Float,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("上传媒体") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "选中的文件: $fileName",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isUploading) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "上传中... ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isUploading
            ) {
                Text("确认上传")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("取消")
            }
        }
    )
}

/**
 * 媒体缩略图（使用 AvatarComponents 提供的统一组件）
 */
@Composable
private fun MediaThumbnail(
    asset: MediaAsset,
    downloadUrl: String?,
    onUrlNeeded: () -> Unit,
    onClick: () -> Unit
) {
    MediaSelfContainedThumbnail(
        mediaId = asset.id,
        modifier = Modifier.size(76.dp),
        onClick = onClick
    )
}
