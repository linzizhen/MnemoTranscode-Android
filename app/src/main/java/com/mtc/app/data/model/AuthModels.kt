package com.mtc.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 用户相关数据模型
 */
data class UserResponse(
    val id: Int,
    val email: String,
    val username: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("subscription_tier") val subscriptionTier: String = "free",
    @SerializedName("monthly_token_limit") val monthlyTokenLimit: Long = 100000L,
    @SerializedName("monthly_token_used") val monthlyTokenUsed: Long = 0L
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val user: UserResponse
)

data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String
)

data class UpdateUserRequest(
    val username: String? = null,
    @SerializedName("subscription_tier") val subscriptionTier: String? = null
)
