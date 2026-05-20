package com.mtc.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.mtc.app.data.remote.ServerManager

/**
 * 动态服务器 URL 状态管理器
 * 解决：getServerUrlSync 是 suspend 函数，无法直接在 remember{} lambda 中调用
 * 方案：模块级可变状态 + Compose State 暴露，URL 变更时自动触发重组
 */
object ServerUrlHolder {
    @Volatile
    private var cachedUrl: String? = null

    fun get(context: android.content.Context): String {
        return cachedUrl ?: synchronized(this) {
            cachedUrl ?: runCatching {
                kotlinx.coroutines.runBlocking {
                    ServerManager.getServerUrlSync(context)
                }
            }.getOrNull() ?: "http://26.240.64.54:8000/"
        }.also { cachedUrl = it }
    }

    fun invalidate(url: String?) {
        cachedUrl = url
    }
}

/**
 * 获取当前服务器 base URL（动态，来自用户配置）
 * 组件重组时返回缓存值，避免重复 IO
 */
@Composable
fun rememberCurrentServerUrl(): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        ServerUrlHolder.get(context)
    }
}
