package com.mtc.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mtc.app.data.model.LlmPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ModelSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showModeSelector by remember { mutableStateOf(false) }
    var showModelSelector by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("配置已保存")
            viewModel.clearSaveSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模型设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveConfiguration() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("保存中...")
                        } else {
                            Text("保存")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                SettingsSectionHeader(title = "快速配置")
            }

            items(viewModel.defaultPresets) { preset ->
                PresetCard(
                    preset = preset,
                    isSelected = uiState.selectedPresetId == preset.id,
                    onClick = { viewModel.selectPreset(preset.id) }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            }

            item {
                SettingsSectionHeader(title = "自定义配置")
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsSelectorItem(
                        title = "接入模式",
                        currentValue = viewModel.modeOptions.find { it.first == uiState.customMode }?.second ?: "",
                        onClick = { showModeSelector = true }
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.customBaseUrl,
                    onValueChange = { viewModel.updateCustomBaseUrl(it) },
                    label = { Text("Base URL *") },
                    placeholder = { Text("例如: https://api.openai.com/v1") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null)
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.customApiKey,
                    onValueChange = { viewModel.updateCustomApiKey(it) },
                    label = { Text("API Key") },
                    placeholder = { Text("可选，部分服务需要") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null)
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.customModel,
                    onValueChange = { viewModel.updateCustomModel(it) },
                    label = { Text("模型名称 *") },
                    placeholder = { Text("例如: gpt-4o-mini") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Psychology, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.availableModels.isNotEmpty()) {
                            IconButton(onClick = { showModelSelector = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "选择模型")
                            }
                        }
                    }
                )
            }

            if (uiState.availableModels.isNotEmpty()) {
                item {
                    Text(
                        text = "可用模型: " + uiState.availableModels.take(5).joinToString(", ") + if (uiState.availableModels.size > 5) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.checkConnection() },
                    enabled = !uiState.isChecking && uiState.customBaseUrl.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    if (uiState.isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("测试中...")
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("测试连接")
                    }
                }
            }

            uiState.checkResult?.let { result ->
                item {
                    ConnectionResultCard(result = result)
                }
            }
        }
    }

    if (showModeSelector) {
        AlertDialog(
            onDismissRequest = { showModeSelector = false },
            title = { Text("选择接入模式") },
            text = {
                Column {
                    viewModel.modeOptions.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateCustomMode(mode)
                                    showModeSelector = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.customMode == mode,
                                onClick = {
                                    viewModel.updateCustomMode(mode)
                                    showModeSelector = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModeSelector = false }) {
                    Text("关闭")
                }
            }
        )
    }

    if (showModelSelector && uiState.availableModels.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showModelSelector = false },
            title = { Text("选择模型") },
            text = {
                LazyColumn {
                    items(uiState.availableModels) { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateCustomModel(model)
                                    showModelSelector = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.customModel == model,
                                onClick = {
                                    viewModel.updateCustomModel(model)
                                    showModelSelector = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(model)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelSelector = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun PresetCard(
    preset: LlmPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (preset.description != null) {
                    Text(
                        text = preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (preset.models != null && preset.models.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "模型: " + preset.models.take(3).joinToString(", ") + if (preset.models.size > 3) "..." else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SettingsSelectorItem(
    title: String,
    currentValue: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
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
}

@Composable
fun ConnectionResultCard(result: com.mtc.app.data.model.LlmCheckResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result.ok)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (result.ok) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (result.ok)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (result.ok) "连接成功" else "连接失败",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (result.ok)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
                if (result.latencyMs != null) {
                    Text(
                        text = "延迟: " + result.latencyMs + "ms",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (result.models.isNotEmpty()) {
                    Text(
                        text = "检测到 " + result.models.size + " 个模型",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (result.error != null) {
                    Text(
                        text = result.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}