package com.mtc.app.ui.screens.dialogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.data.model.ChatMessage
import com.mtc.app.data.model.ClientLlmConfig
import com.mtc.app.data.model.LlmPreset
import com.mtc.app.domain.model.DialogueMessage
import com.mtc.app.domain.model.Member
import com.mtc.app.domain.repository.ArchiveRepository
import com.mtc.app.domain.repository.DialogueRepository
import com.mtc.app.domain.repository.MemoryRepository
import com.mtc.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DialogueUiState(
    val isLoading: Boolean = false,
    val member: Member? = null,
    val messages: List<DialogueMessage> = emptyList(),
    val currentMessage: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
    val sessionId: String = UUID.randomUUID().toString(),
    val isPlaying: Boolean = false,
    val playingMessageIndex: Int? = null,
    val mnemoMode: Boolean = false,
    val clientLlm: ClientLlmConfig? = null,
    // 增强功能
    val currentModelName: String? = null,        // 当前模型名称
    val currentModelProvider: String? = null,   // 当前模型提供商
    val isExtractingMemory: Boolean = false,    // 是否正在提炼记忆
    val extractedMemories: List<ExtractedMemory> = emptyList(),  // 提炼出的记忆
    val isSavingAsMemory: Boolean = false,       // 是否正在保存为记忆
    val saveSuccess: Boolean = false,            // 保存成功标志
    val showMemoryPreview: Boolean = false        // 显示记忆预览
)

/**
 * 提炼出的记忆数据
 */
data class ExtractedMemory(
    val title: String,
    val content: String,
    val emotionLabel: String?,
    val timestamp: String?,
    val location: String?
)

