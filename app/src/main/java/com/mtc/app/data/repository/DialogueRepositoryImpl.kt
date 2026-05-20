package com.mtc.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mtc.app.data.llm.LocalLlmClient
import com.mtc.app.data.model.ClientLlmConfig
import com.mtc.app.data.remote.MtcApiService
import com.mtc.app.data.remote.PreferencesManager
import com.mtc.app.data.remote.LlmConfigLocal
import com.mtc.app.domain.model.DialogueMessage as DomainDialogueMessage
import com.mtc.app.domain.model.DialogueResult
import com.mtc.app.domain.repository.DialogueRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DialogueRepositoryImpl @Inject constructor(
    private val apiService: MtcApiService,
    @ApplicationContext private val context: Context
) : DialogueRepository {

    private val llmClient = LocalLlmClient()
    private val gson = Gson()

    private fun parseErrorMessage(responseCode: Int, errorBody: String?): String {
        val body = errorBody
        if (body.isNullOrBlank()) {
            return when (responseCode) {
                401 -> "认证失败，请检查 API Key 是否正确"
                403 -> "无权限访问，请检查模型配置"
                404 -> "模型不存在，请检查模型名称"
                429 -> "请求过于频繁，请稍后重试"
                500 -> "服务器内部错误，请稍后重试"
                502, 503, 504 -> "服务暂时不可用，请稍后重试"
                else -> "请求失败 ($responseCode)"
            }
        }
        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            (json.get("message")?.asString ?: body).take(500)
        } catch (e: Exception) {
            body.take(500)
        }
    }

    /**
     * 直接调用 LLM API，不再依赖 Python 后端
     */
    override suspend fun chat(
        message: String,
        archiveId: Int?,
        memberId: Int?,
        sessionId: String?,
        clientLlm: ClientLlmConfig?
    ): Result<DialogueResult> = withContext(Dispatchers.IO) {
        try {
            val llmConfig = resolveLlmConfig(clientLlm)

            if (llmConfig.baseUrl.isBlank() || llmConfig.model.isBlank()) {
                return@withContext Result.failure(
                    Exception("请先在「模型设置」中配置 LLM API")
                )
            }

            val historyMessages = loadLocalHistory(sessionId)
            val chatMessages = historyMessages.map {
                LocalLlmClient.ChatMessage(role = it.role, content = it.content)
            } + LocalLlmClient.ChatMessage(role = "user", content = message)

            val memberName = loadMemberName(archiveId, memberId)
            val systemPrompt = buildSystemPrompt(memberName)

            val llmConfigData = LocalLlmClient.LlmConfig(
                baseUrl = llmConfig.baseUrl,
                apiKey = llmConfig.apiKey,
                model = llmConfig.model,
                mode = llmConfig.mode
            )

            val llmResult = llmClient.chat(
                config = llmConfigData,
                messages = chatMessages,
                systemPrompt = systemPrompt
            )

            llmResult.fold(
                onSuccess = { response ->
                    Result.success(DialogueResult(
                        reply = response.reply,
                        memberName = memberName,
                        sessionId = sessionId ?: "",
                        mnemoMode = false,
                        memoriesCreated = 0
                    ))
                },
                onFailure = { error ->
                    Result.failure(Exception(error.message ?: "LLM 调用失败"))
                }
            )
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Unable to resolve host") == true ->
                    "网络连接失败，请检查网络设置"
                e.message?.contains("timeout") == true ->
                    "请求超时，请检查网络或服务器状态"
                e.message?.contains("connection") == true ->
                    "无法连接到 LLM 服务器，请检查 Base URL"
                else -> e.message ?: "发送消息失败"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    private suspend fun resolveLlmConfig(clientLlm: ClientLlmConfig?): LlmConfigLocal {
        return if (clientLlm != null && clientLlm.baseUrl.isNotBlank() && clientLlm.model.isNotBlank()) {
            LlmConfigLocal(
                mode = clientLlm.mode,
                baseUrl = clientLlm.baseUrl,
                apiKey = clientLlm.apiKey,
                model = clientLlm.model
            )
        } else {
            PreferencesManager.getLlmConfigSync(context)
        }
    }

    private suspend fun loadLocalHistory(sessionId: String?): List<DomainDialogueMessage> {
        if (sessionId == null) return emptyList()
        return try {
            val response = apiService.getDialogueHistory(
                com.mtc.app.data.model.DialogueHistoryRequest(
                    sessionId = sessionId,
                    archiveId = null,
                    memberId = null,
                    limit = 10
                )
            )
            if (response.isSuccessful) {
                response.body()?.messages?.map {
                    DomainDialogueMessage(it.role, it.content)
                } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun loadMemberName(archiveId: Int?, memberId: Int?): String {
        if (archiveId == null || memberId == null) return "AI 助手"
        return try {
            val response = apiService.getMember(archiveId, memberId)
            if (response.isSuccessful) {
                response.body()?.name ?: "AI 助手"
            } else {
                "AI 助手"
            }
        } catch (e: Exception) {
            "AI 助手"
        }
    }

    private fun buildSystemPrompt(memberName: String): String {
        return """你是一个有着温暖灵魂的数字人物，名叫 $memberName。

## 对话规则
- 用 $memberName 的口吻和性格来回应
- 回忆中的往事用第一人称叙述，仿佛你真的经历过
- 保持对话的情感温度，不要过于机械
- 回复应该简洁自然，符合日常对话节奏
- 如果有人问到你记忆中不存在的事情，诚实但温和地回应

请以 $memberName 的身份回复：
"""
    }

    // ========== 以下方法仍使用后端（本地数据管理） ==========

    override suspend fun getHistory(
        sessionId: String,
        archiveId: Int?,
        memberId: Int?,
        limit: Int
    ): Result<List<DomainDialogueMessage>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDialogueHistory(
                com.mtc.app.data.model.DialogueHistoryRequest(sessionId, archiveId, memberId, limit)
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!.messages.map {
                    DomainDialogueMessage(it.role, it.content)
                })
            } else {
                Result.failure(Exception("获取历史失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearHistory(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.clearDialogueHistory(sessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearHistoryByMember(archiveId: Int?, memberId: Int?): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                apiService.clearDialogueHistoryByMember(
                    com.mtc.app.data.model.DeleteHistoryRequest(archiveId, memberId)
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun bootstrapMessages(
        archiveId: Int,
        memberId: Int,
        messages: List<DomainDialogueMessage>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = com.mtc.app.data.model.BootstrapMessagesRequest(
                archiveId = archiveId,
                memberId = memberId,
                messages = messages.map {
                    com.mtc.app.data.model.DialogueMessageDto(it.role, it.content)
                }
            )
            val response = apiService.bootstrapMessages(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("引导消息失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
