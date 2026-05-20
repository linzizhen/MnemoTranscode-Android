package com.mtc.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 档案相关数据模型
 */
data class ArchiveResponse(
    val id: Int,
    val name: String,
    val description: String?,
    @SerializedName("archive_type") val archiveType: String,
    @SerializedName("owner_id") val ownerId: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("member_count") val memberCount: Int = 0,
    @SerializedName("memory_count") val memoryCount: Int = 0
)

data class CreateArchiveRequest(
    val name: String,
    val description: String?,
    @SerializedName("archive_type") val archiveType: String = "family"
)

data class UpdateArchiveRequest(
    val name: String?,
    val description: String?
)

/**
 * 成员相关数据模型
 * 使用 @SerializedName 指定后端 snake_case JSON 字段名
 */
data class MemberResponse(
    val id: Int,
    val name: String,
    @SerializedName("relationship_type") val relationshipType: String,
    @SerializedName("archive_id") val archiveId: Int,
    @SerializedName("birth_year") val birthYear: Int?,
    @SerializedName("end_year") val endYear: Int?,
    val bio: String?,
    val status: String = "active",
    @SerializedName("voice_profile_id") val voiceProfileId: String?,
    @SerializedName("emotion_tags") val emotionTags: List<String> = emptyList(),
    @SerializedName("memory_count") val memoryCount: Int = 0,
    @SerializedName("created_at") val createdAt: String,
    // 兼容字段（后端 model_serializer 动态派生）
    @SerializedName("is_alive") val isAlive: Boolean? = null,
    @SerializedName("death_year") val deathYear: Int? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

/**
 * 成员头像上传响应（后端直接返回 MemberResponse，不需要包装）
 * 注意：后端 upload_member_avatar 返回的是 MemberResponse 而非 {avatar_url, member}
 */
typealias MemberAvatarResponse = MemberResponse

data class CreateMemberRequest(
    val name: String,
    @SerializedName("relationship_type") val relationshipType: String,
    @SerializedName("birth_year") val birthYear: Int? = null,
    val status: String = "active",
    @SerializedName("end_year") val endYear: Int? = null,
    val bio: String? = null
)

data class UpdateMemberRequest(
    val name: String? = null,
    @SerializedName("relationship_type") val relationshipType: String? = null,
    @SerializedName("birth_year") val birthYear: Int? = null,
    val status: String? = null,
    @SerializedName("end_year") val endYear: Int? = null,
    val bio: String? = null
)

/**
 * 成员头像文件响应（获取签名 URL）
 */
data class MemberAvatarFileResponse(
    @SerializedName("get_url") val getUrl: String,
    @SerializedName("expires_in") val expiresIn: Int
)

// ========== 用量统计相关模型（支持 channel 分离）==========

/**
 * 用量统计响应（新版 - 分离 subscription 和 user_key channel）
 */
data class UsageStatsResponseV2(
    @SerializedName("subscription_channel") val subscriptionChannel: ChannelUsage,
    @SerializedName("user_key_channel") val userKeyChannel: ChannelUsage?,
    val remaining: Long,
    @SerializedName("usage_percent") val usagePercent: Float
)

/**
 * 单个 channel 的用量
 */
data class ChannelUsage(
    @SerializedName("monthly_used") val monthlyUsed: Long,
    @SerializedName("monthly_limit") val monthlyLimit: Long,
    @SerializedName("usage_by_type") val usageByType: Map<String, Long> = emptyMap(),
    @SerializedName("usage_by_day") val usageByDay: List<UsageByDay> = emptyList()
)

/**
 * 记忆相关数据模型
 */
data class MemoryResponse(
    val id: Int,
    val title: String,
    @SerializedName("content_text") val contentText: String,
    @SerializedName("member_id") val memberId: Int,
    @SerializedName("archive_id") val archiveId: Int,
    @SerializedName("emotion_label") val emotionLabel: String?,
    @SerializedName("timestamp") val timestamp: String?,
    val location: String?,
    @SerializedName("vector_embedding_id") val vectorEmbeddingId: String?,
    @SerializedName("is_capsule") val isCapsule: Boolean = false,
    @SerializedName("unlock_date") val unlockDate: String?,
    @SerializedName("media_refs") val mediaRefs: List<String> = emptyList(),
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class CreateMemoryRequest(
    @SerializedName("member_id") val memberId: Int,
    val title: String,
    @SerializedName("content_text") val contentText: String,
    val timestamp: String? = null,
    val location: String? = null,
    @SerializedName("emotion_label") val emotionLabel: String? = null
)

data class UpdateMemoryRequest(
    val title: String?,
    @SerializedName("content_text") val contentText: String?,
    @SerializedName("emotion_label") val emotionLabel: String?
)

data class MemorySearchRequest(
    val query: String,
    @SerializedName("archive_id") val archiveId: Int? = null,
    @SerializedName("member_id") val memberId: Int? = null,
    @SerializedName("emotion_label") val emotionLabel: String? = null,
    val limit: Int = 10
)

data class MemorySearchResponse(
    val results: List<MemoryResponse>,
    val query: String,
    val total: Int
)
