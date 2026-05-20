package com.mtc.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * AI 对话相关数据模型
 */
data class DialogueRequest(
    val message: String,
    @SerializedName("archive_id") val archiveId: Int? = null,
    @SerializedName("member_id") val memberId: Int? = null,
    @SerializedName("channel") val channel: String = "app",
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("history_limit") val historyLimit: Int = 10,
    @SerializedName("client_llm") val clientLlm: ClientLlmConfig? = null,
    @SerializedName("extract_memories_after") val extractMemoriesAfter: Boolean = true
)

/**
 * 客户端 LLM 配置（用于使用用户自己的 API Key）
 */
data class ClientLlmConfig(
    @SerializedName("base_url") val baseUrl: String,
    @SerializedName("api_key") val apiKey: String,
    val model: String,
    val mode: String = "openai"
)

data class DialogueResponse(
    val reply: String,
    val channel: String,
    @SerializedName("member_id") val memberId: Int?,
    @SerializedName("member_name") val memberName: String?,
    @SerializedName("tts_audio_url") val ttsAudioUrl: String?,
    @SerializedName("session_id") val sessionId: String?,
    @SerializedName("mnemo_mode") val mnemoMode: Boolean = false,
    @SerializedName("memories_created") val memoriesCreated: Int? = 0
)

data class DialogueHistoryRequest(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("archive_id") val archiveId: Int? = null,
    @SerializedName("member_id") val memberId: Int? = null,
    val limit: Int = 20
)

data class DialogueHistoryResponse(
    @SerializedName("session_id") val sessionId: String,
    val messages: List<DialogueMessageDto>
)

data class DialogueMessageDto(
    val role: String,
    val content: String,
    val timestamp: String? = null
)

/**
 * 删除历史记录的请求
 */
data class DeleteHistoryRequest(
    @SerializedName("archive_id") val archiveId: Int? = null,
    @SerializedName("member_id") val memberId: Int? = null
)

/**
 * 获取对话消息列表响应
 */
data class DialogueMessagesResponse(
    val messages: List<DialogueMessageDto>,
    val total: Int
)

/**
 * 引导消息请求（用于本地存储迁移到服务端）
 */
data class BootstrapMessagesRequest(
    @SerializedName("archive_id") val archiveId: Int,
    @SerializedName("member_id") val memberId: Int,
    val messages: List<DialogueMessageDto>
)
