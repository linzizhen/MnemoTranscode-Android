package com.mtc.app.data.repository

import com.mtc.app.data.local.*
import com.mtc.app.data.model.*
import com.mtc.app.data.remote.MtcApiService
import com.mtc.app.domain.model.*
import com.mtc.app.domain.repository.MemoryRepository
import com.mtc.app.ui.screens.dialogue.ExtractedMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 记忆 Repository 实现
 */
@Singleton
class MemoryRepositoryImpl @Inject constructor(
    private val apiService: MtcApiService,
    private val memoryDao: MemoryDao
) : MemoryRepository {

    override suspend fun getMemories(archiveId: Int?, memberId: Int?, emotionLabel: String?, skip: Int, limit: Int): Result<List<Memory>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMemories(archiveId, memberId, emotionLabel, skip, limit)
            if (response.isSuccessful) {
                val memories = response.body()!!.map { it.toDomain() }
                if (skip == 0) {
                    memoryDao.insertMemories(response.body()!!.map { it.toEntity() })
                }
                Result.success(memories)
            } else {
                val cached = if (memberId != null) {
                    memoryDao.getMemoriesByMember(memberId).map { it.toDomain() }
                } else if (archiveId != null) {
                    memoryDao.getMemoriesByArchive(archiveId).map { it.toDomain() }
                } else {
                    emptyList()
                }
                if (cached.isNotEmpty()) {
                    Result.success(cached)
                } else {
                    Result.failure(Exception("获取记忆列表失败"))
                }
            }
        } catch (e: Exception) {
            val cached = if (memberId != null) {
                memoryDao.getMemoriesByMember(memberId).map { it.toDomain() }
            } else if (archiveId != null) {
                memoryDao.getMemoriesByArchive(archiveId).map { it.toDomain() }
            } else {
                emptyList()
            }
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getMemory(memoryId: Int): Result<Memory> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMemory(memoryId)
            if (response.isSuccessful) {
                val memory = response.body()!!.toDomain()
                memoryDao.insertMemory(response.body()!!.toEntity())
                Result.success(memory)
            } else {
                val cached = memoryDao.getMemoryById(memoryId)?.toDomain()
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure(Exception("获取记忆详情失败"))
                }
            }
        } catch (e: Exception) {
            val cached = memoryDao.getMemoryById(memoryId)?.toDomain()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun createMemory(archiveId: Int?, memberId: Int, title: String, contentText: String, timestamp: String?, location: String?, emotionLabel: String?): Result<Memory> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createMemory(
                CreateMemoryRequest(
                    memberId = memberId,
                    title = title,
                    contentText = contentText,
                    timestamp = timestamp,
                    location = location,
                    emotionLabel = emotionLabel
                )
            )
            if (response.isSuccessful) {
                val memory = response.body()!!.toDomain()
                memoryDao.insertMemory(response.body()!!.toEntity())
                Result.success(memory)
            } else {
                Result.failure(Exception("创建记忆失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMemory(memoryId: Int, title: String?, contentText: String?, emotionLabel: String?): Result<Memory> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateMemory(memoryId, UpdateMemoryRequest(title, contentText, emotionLabel))
            if (response.isSuccessful) {
                val memory = response.body()!!.toDomain()
                memoryDao.insertMemory(response.body()!!.toEntity())
                Result.success(memory)
            } else {
                Result.failure(Exception("更新记忆失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMemory(memoryId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.deleteMemory(memoryId)
            memoryDao.getMemoryById(memoryId)?.let { memoryDao.deleteMemory(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchMemories(query: String, archiveId: Int?, memberId: Int?, limit: Int): Result<List<Memory>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchMemories(MemorySearchRequest(query, archiveId, memberId, null, limit))
            if (response.isSuccessful) {
                Result.success(response.body()!!.results.map { it.toDomain() })
            } else {
                Result.failure(Exception("搜索记忆失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun extractMemoriesFromConversation(
        archiveId: Int?,
        memberId: Int?,
        messages: List<ChatMessage>
    ): Result<List<ExtractedMemory>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.extractMemoriesFromConversation(
                ExtractMemoriesRequest(
                    archiveId = archiveId,
                    memberId = memberId,
                    messages = messages
                )
            )
            if (response.isSuccessful) {
                val body = response.body()!!
                val memories = body.memories.map { memory ->
                    ExtractedMemory(
                        title = memory.title,
                        content = memory.contentText,
                        emotionLabel = memory.emotionLabel,
                        timestamp = memory.timestamp,
                        location = memory.location
                    )
                }
                Result.success(memories)
            } else {
                Result.failure(Exception("提炼记忆失败"))
            }
        } catch (e: Exception) {
            // 如果 API 调用失败，尝试使用本地简单提取
            Result.success(extractMemoriesLocally(messages))
        }
    }

    /**
     * 本地简单记忆提取（当后端 API 不可用时使用）
     */
    private fun extractMemoriesLocally(messages: List<ChatMessage>): List<ExtractedMemory> {
        val memories = mutableListOf<ExtractedMemory>()
        
        for (msg in messages) {
            if (msg.role == "assistant" && msg.content.length > 20) {
                val content = msg.content
                memories.add(
                    ExtractedMemory(
                        title = content.take(30) + if (content.length > 30) "..." else "",
                        content = content,
                        emotionLabel = detectEmotion(content),
                        timestamp = msg.timestamp,
                        location = null
                    )
                )
            }
        }
        return memories.take(5)
    }

    /**
     * 简单情感检测
     */
    private fun detectEmotion(text: String): String? {
        val emotions = mapOf(
            "joy" to listOf("开心", "高兴", "快乐", "幸福", "美好", "愉快", "欢乐"),
            "love" to listOf("爱", "喜欢", "关心", "温暖", "亲情", "思念", "想念"),
            "sadness" to listOf("悲伤", "难过", "伤心", "痛苦", "失落", "遗憾"),
            "nostalgia" to listOf("怀念", "回忆", "过去", "小时候", "曾经", "往事"),
            "gratitude" to listOf("感谢", "谢谢", "感激", "感恩"),
            "peaceful" to listOf("平静", "安宁", "和谐", "温馨", "舒适")
        )
        
        for ((emotion, keywords) in emotions) {
            for (keyword in keywords) {
                if (text.contains(keyword)) {
                    return emotion
                }
            }
        }
        return null
    }

    override suspend fun getMnemoGraph(archiveId: Int?, memberId: Int): Result<Pair<List<GraphNode>, List<GraphEdge>>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMnemoGraph(memberId)
            if (response.isSuccessful) {
                val graphData = response.body()!!
                val nodes = graphData.nodes.map { node ->
                    GraphNode(
                        id = node.id,
                        label = node.label,
                        nodeType = node.nodeType.lowercase().replaceFirstChar { it.uppercase() },
                        memoryId = node.memoryId
                    )
                }
                val edges = graphData.edges.mapIndexed { index, edge ->
                    GraphEdge(
                        id = "edge_$index",
                        fromId = edge.fromNodeId,
                        toId = edge.toNodeId,
                        edgeType = edge.edgeType,
                        weight = edge.weight
                    )
                }
                Result.success(Pair(nodes, edges))
            } else {
                Result.failure(Exception("获取关系图失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// DTO -> Domain Model
private fun MemoryResponse.toDomain() = Memory(
    id = id,
    memberId = memberId,
    archiveId = archiveId,
    title = title,
    contentText = contentText,
    emotionLabel = emotionLabel,
    timestamp = timestamp,
    location = location,
    isCapsule = isCapsule,
    unlockDate = unlockDate,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun MemoryResponse.toEntity() = MemoryEntity(
    id = id,
    memberId = memberId,
    archiveId = archiveId,
    title = title,
    contentText = contentText,
    emotionLabel = emotionLabel,
    timestamp = timestamp,
    location = location,
    isCapsule = isCapsule,
    unlockDate = unlockDate,
    mediaRefs = mediaRefs.joinToString(","),
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun MemoryEntity.toDomain() = Memory(
    id = id,
    memberId = memberId,
    archiveId = archiveId,
    title = title,
    contentText = contentText,
    emotionLabel = emotionLabel,
    timestamp = timestamp,
    location = location,
    isCapsule = isCapsule,
    unlockDate = unlockDate,
    createdAt = createdAt,
    updatedAt = updatedAt
)
