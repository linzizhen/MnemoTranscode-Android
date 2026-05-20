package com.mtc.app.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 用户领域模型
 */
data class User(
    val id: Int,
    val email: String,
    val username: String,
    val isActive: Boolean,
    val createdAt: String?,
    val avatarUrl: String? = null,
    val subscriptionTier: String? = null,
    val monthlyTokenLimit: Int? = null,
    val monthlyTokenUsed: Int? = null
) {
    val subscriptionTierResolved: String get() = subscriptionTier ?: "free"
    val monthlyTokenLimitResolved: Int get() = monthlyTokenLimit ?: 100000
    val monthlyTokenUsedResolved: Int get() = monthlyTokenUsed ?: 0
}

/**
 * 档案领域模型
 */
data class Archive(
    val id: Int,
    val name: String,
    val description: String?,
    val archiveType: String,
    val ownerId: Int,
    val createdAt: String,
    val updatedAt: String,
    val memberCount: Int,
    val memoryCount: Int
) {
    val archiveTypeDisplay: String
        get() = when (archiveType) {
            "family" -> "家族记忆"
            "lover" -> "恋人记忆"
            "friend" -> "挚友记忆"
            "relative" -> "至亲记忆"
            "celebrity" -> "伟人记忆"
            "nation" -> "国家历史"
            else -> archiveType
        }

    val archiveTypeIcon: String
        get() = when (archiveType) {
            "family" -> "family"
            "lover" -> "lover"
            "friend" -> "friend"
            "relative" -> "relative"
            "celebrity" -> "celebrity"
            "nation" -> "nation"
            else -> "folder"
        }
}

/**
 * 成员领域模型
 */
data class Member(
    val id: Int,
    val archiveId: Int,
    val name: String,
    val relationshipType: String,
    val birthYear: Int?,
    val endYear: Int?,
    val bio: String?,
    val status: String,
    val voiceProfileId: String?,
    val emotionTags: List<String>,
    val memoryCount: Int,
    val createdAt: String,
    val avatarUrl: String? = null
) {
    val isAlive: Boolean get() = status == "active"
    val deathYear: Int? get() = if (status == "passed") endYear else null

    val lifespan: String
        get() = when {
            birthYear != null && endYear != null -> "$birthYear - $endYear"
            birthYear != null && !isAlive -> "出生于 $birthYear"
            birthYear != null -> "出生于 $birthYear"
            else -> ""
        }

    val statusDisplay: String
        get() = when (status) {
            "active" -> "在世"
            "passed" -> "已故"
            "distant" -> "疏远"
            "pet" -> "宠物"
            else -> status
        }
}

/**
 * 记忆领域模型
 */
data class Memory(
    val id: Int,
    val memberId: Int,
    val archiveId: Int,
    val title: String,
    val contentText: String,
    val emotionLabel: String?,
    val timestamp: String?,
    val location: String?,
    val isCapsule: Boolean,
    val unlockDate: String?,
    val createdAt: String,
    val updatedAt: String
) {
    val emotionIcon: String
        get() = when (emotionLabel) {
            "joy" -> "joy"
            "love" -> "love"
            "anger" -> "anger"
            "sadness" -> "sadness"
            "fear" -> "fear"
            "surprise" -> "surprise"
            "nostalgia" -> "nostalgia"
            "gratitude" -> "gratitude"
            "regret" -> "regret"
            "peaceful" -> "peaceful"
            else -> "neutral"
        }

    val emotionDisplay: String
        get() = when (emotionLabel) {
            "joy" -> "喜悦"
            "love" -> "爱"
            "anger" -> "愤怒"
            "sadness" -> "悲伤"
            "fear" -> "恐惧"
            "surprise" -> "惊讶"
            "nostalgia" -> "怀念"
            "gratitude" -> "感激"
            "regret" -> "遗憾"
            "peaceful" -> "平静"
            else -> "无"
        }
}

/**
 * AI 对话消息
 */
data class DialogueMessage(
    val role: String,
    val content: String
) {
    val isUser: Boolean get() = role == "user"
    val isAssistant: Boolean get() = role == "assistant"
}

data class DialogueResult(
    val reply: String,
    val memberName: String?,
    val sessionId: String?,
    val mnemoMode: Boolean = false,
    val memoriesCreated: Int = 0
)

data class Storybook(
    val story: String,
    val archiveId: Int,
    val memberId: Int?,
    val style: String,
    val memoryCount: Int
)

data class Capsule(
    val id: Int,
    val memberId: Int?,
    val title: String,
    val content: String?,
    val unlockDate: String,
    val status: String,
    val createdAt: String,
    val message: String?
) {
    val isLocked: Boolean get() = status == "locked"
    val isUnlocked: Boolean get() = status == "unlocked"

    val statusDisplay: String
        get() = when (status) {
            "locked" -> "锁定中"
            "unlocked" -> "已解锁"
            else -> status
        }
}

