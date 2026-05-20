package com.mtc.app.ui.screens.preferences

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.data.remote.PreferencesManager
import com.mtc.app.domain.model.Quota
import com.mtc.app.domain.model.UsageStats
import com.mtc.app.domain.model.UserPreferences
import com.mtc.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreferencesUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val preferences: UserPreferences? = null,
    val usageStats: UsageStats? = null,
    val quota: Quota? = null,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreferencesUiState())
    val uiState: StateFlow<PreferencesUiState> = _uiState.asStateFlow()

    val themeOptions = listOf(
        "light" to "浅色模式",
        "dark" to "深色模式",
        "auto" to "跟随系统"
    )

    val colorOptions = listOf(
        "jade" to "翡翠绿",
        "amber" to "琥珀色",
        "rose" to "玫瑰红",
        "sky" to "天空蓝",
        "violet" to "紫罗兰",
        "forest" to "森林绿"
    )

    val cardStyleOptions = listOf(
        "glass" to "毛玻璃",
        "minimal" to "简约",
        "elevated" to "悬浮"
    )

    val fontSizeOptions = listOf(
        "small" to "小",
        "medium" to "中",
        "large" to "大"
    )

    init {
        loadPreferences()
    }

    fun loadPreferences() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            preferencesRepository.getPreferences()
                .onSuccess { prefs ->
                    // 同步到本地 PreferencesManager
                    PreferencesManager.syncFromBackend(
                        context = context,
                        theme = prefs.theme,
                        primaryColor = prefs.primaryColor,
                        cardStyle = prefs.cardStyle,
                        fontSize = prefs.fontSize
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        preferences = prefs
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }

            // 同时加载用量统计
            preferencesRepository.getUsageStats()
                .onSuccess { stats ->
                    _uiState.value = _uiState.value.copy(usageStats = stats)
                }

            preferencesRepository.getQuota()
                .onSuccess { quota ->
                    _uiState.value = _uiState.value.copy(quota = quota)
                }
        }
    }

    private fun updatePreferences(
        theme: String? = null,
        primaryColor: String? = null,
        cardStyle: String? = null,
        fontSize: String? = null,
        aiMemorySync: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            // 本地优先更新（即时生效）
            theme?.let { PreferencesManager.updateTheme(context, it) }
            primaryColor?.let { PreferencesManager.updatePrimaryColor(context, it) }
            cardStyle?.let { PreferencesManager.updateCardStyle(context, it) }
            fontSize?.let { PreferencesManager.updateFontSize(context, it) }

            preferencesRepository.updatePreferences(
                theme = theme,
                primaryColor = primaryColor,
                cardStyle = cardStyle,
                fontSize = fontSize,
                aiMemorySync = aiMemorySync
            )
                .onSuccess { prefs ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        preferences = prefs,
                        saveSuccess = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message
                    )
                }
        }
    }

    fun updateTheme(theme: String) {
        updatePreferences(theme = theme)
    }

    fun updatePrimaryColor(color: String) {
        updatePreferences(primaryColor = color)
    }

    fun updateCardStyle(style: String) {
        updatePreferences(cardStyle = style)
    }

    fun updateFontSize(size: String) {
        updatePreferences(fontSize = size)
    }

    fun updateAiMemorySync(enabled: Boolean) {
        updatePreferences(aiMemorySync = if (enabled) "on" else "off")
    }

    fun resetSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
