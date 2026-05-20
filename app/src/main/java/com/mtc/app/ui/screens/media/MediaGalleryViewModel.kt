package com.mtc.app.ui.screens.media

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.domain.model.MediaAsset
import com.mtc.app.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

data class MediaGalleryUiState(
    val isLoading: Boolean = false,
    val mediaAssets: List<MediaAsset> = emptyList(),
    val selectedAsset: MediaAsset? = null,
    // mediaId -> downloadUrl（Gallery 和详情弹窗共用）
    val downloadUrls: Map<Int, String> = emptyMap(),
    val selectedFilter: String? = null,
    val error: String? = null,
    // 上传相关
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val uploadError: String? = null,
    val uploadSuccess: Boolean = false,
    // 删除相关
    val isDeleting: Boolean = false,
    val deleteError: String? = null
)

@HiltViewModel
class MediaGalleryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaGalleryUiState())
    val uiState: StateFlow<MediaGalleryUiState> = _uiState.asStateFlow()

    val filterOptions = listOf(
        null to "全部",
        "archive_photo" to "照片",
        "archive_video" to "视频",
        "archive_audio" to "音频",
        "avatar" to "头像",
        "voice_sample" to "声纹",
        "archive_sticker" to "表情包"
    )

    init {
        loadMediaAssets()
    }

    fun loadMediaAssets(purpose: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedFilter = purpose)
            mediaRepository.getMediaAssets(purpose = purpose)
                .onSuccess { assets ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        mediaAssets = assets
                    )
                    // 预加载所有图片的 downloadUrl
                    assets.forEach { asset ->
                        if (asset.isImage) fetchDownloadUrl(asset.id)
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    private fun fetchDownloadUrl(assetId: Int) {
        viewModelScope.launch {
            if (_uiState.value.downloadUrls.containsKey(assetId)) return@launch
            mediaRepository.getMediaDownloadUrl(assetId)
                .onSuccess { url ->
                    val updated = _uiState.value.downloadUrls.toMutableMap()
                    updated[assetId] = url
                    _uiState.value = _uiState.value.copy(downloadUrls = updated)
                }
        }
    }

    fun selectAsset(asset: MediaAsset) {
        viewModelScope.launch {
            val cachedUrl = _uiState.value.downloadUrls[asset.id]
            if (cachedUrl != null) {
                _uiState.value = _uiState.value.copy(selectedAsset = asset)
            } else {
                mediaRepository.getMediaDownloadUrl(asset.id)
                    .onSuccess { url ->
                        val updated = _uiState.value.downloadUrls.toMutableMap()
                        updated[asset.id] = url
                        _uiState.value = _uiState.value.copy(
                            downloadUrls = updated,
                            selectedAsset = asset
                        )
                    }
                    .onFailure {
                        _uiState.value = _uiState.value.copy(selectedAsset = asset)
                    }
            }
        }
    }

    fun clearSelectedAsset() {
        _uiState.value = _uiState.value.copy(selectedAsset = null)
    }

    fun setFilter(purpose: String?) {
        loadMediaAssets(purpose)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 上传图片
     */
    fun uploadImage(
        context: Context,
        uri: Uri,
        purpose: String = "archive_photo",
        archiveId: Int? = null,
        memberId: Int? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUploading = true,
                uploadProgress = 0f,
                uploadError = null,
                uploadSuccess = false
            )

            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val filename = "image_${System.currentTimeMillis()}.${getExtensionFromMimeType(mimeType)}"

                // 打开输入流
                val inputStream: InputStream = contentResolver.openInputStream(uri)
                    ?: throw Exception("无法读取文件")

                // 获取文件大小
                inputStream.use { stream ->
                    val bytes = stream.readBytes()
                    val size = bytes.size.toLong()

                    mediaRepository.uploadMedia(
                        filename = filename,
                        contentType = mimeType,
                        inputStream = bytes.inputStream(),
                        size = size,
                        purpose = purpose,
                        archiveId = archiveId,
                        memberId = memberId,
                        onProgress = { progress ->
                            _uiState.value = _uiState.value.copy(uploadProgress = progress)
                        }
                    ).onSuccess { asset ->
                        _uiState.value = _uiState.value.copy(
                            isUploading = false,
                            uploadProgress = 1f,
                            uploadSuccess = true
                        )
                        // 刷新列表
                        loadMediaAssets(_uiState.value.selectedFilter)
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isUploading = false,
                            uploadError = e.message ?: "上传失败"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    uploadError = e.message ?: "上传失败"
                )
            }
        }
    }

    /**
     * 上传表情包
     */
    fun uploadSticker(
        context: Context,
        uri: Uri,
        archiveId: Int? = null,
        memberId: Int? = null
    ) {
        uploadImage(context, uri, "archive_sticker", archiveId, memberId)
    }

    fun clearUploadState() {
        _uiState.value = _uiState.value.copy(
            isUploading = false,
            uploadProgress = 0f,
            uploadError = null,
            uploadSuccess = false
        )
    }

    /**
     * 删除媒体资产
     */
    fun deleteAsset(asset: MediaAsset) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, deleteError = null)
            mediaRepository.deleteMediaAsset(asset.id)
                .onSuccess {
                    // 从列表中移除已删除的资产
                    val updatedAssets = _uiState.value.mediaAssets.filter { it.id != asset.id }
                    val updatedUrls = _uiState.value.downloadUrls.toMutableMap().apply { remove(asset.id) }
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        mediaAssets = updatedAssets,
                        downloadUrls = updatedUrls,
                        selectedAsset = null
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        deleteError = e.message ?: "删除失败"
                    )
                }
        }
    }

    fun clearDeleteError() {
        _uiState.value = _uiState.value.copy(deleteError = null)
    }

    private fun getExtensionFromMimeType(mimeType: String): String {
        return when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            else -> "jpg"
        }
    }
}
