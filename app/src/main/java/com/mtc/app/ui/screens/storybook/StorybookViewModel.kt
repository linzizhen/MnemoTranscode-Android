package com.mtc.app.ui.screens.storybook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.data.model.ClientLlmConfig
import com.mtc.app.data.model.LlmPreset
import com.mtc.app.domain.model.Archive
import com.mtc.app.domain.model.Member
import com.mtc.app.domain.model.Storybook
import com.mtc.app.domain.repository.ArchiveRepository
import com.mtc.app.domain.repository.PreferencesRepository
import com.mtc.app.domain.repository.StorybookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorybookUiState(
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val archives: List<Archive> = emptyList(),
    val members: List<Member> = emptyList(),
    val selectedArchiveId: Int? = null,
    val selectedMemberId: Int? = null,
    val selectedStyle: String = "nostalgic",
    val story: Storybook? = null,
    val error: String? = null,
    val currentModelName: String? = null,
    val currentModelProvider: String? = null
)

@HiltViewModel
class StorybookViewModel @Inject constructor(
    private val storybookRepository: StorybookRepository,
    private val archiveRepository: ArchiveRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorybookUiState())
    val uiState: StateFlow<StorybookUiState> = _uiState.asStateFlow()

    val storyStyles = listOf(
        "nostalgic" to "怀旧温情",
        "literary" to "文学风格",
        "simple" to "简洁直白",
        "dialogue" to "对话风格"
    )

    private var clientLlm: ClientLlmConfig? = null

    /** 与 DialogueViewModel 保持一致的默认模型预设 */
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
        loadLlmConfig()
        loadArchives()
    }

    private fun resolveApiModel(baseUrl: String, savedModel: String?): String {
        if (savedModel.isNullOrBlank()) return ""
        val urlDomain = baseUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")
        val matchedPreset = defaultPresets.find {
            val presetDomain = it.baseUrl
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
            presetDomain == urlDomain
        }
        return if (matchedPreset != null && savedModel == matchedPreset.name) {
            matchedPreset.defaultModel ?: savedModel
        } else {
            savedModel
        }
    }

    private fun getPresetApiKey(baseUrl: String?): String {
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

    private fun loadLlmConfig() {
        viewModelScope.launch {
            preferencesRepository.getPreferences()
                .onSuccess { prefs ->
                    val effectiveBaseUrl = prefs.llmBaseUrl ?: "https://token-plan-cn.xiaomimimo.com/v1"
                    val apiModel = resolveApiModel(effectiveBaseUrl, prefs.llmModel)
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
                        ?: getPresetApiKey(effectiveBaseUrl)
                    clientLlm = ClientLlmConfig(
                        mode = prefs.llmMode ?: "openai",
                        baseUrl = effectiveBaseUrl,
                        apiKey = apiKey,
                        model = apiModel.ifBlank { "mimo-v2.5-pro" }
                    )
                    _uiState.value = _uiState.value.copy(
                        currentModelName = displayName,
                        currentModelProvider = providerName
                    )
                }
                .onFailure {
                    clientLlm = ClientLlmConfig(
                        mode = "openai",
                        baseUrl = "https://token-plan-cn.xiaomimimo.com/v1",
                        apiKey = "tp-cim9biqmmtdd306eoi1dcbq9zd1a1oa1u0vgq6hh760puw45",
                        model = "mimo-v2.5-pro"
                    )
                    _uiState.value = _uiState.value.copy(
                        currentModelName = "小米 MiMo",
                        currentModelProvider = "小米 MiMo"
                    )
                }
        }
    }

    private fun loadArchives() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            archiveRepository.getArchives()
                .onSuccess { archives ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        archives = archives,
                        selectedArchiveId = archives.firstOrNull()?.id
                    )
                    if (archives.isNotEmpty()) {
                        loadMembers(archives.first().id)
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    fun selectArchive(archiveId: Int) {
        _uiState.value = _uiState.value.copy(
            selectedArchiveId = archiveId,
            selectedMemberId = null,
            story = null
        )
        loadMembers(archiveId)
    }

    fun selectMember(memberId: Int?) {
        _uiState.value = _uiState.value.copy(
            selectedMemberId = memberId,
            story = null
        )
    }

    fun selectStyle(style: String) {
        _uiState.value = _uiState.value.copy(
            selectedStyle = style,
            story = null
        )
    }

    private fun loadMembers(archiveId: Int) {
        viewModelScope.launch {
            archiveRepository.getMembers(archiveId)
                .onSuccess { members ->
                    _uiState.value = _uiState.value.copy(members = members)
                }
        }
    }

    fun generateStory() {
        val state = _uiState.value
        val archiveId = state.selectedArchiveId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null)
            storybookRepository.generateStory(
                archiveId = archiveId,
                memberId = state.selectedMemberId,
                style = state.selectedStyle,
                clientLlm = clientLlm
            )
                .onSuccess { story ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        story = story
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = e.message ?: "生成失败"
                    )
                }
        }
    }

    fun clearStory() {
        _uiState.value = _uiState.value.copy(story = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
