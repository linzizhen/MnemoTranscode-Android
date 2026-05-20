package com.mtc.app.data.repository

import com.mtc.app.data.model.*
import com.mtc.app.data.remote.MtcApiService
import com.mtc.app.domain.model.Storybook
import com.mtc.app.domain.repository.StorybookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorybookRepositoryImpl @Inject constructor(
    private val apiService: MtcApiService
) : StorybookRepository {

    override suspend fun generateStory(
        archiveId: Int,
        memberId: Int?,
        style: String,
        clientLlm: ClientLlmConfig?
    ): Result<Storybook> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.generateStory(
                StorybookGenerateRequest(
                    archiveId = archiveId,
                    memberId = memberId,
                    style = style,
                    clientLlm = clientLlm
                )
            )
            if (response.isSuccessful) {
                val body = response.body()!!
                Result.success(Storybook(
                    story = body.story,
                    archiveId = body.archiveId,
                    memberId = body.memberId,
                    style = body.style,
                    memoryCount = body.memoryCount
                ))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = when (response.code()) {
                    400 -> "请求参数错误，请检查输入"
                    401 -> "认证失败，请检查 API Key"
                    404 -> "档案或成员不存在"
                    500 -> "服务器内部错误，请稍后重试"
                    else -> errorBody ?: "生成故事书失败 (${response.code()})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Unable to resolve host") == true ->
                    "网络连接失败，请检查网络设置"
                e.message?.contains("timeout") == true ->
                    "请求超时，请检查网络或服务器状态"
                else -> e.message ?: "生成故事书失败"
            }
            Result.failure(Exception(errorMessage))
        }
    }
}
