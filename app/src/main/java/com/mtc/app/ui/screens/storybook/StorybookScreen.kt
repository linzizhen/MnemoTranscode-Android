package com.mtc.app.ui.screens.storybook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.mtc.app.domain.model.Archive
import com.mtc.app.domain.model.Member
import com.mtc.app.ui.components.EmptyState
import com.mtc.app.ui.components.ErrorState
import com.mtc.app.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorybookScreen(
    archiveId: Int,
    onNavigateBack: () -> Unit,
    viewModel: StorybookViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(archiveId) {
        viewModel.selectArchive(archiveId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("故事书生成") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> {
                LoadingIndicator(modifier = Modifier.padding(padding))
            }
            uiState.error != null && uiState.archives.isEmpty() -> {
                ErrorState(
                    message = uiState.error ?: "",
                    onRetry = { },
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.archives.isEmpty() -> {
                EmptyState(
                    message = "暂无档案，请先创建档案",
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ArchiveSelector(
                        archives = uiState.archives,
                        selectedArchiveId = uiState.selectedArchiveId,
                        onSelectArchive = { viewModel.selectArchive(it) }
                    )

                    if (uiState.members.isNotEmpty()) {
                        MemberSelector(
                            members = uiState.members,
                            selectedMemberId = uiState.selectedMemberId,
                            onSelectMember = { viewModel.selectMember(it) }
                        )
                    }

                    StyleSelector(
                        styles = viewModel.storyStyles,
                        selectedStyle = uiState.selectedStyle,
                        onSelectStyle = { viewModel.selectStyle(it) }
                    )

                    Button(
                        onClick = { viewModel.generateStory() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isGenerating && uiState.selectedArchiveId != null
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("生成中...")
                        } else {
                            Icon(Icons.Default.AutoStories, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("生成故事书")
                        }
                    }

                    AnimatedVisibility(visible = uiState.error != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.error ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(Icons.Default.Close, contentDescription = "关闭")
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = uiState.story != null) {
                        StoryResultCard(
                            story = uiState.story!!.story,
                            memoryCount = uiState.story!!.memoryCount,
                            style = uiState.story!!.style,
                            snackbarHostState = snackbarHostState,
                            onClear = { viewModel.clearStory() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveSelector(
    archives: List<Archive>,
    selectedArchiveId: Int?,
    onSelectArchive: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedArchive = archives.find { it.id == selectedArchiveId }

    Column {
        Text(
            text = "选择档案",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedArchive?.name ?: "请选择档案",
                onValueChange = {},
                readOnly = true,
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
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberSelector(
    members: List<Member>,
    selectedMemberId: Int?,
    onSelectMember: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedMember = members.find { it.id == selectedMemberId }

    Column {
        Text(
            text = "选择成员（可选）",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedMember?.name ?: "全部成员",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("全部成员") },
                    onClick = {
                        onSelectMember(null)
                        expanded = false
                    }
                )
                members.forEach { member ->
                    DropdownMenuItem(
                        text = { Text(member.name) },
                        onClick = {
                            onSelectMember(member.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StyleSelector(
    styles: List<Pair<String, String>>,
    selectedStyle: String,
    onSelectStyle: (String) -> Unit
) {
    Column {
        Text(
            text = "故事风格",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            styles.forEach { (key, label) ->
                FilterChip(
                    selected = selectedStyle == key,
                    onClick = { onSelectStyle(key) },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StoryResultCard(
    story: String,
    memoryCount: Int,
    style: String,
    snackbarHostState: SnackbarHostState,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val styleLabel = when (style) {
        "nostalgic" -> "怀旧温情"
        "literary" -> "文学风格"
        "simple" -> "简洁直白"
        "dialogue" -> "对话风格"
        else -> style
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
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
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "生成的故事",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Row {
                    FilledTonalIconButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("故事书全文", story)
                            clipboard.setPrimaryClip(clip)
                            scope.launch {
                                snackbarHostState.showSnackbar("已复制到剪贴板", duration = SnackbarDuration.Short)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "复制全文",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("$memoryCount 条记忆") }
                )
                AssistChip(
                    onClick = {},
                    label = { Text(styleLabel) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = story,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Start
            )
        }
    }
}
