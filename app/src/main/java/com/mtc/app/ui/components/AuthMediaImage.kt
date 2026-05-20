package com.mtc.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 带鉴权的媒体图片组件。
 * 有 presigned downloadUrl → Coil 加载（使用 auth-aware ImageLoader）
 * 无 URL → 通过 MediaRepository（带 token）字节流加载
 */
@Composable
fun AuthMediaImage(
    mediaId: Int,
    downloadUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholderSize: Dp = 32.dp,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isRepoLoading by remember { mutableStateOf(false) }
    var repoLoadFailed by remember { mutableStateOf(false) }

    val hasPresignedUrl = !downloadUrl.isNullOrBlank()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (hasPresignedUrl) {
            // 路径1：Coil + presigned URL（使用全局 auth-aware ImageLoader，由 MtcApplication.ImageLoaderFactory 设置）
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(downloadUrl)
                    .crossfade(200)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(placeholderSize), strokeWidth = 2.dp)
                    }
                },
                error = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.BrokenImage, null,
                            Modifier.size(placeholderSize),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        } else {
            // 路径2：Repository 字节流加载（无 presigned URL 时兜底）
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            } else if (isRepoLoading) {
                CircularProgressIndicator(modifier = Modifier.size(placeholderSize), strokeWidth = 2.dp)
            } else if (repoLoadFailed) {
                Icon(
                    Icons.Default.BrokenImage, null,
                    Modifier.size(placeholderSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // 无 presigned URL 时，通过 Repository 加载字节流
    if (!hasPresignedUrl) {
        LaunchedEffect(mediaId) {
            isRepoLoading = true
            repoLoadFailed = false
            try {
                val repo = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    MediaRepositoryEntryPoint::class.java
                ).mediaRepository()

                val bytes = withContext(Dispatchers.IO) {
                    repo.downloadMediaAsBytes(mediaId).getOrNull()
                }
                if (bytes != null) {
                    bitmap = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                } else {
                    repoLoadFailed = true
                }
            } catch (_: Exception) {
                repoLoadFailed = true
            }
            isRepoLoading = false
        }
    }
}
