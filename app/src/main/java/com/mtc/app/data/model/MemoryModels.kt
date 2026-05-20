package com.mtc.app.data.model

import com.google.gson.annotations.SerializedName

// ========== 记忆关系图（Mnemo Graph）相关模型 ==========

/**
 * 记忆关系图节点类型（直接用 String 避免枚举反序列化问题）
 */
typealias MnemoNodeType = String

/**
 * 记忆关系图边类型
 */
typealias MnemoEdgeType = String

/**
 * 记忆关系图节点
 */
data class MnemoNode(
    val id: String,
    @SerializedName("node_type") val nodeType: MnemoNodeType,
    val label: String,
    @SerializedName("memory_id") val memoryId: Int?
)

/**
 * 记忆关系图边
 */
data class MnemoEdge(
    @SerializedName("from_id") val fromNodeId: String,
    @SerializedName("to_id") val toNodeId: String,
    @SerializedName("edge_type") val edgeType: MnemoEdgeType,
    val weight: Float
)

/**
 * 记忆关系图响应
 */
data class MnemoGraphResponse(
    @SerializedName("member_id") val memberId: Int,
    val nodes: List<MnemoNode>,
    val edges: List<MnemoEdge>
)

/**
 * 记忆关系图请求
 */
data class MnemoGraphRequest(
    @SerializedName("archive_id") val archiveId: Int? = null,
    @SerializedName("member_id") val memberId: Int? = null,
    @SerializedName("exclude_orphaned") val excludeOrphaned: Boolean = true,
    val limit: Int = 100
)

// ========== 批量删除相关模型 ==========

/**
 * 批量删除请求
 */
data class BatchDeleteRequest(
    val ids: List<Int>
)

/**
 * 批量删除响应
 */
data class BatchDeleteResponse(
    val deleted: Int,
    val failed: List<Int> = emptyList()
)

// ========== 聊天导入相关模型 ==========

/**
 * 聊天导入请求（同步模式）
 * 对应后端 ChatImportRequest: member_id + raw_text + source + build_graph
 */
data class ChatImportRequest(
    @SerializedName("member_id") val memberId: Int,
    @SerializedName("raw_text") val rawText: String,
    val source: String = "auto",
    @SerializedName("build_graph") val buildGraph: Boolean = true
)

/**
 * 单条聊天消息
 */
data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: String? = null
)

/**
 * 聊天导入响应
 * 对应后端 ChatImportResponse
 */
data class ChatImportResponse(
    @SerializedName("created_count") val createdCount: Int,
    @SerializedName("memory_ids") val memoryIds: List<Int>,
    @SerializedName("graph_temporal_edges") val graphTemporalEdges: Int,
    @SerializedName("graph_llm_edges") val graphLlmEdges: Int,
    @SerializedName("vectors_deferred") val vectorsDeferred: Boolean = false
)

// ========== 从对话中提取记忆 ==========

/**
 * 从对话中提取记忆的请求
 */
data class ExtractMemoriesRequest(
    @SerializedName("archive_id") val archiveId: Int?,
    @SerializedName("member_id") val memberId: Int?,
    val messages: List<ChatMessage>,
    @SerializedName("client_llm") val clientLlm: ClientLlmConfig? = null
)

/**
 * 从对话中提取记忆的响应
 */
data class ExtractMemoriesResponse(
    val memories: List<MemoryResponse>,
    val count: Int
)

// ========== 聊天导入（全功能）相关模型 ==========

/**
 * 解析模式
 */
enum class ParseMode {
    @SerializedName("auto") AUTO,      // 自动识别（推荐）
    @SerializedName("wechat") WECHAT,  // 微信风格（日期/昵称行）
    @SerializedName("plain") PLAIN     // 纯文本：按空行分段
}

/**
 * 聊天导入配置（弹窗选项）
 */
data class ChatImportConfig(
    val parseMode: ParseMode = ParseMode.AUTO,
    val useAiRefine: Boolean = true,    // 使用AI精炼记忆正文
    val buildRelationNetwork: Boolean = false // 导入后构建记忆关系网
)
