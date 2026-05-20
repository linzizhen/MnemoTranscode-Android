package com.mtc.app.domain.repository

import com.mtc.app.domain.model.Quota
import com.mtc.app.domain.model.UsageStats
import com.mtc.app.domain.model.UserPreferences

/**
 * 用户偏好 Repository 接口
 */
interface PreferencesRepository {
    suspend fun getPreferences(): Result<UserPreferences>
    suspend fun updatePreferences(
        theme: String? = null,
        primaryColor: String? = null,
        cardStyle: String? = null,
        fontSize: String? = null,
        dashboardLayout: String? = null,
        customCss: String? = null,
        appBackgroundUrl: String? = null,
        aiMemorySync: String? = null
    ): Result<UserPreferences>
    suspend fun updateLlmConfig(
        mode: String,
        baseUrl: String,
        apiKey: String?,
        model: String
    ): Result<UserPreferences>
    suspend fun getUsageStats(): Result<UsageStats>
    suspend fun getQuota(): Result<Quota>
}
