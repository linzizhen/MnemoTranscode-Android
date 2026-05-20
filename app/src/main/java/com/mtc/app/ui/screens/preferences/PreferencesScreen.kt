package com.mtc.app.ui.screens.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mtc.app.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onNavigateBack: () -> Unit,
    viewModel: PreferencesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("设置已保存")
            viewModel.resetSaveSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            snackbarHostState.showSnackbar(uiState.error!!)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPreferences() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // 用量卡片
                uiState.quota?.let { quota ->
                    QuotaCard(
                        quota = quota,
                        usageStats = uiState.usageStats
                    )
                }

                // 外观设置
                PreferencesSection(title = "外观") {
                    val prefs = uiState.preferences

                    // 主题
                    PreferencesSelector(
                        title = "主题模式",
                        icon = Icons.Default.Brightness6,
                        currentValue = prefs?.themeDisplay ?: "浅色模式",
                        options = viewModel.themeOptions.map { it.second to it.first },
                        onSelect = { viewModel.updateTheme(it) }
                    )

                    // 主题色
                    PreferencesSelector(
                        title = "主题色",
                        icon = Icons.Default.Palette,
                        currentValue = prefs?.primaryColorDisplay ?: "翡翠绿",
                        options = viewModel.colorOptions.map { it.second to it.first },
                        onSelect = { viewModel.updatePrimaryColor(it) }
                    )

                    // 卡片风格
                    PreferencesSelector(
                        title = "卡片风格",
                        icon = Icons.Default.Layers,
                        currentValue = prefs?.cardStyleDisplay ?: "毛玻璃",
                        options = viewModel.cardStyleOptions.map { it.second to it.first },
                        onSelect = { viewModel.updateCardStyle(it) }
                    )

                    // 字体大小
                    PreferencesSelector(
                        title = "字体大小",
                        icon = Icons.Default.FormatSize,
                        currentValue = prefs?.fontSizeDisplay ?: "中",
                        options = viewModel.fontSizeOptions.map { it.second to it.first },
                        onSelect = { viewModel.updateFontSize(it) }
                    )
                }

                // AI 设置
                PreferencesSection(title = "AI 设置") {
                    val prefs = uiState.preferences
                    val aiSyncEnabled = prefs?.aiMemorySync == "on"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "AI 记忆同步",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "自动整理和归类记忆",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = aiSyncEnabled,
                            onCheckedChange = { viewModel.updateAiMemorySync(it) },
                            enabled = !uiState.isSaving
                        )
                    }
                }

                // 应用信息
                PreferencesSection(title = "关于") {
                    PreferencesItem(
                        title = "版本",
                        icon = Icons.Default.Info,
                        subtitle = "1.0.0"
                    )
                    PreferencesItem(
                        title = "隐私政策",
                        icon = Icons.Default.Policy,
                        subtitle = null,
                        onClick = { }
                    )
                    PreferencesItem(
                        title = "使用条款",
                        icon = Icons.Default.Description,
                        subtitle = null,
                        onClick = { }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun QuotaCard(
    quota: com.mtc.app.domain.model.Quota,
    usageStats: com.mtc.app.domain.model.UsageStats?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quota.tierIcon,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = quota.tierDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "本月用量: ${quota.monthlyUsed} / ${quota.monthlyLimit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { quota.usagePercent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (quota.usagePercent > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "剩余: ${quota.remaining} tokens",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "${(quota.usagePercent * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun PreferencesSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                content()
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun PreferencesItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subtitle: String?,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (onClick != null) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun <T> PreferencesSelector(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    currentValue: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currentValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        options.forEach { (label, value) ->
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    onSelect(value)
                    expanded = false
                },
                trailingIcon = {
                    if (label == currentValue) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            )
        }
    }
}
