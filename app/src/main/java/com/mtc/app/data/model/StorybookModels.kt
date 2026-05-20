package com.mtc.app.data.model

import com.google.gson.annotations.SerializedName

data class StorybookGenerateRequest(
    @SerializedName("archive_id") val archiveId: Int,
    @SerializedName("member_id") val memberId: Int? = null,
    val style: String = "nostalgic",
    @SerializedName("client_llm") val clientLlm: ClientLlmConfig? = null
)

data class StorybookResponse(
    val story: String,
    @SerializedName("archive_id") val archiveId: Int,
    @SerializedName("member_id") val memberId: Int?,
    val style: String,
    @SerializedName("memory_count") val memoryCount: Int
)
