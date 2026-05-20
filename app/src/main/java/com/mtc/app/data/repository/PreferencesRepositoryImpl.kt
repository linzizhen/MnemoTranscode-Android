package com.mtc.app.data.repository

import android.content.Context
import com.mtc.app.data.model.*
import com.mtc.app.data.remote.MtcApiService
import com.mtc.app.data.remote.PreferencesManager
import com.mtc.app.domain.model.Quota
import com.mtc.app.domain.model.UsageStats
import com.mtc.app.domain.model.UserPreferences
import com.mtc.app.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val apiService: MtcApiService,
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    override suspend fun getPreferences(): Result<UserPreferences> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPreferences()
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                // 后端不可用，尝试从本地加载
                loadFromLocal()
            }
        } catch (e: Exception) {
            // 网络失败，从本地加载
            loadFromLocal()
        }
    }

    private suspend fun loadFromLocal(): Result<UserPreferences> {
        val local = PreferencesManager.getLlmConfigSync(context)
        return if (local.baseUrl.isNotBlank() || local.model.isNotBlank()) {
            Result.success(UserPreferences(
                llmMode = local.mode,
                llmBaseUrl = local.baseUrl,
                llmApiKey = local.apiKey,
                llmModel = local.model
            ))
        } else {
            Result.failure(Exception("无法获取偏好设置"))
        }
    }

    override suspend fun updatePreferences(
        theme: String?,
        primaryColor: String?,
        cardStyle: String?,
        fontSize: String?,
        dashboardLayout: String?,
        customCss: String?,
        appBackgroundUrl: String?,
        aiMemorySync: String?
    ): Result<UserPreferences> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updatePreferences(
                PreferencesUpdateRequest(
                    theme = theme,
                    primaryColor = primaryColor,
                    cardStyle = cardStyle,
                    fontSize = fontSize,
                    dashboardLayout = dashboardLayout,
                    customCss = customCss,
                    appBackgroundUrl = appBackgroundUrl,
                    aiMemorySync = aiMemorySync
                )
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("更新偏好设置失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLlmConfig(
        mode: String,
        baseUrl: String,
        apiKey: String?,
        model: String
    ): Result<UserPreferences> = withContext(Dispatchers.IO) {
        try {
            // 同时保存到本地（离线可用）
            PreferencesManager.saveLlmConfig(context, mode, baseUrl, apiKey, model)

            val response = apiService.updatePreferences(
                PreferencesUpdateRequest(
                    llmMode = mode,
                    llmBaseUrl = baseUrl,
                    llmApiKey = apiKey,
                    llmModel = model
                )
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                // 后端失败但本地已保存，视为成功
                Result.success(UserPreferences(
                    llmMode = mode,
                    llmBaseUrl = baseUrl,
                    llmApiKey = apiKey,
                    llmModel = model
                ))
            }
        } catch (e: Exception) {
            // 网络失败时，配置已保存到本地，视为成功
            PreferencesManager.saveLlmConfig(context, mode, baseUrl, apiKey, model)
            Result.success(UserPreferences(
                llmMode = mode,
                llmBaseUrl = baseUrl,
                llmApiKey = apiKey,
                llmModel = model
            ))
        }
    }

    override suspend fun getUsageStats(): Result<UsageStats> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUsageStats()
            if (response.isSuccessful) {
                val body = response.body()!!
                Result.success(UsageStats(
                    monthlyUsed = body.monthlyUsed,
                    monthlyLimit = body.monthlyLimit,
                    usageByType = body.usageByType,
                    remaining = body.remaining,
                    usagePercent = body.usagePercent
                ))
            } else {
                Result.failure(Exception("获取用量统计失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getQuota(): Result<Quota> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getQuota()
            if (response.isSuccessful) {
                val body = response.body()!!
                Result.success(Quota(
                    subscriptionTier = body.subscriptionTier,
                    monthlyLimit = body.monthlyLimit,
                    monthlyUsed = body.monthlyUsed,
                    remaining = body.remaining,
                    usagePercent = body.usagePercent,
                    resetAt = body.resetAt
                ))
            } else {
                Result.failure(Exception("获取配额信息失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun PreferencesResponse.toDomain() = UserPreferences(
    id = id,
    userId = userId,
    theme = theme,
    primaryColor = primaryColor,
    cardStyle = cardStyle,
    fontSize = fontSize,
    dashboardLayout = dashboardLayout,
    customCss = customCss,
    appBackgroundUrl = appBackgroundUrl,
    aiMemorySync = aiMemorySync,
    llmMode = llmMode,
    llmBaseUrl = llmBaseUrl,
    llmApiKey = llmApiKey,
    llmModel = llmModel
)
