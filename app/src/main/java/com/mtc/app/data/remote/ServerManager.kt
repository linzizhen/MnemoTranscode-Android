package com.mtc.app.data.remote

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.serverDataStore: DataStore<Preferences> by preferencesDataStore(name = "mtc_server")

/**
 * 管理后端服务器地址
 * 用户可自定义输入服务器 IP 和端口
 */
object ServerManager {
    private val SERVER_URL_KEY = stringPreferencesKey("server_url")
    const val DEFAULT_SERVER_URL = "http://26.240.64.54:8000"

    // 保存服务器地址
    suspend fun saveServerUrl(context: Context, url: String) {
        val formattedUrl = if (url.endsWith("/")) url else "$url/"
        context.serverDataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = formattedUrl
        }
    }

    // 获取服务器地址（Flow）
    fun getServerUrl(context: Context): Flow<String> {
        return context.serverDataStore.data.map { preferences ->
            preferences[SERVER_URL_KEY] ?: DEFAULT_SERVER_URL
        }
    }

    // 获取服务器地址（同步）
    // 注意：此方法不再自动保存默认值；首次使用时若无保存值，返回 null
    suspend fun getServerUrlSync(context: Context): String? {
        val stored = context.serverDataStore.data.first()[SERVER_URL_KEY]
        return if (!stored.isNullOrBlank()) {
            stored
        } else {
            null
        }
    }

    // 检查是否已配置服务器（首次启动不会自动保存默认值）
    suspend fun isServerConfigured(context: Context): Boolean {
        val url = context.serverDataStore.data.first()[SERVER_URL_KEY]
        return !url.isNullOrBlank()
    }

    // 清除服务器配置
    suspend fun clearServerUrl(context: Context) {
        context.serverDataStore.edit { preferences ->
            preferences.remove(SERVER_URL_KEY)
        }
    }

    // 获取默认服务器地址
    fun getDefaultServerUrl(): String = DEFAULT_SERVER_URL
}
