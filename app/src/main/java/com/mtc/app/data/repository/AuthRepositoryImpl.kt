package com.mtc.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mtc.app.data.local.*
import com.mtc.app.data.model.*
import com.mtc.app.data.remote.MtcApiService
import com.mtc.app.data.remote.TokenManager
import com.mtc.app.domain.model.*
import com.mtc.app.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证 Repository 实现
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: MtcApiService,
    @ApplicationContext private val context: Context
) : AuthRepository {
    private val gson = Gson()

    private fun <T> parseErrorBody(response: Response<T>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val json = gson.fromJson(errorBody, JsonObject::class.java)
                // 尝试多种错误字段
                val msg = json.get("detail")?.asString
                    ?: json.get("message")?.asString
                    ?: json.get("error")?.asString
                    ?: json.get("msg")?.asString
                if (msg != null && msg.isNotBlank()) return msg
                // Pydantic 校验错误格式：[{"loc":[...],"msg":"...","type":"..."}]
                val detail = json.get("detail")
                if (detail != null && detail.isJsonArray) {
                    return detail.asJsonArray.joinToString("; ") { item ->
                        val obj = item.asJsonObject
                        val loc = obj.get("loc")?.asJsonArray?.joinToString(".") ?: ""
                        val msgText = obj.get("msg")?.asString ?: item.toString()
                        if (loc.isNotEmpty()) "$loc: $msgText" else msgText
                    }
                }
                // 兜底：返回原始 errorBody 截断
                return errorBody.take(300)
            }
            // 无 errorBody 时用 HTTP 状态码映射
            when (response.code()) {
                401 -> "邮箱或密码错误"
                403 -> "无权限访问"
                404 -> "资源不存在"
                409 -> "邮箱已被注册"
                422 -> "注册信息格式不正确"
                429 -> "请求过于频繁，请稍后重试"
                500, 502, 503, 504 -> "服务器错误，请稍后重试"
                else -> "请求失败 (${response.code()})"
            }
        } catch (e: Exception) {
            when (response.code()) {
                401 -> "邮箱或密码错误"
                409 -> "邮箱已被注册"
                422 -> "注册信息格式不正确"
                429 -> "请求过于频繁，请稍后重试"
                500, 502, 503, 504 -> "服务器错误，请稍后重试"
                else -> "请求失败 (${response.code()})"
            }
        }
    }

    override suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(username = email, password = password)
            if (response.isSuccessful) {
                val tokenResponse = response.body()!!
                TokenManager.saveToken(context, tokenResponse.accessToken)
                Result.success(tokenResponse.user.toDomain())
            } else {
                Result.failure(Exception(parseErrorBody(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, username: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.register(RegisterRequest(email, username, password))
            if (response.isSuccessful) {
                val tokenResponse = response.body()!!
                TokenManager.saveToken(context, tokenResponse.accessToken)
                Result.success(tokenResponse.user.toDomain())
            } else {
                Result.failure(Exception(parseErrorBody(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(parseErrorBody(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        TokenManager.clearToken(context)
    }

    override suspend fun uploadAvatar(filename: String, mimeType: String, bytes: ByteArray): Result<User> = withContext(Dispatchers.IO) {
        try {
            val mediaType = mimeType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
            val fileBody = bytes.toRequestBody(mediaType)
            val filePart = okhttp3.MultipartBody.Part.createFormData("file", filename, fileBody)
            val response = apiService.uploadAvatar(filePart)
            if (response.isSuccessful) {
                Result.success(response.body()!!.user.toDomain())
            } else {
                Result.failure(Exception("上传头像失败 (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAvatar(): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteAvatar()
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("删除头像失败 (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        TokenManager.getToken(context) != null
    }
}

// 扩展函数：DTO -> Domain Model
private fun UserResponse.toDomain() = User(
    id = id,
    email = email,
    username = username,
    isActive = isActive,
    createdAt = createdAt,
    avatarUrl = avatarUrl,
    subscriptionTier = subscriptionTier,
    monthlyTokenLimit = if (monthlyTokenLimit > 0) monthlyTokenLimit.toInt() else null,
    monthlyTokenUsed = if (monthlyTokenUsed >= 0) monthlyTokenUsed.toInt() else 0
)
