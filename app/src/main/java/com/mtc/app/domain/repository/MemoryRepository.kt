package com.mtc.app.domain.repository

import com.mtc.app.data.model.ChatMessage
import com.mtc.app.domain.model.GraphEdge
import com.mtc.app.domain.model.GraphNode
import com.mtc.app.domain.model.Memory

/**
 * 记忆 Repository 接口
 */
interface MemoryRepository {
    suspend fun getMemories(archiveId: Int? = null, memberId: Int? = null, emotionLabel: String? = null, skip: Int = 0, limit: Int = 20): Result<List<Memory>>
    suspend fun getMemory(memoryId: Int): Result<Memory>
    suspend fun createMemory(archiveId: Int?, memberId: Int, title: String, contentText: String, timestamp: String? = null, location: String? = null, emotionLabel: String? = null): Result<Memory>
    suspend fun updateMemory(memoryId: Int, title: String?, contentText: String?, emotionLabel: String?): Result<Memory>
    suspend fun deleteMemory(memoryId: Int): Result<Unit>
    suspend fun searchMemories(query: String, archiveId: Int? = null, memberId: Int? = null, limit: Int = 10): Result<List<Memory>>
    
    /**
     * 从对话中提炼记忆
     * @param archiveId 档案 ID
     * @param memberId 成员 ID
     * @param messages 对话消息列表
     * @return 提炼出的记忆列表
     */
    suspend fun extractMemoriesFromConversation(archiveId: Int?, memberId: Int?, messages: List<ChatMessage>): Result<List<com.mtc.app.ui.screens.dialogue.ExtractedMemory>>
    
    /**
     * 获取成员的记忆关系图数据
     * @param archiveId 档案 ID
     * @param memberId 成员 ID
     * @return 图节点和边的列表
     */
    suspend fun getMnemoGraph(archiveId: Int?, memberId: Int): Result<Pair<List<GraphNode>, List<GraphEdge>>>
}
