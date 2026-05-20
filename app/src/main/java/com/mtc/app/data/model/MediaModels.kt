package com.mtc.app.data.model

import com.google.gson.annotations.SerializedName

data class MediaAssetResponse(
    val id: Int? = null,
    @SerializedName("media_id") val mediaId: Int? = null,
    @SerializedName("object_key") val objectKey: String,
    val bucket: String = "",
    @SerializedName("content_type") val contentType: String = "application/octet-stream",
    val size: Int = 0,
    val purpose: String? = null,
    @SerializedName("archive_id") val archiveId: Int? = null,
    @SerializedName("member_id") val memberId: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null
) {
    /** 返回有效 ID（优先用 media_id，fallback 到 id）*/
    val effectiveId: Int get() = mediaId ?: id ?: 0
}

data class MediaUploadInitRequest(
    val filename: String,
    @SerializedName("content_type") val contentType: String,
    val size: Int,
    val purpose: String,
    @SerializedName("archive_id") val archiveId: Int? = null,
    @SerializedName("member_id") val memberId: Int? = null
)

data class MediaUploadInitResponse(
    @SerializedName("upload_id") val uploadId: String,
    @SerializedName("object_key") val objectKey: String,
    @SerializedName("put_url") val putUrl: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("required_headers") val requiredHeaders: Map<String, String>? = null
)

data class MediaUploadCompleteRequest(
    @SerializedName("upload_id") val uploadId: String,
    @SerializedName("object_key") val objectKey: String,
    val etag: String? = null,
    val size: Int? = null
)

data class MediaUploadCompleteResponse(
    @SerializedName("media_id") val mediaId: Int?,
    @SerializedName("object_key") val objectKey: String,
    val bucket: String? = null,
    val status: String? = null
)

data class MediaDownloadUrlResponse(
    @SerializedName("get_url") val getUrl: String,
    @SerializedName("expires_in") val expiresIn: Int
)
