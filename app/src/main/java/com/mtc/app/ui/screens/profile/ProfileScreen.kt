package com.mtc.app.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mtc.app.ui.components.LoadingIndicator
import com.mtc.app.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToAccountInfo: () -> Unit,
    onNavigateToModelSettings: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToServerConfig: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAvatarPickerDialog by remember { mutableStateOf(false) }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAvatarUri = uri
            showAvatarPickerDialog = true
        }
    }

    // 上传成功后清除状态
    LaunchedEffect(uiState.avatarUploadSuccess) {
        if (uiState.avatarUploadSuccess) {
            snackbarHostState.showSnackbar("头像更新成功")
            viewModel.clearAvatarUploadState()
        }
    }

    // 上传失败后显示错误
    LaunchedEffect(uiState.avatarUploadError) {
        uiState.avatarUploadError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearAvatarUploadState()
        }
    }

    // 头像上传确认对话框
    if (showAvatarPickerDialog && selectedAvatarUri != null) {
        AvatarUploadDialog(
            isUploading = uiState.isUploadingAvatar,
            onConfirm = {
                selectedAvatarUri?.let { viewModel.uploadAvatar(it) }
                showAvatarPickerDialog = false
                selectedAvatarUri = null
            },
            onDismiss = {
                showAvatarPickerDialog = false
                selectedAvatarUri = null
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.logout { onLogout() }
                        showLogoutDialog = false
                    }
                ) {
                    Text("退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人中心") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 用户信息卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 头像区域（可点击更换）
                        if (uiState.user != null) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                UserAvatar(
                                    avatarUrl = uiState.user!!.avatarUrl,
                                    username = uiState.user!!.username,
                                    size = 80.dp
                                )
                                FilledIconButton(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    modifier = Modifier.size(28.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "更换头像",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = uiState.user!!.username,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Text(
                                text = uiState.user!!.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 订阅标签
                            val tierIcon = when (uiState.user!!.subscriptionTierResolved) {
                                "free" -> "免费版"
                                "lite" -> "Lite"
                                "pro" -> "专业版"
                                "max" -> "Max"
                                "enterprise" -> "企业版"
                                else -> uiState.user!!.subscriptionTierResolved
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(tierIcon) }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 更换头像按钮
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("更换头像")
                            }

                            // 删除头像按钮（有头像时才显示）
                            if (!uiState.user?.avatarUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { viewModel.deleteAvatar() },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("删除头像")
                                }
                            }
                        } else {
                            // 加载中或无用户时的占位
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "加载中...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // 设置分组标题
            item {
                Text(
                    text = "账户设置",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 账号信息
            item {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "账号信息",
                    subtitle = "查看和修改个人信息",
                    onClick = onNavigateToAccountInfo
                )
            }

            // 偏好设置
            item {
                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "偏好设置",
                    subtitle = "主题、颜色、卡片风格等",
                    onClick = onNavigateToPreferences
                )
            }

            // 设置分组标题
            item {
                Text(
                    text = "订阅与模型",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 订阅管理
            item {
                SettingsItem(
                    icon = Icons.Default.Paid,
                    title = "订阅管理",
                    subtitle = "查看和切换订阅等级",
                    onClick = onNavigateToSubscription
                )
            }

            // AI 模型设置
            item {
                SettingsItem(
                    icon = Icons.Default.Psychology,
                    title = "AI 模型设置",
                    subtitle = "配置大语言模型和 API",
                    onClick = onNavigateToModelSettings
                )
            }

            // 设置分组标题
            item {
                Text(
                    text = "其他",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 服务器设置
            item {
                SettingsItem(
                    icon = Icons.Default.Cloud,
                    title = "服务器设置",
                    subtitle = "配置后端服务器地址",
                    onClick = onNavigateToServerConfig
                )
            }

            // 关于MTC
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MTC v1.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 退出登录
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("退出登录")
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
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
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 头像上传确认对话框
 */
@Composable
private fun AvatarUploadDialog(
    isUploading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("更换头像") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("上传中...", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("确定要使用这张图片作为头像吗？")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isUploading
            ) {
                Text("确认")
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