data class MediaAsset(
    val id: Int,
    val objectKey: String,
    val bucket: String,
    val contentType: String,
    val size: Int,
    val purpose: String,
    val archiveId: Int?,
    val memberId: Int?,
    val createdAt: String,
    val downloadUrl: String? = null
) {
    val isImage: Boolean get() = contentType.startsWith("image/")
    val isVideo: Boolean get() = contentType.startsWith("video/")
    val isAudio: Boolean get() = contentType.startsWith("audio/")

    val purposeDisplay: String
        get() = when (purpose) {
            "avatar" -> "头像"
            "voice_sample" -> "声纹"
            "archive_photo" -> "照片"
            "archive_video" -> "视频"
            "archive_audio" -> "音频"
            "archive_sticker" -> "表情包"
            "sticker" -> "表情包"
            else -> "其他"
        }

    val sizeDisplay: String
        get() = when {
            size >= 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.1f KB", size / 1024.0)
            else -> "$size B"
        }
}

data class UserPreferences(
    val id: Int = 0,
    val userId: Int = 0,
    val theme: String = "light",
    val primaryColor: String = "jade",
    val cardStyle: String = "glass",
    val fontSize: String = "medium",
    val dashboardLayout: String = "grid",
    val customCss: String? = null,
    val appBackgroundUrl: String? = null,
    val aiMemorySync: String = "on",
    // LLM 模型配置
    val llmMode: String? = null,
    val llmBaseUrl: String? = null,
    val llmApiKey: String? = null,
    val llmModel: String? = null
) {
    val themeDisplay: String
        get() = when (theme) {
            "light" -> "浅色"
            "dark" -> "深色"
            "auto" -> "跟随系统"
            else -> theme
        }

    val primaryColorDisplay: String
        get() = when (primaryColor) {
            "jade" -> "翡翠绿"
            "amber" -> "琥珀色"
            "rose" -> "玫瑰红"
            "sky" -> "天空蓝"
            "violet" -> "紫罗兰"
            "forest" -> "森林绿"
            else -> primaryColor
        }

    val cardStyleDisplay: String
        get() = when (cardStyle) {
            "glass" -> "毛玻璃"
            "minimal" -> "简约"
            "elevated" -> "悬浮"
            else -> cardStyle
        }

    val fontSizeDisplay: String
        get() = when (fontSize) {
            "small" -> "小"
            "medium" -> "中"
            "large" -> "大"
            else -> fontSize
        }

    val aiMemorySyncDisplay: String
        get() = when (aiMemorySync) {
            "on" -> "开启"
            "off" -> "关闭"
            else -> aiMemorySync
        }
}

data class UsageStats(
    val monthlyUsed: Long,
    val monthlyLimit: Long,
    val usageByType: Map<String, Long> = emptyMap(),
    val remaining: Long,
    val usagePercent: Float
)

data class Quota(
    val subscriptionTier: String,
    val monthlyLimit: Long,
    val monthlyUsed: Long,
    val remaining: Long,
    val usagePercent: Float,
    val resetAt: String?
) {
    val tierDisplay: String
        get() = when (subscriptionTier) {
            "free" -> "免费版"
            "pro" -> "专业版"
            "enterprise" -> "企业版"
            else -> subscriptionTier
        }

    val tierIcon: String
        get() = when (subscriptionTier) {
            "free" -> "free"
            "pro" -> "pro"
            "enterprise" -> "enterprise"
            else -> "free"
        }
}

data class VoiceProfile(
    val id: Int,
    val name: String,
    val description: String?,
    val voiceId: String?,
    val engine: String?,
    val memberId: Int?,
    val sampleCount: Int,
    val createdAt: String
)

data class TimelineEntry(
    val title: String,
    val year: Int?,
    val description: String,
    val memoryIds: List<Int>
)

// ========== 记忆关系图（Mnemo Graph）模型 ==========

/**
 * 记忆关系图节点
 * 用于力导向图可视化
 */
data class GraphNode(
    val id: String,
    val label: String,
    val nodeType: String,
    val memoryId: Int? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    val fx: Float? = null,
    val fy: Float? = null,
    val vx: Float = 0f,
    val vy: Float = 0f
) {
    companion object {
        // 节点类型常量
        const val TYPE_PERSON = "Person"
        const val TYPE_EVENT = "Event"
        const val TYPE_EMOTION = "Emotion"
        const val TYPE_SEMANTIC_FACT = "SemanticFact"
    }
}

/**
 * 记忆关系图边
 * 用于力导向图可视化
 */
data class GraphEdge(
    val id: String,
    @field:SerializedName("from_node_id") val fromId: String,
    @field:SerializedName("to_node_id") val toId: String,
    val edgeType: String,
    val weight: Float = 0.5f
) {
    companion object {
        // 边类型常量
        const val EDGE_TEMPORAL_NEXT = "TEMPORAL_NEXT"
        const val EDGE_CAUSED_BY = "CAUSED_BY"
        const val EDGE_RELATED_TO = "RELATED_TO"
        const val EDGE_EMOTIONALLY_LINKED = "EMOTIONALLY_LINKED"
        const val EDGE_SUPPORTS = "SUPPORTS"
        const val EDGE_COACTIVATED_WITH = "COACTIVATED_WITH"
    }
}
