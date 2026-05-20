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

private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "mtc_preferences")

/**
 * 统一管理用户本地偏好（主题、颜色等）
 * 与后端偏好设置同步，本地优先保证即时响应
 */
object PreferencesManager {
    private val THEME_KEY = stringPreferencesKey("theme")
    private val PRIMARY_COLOR_KEY = stringPreferencesKey("primary_color")
    private val CARD_STYLE_KEY = stringPreferencesKey("card_style")
    private val FONT_SIZE_KEY = stringPreferencesKey("font_size")
    private val LLM_MODE_KEY = stringPreferencesKey("llm_mode")
    private val LLM_BASE_URL_KEY = stringPreferencesKey("llm_base_url")
    private val LLM_API_KEY_KEY = stringPreferencesKey("llm_api_key")
    private val LLM_MODEL_KEY = stringPreferencesKey("llm_model")

    // 同步偏好到本地存储
    suspend fun syncFromBackend(
        context: Context,
        theme: String,
        primaryColor: String,
        cardStyle: String,
        fontSize: String
    ) {
        context.preferencesDataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
            preferences[PRIMARY_COLOR_KEY] = primaryColor
            preferences[CARD_STYLE_KEY] = cardStyle
            preferences[FONT_SIZE_KEY] = fontSize
        }
    }

    // 获取主题
    fun getTheme(context: Context): Flow<String> {
        return context.preferencesDataStore.data.map { preferences ->
            preferences[THEME_KEY] ?: "light"
        }
    }

    // 获取主题色
    fun getPrimaryColor(context: Context): Flow<String> {
        return context.preferencesDataStore.data.map { preferences ->
            preferences[PRIMARY_COLOR_KEY] ?: "jade"
        }
    }

    // 获取卡片风格
    fun getCardStyle(context: Context): Flow<String> {
        return context.preferencesDataStore.data.map { preferences ->
            preferences[CARD_STYLE_KEY] ?: "glass"
        }
    }

    // 获取字体大小
    fun getFontSize(context: Context): Flow<String> {
        return context.preferencesDataStore.data.map { preferences ->
            preferences[FONT_SIZE_KEY] ?: "medium"
        }
    }

    // 获取所有偏好（同步）
    suspend fun getAllSync(context: Context): ThemePreferences {
        val prefs = context.preferencesDataStore.data.first()
        return ThemePreferences(
            theme = prefs[THEME_KEY] ?: "light",
            primaryColor = prefs[PRIMARY_COLOR_KEY] ?: "jade",
            cardStyle = prefs[CARD_STYLE_KEY] ?: "glass",
            fontSize = prefs[FONT_SIZE_KEY] ?: "medium"
        )
    }

    // 本地更新主题
    suspend fun updateTheme(context: Context, theme: String) {
        context.preferencesDataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    // 本地更新颜色
    suspend fun updatePrimaryColor(context: Context, color: String) {
        context.preferencesDataStore.edit { preferences ->
            preferences[PRIMARY_COLOR_KEY] = color
        }
    }

    // 本地更新卡片风格
    suspend fun updateCardStyle(context: Context, style: String) {
        context.preferencesDataStore.edit { preferences ->
            preferences[CARD_STYLE_KEY] = style
        }
    }

    // 本地更新字体大小
    suspend fun updateFontSize(context: Context, size: String) {
        context.preferencesDataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = size
        }
    }

    // ========== LLM 配置本地存储 ==========

    // 本地保存 LLM 配置
    suspend fun saveLlmConfig(
        context: Context,
        mode: String,
        baseUrl: String,
        apiKey: String?,
        model: String
    ) {
        context.preferencesDataStore.edit { preferences ->
            preferences[LLM_MODE_KEY] = mode
            preferences[LLM_BASE_URL_KEY] = baseUrl
            if (!apiKey.isNullOrBlank()) {
                preferences[LLM_API_KEY_KEY] = apiKey
            }
            preferences[LLM_MODEL_KEY] = model
        }
    }

    // 获取本地 LLM 配置（同步）
    suspend fun getLlmConfigSync(context: Context): LlmConfigLocal {
        val prefs = context.preferencesDataStore.data.first()
        return LlmConfigLocal(
            mode = prefs[LLM_MODE_KEY] ?: "openai",
            baseUrl = prefs[LLM_BASE_URL_KEY] ?: "",
            apiKey = prefs[LLM_API_KEY_KEY] ?: "",
            model = prefs[LLM_MODEL_KEY] ?: ""
        )
    }

    // 清除 LLM API Key
    suspend fun clearLlmApiKey(context: Context) {
        context.preferencesDataStore.edit { preferences ->
            preferences.remove(LLM_API_KEY_KEY)
        }
    }
}

data class ThemePreferences(
    val theme: String = "light",
    val primaryColor: String = "jade",
    val cardStyle: String = "glass",
    val fontSize: String = "medium"
) {
    val isDarkMode: Boolean
        get() = when (theme) {
            "dark" -> true
            "light" -> false
            "auto" -> false
            else -> false
        }

    val isAutoMode: Boolean
        get() = theme == "auto"
}

data class LlmConfigLocal(
    val mode: String = "openai",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = ""
)
