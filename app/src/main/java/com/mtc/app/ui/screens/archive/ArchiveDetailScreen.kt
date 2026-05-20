package com.mtc.app.ui.screens.archive

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mtc.app.domain.model.Archive
import com.mtc.app.domain.model.MediaAsset
import com.mtc.app.domain.model.Member
import com.mtc.app.ui.components.ErrorState
import com.mtc.app.ui.components.LoadingIndicator
import com.mtc.app.ui.components.MemberAvatar
import com.mtc.app.ui.components.MediaThumbnailImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveDetailScreen(
    archiveId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToMember: (Int) -> Unit,
    onNavigateToCreateMember: () -> Unit,
    onNavigateToDialogue: (Int) -> Unit,
    onNavigateToTimeline: (Int) -> Unit,
    onNavigateToStorybook: (Int) -> Unit,
    viewModel: ArchiveDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // 图片选择器 - 用于更换头像和上传媒体
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            selectedFileName = getFileName(context, it)
            showUploadDialog = true
        }
    }

    // 上传确认对话框
    if (showUploadDialog && selectedImageUri != null) {
        UploadConfirmDialog(
            fileName = selectedFileName ?: "未知文件",
            isUploading = uploadState.isUploading,
            progress = uploadState.progress,
            error = uploadState.error,
            onConfirm = {
                selectedImageUri?.let { uri ->
                    viewModel.uploadMedia(
                        context = context,
                        uri = uri,
                        filename = selectedFileName ?: "image.jpg",
                        archiveId = archiveId,
                        memberId = null
                    )
                }
            },
            onDismiss = {
                showUploadDialog = false
                selectedImageUri = null
                selectedFileName = null
            }
        )
    }

    // 上传成功提示
    LaunchedEffect(uploadState.isSuccess) {
        if (uploadState.isSuccess) {
            showUploadDialog = false
            selectedImageUri = null
            selectedFileName = null
        }
    }

    LaunchedEffect(archiveId) {
        viewModel.loadArchive(archiveId)
    }

    // 导入聊天记录弹窗 - 已移至成员详情页

    Scaffold { padding ->
        when {
            uiState.isLoading -> {
                LoadingIndicator(modifier = Modifier.padding(padding))
            }
            uiState.error != null -> {
                ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadArchive(archiveId) },
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.archive != null -> {
                val archive = uiState.archive!!
                val selectedMember = uiState.members.firstOrNull()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // 1. 顶部导航栏
                    item {
                        WireframeTopBar(
                            title = archive.name,
                            onNavigateBack = onNavigateBack
                        )
                    }

                    // 2. 档案卡头部（档案名称 + 简介）
                    item {
                        ArchiveHeaderWireframe(
                            archive = archive,
                            onChatClick = {
                                uiState.members.firstOrNull()?.let { onNavigateToDialogue(it.id) }
                            }
                        )
                    }

                    // 3. 简介文本区
                    item {
                        ArchiveBioWireframe(description = archive.description)
                    }

                    // 4. 操作按钮组
                    item {
                        ActionButtonsWireframe(
                            onChatClick = {
                                selectedMember?.let { onNavigateToDialogue(it.id) }
                            }
                        )
                    }

                    // 4.1 快速操作入口（时间线、故事书）
                    item {
                        QuickActionsSection(
                            onNavigateToTimeline = { onNavigateToTimeline(archiveId) },
                            onNavigateToStorybook = { onNavigateToStorybook(archiveId) }
                        )
                    }

                    // 5. 成员列表模块
                    item {
                        MembersSectionWireframe(
                            members = uiState.members,
                            onMemberClick = onNavigateToMember,
                            onAddMemberClick = onNavigateToCreateMember
                        )
                    }

                    // 6. 档案记忆列表（替换原档案概览）
                    item {
                        ArchiveMemoryListCard(
                            memories = uiState.memories,
                            isLoading = uiState.isLoadingMemories,
                            archiveName = archive.name
                        )
                    }
                }
            }
        }
    }
}

/**
 * 线框图顶部导航栏
 */
