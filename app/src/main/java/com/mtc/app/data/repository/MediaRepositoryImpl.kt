package com.mtc.app.data.repository

import com.mtc.app.data.model.*
import com.mtc.app.data.remote.MtcApiService
import com.mtc.app.domain.model.MediaAsset
import com.mtc.app.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val apiService: MtcApiService
) : MediaRepository {

    override suspend fun getMediaAssets(
        memberId: Int?,
        archiveId: Int?,
        purpose: String?
    ): Result<List<MediaAsset>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMediaAssets(memberId, archiveId, purpose)
            if (response.isSuccessful) {
                Result.success(response.body()!!.map { it.toDomain() })
            } else {
                Result.failure(Exception("获取媒体列表失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMediaDownloadUrl(mediaId: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMediaDownloadUrl(mediaId)
            if (response.isSuccessful) {
                val url = response.body()?.getUrl
                if (url != null && url.isNotEmpty()) {
                    Result.success(url)
                } else {
                    Result.failure(Exception("获取下载链接失败：URL为空"))
                }
            } else {
                Result.failure(Exception("获取下载链接失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadMedia(
        filename: String,
        contentType: String,
        inputStream: InputStream,
        size: Long,
        purpose: String,
        archiveId: Int?,
        memberId: Int?,
        onProgress: (Float) -> Unit
    ): Result<MediaAsset> = withContext(Dispatchers.IO) {
        try {
            // 步骤1: 初始化上传，获取预签名URL
            val initRequest = MediaUploadInitRequest(
                filename = filename,
                contentType = contentType,
                size = size.toInt(),
                purpose = purpose,
                archiveId = archiveId,
                memberId = memberId
            )
            val initResponse = apiService.initMediaUpload(initRequest)
            if (!initResponse.isSuccessful) {
                return@withContext Result.failure(Exception("初始化上传失败"))
            }
            val initData = initResponse.body()!!

            // 步骤2: 直接上传文件到 MinIO 预签名URL
            onProgress(0.3f)
            val bytes = inputStream.readBytes()
            val requestBody = bytes.toRequestBody(contentType.toMediaTypeOrNull())
            
            // 使用 OkHttp 上传（这里简化处理，实际可能需要单独的 HTTP 客户端）
            val uploadClient = okhttp3.OkHttpClient()
            val uploadRequest = okhttp3.Request.Builder()
                .url(initData.putUrl)
                .put(requestBody)
                .apply {
                    initData.requiredHeaders?.forEach { (key, value) ->
                        addHeader(key, value)
                    }
                }
                .build()
            
            val uploadResponse = uploadClient.newCall(uploadRequest).execute()
            if (!uploadResponse.isSuccessful) {
                return@withContext Result.failure(Exception("上传文件失败: ${uploadResponse.code}"))
            }
            onProgress(0.7f)

            // 步骤3: 完成上传
            val etag = uploadResponse.header("ETag")?.replace("\"", "")
            val completeRequest = MediaUploadCompleteRequest(
                uploadId = initData.uploadId,
                objectKey = initData.objectKey,
                etag = etag,
                size = size.toInt()
            )
            val completeResponse = apiService.completeMediaUpload(completeRequest)
            if (!completeResponse.isSuccessful) {
                return@withContext Result.failure(Exception("完成上传失败"))
            }
            onProgress(1.0f)

            // 返回创建的媒体资产
            Result.success(
                MediaAsset(
                    id = completeResponse.body()!!.mediaId ?: 0,
                    objectKey = initData.objectKey,
                    bucket = (completeResponse.body()!!.bucket ?: "mtc-media").ifEmpty { "mtc-media" },
                    contentType = contentType,
                    size = size.toInt(),
                    purpose = purpose,
                    archiveId = archiveId,
                    memberId = memberId,
                    createdAt = ""
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 直接上传媒体文件（multipart form）
     */
    override suspend fun uploadMediaDirect(
        filename: String,
        contentType: String,
        inputStream: InputStream,
        purpose: String,
        archiveId: Int?,
        memberId: Int?,
        onProgress: (Float) -> Unit
    ): Result<MediaAsset> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f)
            val bytes = inputStream.readBytes()
            
            // 创建 multipart 请求
            val mediaType = contentType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
            val fileBody = bytes.toRequestBody(mediaType)
            val filePart = okhttp3.MultipartBody.Part.createFormData("file", filename, fileBody)
            
            val purposePart = purpose.toRequestBody("text/plain".toMediaTypeOrNull())
            val archiveIdPart = archiveId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val memberIdPart = memberId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            
            onProgress(0.3f)
            
            val response = apiService.uploadMediaDirect(
                file = filePart,
                purpose = purposePart,
                archiveId = archiveIdPart,
                memberId = memberIdPart
            )
            
            onProgress(0.9f)
            
            if (response.isSuccessful) {
                val body = response.body()!!
                onProgress(1.0f)
                Result.success(body.toDomain())
            } else {
                Result.failure(Exception("上传失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadMediaAsBytes(mediaId: Int): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMediaFileStream(mediaId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.bytes())
                } else {
                    Result.failure(Exception("响应体为空"))
                }
            } else {
                Result.failure(Exception("下载媒体失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMediaAsset(mediaId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.deleteMediaAsset(mediaId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun MediaAssetResponse.toDomain() = MediaAsset(
    id = effectiveId,
    objectKey = objectKey,
    bucket = (bucket ?: "mtc-media").ifEmpty { "mtc-media" },
    contentType = (contentType ?: "application/octet-stream").ifEmpty { "application/octet-stream" },
    size = size,
    purpose = purpose ?: "general",
    archiveId = archiveId,
    memberId = memberId,
    createdAt = createdAt ?: ""
)
