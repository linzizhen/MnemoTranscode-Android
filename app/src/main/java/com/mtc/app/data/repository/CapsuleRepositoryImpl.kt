package com.mtc.app.data.repository

import com.mtc.app.data.model.*
import com.mtc.app.data.remote.MtcApiService
import com.mtc.app.domain.model.Capsule
import com.mtc.app.domain.repository.CapsuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapsuleRepositoryImpl @Inject constructor(
    private val apiService: MtcApiService
) : CapsuleRepository {

    override suspend fun getCapsules(memberId: Int?): Result<List<Capsule>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCapsules(memberId)
            if (response.isSuccessful) {
                Result.success(response.body()!!.map { it.toDomain() })
            } else {
                Result.failure(Exception("获取胶囊列表失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCapsule(capsuleId: Int): Result<Capsule> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCapsule(capsuleId)
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("获取胶囊详情失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createCapsule(
        memberId: Int,
        title: String,
        content: String,
        unlockDate: String,
        recipients: List<Int>?
    ): Result<Capsule> = withContext(Dispatchers.IO) {
        try {
            // 使用 Query 参数调用 API（后端使用查询参数）
            val response = apiService.createCapsule(
                memberId = memberId,
                title = title,
                content = content,
                unlockDate = unlockDate,
                recipients = recipients
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = when {
                    response.code() == 400 -> "请求参数错误，请检查输入"
                    response.code() == 401 -> "未授权，请重新登录"
                    response.code() == 403 -> "无权限访问该成员"
                    response.code() == 404 -> "成员不存在"
                    response.code() == 422 -> "数据验证失败: ${errorBody ?: ""}"
                    response.code() == 500 -> "服务器错误，请稍后重试"
                    else -> "创建失败 (HTTP ${response.code()}): ${errorBody ?: "未知错误"}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("网络错误: ${e.message ?: "未知错误"}"))
        }
    }

    override suspend fun forceUnlockCapsule(capsuleId: Int): Result<Capsule> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.forceUnlockCapsule(capsuleId)
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("强制解锁胶囊失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun CapsuleResponse.toDomain() = Capsule(
    id = id,
    memberId = memberId,
    title = title,
    content = content,
    unlockDate = unlockDate,
    status = status,
    createdAt = createdAt,
    message = message
)

private fun CapsuleForceUnlockResponse.toDomain() = Capsule(
    id = id,
    memberId = memberId,
    title = title,
    content = content,
    unlockDate = unlockDate,
    status = status,
    createdAt = createdAt,
    message = message
)
