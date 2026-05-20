package com.mtc.app.data.llm

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Android 本地 LLM 客户端
 * 直接调用各 LLM API，无需 Python 后端
 */
class LocalLlmClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    data class LlmConfig(
        val baseUrl: String,
        val apiKey: String,
        val model: String,
        val mode: String = "openai"
    )

    data class ChatMessage(
        val role: String,
        val content: String
    )

    data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double = 0.7,
        val maxTokens: Int = 2048
    )

    data class ChatResponse(
        val reply: String,
        val model: String?,
        val usage: Usage?
    )

    data class Usage(
        val promptTokens: Int?,
        val completionTokens: Int?,
        val totalTokens: Int?
    )

    /**
     * 直接调用 LLM API 获取回复
     * 支持 OpenAI 兼容 / Google Gemini / Anthropic Claude / 智谱 GLM / Ollama
     */
    suspend fun chat(
        config: LlmConfig,
        messages: List<ChatMessage>,
        systemPrompt: String? = null
    ): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            val allMessages = buildList {
                if (!systemPrompt.isNullOrBlank()) {
                    add(ChatMessage("system", systemPrompt))
                }
                addAll(messages)
            }

            val requestBody: String
            val headers: Map<String, String>

            when (config.mode) {
                "google" -> {
                    requestBody = buildGeminiRequest(config, allMessages)
                    headers = emptyMap()
                }
                "anthropic" -> {
                    requestBody = buildAnthropicRequest(config, allMessages)
                    headers = mapOf(
                        "x-api-key" to config.apiKey,
                        "anthropic-version" to "2023-06-01",
                        "Content-Type" to "application/json"
                    )
                }
                else -> {
                    requestBody = buildOpenAIRequest(config, allMessages)
                    headers = if (config.apiKey.isNotBlank()) {
                        mapOf("Authorization" to "Bearer ${config.apiKey}")
                    } else {
                        emptyMap()
                    }
                }
            }

            val url = buildUrl(config)
            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json".toMediaType()))

            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val bodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorFromBody(bodyStr, response.code)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val chatResponse = parseResponse(bodyStr, config.mode)
            Result.success(chatResponse)
        } catch (e: Exception) {
            Result.failure(Exception("LLM 调用失败: ${e.message}"))
        }
    }

    /**
     * 测试连接并获取可用模型列表
     */
    suspend fun checkConnection(config: LlmConfig): Result<Pair<Boolean, List<String>>> =
        withContext(Dispatchers.IO) {
            try {
                when (config.mode) {
                    "ollama" -> checkOllama(config)
                    "google" -> checkGoogleModels(config)
                    else -> checkOpenAICompat(config)
                }
            } catch (e: Exception) {
                Result.failure(Exception("连接失败: ${e.message}"))
            }
        }

    // ========== 私有方法 ==========

    private fun buildUrl(config: LlmConfig): String {
        val base = config.baseUrl.trimEnd('/')
        return when (config.mode) {
            "google" -> "$base/models?key=${config.apiKey}"
            "ollama" -> "$base/api/tags"
            "anthropic" -> "$base/v1/messages"
            else -> "$base/chat/completions"
        }
    }

    private fun buildOpenAIRequest(config: LlmConfig, messages: List<ChatMessage>): String {
        val request = ChatRequest(
            model = config.model,
            messages = messages,
            temperature = 0.7,
            maxTokens = 2048
        )
        return gson.toJson(request)
    }

    private fun buildGeminiRequest(config: LlmConfig, messages: List<ChatMessage>): String {
        val systemMsg = messages.find { it.role == "system" }
        val chatMsgs = messages.filter { it.role != "system" }

        val contents = chatMsgs.map { msg ->
            JsonObject().apply {
                addProperty("role", if (msg.role == "model") "model" else "user")
                addProperty("parts", gson.toJson(listOf(mapOf("text" to msg.content))))
            }
        }

        return JsonObject().apply {
            add("contents", gson.toJsonTree(contents))
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", 0.7)
                addProperty("maxOutputTokens", 2048)
            })
            if (systemMsg != null) {
                add("systemInstruction", JsonObject().apply {
                    addProperty("parts", gson.toJson(listOf(mapOf("text" to systemMsg.content))))
                })
            }
        }.toString()
    }

    private fun buildAnthropicRequest(config: LlmConfig, messages: List<ChatMessage>): String {
        val systemMsg = messages.find { it.role == "system" }
        val chatMsgs = messages.filter { it.role != "system" }

        return JsonObject().apply {
            addProperty("model", config.model)
            addProperty("max_tokens", 2048)
            addProperty("temperature", 0.7)
            systemMsg?.let { addProperty("system", it.content) }
            add("messages", gson.toJsonTree(chatMsgs.map { msg ->
                JsonObject().apply {
                    addProperty("role", msg.role)
                    addProperty("content", msg.content)
                }
            }))
        }.toString()
    }

    private fun checkOpenAICompat(config: LlmConfig): Result<Pair<Boolean, List<String>>> {
        val requestBuilder = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/models")
            .get()

        if (config.apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${config.apiKey}")
        }

        val response = httpClient.newCall(requestBuilder.build()).execute()
        val bodyStr = response.body?.string() ?: ""

        return if (response.isSuccessful) {
            try {
                val json = gson.fromJson(bodyStr, JsonObject::class.java)
                val models = json.getAsJsonArray("data")
                    ?.mapNotNull { it.asJsonObject.get("id")?.asString }
                    ?: emptyList()
                Result.success(Pair(true, models.take(20)))
            } catch (e: Exception) {
                Result.success(Pair(true, emptyList()))
            }
        } else {
            Result.failure(Exception(parseErrorFromBody(bodyStr, response.code)))
        }
    }

    private fun checkOllama(config: LlmConfig): Result<Pair<Boolean, List<String>>> {
        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/api/tags")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val bodyStr = response.body?.string() ?: ""

        return if (response.isSuccessful) {
            try {
                val json = gson.fromJson(bodyStr, JsonObject::class.java)
                val models = json.getAsJsonArray("models")
                    ?.mapNotNull { it.asJsonObject.get("name")?.asString }
                    ?: emptyList()
                Result.success(Pair(true, models))
            } catch (e: Exception) {
                Result.success(Pair(true, emptyList()))
            }
        } else {
            Result.failure(Exception(parseErrorFromBody(bodyStr, response.code)))
        }
    }

    private fun checkGoogleModels(config: LlmConfig): Result<Pair<Boolean, List<String>>> {
        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/models?key=${config.apiKey}")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val bodyStr = response.body?.string() ?: ""

        return if (response.isSuccessful) {
            try {
                val json = gson.fromJson(bodyStr, JsonObject::class.java)
                val models = json.getAsJsonArray("models")
                    ?.mapNotNull { it.asJsonObject.get("name")?.asString?.substringAfterLast('/') }
                    ?.filter { it.startsWith("gemini") }
                    ?: emptyList()
                Result.success(Pair(true, models))
            } catch (e: Exception) {
                Result.success(Pair(true, emptyList()))
            }
        } else {
            Result.failure(Exception(parseErrorFromBody(bodyStr, response.code)))
        }
    }

    private fun parseResponse(bodyStr: String, mode: String): ChatResponse {
        return when (mode) {
            "anthropic" -> parseAnthropicResponse(bodyStr)
            "google" -> parseGeminiResponse(bodyStr)
            else -> parseOpenAIResponse(bodyStr)
        }
    }

    private fun parseOpenAIResponse(bodyStr: String): ChatResponse {
        val json = gson.fromJson(bodyStr, JsonObject::class.java)
        val choices = json.getAsJsonArray("choices")
            ?: throw Exception("响应缺少 choices 字段")
        val message = choices[0].asJsonObject.get("message")
            ?: throw Exception("响应缺少 message 字段")
        val content = message.asJsonObject.get("content")?.asString ?: ""
        val model = json.get("model")?.asString
        val usage = json.get("usage")?.asJsonObject

        return ChatResponse(
            reply = content,
            model = model,
            usage = usage?.let {
                Usage(
                    promptTokens = it.get("prompt_tokens")?.asInt,
                    completionTokens = it.get("completion_tokens")?.asInt,
                    totalTokens = it.get("total_tokens")?.asInt
                )
            }
        )
    }

    private fun parseGeminiResponse(bodyStr: String): ChatResponse {
        val json = gson.fromJson(bodyStr, JsonObject::class.java)
        val candidates = json.getAsJsonArray("candidates")
            ?: throw Exception("Gemini 响应缺少 candidates")
        val content = candidates[0].asJsonObject
            .getAsJsonArray("content")
            ?.get(0)?.asJsonObject
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString ?: ""
        val model = json.get("modelVersion")?.asString

        return ChatResponse(reply = content, model = model, usage = null)
    }

    private fun parseAnthropicResponse(bodyStr: String): ChatResponse {
        val json = gson.fromJson(bodyStr, JsonObject::class.java)
        val content = json.get("content")?.asJsonArray
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString ?: ""
        val model = json.get("model")?.asString

        return ChatResponse(reply = content, model = model, usage = null)
    }

    private fun parseErrorFromBody(bodyStr: String, httpCode: Int): String {
        return try {
            val json = gson.fromJson(bodyStr, JsonObject::class.java)
            json.get("error")?.let { err ->
                when {
                    err.isJsonPrimitive -> err.asString
                    err.isJsonObject -> {
                        val obj = err.asJsonObject
                        obj.get("message")?.asString
                            ?: obj.get("error")?.asString
                            ?: obj.get("type")?.asString
                            ?: bodyStr.take(300)
                    }
                    else -> bodyStr.take(300)
                }
            } ?: json.get("message")?.asString
                ?: when (httpCode) {
                    401 -> "认证失败，请检查 API Key 是否正确"
                    403 -> "无权限访问，请检查模型配置"
                    404 -> "模型不存在，请检查模型名称"
                    429 -> "请求过于频繁，请稍后重试"
                    else -> "请求失败 ($httpCode)"
                }
        } catch (e: Exception) {
            when (httpCode) {
                401 -> "认证失败，请检查 API Key 是否正确"
                403 -> "无权限访问，请检查模型配置"
                404 -> "模型不存在，请检查模型名称"
                429 -> "请求过于频繁，请稍后重试"
                else -> "请求失败 ($httpCode)"
            }
        }
    }
}