@Composable
fun WireframeTopBar(
    title: String,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 线框图档案卡头部 - 重新设计：档案名称 + 简介 + 统计信息融为一体
 */
@Composable
fun ArchiveHeaderWireframe(
    archive: Archive,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = archive.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (!archive.description.isNullOrBlank()) {
                        Text(
                            text = archive.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AssistChip(
                    onClick = onChatClick,
                    label = { Text("开启对话", style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

/**
 * 线框图简介文本区
 */
@Composable
fun ArchiveBioWireframe(description: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 线框图操作按钮组
 */
@Composable
fun ActionButtonsWireframe(
    onChatClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 主按钮：与Ta对话
        Button(
            onClick = onChatClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("与Ta对话")
        }
    }
}

/**
 * 线框图媒体模块
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaModuleWireframe(
    mediaAssets: List<MediaAsset>,
    downloadUrls: Map<Int, String> = emptyMap(),
    isLoading: Boolean,
    onUploadClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("all") }
    val filters = listOf("all" to "全部", "image" to "照片", "video" to "视频", "audio" to "语音")

    val filteredAssets = when (selectedFilter) {
        "image" -> mediaAssets.filter { it.isImage }
        "video" -> mediaAssets.filter { it.isVideo }
        "audio" -> mediaAssets.filter { it.isAudio }
        else -> mediaAssets
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 标题 + 上传按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "媒体",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = onUploadClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("上传")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 分类标签
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 内容网格
        when {
            isLoading -> {
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredAssets) { asset ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (asset.isImage) {
                                val downloadUrl = downloadUrls[asset.id]
                                if (downloadUrl != null) {
                                    AsyncImage(
                                        model = downloadUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    imageVector = when {
                                        asset.isImage -> Icons.Default.Photo
                                        asset.isVideo -> Icons.Default.PlayCircle
                                        asset.isAudio -> Icons.Default.Mic
                                        else -> Icons.Default.InsertDriveFile
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 线框图标签
 */
@Composable
fun WireframeChip(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * 线框图导入聊天记录弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportChatDialog(
    onDismiss: () -> Unit,
    onSelectFile: () -> Unit,
    onImport: (String) -> Unit
) {
    var selectedMode by remember { mutableStateOf("auto") }
    var extractMemories by remember { mutableStateOf(true) }
    var inputText by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 1. 弹窗标题栏：标题 + 关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "导入聊天记录",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. 说明文本区
                Text(
                    text = "上传微信/QQ 聊天记录 TXT 文件，AI 将自动解析并导入对话历史。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. 文件上传区：拖拽区域 + 选择按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (selectedFileName != null) {
                            // 已选择文件状态
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = selectedFileName!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = onSelectFile) {
                                Text("重新选择")
                            }
                        } else {
                            // 未选择文件状态
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "拖拽文件到此处",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onSelectFile,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("选择txt文件")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. 解析模式下拉选择
                Text(
                    text = "解析模式",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedMode) {
                            "auto" -> "自动识别"
                            "wechat" -> "微信格式"
                            "qq" -> "QQ格式"
                            else -> "自动识别"
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(4.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("自动识别") },
                            onClick = {
                                selectedMode = "auto"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("微信格式") },
                            onClick = {
                                selectedMode = "wechat"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("QQ格式") },
                            onClick = {
                                selectedMode = "qq"
                                expanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. 功能选项复选框组
                Column {
                    Text(
                        text = "功能选项",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = extractMemories,
                            onCheckedChange = { extractMemories = it }
                        )
                        Text("从对话中提炼记忆")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 6. 可编辑文本输入区
                Text(
                    text = "或直接粘贴对话内容",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("在此粘贴对话内容...") },
                    shape = RoundedCornerShape(4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 确认按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // 如果有输入文本或选择了文件，则导入
                            if (inputText.isNotBlank()) {
                                onImport(inputText)
                            } else {
                                // 无内容时也调用，弹出提示
                                onImport("")
                            }
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("导入")
                    }
                }
            }
        }
    }
}

/**
 * 成员卡片（保留原有）
 */
@Composable
fun MemberCard(
    member: Member,
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
                imageVector = Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = member.relationshipType,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "${member.memoryCount} 记忆",
                style = MaterialTheme.typography.labelSmall
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

/**
 * 线框图成员模块
 */
@Composable
fun MembersSectionWireframe(
    members: List<Member>,
    onMemberClick: (Int) -> Unit,
    onAddMemberClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 标题 + 添加按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "成员",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = onAddMemberClick) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加成员")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (members.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.GroupAdd,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "暂无成员，点击添加",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // 成员列表
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                members.forEach { member ->
                    MemberCard(
                        member = member,
                        onClick = { onMemberClick(member.id) }
                    )
                }
            }
        }
    }
}

/**
 * 快速操作区（保留原有）
 */
@Composable
fun QuickActionsSection(
    onNavigateToTimeline: () -> Unit,
    onNavigateToStorybook: () -> Unit
) {
    Text(
        text = "快速操作",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onNavigateToTimeline,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Timeline, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("时间线")
        }
        OutlinedButton(
            onClick = onNavigateToStorybook,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.AutoStories, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("生成故事")
        }
    }
}

/**
 * 档案信息卡片（保留原有）
 */
@Composable
fun ArchiveInfoCard(archive: Archive) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (archive.archiveType) {
                        "family" -> Icons.Default.FamilyRestroom
                        "lover" -> Icons.Default.Favorite
                        "friend" -> Icons.Default.Handshake
                        else -> Icons.Default.Folder
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = archive.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    AssistChip(onClick = { }, label = { Text(archive.archiveTypeDisplay) })
                }
            }
            if (!archive.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = archive.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * 上传确认对话框
 */
@Composable
fun UploadConfirmDialog(
    fileName: String,
    isUploading: Boolean,
    progress: Float,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("确认上传") },
        text = {
            Column {
                Text("文件名: $fileName")
                if (isUploading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "上传中... ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isUploading
            ) {
                Text(if (isUploading) "上传中..." else "上传")
            }
        },
        dismissButton = {
            if (!isUploading) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

/**
 * 获取文件名
 */
private fun getFileName(context: Context, uri: Uri): String {
    var result = "image.jpg"
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == "image.jpg") {
        uri.lastPathSegment?.let { result = it }
    }
    return result
}

/**
 * 档案概览卡片：展示档案中的成员数量和统计信息
 * 替换原来档案库中的媒体库模块
 */
@Composable
private fun ArchiveOverviewCard(
    memberCount: Int,
    archiveName: String,
    onMembersClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "档案概览",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onMembersClick)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$memberCount",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "位成员",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (memberCount == 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "暂无成员，点击下方按钮添加第一位成员",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 档案记忆列表卡片：展示档案中所有成员的记忆
 */
@Composable
private fun ArchiveMemoryListCard(
    memories: List<com.mtc.app.domain.model.Memory>,
    isLoading: Boolean,
    archiveName: String
) {
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
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "记忆列表",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = "${memories.size} 条",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading && memories.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                memories.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AutoStories,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "暂无记忆",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "在成员详情中添加记忆",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        memories.take(10).forEach { memory ->
                            MemoryListItem(memory = memory)
                        }
                        if (memories.size > 10) {
                            TextButton(
                                onClick = { },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("查看全部 ${memories.size} 条记忆")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 记忆列表项
 */
@Composable
private fun MemoryListItem(memory: com.mtc.app.domain.model.Memory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 情感标签
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (memory.emotionLabel) {
                        "joy" -> Color(0xFFFFF9C4)
                        "sadness" -> Color(0xFFBBDEFB)
                        "anger" -> Color(0xFFFFCDD2)
                        "fear" -> Color(0xFFE1BEE7)
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (memory.emotionLabel) {
                    "joy" -> Icons.Default.SentimentSatisfied
                    "sadness" -> Icons.Default.SentimentDissatisfied
                    "anger" -> Icons.Default.SentimentVeryDissatisfied
                    else -> Icons.Default.SentimentNeutral
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = memory.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!memory.contentText.isNullOrBlank()) {
                Text(
                    text = memory.contentText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (memory.isCapsule) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "记忆胶囊",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
