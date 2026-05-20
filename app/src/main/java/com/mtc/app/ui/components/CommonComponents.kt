package com.mtc.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * 用户头像组件
 * - 如果有 avatarUrl，尝试加载网络图片
 * - 如果加载失败或无 URL，显示字母头像
 */
@Composable
fun UserAvatar(
    avatarUrl: String?,
    username: String,
    size: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val baseUrl = rememberCurrentServerUrl()
    // 拼接完整的头像 URL（后端返回的是相对路径）
    val fullAvatarUrl = if (!avatarUrl.isNullOrBlank()) {
        if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
            avatarUrl
        } else {
            "${baseUrl.trimEnd('/')}$avatarUrl"
        }
    } else null

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (fullAvatarUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(fullAvatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "用户头像",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // 字母头像兜底
            val backgroundColor = MaterialTheme.colorScheme.primary
            val letter = username.take(1).uppercase()
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = backgroundColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        style = if (size >= 80.dp) {
                            MaterialTheme.typography.headlineLarge
                        } else if (size >= 56.dp) {
                            MaterialTheme.typography.headlineMedium
                        } else if (size >= 40.dp) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * 简化版头像（仅字母，用于小尺寸场景）
 */
@Composable
fun LetterAvatar(
    letter: String,
    size: Dp = 40.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = backgroundColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = letter.take(1).uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

/**
 * 通用加载状态组件
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String = "加载中..."
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 通用错误状态组件
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

/**
 * 通用空状态组件
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (action != null) {
                Spacer(modifier = Modifier.height(16.dp))
                action()
            }
        }
    }
}
