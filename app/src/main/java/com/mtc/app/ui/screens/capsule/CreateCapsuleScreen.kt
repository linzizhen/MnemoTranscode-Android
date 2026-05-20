package com.mtc.app.ui.screens.capsule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCapsuleScreen(
    memberId: Int,
    archiveId: Int,
    onNavigateBack: () -> Unit,
    viewModel: CreateCapsuleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(30)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000
    )

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建记忆胶囊") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "给未来的自己或亲人留下一封时间胶囊",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("胶囊标题") },
                placeholder = { Text("输入一个有意义的标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.error != null && title.isBlank()
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("胶囊内容") },
                placeholder = { Text("写下你想对未来的TA说的话...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10,
                isError = uiState.error != null && content.isBlank()
            )

            // 日期选择
            OutlinedTextField(
                value = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                onValueChange = {},
                label = { Text("解锁日期") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                    }
                }
            )

            Text(
                text = "胶囊将在指定日期后解锁，在此之前内容不可见",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // 使用后端期望的日期格式：yyyy-MM-dd
                    val dateStr = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    viewModel.createCapsule(
                        memberId = memberId,
                        title = title,
                        content = content,
                        unlockDate = dateStr
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && title.isNotBlank() && content.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("封存胶囊")
            }
        }
    }
}
