package com.mtc.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mtc.app.domain.repository.MediaRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Hilt EntryPoint 用于在 Composable 中获取 Repository */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MediaRepositoryEntryPoint {
    fun mediaRepository(): MediaRepository
}

/**
 * 成员头像组件
 */
@Composable
fun MemberAvatar(
    avatarUrl: String?,
    name: String,
    isAlive: Boolean = true,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val baseUrl = rememberCurrentServerUrl()

    val fullAvatarUrl = when {
        avatarUrl.isNullOrBlank() -> null
        avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://") -> avatarUrl
        else -> "${baseUrl.trimEnd('/')}${avatarUrl}"
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (fullAvatarUrl != null) {
            // 使用 auth-aware ImageLoader（由 MtcApplication.ImageLoaderFactory 全局注入 Bearer Token）
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(fullAvatarUrl)
                    .crossfade(300)
                    .build(),
                contentDescription = "$name 的头像",
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(size * 0.5f),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                error = {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        modifier = Modifier.size(size * 0.6f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        } else {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(size * 0.6f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 媒体缩略图组件。
 * 图片加载路径：
 *   LocalContext.imageLoader（带 AuthInterceptor 的 OkHttpClient）
 *   → GET ${BASE_URL}/api/v1/media/${mediaId}/file
 *   → 后端 streaming_response_for_object_key 从 MinIO 拉流返回原始图片字节
 *
 * 使用方式：
 *   优先传入已预加载的 downloadUrl；若无则传入 mediaId（组件内直接加载）。
 *   不要同时传 objectKey + mediaId，objectKey 仅作为兜底 fallback。
 */
@Composable
fun MediaThumbnailImage(
    objectKey: String = "",
    downloadUrl: String? = null,
    mediaId: Int? = null,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val baseUrl = rememberCurrentServerUrl()

    val imageUrl: String? = downloadUrl
        ?: (mediaId?.takeIf { it > 0 }?.let { "${baseUrl.trimEnd('/')}/api/v1/media/$it/file" })
        ?: objectKey.takeIf { it.isNotBlank() }?.let { "${baseUrl.trimEnd('/')}/api/v1/media/file?object_key=$it" }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            // 使用 auth-aware ImageLoader（由 MtcApplication.ImageLoaderFactory 全局注入 Bearer Token）
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(200)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                error = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.BrokenImage,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        } else {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 自包含媒体缩略图（不依赖外部 downloadUrl，直接用 mediaId 加载）。
 * 用于媒体库列表等需要自行加载 URL 的场景。
 */
@Composable
fun MediaSelfContainedThumbnail(
    mediaId: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val baseUrl = rememberCurrentServerUrl()
    val imageUrl = "${baseUrl.trimEnd('/')}/api/v1/media/$mediaId/file"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
            // 使用 auth-aware ImageLoader（由 MtcApplication.ImageLoaderFactory 全局注入 Bearer Token）
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(200)
                    .build(),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                loading = {
                Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            error = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.BrokenImage,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}
