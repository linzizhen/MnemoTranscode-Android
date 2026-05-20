package com.mtc.app.data.model

import com.google.gson.annotations.SerializedName

// ========== 用量相关模型 ==========

data class UsageStatsResponse(
    @SerializedName("monthly_used") val monthlyUsed: Long,
    @SerializedName("monthly_used_user_key") val monthlyUsedUserKey: Long = 0,
    @SerializedName("monthly_limit") val monthlyLimit: Long,
    @SerializedName("usage_by_type") val usageByType: Map<String, Long> = emptyMap(),
    @SerializedName("usage_by_day") val usageByDay: List<UsageByDay> = emptyList(),
    val remaining: Long,
    @SerializedName("usage_percent") val usagePercent: Float,
    @SerializedName("storage_used") val storageUsed: Long = 0,
    @SerializedName("storage_quota") val storageQuota: Long = 0,
    @SerializedName("storage_usage_percent") val storageUsagePercent: Float = 0f
)

data class UsageByDay(
    val date: String,
    val tokens: Long
)

data class QuotaResponse(
    @SerializedName("subscription_tier") val subscriptionTier: String,
    @SerializedName("monthly_limit") val monthlyLimit: Long,
    @SerializedName("monthly_used") val monthlyUsed: Long,
    val remaining: Long,
    @SerializedName("usage_percent") val usagePercent: Float,
    @SerializedName("reset_at") val resetAt: String? = null
)

data class UsageHistoryResponse(
    val records: List<UsageRecordResponse>,
    val total: Int,
    val page: Int,
    @SerializedName("page_size") val pageSize: Int
)

data class UsageRecordResponse(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("action_type") val actionType: String,
    @SerializedName("token_count") val tokenCount: Long?,
    val cost: Long = 0,
    @SerializedName("model_name") val modelName: String? = null,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("created_at") val createdAt: String
)

// ========== 订阅相关模型 ==========

data class SubscriptionTierUpdate(
    val tier: String
)

data class SubscriptionResponse(
    val tier: String,
    @SerializedName("monthly_limit") val monthlyLimit: Long,
    @SerializedName("monthly_used") val monthlyUsed: Long,
    val remaining: Long,
    @SerializedName("usage_percent") val usagePercent: Float,
    @SerializedName("expires_at") val expiresAt: String? = null,
    val features: List<String> = emptyList()
)

// ========== LLM 探测模型 ==========

data class LlmCheckRequest(
    val mode: String = "openai",
    @SerializedName("base_url") val baseUrl: String,
    @SerializedName("api_key") val apiKey: String? = null
)

data class LlmCheckResponse(
    val ok: Boolean,
    @SerializedName("latency_ms") val latencyMs: Int?,
    val error: String?,
    val models: List<String>
)

/**
 * LLM 预设配置（本地模型，用于 ModelSettingsScreen）
 */
data class LlmPreset(
    val id: String,
    val name: String,
    val mode: String,
    @SerializedName("base_url") val baseUrl: String,
    @SerializedName("default_model") val defaultModel: String?,
    val models: List<String>?,
    val description: String?,
    val apiKey: String? = null
)

// ========== AI 记忆同步模型 ==========

data class AIMemorySummary(
    val summary: String? = null,
    @SerializedName("summary_text") val summaryText: String? = null,
    val content: String? = null,
    val text: String? = null,
    val timestamp: String? = null
)

data class AIMemoryContextResponse(
    val summaries: List<AIMemorySummary> = emptyList(),
    @SerializedName("last_updated") val lastUpdated: String? = null
)

data class AIMemoryUpdateRequest(
    val context: AIMemoryContextData
)

data class AIMemoryContextData(
    val summaries: List<AIMemorySummary> = emptyList()
)

// ========== 头像上传 ==========

data class AvatarUploadResponse(
    val url: String,
    val user: UserResponse
)
