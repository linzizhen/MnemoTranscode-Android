package com.mtc.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtc.app.data.remote.ServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 服务器配置页面 + 连接诊断工具
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigScreen(
    onServerConfigured: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var testResult by remember { mutableStateOf<TestResult?>(null) }

    // 加载已保存的服务器地址
    LaunchedEffect(Unit) {
        val savedUrl = ServerManager.getServerUrlSync(context)
        serverUrl = savedUrl?.trimEnd('/') ?: ""
        isLoading = false
    }

    // 连接测试函数
    fun testConnection(url: String) {
        if (url.isBlank()) {
            testResult = TestResult.Error("请先输入服务器地址")
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            testResult = TestResult.Error("地址必须以 http:// 或 https:// 开头")
            return
        }

        scope.launch {
            isTesting = true
            testResult = TestResult.Testing

            try {
                // 用 kotlinx.coroutines 的 delay 模拟超时检测
                val startTime = System.currentTimeMillis()
                val baseUrl = if (url.endsWith("/")) url else "$url/"

                // 简单网络检测：尝试解析域名/ping 逻辑
                // 由于 Android 没有内置 ping，我们用 URLConnection 做轻量探测
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val conn = java.net.URL("$baseUrl").openConnection() as java.net.HttpURLConnection
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        conn.requestMethod = "GET"
                        conn.instanceFollowRedirects = true
                        val responseCode = conn.responseCode
                        val elapsed = System.currentTimeMillis() - startTime

                        if (responseCode in 200..399 || responseCode == 401 || responseCode == 403) {
                            testResult = TestResult.Success(
                                "连接成功 (HTTP $responseCode)，耗时 ${elapsed}ms",
                                baseUrl
                            )
                        } else {
                            testResult = TestResult.Error("服务器返回 HTTP $responseCode，请检查后端是否正常运行")
                        }
                        conn.disconnect()
                    } catch (e: java.net.SocketTimeoutException) {
                        testResult = TestResult.Error("连接超时（5秒），请检查服务器是否运行或 IP 是否正确")
                    } catch (e: java.net.UnknownHostException) {
                        testResult = TestResult.Error("无法解析服务器地址，请检查 IP 或域名是否正确")
                    } catch (e: java.net.ConnectException) {
                        testResult = TestResult.Error("无法连接到服务器，请检查服务器是否运行或端口是否正确")
                    } catch (e: Exception) {
                        testResult = TestResult.Error("连接失败: ${e.javaClass.simpleName}")
                    }
                }
            } catch (e: Exception) {
                testResult = TestResult.Error("测试异常: ${e.message}")
            } finally {
                isTesting = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "服务器配置",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "请输入后端服务器地址",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            // 当前已保存的地址（仅首次启动时显示提示）
            val currentSaved = remember { ServerManager.getDefaultServerUrl() }
            if (serverUrl.isBlank()) {
                Text(
                    text = "首次使用，请填写你的后端服务器地址",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "示例: http://192.168.1.100:8000",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Text(
                    text = "当前地址: $currentSaved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 服务器地址输入
            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    errorMessage = null
                    testResult = null // 重新输入时清除测试结果
                },
                label = { Text("服务器地址") },
                placeholder = { Text("http://192.168.1.100:8000") },
                leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { testConnection(serverUrl) }
                ),
                singleLine = true,
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "示例: http://192.168.1.100:8000 或 http://10.0.0.1:8000",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 连接诊断卡片
            testResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (result) {
                            is TestResult.Success -> MaterialTheme.colorScheme.primaryContainer
                            is TestResult.Error -> MaterialTheme.colorScheme.errorContainer
                            is TestResult.Testing -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (result) {
                            is TestResult.Success -> {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "连接成功",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = result.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            is TestResult.Error -> {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "连接失败",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = result.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            is TestResult.Testing -> {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "正在测试连接...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 测试连接按钮
            OutlinedButton(
                onClick = { testConnection(serverUrl) },
                enabled = !isTesting && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.NetworkPing, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTesting) "测试中..." else "测试连接")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 确认按钮
            Button(
                onClick = {
                    val trimmedUrl = serverUrl.trim()
                    if (trimmedUrl.isEmpty()) {
                        errorMessage = "请输入服务器地址"
                        return@Button
                    }

                    if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                        errorMessage = "地址必须以 http:// 或 https:// 开头"
                        return@Button
                    }

                    isSaving = true
                    scope.launch {
                        try {
                            ServerManager.saveServerUrl(context, trimmedUrl)
                            onServerConfigured()
                        } catch (e: Exception) {
                            errorMessage = "保存失败: ${e.message}"
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving && !isTesting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("确认并继续")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private sealed class TestResult {
    data class Success(val message: String, val url: String) : TestResult()
    data class Error(val message: String) : TestResult()
    data object Testing : TestResult()
}
