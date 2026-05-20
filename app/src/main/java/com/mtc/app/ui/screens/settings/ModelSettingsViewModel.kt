package com.mtc.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.data.llm.LocalLlmClient
import com.mtc.app.data.model.LlmCheckResponse
import com.mtc.app.data.model.LlmPreset
import com.mtc.app.data.remote.MtcApiService
import com.mtc.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelSettingsUiState(
    val isLoading: Boolean = false,
    val presets: List<LlmPreset> = emptyList(),
    val selectedPresetId: String? = null,
    val customMode: String = "openai",
    val customBaseUrl: String = "",
    val customApiKey: String = "",
    val customModel: String = "",
    val isChecking: Boolean = false,
    val checkResult: LlmCheckResponse? = null,
    val availableModels: List<String> = emptyList(),
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class ModelSettingsViewModel @Inject constructor(
    private val apiService: MtcApiService,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val llmClient = LocalLlmClient()

    private val _uiState = MutableStateFlow(ModelSettingsUiState())
    val uiState: StateFlow<ModelSettingsUiState> = _uiState.asStateFlow()

    /**
     * 接入模式选项
     * 与后端 llm_probe.py 支持的模式一致
     */
    val modeOptions = listOf(
        "openai" to "OpenAI 兼容",
        "ollama" to "Ollama (本地)",
        "google" to "Google Gemini",
        "anthropic" to "Anthropic Claude",
        "zhipu" to "智谱 GLM"
    )

    /**
     * 默认预设配置
     * 按照 LLM.txt 规范，与前端 llmPresets.ts 顺序一致
     */
    val defaultPresets = listOf(
        // 1. Google Gemini
        LlmPreset(
            id = "google-gemini",
            name = "Google Gemini",
            mode = "google",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            defaultModel = "gemini-3.1-pro-preview",
            models = listOf(
                "gemini-3.1-pro-preview",
                "gemini-3-flash-preview",
                "gemini-3.1-flash-lite-preview"
            ),
            description = "Google Gemini 系列模型，支持多模态"
        ),
        // 2. OpenAI GPT
        LlmPreset(
            id = "openai-gpt",
            name = "OpenAI GPT",
            mode = "openai",
            baseUrl = "https://api.openai.com/v1",
            defaultModel = "gpt-5.5",
            models = listOf(
                "gpt-5.5",
                "gpt-5.4",
                "gpt-5.4-mini",
                "o3"
            ),
            description = "OpenAI 官方 API，GPT-5 系列"
        ),
        // 3. Anthropic Claude
        LlmPreset(
            id = "anthropic-claude",
            name = "Anthropic Claude",
            mode = "anthropic",
            baseUrl = "https://api.anthropic.com",
            defaultModel = "claude-opus-4-7",
            models = listOf(
                "claude-opus-4-7",
                "claude-sonnet-4-6",
                "claude-haiku-4-5-20251001"
            ),
            description = "Anthropic Claude 系列模型"
        ),
        // 4. xAI Grok
        LlmPreset(
            id = "xai-grok",
            name = "xAI Grok",
            mode = "openai",
            baseUrl = "https://api.x.ai/v1",
            defaultModel = "grok-4.20-reasoning",
            models = listOf(
                "grok-4.20-reasoning",
                "grok-4"
            ),
            description = "xAI Grok 系列模型"
        ),
        // 5. Meta Llama
        LlmPreset(
            id = "meta-llama",
            name = "Meta Llama",
            mode = "openai",
            baseUrl = "https://api.llama.com/v1",
            defaultModel = "Llama-4-Maverick-17B-128E-Instruct-FP8",
            models = listOf(
                "Llama-4-Maverick-17B-128E-Instruct-FP8",
                "Llama-4-Scout-17B-16E-Instruct-FP8"
            ),
            description = "Meta Llama 4 系列模型"
        ),
        // 6. DeepSeek V4
        LlmPreset(
            id = "deepseek-v4",
            name = "DeepSeek V4",
            mode = "openai",
            baseUrl = "https://api.deepseek.com/v1",
            defaultModel = "deepseek-v4-flash",
            models = listOf(
                "deepseek-v4-pro",
                "deepseek-v4-flash"
            ),
            description = "DeepSeek 深度求索 V4 系列"
        ),
        // 7. 阿里云 Qwen
        LlmPreset(
            id = "aliyun-qwen",
            name = "阿里云 Qwen",
            mode = "openai",
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            defaultModel = "qwen3-max",
            models = listOf(
                "qwen3-max",
                "qwen3.6-plus",
                "qwen3.6-flash"
            ),
            description = "阿里云百炼 Qwen3.6 系列"
        ),
        // 8. 字节跳动 Doubao
        LlmPreset(
            id = "doubao",
            name = "火山引擎 Doubao",
            mode = "openai",
            baseUrl = "https://api.doubao-ai.com/v1",
            defaultModel = "doubao-seed-2-0-pro-260215",
            models = listOf(
                "doubao-seed-2-0-pro-260215",
                "doubao-seed-2-0-lite-260215",
                "doubao-seed-2-0-mini-260215",
                "doubao-seed-2-0-code-preview-260215"
            ),
            description = "字节跳动火山引擎 Doubao Seed 2.0"
        ),
        // 9. 智谱 GLM
        LlmPreset(
            id = "zhipu-glm",
            name = "智谱 GLM",
            mode = "zhipu",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            defaultModel = "glm-5.1",
            models = listOf(
                "glm-5.1",
                "glm-5",
                "glm-4.5-air"
            ),
            description = "智谱 AI GLM 5.1 系列，国内合规"
        ),
        // 10. Moonshot Kimi
        LlmPreset(
            id = "moonshot-kimi",
            name = "Moonshot Kimi",
            mode = "openai",
            baseUrl = "https://api.moonshot.cn/v1",
            defaultModel = "kimi-k2.6",
            models = listOf(
                "kimi-k2.6"
            ),
            description = "月之暗面 Kimi 2.6"
        ),
        // 11. MiniMax
        LlmPreset(
            id = "minimax",
            name = "MiniMax",
            mode = "openai",
            baseUrl = "https://api.minimaxi.com/v1",
            defaultModel = "MiniMax-M2.7",
            models = listOf(
                "MiniMax-M2.7"
            ),
            description = "MiniMax M2.7 系列"
        ),
        // 12. 硅基流动
        LlmPreset(
            id = "siliconflow",
            name = "硅基流动",
            mode = "openai",
            baseUrl = "https://api.siliconflow.cn/v1",
            defaultModel = "Pro/zai-org/GLM-5",
            models = listOf(
                "Pro/zai-org/GLM-5",
                "Qwen/Qwen3.5-397B-A17B",
                "Pro/deepseek-ai/DeepSeek-V3.2"
            ),
            description = "硅基流动聚合平台"
        ),
        // 13. 小米 MiMo
        LlmPreset(
            id = "xiaomi-mimo",
            name = "小米 MiMo",
            mode = "openai",
            baseUrl = "https://token-plan-cn.xiaomimimo.com/v1",
            defaultModel = "mimo-v2.5-pro",
            models = listOf(
                "mimo-v2.5-pro",
                "mimo-v2.5"
            ),
            description = "小米 MiMo V2.5",
            apiKey = "tp-cim9biqmmtdd306eoi1dcbq9zd1a1oa1u0vgq6hh760puw45"
        ),
        // 14. Ollama 本地
        LlmPreset(
            id = "ollama",
            name = "Ollama 本地",
            mode = "ollama",
            baseUrl = "http://localhost:11434",
            defaultModel = "qwen2.5:7b",
            models = null,
            description = "本地运行的大语言模型，无 API 费用"
        )
    )

    init {
        // 加载已有的 LLM 配置
        loadSavedConfig()
    }

    /**
     * 加载已保存的 LLM 配置
     */
    private fun loadSavedConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            preferencesRepository.getPreferences()
                .onSuccess { prefs ->
                    if (prefs.llmMode != null || prefs.llmBaseUrl != null || prefs.llmModel != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            customMode = prefs.llmMode ?: "openai",
                            customBaseUrl = prefs.llmBaseUrl ?: "",
                            customApiKey = "",
                            customModel = prefs.llmModel ?: "",
                            // 尝试匹配预设
                            selectedPresetId = defaultPresets.find {
                                it.baseUrl == prefs.llmBaseUrl && it.mode == prefs.llmMode
                            }?.id
                        )
                    } else {
                        // 默认选择小米 MiMo 并保存
                        val xiaomiPreset = defaultPresets.find { it.id == "xiaomi-mimo" }
                        if (xiaomiPreset != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                selectedPresetId = "xiaomi-mimo",
                                customMode = xiaomiPreset.mode,
                                customBaseUrl = xiaomiPreset.baseUrl,
                                customApiKey = xiaomiPreset.apiKey ?: "",
                                customModel = xiaomiPreset.defaultModel ?: "",
                                availableModels = xiaomiPreset.models ?: emptyList()
                            )
                            // 自动保存到后端
                            saveLlmConfigToBackend(xiaomiPreset)
                        }
                    }
                }
                .onFailure {
                    // 默认选择小米 MiMo 并保存
                    val xiaomiPreset = defaultPresets.find { it.id == "xiaomi-mimo" }
                    if (xiaomiPreset != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            selectedPresetId = "xiaomi-mimo",
                            customMode = xiaomiPreset.mode,
                            customBaseUrl = xiaomiPreset.baseUrl,
                            customApiKey = xiaomiPreset.apiKey ?: "",
                            customModel = xiaomiPreset.defaultModel ?: "",
                            availableModels = xiaomiPreset.models ?: emptyList()
                        )
                        // 自动保存到后端
                        saveLlmConfigToBackend(xiaomiPreset)
                    }
                }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    /**
     * 保存 LLM 配置到后端
     */
    private fun saveLlmConfigToBackend(preset: LlmPreset) {
        viewModelScope.launch {
            preferencesRepository.updateLlmConfig(
                mode = preset.mode,
                baseUrl = preset.baseUrl,
                apiKey = preset.apiKey,
                model = preset.defaultModel ?: ""
            )
        }
    }

    fun selectPreset(presetId: String) {
        val preset = defaultPresets.find { it.id == presetId }
        if (preset != null) {
            _uiState.value = _uiState.value.copy(
                selectedPresetId = presetId,
                customMode = preset.mode,
                customBaseUrl = preset.baseUrl,
                customApiKey = preset.apiKey ?: "",
                customModel = preset.defaultModel ?: "",
                availableModels = preset.models ?: emptyList(),
                checkResult = null
            )
        }
    }

    fun updateCustomBaseUrl(url: String) {
        _uiState.value = _uiState.value.copy(
            selectedPresetId = null,
            customBaseUrl = url,
            checkResult = null,
            availableModels = emptyList()
        )
    }

    fun updateCustomApiKey(apiKey: String) {
        _uiState.value = _uiState.value.copy(customApiKey = apiKey)
    }

    fun updateCustomModel(model: String) {
        _uiState.value = _uiState.value.copy(customModel = model)
    }

    fun updateCustomMode(mode: String) {
        _uiState.value = _uiState.value.copy(
            selectedPresetId = null,
            customMode = mode,
            checkResult = null,
            availableModels = emptyList()
        )
    }

    fun checkConnection() {
        val state = _uiState.value
        if (state.customBaseUrl.isBlank()) {
            _uiState.value = state.copy(error = "请输入 Base URL")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true, error = null, checkResult = null)
            try {
                val config = LocalLlmClient.LlmConfig(
                    baseUrl = state.customBaseUrl,
                    apiKey = state.customApiKey.ifBlank { "" },
                    model = state.customModel.ifBlank { "gpt-4o-mini" },
                    mode = state.customMode
                )
                val result = llmClient.checkConnection(config)
                result.fold(
                    onSuccess = { (ok, models) ->
                        _uiState.value = _uiState.value.copy(
                            isChecking = false,
                            checkResult = LlmCheckResponse(
                                ok = ok,
                                latencyMs = null,
                                error = if (!ok) "连接失败" else null,
                                models = models
                            ),
                            availableModels = models
                        )
                        if (!ok) {
                            _uiState.value = _uiState.value.copy(error = "连接失败")
                        }
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isChecking = false,
                            error = "连接失败: ${e.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    error = "连接失败: ${e.message}"
                )
            }
        }
    }

    fun saveConfiguration() {
        val state = _uiState.value
        if (state.customBaseUrl.isBlank()) {
            _uiState.value = state.copy(error = "请输入 Base URL")
            return
        }
        if (state.customModel.isBlank()) {
            _uiState.value = state.copy(error = "请选择或输入模型名称")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            preferencesRepository.updateLlmConfig(
                mode = state.customMode,
                baseUrl = state.customBaseUrl,
                apiKey = state.customApiKey.ifBlank { null },
                model = state.customModel
            ).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "保存失败: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}
