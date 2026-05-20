package com.mtc.app.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mtc.app.ui.components.LoadingIndicator
import com.mtc.app.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountInfoScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("保存成功")
            viewModel.clearSaveSuccess()
        }
    }

    if (showEditUsernameDialog) {
        EditUsernameDialog(
            currentUsername = uiState.user?.username ?: "",
            onDismiss = { showEditUsernameDialog = false },
            onConfirm = { newUsername ->
                viewModel.updateUsername(newUsername)
                showEditUsernameDialog = false
            }
        )
    }

    if (showEditEmailDialog) {
        EditEmailDialog(
            currentEmail = uiState.user?.email ?: "",
            onDismiss = { showEditEmailDialog = false },
            onConfirm = { newEmail ->
                viewModel.updateEmail(newEmail)
                showEditEmailDialog = false
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { oldPwd, newPwd ->
                viewModel.updatePassword(oldPwd, newPwd)
                showChangePasswordDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账号信息") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
        } else if (uiState.user != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // 用户信息卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        UserAvatar(
                            avatarUrl = uiState.user!!.avatarUrl,
                            username = uiState.user!!.username,
                            size = 96.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
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
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    when (uiState.user!!.subscriptionTierResolved) {
                                        "free" -> "免费版"
                                        "pro" -> "专业版"
                                        "enterprise" -> "企业版"
                                        else -> uiState.user!!.subscriptionTierResolved
                                    }
                                )
                            }
                        )
                    }
                }

                // 基本信息
                Text(
                    text = "基本信息",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column {
                        AccountInfoItem(
                            icon = Icons.Default.Person,
                            title = "用户名",
                            value = uiState.user!!.username,
                            onClick = { showEditUsernameDialog = true }
                        )
                        HorizontalDivider()
                        AccountInfoItem(
                            icon = Icons.Default.Email,
                            title = "邮箱",
                            value = uiState.user!!.email,
                            onClick = { showEditEmailDialog = true }
                        )
                        HorizontalDivider()
                        AccountInfoItem(
                            icon = Icons.Default.CalendarMonth,
                            title = "注册时间",
                            value = uiState.user!!.createdAt?.take(10) ?: "未知",
                            onClick = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 安全设置
                Text(
                    text = "安全设置",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    AccountInfoItem(
                        icon = Icons.Default.Lock,
                        title = "修改密码",
                        value = "******",
                        onClick = { showChangePasswordDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AccountInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: (() -> Unit)?
) {
    val modifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }

    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun EditUsernameDialog(
    currentUsername: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var username by remember { mutableStateOf(currentUsername) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改用户名") },
        text = {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("用户名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(username) },
                enabled = username.isNotBlank() && username != currentUsername
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun EditEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var email by remember { mutableStateOf(currentEmail) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改邮箱") },
        text = {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("邮箱") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(email) },
                enabled = email.isNotBlank() && email != currentEmail
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改密码") },
        text = {
            Column {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("当前密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("新密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("确认密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(oldPassword, newPassword) },
                enabled = oldPassword.isNotBlank() &&
                        newPassword.isNotBlank() &&
                        newPassword == confirmPassword &&
                        newPassword.length >= 6
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
