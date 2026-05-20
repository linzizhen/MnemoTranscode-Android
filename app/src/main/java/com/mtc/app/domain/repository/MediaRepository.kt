package com.mtc.app.domain.repository

import com.mtc.app.domain.model.MediaAsset
import java.io.InputStream

/**
 * 媒体 Repository 接口
 */
interface MediaRepository {
    suspend fun getMediaAssets(memberId: Int? = null, archiveId: Int? = null, purpose: String? = null): Result<List<MediaAsset>>
    suspend fun getMediaDownloadUrl(mediaId: Int): Result<String>
    suspend fun uploadMedia(
        filename: String,
        contentType: String,
        inputStream: InputStream,
        size: Long,
        purpose: String,
        archiveId: Int? = null,
        memberId: Int? = null,
        onProgress: (Float) -> Unit = {}
    ): Result<MediaAsset>
    
    /**
     * 直接上传媒体文件（multipart form）
     */
    suspend fun uploadMediaDirect(
        filename: String,
        contentType: String,
        inputStream: InputStream,
        purpose: String,
        archiveId: Int? = null,
        memberId: Int? = null,
        onProgress: (Float) -> Unit = {}
    ): Result<MediaAsset>

    /**
     * 经 API 鉴权下载媒体文件原始字节（用于图片展示）
     */
    suspend fun downloadMediaAsBytes(mediaId: Int): Result<ByteArray>

    /**
     * 删除媒体资产
     */
    suspend fun deleteMediaAsset(mediaId: Int): Result<Unit>
}
