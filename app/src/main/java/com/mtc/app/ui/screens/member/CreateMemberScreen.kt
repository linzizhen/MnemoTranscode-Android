package com.mtc.app.ui.screens.member

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMemberScreen(
    archiveId: Int,
    onNavigateBack: () -> Unit,
    onMemberCreated: (Int) -> Unit,
    viewModel: CreateMemberViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var relationshipType by remember { mutableStateOf("") }
    var birthYear by remember { mutableStateOf("") }
    var endYear by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("active") }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && uiState.createdMemberId != null) {
            onMemberCreated(uiState.createdMemberId!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加成员") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("成员姓名 *") },
                placeholder = { Text("例如：张三") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = relationshipType,
                onValueChange = { relationshipType = it },
                label = { Text("关系 *") },
                placeholder = { Text("例如：父亲、母亲、祖父") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = birthYear,
                    onValueChange = { birthYear = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("出生年份") },
                    placeholder = { Text("例如：1960") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = endYear,
                    onValueChange = { endYear = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("去世年份") },
                    placeholder = { Text("例如：2020") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = status == "passed",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "是否在世",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = status == "active",
                    onCheckedChange = { isAlive ->
                        status = if (isAlive) "active" else "passed"
                    }
                )
            }

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("人物简介") },
                placeholder = { Text("简要描述这个人的生平") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.createMember(
                        archiveId = archiveId,
                        name = name,
                        relationshipType = relationshipType,
                        birthYear = birthYear.toIntOrNull(),
                        endYear = endYear.toIntOrNull(),
                        status = status,
                        bio = bio.ifBlank { null }
                    )
                },
                enabled = !uiState.isLoading && name.isNotBlank() && relationshipType.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("添加成员")
                }
            }
        }
    }
}
