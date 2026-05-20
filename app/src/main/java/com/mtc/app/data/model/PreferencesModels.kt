package com.mtc.app.data.model

import com.google.gson.annotations.SerializedName

data class PreferencesResponse(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    val theme: String = "light",
    @SerializedName("primary_color") val primaryColor: String = "jade",
    @SerializedName("card_style") val cardStyle: String = "glass",
    @SerializedName("font_size") val fontSize: String = "medium",
    @SerializedName("dashboard_layout") val dashboardLayout: String = "grid",
    @SerializedName("custom_css") val customCss: String? = null,
    @SerializedName("app_background_url") val appBackgroundUrl: String? = null,
    @SerializedName("ai_memory_sync") val aiMemorySync: String = "on",
    @SerializedName("llm_mode") val llmMode: String? = null,
    @SerializedName("llm_base_url") val llmBaseUrl: String? = null,
    @SerializedName("llm_api_key") val llmApiKey: String? = null,
    @SerializedName("llm_model") val llmModel: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class PreferencesUpdateRequest(
    val theme: String? = null,
    @SerializedName("primary_color") val primaryColor: String? = null,
    @SerializedName("card_style") val cardStyle: String? = null,
    @SerializedName("font_size") val fontSize: String? = null,
    @SerializedName("dashboard_layout") val dashboardLayout: String? = null,
    @SerializedName("custom_css") val customCss: String? = null,
    @SerializedName("app_background_url") val appBackgroundUrl: String? = null,
    @SerializedName("ai_memory_sync") val aiMemorySync: String? = null,
    @SerializedName("llm_mode") val llmMode: String? = null,
    @SerializedName("llm_base_url") val llmBaseUrl: String? = null,
    @SerializedName("llm_api_key") val llmApiKey: String? = null,
    @SerializedName("llm_model") val llmModel: String? = null
)
