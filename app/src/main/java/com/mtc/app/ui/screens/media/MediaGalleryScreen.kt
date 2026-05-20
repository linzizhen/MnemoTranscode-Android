package com.mtc.app.ui.screens.media

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mtc.app.ui.components.MediaThumbnailImage
import com.mtc.app.ui.components.AuthMediaImage
import com.mtc.app.domain.model.MediaAsset
import com.mtc.app.ui.components.EmptyState
import com.mtc.app.ui.components.ErrorState
import com.mtc.app.ui.components.LoadingIndicator
import com.mtc.app.ui.components.rememberCurrentServerUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGalleryScreen(
    onNavigateBack: () -> Unit,
    viewModel: MediaGalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var uploadType by remember { mutableStateOf("archive_photo") } // "archive_photo" or "archive_sticker"
    var deleteConfirmAsset by remember { mutableStateOf<MediaAsset?>(null) }

    // 删除确认弹窗
    deleteConfirmAsset?.let { asset ->
        AlertDialog(
            onDismissRequest = { deleteConfirmAsset = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除这张${asset.purposeDisplay}吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAsset(asset)
                        deleteConfirmAsset = null
                    },
                    enabled = !uiState.isDeleting
                ) {
                    if (uiState.isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmAsset = null }, enabled = !uiState.isDeleting) {
                    Text("取消")
                }
            }
        )
    }

    // 权限
    val requiredPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            showUploadDialog = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        }
    }

    fun openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            imagePickerLauncher.launch("image/*")
        } else {
            if (ContextCompat.checkSelfPermission(context, requiredPermission) == PackageManager.PERMISSION_GRANTED) {
                imagePickerLauncher.launch("image/*")
            } else {
                permissionLauncher.launch(requiredPermission)
            }
        }
    }

    LaunchedEffect(uiState.uploadSuccess) {
        if (uiState.uploadSuccess) {
            showUploadDialog = false
            selectedUri = null
            viewModel.clearUploadState()
        }
    }

    if (showUploadDialog && selectedUri != null) {
        UploadDialog(
            context = context,
            uri = selectedUri!!,
            isUploading = uiState.isUploading,
            progress = uiState.uploadProgress,
            error = uiState.uploadError,
            uploadType = uploadType,
            onConfirm = {
                if (uploadType == "archive_sticker") {
                    viewModel.uploadSticker(context = context, uri = selectedUri!!)
                } else {
                    viewModel.uploadImage(context = context, uri = selectedUri!!, purpose = "archive_photo")
                }
            },
            onDismiss = {
                showUploadDialog = false
                selectedUri = null
                viewModel.clearUploadState()
            }
        )
    }

    if (uiState.selectedAsset != null) {
        val asset = uiState.selectedAsset!!
        MediaDetailDialog(
            asset = asset,
            imageUrl = uiState.downloadUrls[asset.id],
            onDismiss = { viewModel.clearSelectedAsset() },
            onDelete = {
                viewModel.clearSelectedAsset()
                deleteConfirmAsset = asset
            },
            isDeleting = uiState.isDeleting
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.deleteError) {
        uiState.deleteError?.let {
            snackbarHostState.showSnackbar("删除失败：$it")
            viewModel.clearDeleteError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("媒体库") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadMediaAssets(uiState.selectedFilter) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        uploadType = "archive_sticker"
                        openImagePicker()
                    },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Mood, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("上传表情包")
                }

                ExtendedFloatingActionButton(
                    onClick = {
                        uploadType = "archive_photo"
                        openImagePicker()
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("上传图片")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = viewModel.filterOptions.indexOfFirst { it.first == uiState.selectedFilter }.coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                viewModel.filterOptions.forEach { (key, label) ->
                    Tab(
                        selected = uiState.selectedFilter == key,
                        onClick = { viewModel.setFilter(key) },
                        text = { Text(label) }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    LoadingIndicator()
                }
                uiState.error != null && uiState.mediaAssets.isEmpty() -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadMediaAssets(uiState.selectedFilter) }
                    )
                }
                uiState.mediaAssets.isEmpty() -> {
                    EmptyState(message = "暂无媒体文件，点击右下角上传")
                }
                else -> {
                    val spacing = 4.dp
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.mediaAssets,
                            key = { asset -> asset.id }
                        ) { asset ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                MediaThumbnailImage(
                                    objectKey = asset.objectKey,
                                    downloadUrl = uiState.downloadUrls[asset.id],
                                    mediaId = asset.id,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // 点击打开详情
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { viewModel.selectAsset(asset) }
                                )

                                // 网格内长按删除（右下角小图标）
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                                        .clickable { deleteConfirmAsset = asset }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 上传对话框（带本地图片预览）
// ============================================================
@Composable
fun UploadDialog(
    context: android.content.Context,
    uri: Uri,
    isUploading: Boolean,
    progress: Float,
    error: String?,
    uploadType: String = "archive_photo",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val uploadText = if (uploadType == "archive_sticker") "正在上传表情包..." else "正在上传图片..."
    val confirmText = if (uploadType == "archive_sticker") "确定要上传这个表情包吗？" else "确定要上传这张图片到媒体库吗？"

    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val drawable = android.graphics.drawable.Drawable.createFromStream(stream, null)
                previewBitmap = drawable?.toBitmap(512, 512)
            }
        } catch (_: Exception) { }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text(if (isUploading) "上传中..." else "确认上传") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isUploading) {
                    Text(uploadText)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    // 本地图片预览
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        previewBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } ?: run {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(confirmText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            if (!isUploading) {
                TextButton(
                    onClick = onConfirm,
                    enabled = error == null
                ) {
                    Text("上传")
                }
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

// ============================================================
// 媒体缩略图（可复用，已在 AvatarComponents 中定义）
// ============================================================
@Composable
fun MediaThumbnail(
    asset: MediaAsset,
    onClick: () -> Unit,
    downloadUrl: String? = null
) {
    MediaThumbnailImage(
        objectKey = asset.objectKey,
        downloadUrl = downloadUrl,
        mediaId = asset.id,
        contentDescription = null,
        modifier = Modifier.aspectRatio(1f),
        onClick = onClick
    )
}

// ============================================================
// 媒体详情弹窗（带完整加载/错误状态）
// ============================================================
@Composable
fun MediaDetailDialog(
    asset: MediaAsset,
    imageUrl: String?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    isDeleting: Boolean = false
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val baseUrl = rememberCurrentServerUrl()

    // 当 downloadUrl 为 null 时，直接用 mediaId 加载图片
    val directImageUrl = imageUrl
        ?: "${baseUrl.trimEnd('/')}/api/v1/media/${asset.id}/file"

    // MediaDetailDialog 使用 AuthMediaImage（有 URL 用 Coil，无 URL 用 Repository 字节流）
    val effectiveImageUrl: String? = if (asset.isImage) imageUrl else null

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这张${asset.purposeDisplay}吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }, enabled = !isDeleting) {
                    Text("取消")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(asset.purposeDisplay) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        // 图片：AuthMediaImage 处理所有加载路径（有 URL 用 Coil，无 URL 用 Repository）
                        asset.isImage -> {
                            AuthMediaImage(
                                mediaId = asset.id,
                                downloadUrl = imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                placeholderSize = 32.dp,
                                contentScale = ContentScale.Fit
                            )
                        }
                        asset.isVideo -> {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        asset.isAudio -> {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                DetailRow("文件名", asset.objectKey.substringAfterLast("/"))
                DetailRow("类型", asset.contentType)
                DetailRow("大小", asset.sizeDisplay)
                DetailRow("用途", asset.purposeDisplay)
                DetailRow("上传时间", asset.createdAt.take(10))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { showDeleteConfirm = true },
                enabled = !isDeleting
            ) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
