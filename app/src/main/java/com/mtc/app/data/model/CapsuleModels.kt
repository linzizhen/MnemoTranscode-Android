package com.mtc.app.data.model

import com.google.gson.annotations.SerializedName

data class CapsuleCreateRequest(
    @SerializedName("member_id") val memberId: Int,
    val title: String,
    val content: String,
    @SerializedName("unlock_date") val unlockDate: String,
    val recipients: List<Int>? = null
)

data class CapsuleResponse(
    val id: Int,
    @SerializedName("member_id") val memberId: Int?,
    val title: String,
    val content: String? = null,
    @SerializedName("unlock_date") val unlockDate: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    val message: String? = null
)

/**
 * 胶囊强制解锁响应
 */
data class CapsuleForceUnlockResponse(
    val id: Int,
    @SerializedName("member_id") val memberId: Int?,
    val title: String,
    val content: String?,
    @SerializedName("unlock_date") val unlockDate: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    val message: String?
)
