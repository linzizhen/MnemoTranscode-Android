package com.mtc.app.domain.repository

import com.mtc.app.data.model.ClientLlmConfig
import com.mtc.app.domain.model.DialogueMessage
import com.mtc.app.domain.model.DialogueResult

/**
 * AI 对话 Repository 接口
 */
interface DialogueRepository {
    suspend fun chat(
        message: String,
        archiveId: Int? = null,
        memberId: Int? = null,
        sessionId: String? = null,
        clientLlm: ClientLlmConfig? = null
    ): Result<DialogueResult>
    suspend fun getHistory(sessionId: String, archiveId: Int? = null, memberId: Int? = null, limit: Int = 20): Result<List<DialogueMessage>>
    suspend fun clearHistory(sessionId: String): Result<Unit>
    suspend fun clearHistoryByMember(archiveId: Int?, memberId: Int?): Result<Unit>
    suspend fun bootstrapMessages(archiveId: Int, memberId: Int, messages: List<DialogueMessage>): Result<Unit>
}