@HiltViewModel
class DialogueViewModel @Inject constructor(
    private val dialogueRepository: DialogueRepository,
    private val archiveRepository: ArchiveRepository,
    private val memoryRepository: MemoryRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DialogueUiState())
    val uiState: StateFlow<DialogueUiState> = _uiState.asStateFlow()

    private var currentArchiveId: Int? = null
    private var currentMemberId: Int? = null

    /**
     * 默认模型预设（与 ModelSettingsViewModel 保持一致）
     * 用于将中文显示名映射为 API 模型名
     */
    private val defaultPresets = listOf(
        LlmPreset(id="xiaomi-mimo", name="小米 MiMo", mode="openai",
            baseUrl="https://token-plan-cn.xiaomimimo.com/v1",
            defaultModel="mimo-v2.5-pro",
            models=listOf("mimo-v2.5-pro","mimo-v2.5"),
            description="小米 MiMo V2.5",
            apiKey="tp-cim9biqmmtdd306eoi1dcbq9zd1a1oa1u0vgq6hh760puw45"),
        LlmPreset(id="deepseek-v4", name="DeepSeek V4", mode="openai",
            baseUrl="https://api.deepseek.com/v1",
            defaultModel="deepseek-v4-flash",
            models=listOf("deepseek-v4-pro","deepseek-v4-flash"),
            description="DeepSeek 深度求索 V4 系列"),
        LlmPreset(id="aliyun-qwen", name="阿里云 Qwen", mode="openai",
            baseUrl="https://dashscope.aliyuncs.com/compatible-mode/v1",
            defaultModel="qwen3-max",
            models=listOf("qwen3-max","qwen3.6-plus","qwen3.6-flash"),
            description="阿里云百炼 Qwen3.6 系列"),
        LlmPreset(id="doubao", name="火山引擎 Doubao", mode="openai",
            baseUrl="https://api.doubao-ai.com/v1",
            defaultModel="doubao-seed-2.0-pro-260215",
            models=listOf("doubao-seed-2.0-pro-260215","doubao-seed-2.0-lite-260215"),
            description="字节跳动火山引擎 Doubao Seed 2.0"),
        LlmPreset(id="zhipu-glm", name="智谱 GLM", mode="zhipu",
            baseUrl="https://open.bigmodel.cn/api/paas/v4",
            defaultModel="glm-5.1",
            models=listOf("glm-5.1","glm-5","glm-4.5-air"),
            description="智谱 AI GLM 5.1 系列"),
        LlmPreset(id="moonshot-kimi", name="Moonshot Kimi", mode="openai",
            baseUrl="https://api.moonshot.cn/v1",
            defaultModel="kimi-k2.6",
            models=listOf("kimi-k2.6"),
            description="月之暗面 Kimi 2.6"),
        LlmPreset(id="google-gemini", name="Google Gemini", mode="google",
            baseUrl="https://generativelanguage.googleapis.com/v1beta",
            defaultModel="gemini-3.1-pro-preview",
            models=listOf("gemini-3.1-pro-preview","gemini-3-flash-preview"),
            description="Google Gemini 系列模型"),
        LlmPreset(id="anthropic-claude", name="Anthropic Claude", mode="anthropic",
            baseUrl="https://api.anthropic.com",
            defaultModel="claude-opus-4-7",
            models=listOf("claude-opus-4-7","claude-sonnet-4-6","claude-haiku-4-5-20251001"),
            description="Anthropic Claude 系列模型"),
        LlmPreset(id="openai-gpt", name="OpenAI GPT", mode="openai",
            baseUrl="https://api.openai.com/v1",
            defaultModel="gpt-5.5",
            models=listOf("gpt-5.5","gpt-5.4","gpt-5.4-mini","o3"),
            description="OpenAI 官方 API，GPT-5 系列"),
        LlmPreset(id="ollama", name="Ollama 本地", mode="ollama",
            baseUrl="http://localhost:11434",
            defaultModel="qwen2.5:7b",
            models=null,
            description="本地运行的大语言模型，无 API 费用")
    )

    init {
        // 立即加载 LLM 配置（同步加载）
        loadLlmConfig()
    }

    /**
     * 根据 Base URL 和保存的模型名，返回实际 API 模型名
     */
    private fun resolveApiModel(baseUrl: String, savedModel: String?): String {
        if (savedModel.isNullOrBlank()) return ""

        // 提取 baseUrl 的域名，用于精确匹配预设
        val urlDomain = baseUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")

        // 通过 baseUrl 找到匹配的预设，再用预设的显示名映射到 API 模型名
        val matchedPreset = defaultPresets.find {
            val presetDomain = it.baseUrl
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
            presetDomain == urlDomain
        }

        return if (matchedPreset != null && savedModel == matchedPreset.name) {
            // savedModel 是中文显示名，映射为 API 模型名
            matchedPreset.defaultModel ?: savedModel
        } else {
            // savedModel 本身可能就是 API 模型名，直接用
            savedModel
        }
    }

    /**
     * 加载 LLM 配置，获取当前使用的模型信息
     */
    private fun loadLlmConfig() {
        viewModelScope.launch {
            preferencesRepository.getPreferences()
                .onSuccess { prefs ->
                    // 区分显示名和 API 模型名
                    val effectiveBaseUrl = prefs.llmBaseUrl ?: "https://token-plan-cn.xiaomimimo.com/v1"
                    val apiModelName = resolveApiModel(effectiveBaseUrl, prefs.llmModel)
                    val displayName = prefs.llmModel ?: "小米 MiMo"
                    val providerName = when (prefs.llmMode) {
                        "google" -> "Google Gemini"
                        "anthropic" -> "Anthropic Claude"
                        "zhipu" -> "智谱 GLM"
                        "ollama" -> "Ollama 本地"
                        "xiaomi" -> "小米 MiMo"
                        else -> "OpenAI 兼容"
                    }
                    val apiKey = prefs.llmApiKey?.takeIf { it.isNotBlank() }
                        ?: getLocalApiKey(effectiveBaseUrl)
                    _uiState.value = _uiState.value.copy(
                        currentModelName = displayName,
                        currentModelProvider = providerName,
                        clientLlm = ClientLlmConfig(
                            mode = prefs.llmMode ?: "openai",
                            baseUrl = effectiveBaseUrl,
                            apiKey = apiKey,
                            model = apiModelName.ifBlank { "mimo-v2.5-pro" }
                        )
                    )
                }
                .onFailure {
                    val apiKey = getLocalApiKey("https://token-plan-cn.xiaomimimo.com/v1")
                    _uiState.value = _uiState.value.copy(
                        currentModelName = "小米 MiMo",
                        currentModelProvider = "小米 MiMo",
                        clientLlm = ClientLlmConfig(
                            mode = "openai",
                            baseUrl = "https://token-plan-cn.xiaomimimo.com/v1",
                            apiKey = apiKey,
                            model = "mimo-v2.5-pro"
                        )
                    )
                }
        }
    }

    /**
     * 从预设列表获取匹配 baseUrl 的 API Key
     */
    private fun getLocalApiKey(baseUrl: String?): String {
        if (baseUrl.isNullOrBlank()) return ""
        val urlDomain = baseUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")
        val matched = defaultPresets.find {
            val presetDomain = it.baseUrl
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
            presetDomain == urlDomain
        }
        return matched?.apiKey ?: ""
    }

    fun loadMember(archiveId: Int, memberId: Int) {
        currentArchiveId = archiveId
        currentMemberId = memberId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            archiveRepository.getMember(archiveId, memberId)
                .onSuccess { member ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        member = member
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }

            loadHistory()
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            dialogueRepository.getHistory(
                sessionId = _uiState.value.sessionId,
                archiveId = currentArchiveId,
                memberId = currentMemberId
            )
                .onSuccess { messages ->
                    _uiState.value = _uiState.value.copy(messages = messages)
                }
        }
    }

    fun updateMessage(message: String) {
        _uiState.value = _uiState.value.copy(currentMessage = message)
    }

    fun sendMessage() {
        val message = _uiState.value.currentMessage.trim()
        if (message.isBlank() || _uiState.value.isSending) return

        viewModelScope.launch {
            val userMessage = DialogueMessage(role = "user", content = message)
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + userMessage,
                currentMessage = "",
                isSending = true,
                error = null
            )

            dialogueRepository.chat(
                message = message,
                archiveId = currentArchiveId,
                memberId = currentMemberId,
                sessionId = _uiState.value.sessionId,
                clientLlm = _uiState.value.clientLlm
            )
                .onSuccess { result ->
                    val assistantMessage = DialogueMessage(role = "assistant", content = result.reply)
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + assistantMessage,
                        isSending = false,
                        mnemoMode = result.mnemoMode
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        error = e.message ?: "发送消息失败"
                    )
                }
        }
    }

    /**
     * 提炼对话中的记忆
     */
    fun extractMemories() {
        val messages = _uiState.value.messages
        if (messages.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isExtractingMemory = true,
                error = null,
                extractedMemories = emptyList()
            )

            try {
                // 转换消息格式
                val chatMessages = messages.map { msg ->
                    ChatMessage(role = msg.role, content = msg.content)
                }
                
                memoryRepository.extractMemoriesFromConversation(
                    archiveId = currentArchiveId,
                    memberId = currentMemberId,
                    messages = chatMessages
                )
                    .onSuccess { memories ->
                        _uiState.value = _uiState.value.copy(
                            isExtractingMemory = false,
                            extractedMemories = memories,
                            showMemoryPreview = memories.isNotEmpty()
                        )
                    }
                    .onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isExtractingMemory = false,
                            error = "提炼记忆失败: ${e.message}"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExtractingMemory = false,
                    error = "提炼记忆失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 将 AI 回复保存为记忆
     */
    fun saveAiReplyAsMemory(title: String, content: String, emotionLabel: String? = null) {
        val memberId = currentMemberId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingAsMemory = true, error = null)

            memoryRepository.createMemory(
                archiveId = currentArchiveId,
                memberId = memberId,
                title = title,
                contentText = content,
                emotionLabel = emotionLabel,
                timestamp = null,
                location = null
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSavingAsMemory = false,
                        saveSuccess = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSavingAsMemory = false,
                        error = "保存记忆失败: ${e.message}"
                    )
                }
        }
    }

    /**
     * 批量保存提炼出的记忆
     */
    fun saveExtractedMemories() {
        val memories = _uiState.value.extractedMemories
        val memberId = currentMemberId
        if (memories.isEmpty() || memberId == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingAsMemory = true, error = null)
            
            var successCount = 0
            var failCount = 0

            for (memory in memories) {
                memoryRepository.createMemory(
                    archiveId = currentArchiveId,
                    memberId = memberId,
                    title = memory.title,
                    contentText = memory.content,
                    emotionLabel = memory.emotionLabel,
                    timestamp = memory.timestamp,
                    location = memory.location
                )
                    .onSuccess { successCount++ }
                    .onFailure { failCount++ }
            }

            _uiState.value = _uiState.value.copy(
                isSavingAsMemory = false,
                saveSuccess = successCount > 0,
                showMemoryPreview = false,
                extractedMemories = emptyList(),
                error = if (failCount > 0) "$successCount 条记忆保存成功，$failCount 条失败" else null
            )
        }
    }

    /**
     * 关闭记忆预览
     */
    fun closeMemoryPreview() {
        _uiState.value = _uiState.value.copy(
            showMemoryPreview = false,
            extractedMemories = emptyList()
        )
    }

    /**
     * 更新客户端 LLM 配置
     */
    fun setClientLlm(clientLlm: ClientLlmConfig?) {
        _uiState.value = _uiState.value.copy(clientLlm = clientLlm)
    }

    fun clearHistory() {
        viewModelScope.launch {
            dialogueRepository.clearHistory(_uiState.value.sessionId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        messages = emptyList(),
                        sessionId = UUID.randomUUID().toString()
                    )
                }
        }
    }

    fun setPlaying(index: Int?) {
        _uiState.value = _uiState.value.copy(
            playingMessageIndex = index,
            isPlaying = index != null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}
