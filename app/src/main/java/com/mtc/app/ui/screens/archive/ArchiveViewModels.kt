package com.mtc.app.ui.screens.archive

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.domain.model.Archive
import com.mtc.app.domain.model.Memory
import com.mtc.app.domain.repository.ArchiveRepository
import com.mtc.app.domain.repository.MediaRepository
import com.mtc.app.domain.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ArchiveListUiState(
    val isLoading: Boolean = false,
    val archives: List<Archive> = emptyList(),
    val error: String? = null
)

data class ArchiveDetailUiState(
    val isLoading: Boolean = false,
    val archive: Archive? = null,
    val members: List<com.mtc.app.domain.model.Member> = emptyList(),
    val memories: List<Memory> = emptyList(),
    val isLoadingMemories: Boolean = false,
    val mediaAssets: List<com.mtc.app.domain.model.MediaAsset> = emptyList(),
    val mediaDownloadUrls: Map<Int, String> = emptyMap(), // assetId -> downloadUrl
    val isLoadingMedia: Boolean = false,
    val error: String? = null
)

data class UploadState(
    val isUploading: Boolean = false,
    val progress: Float = 0f,
    val isSuccess: Boolean = false,
    val error: String? = null
)

data class CreateArchiveUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val createdArchiveId: Int? = null,
    val error: String? = null
)

@HiltViewModel
class ArchiveListViewModel @Inject constructor(
    private val archiveRepository: ArchiveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveListUiState())
    val uiState: StateFlow<ArchiveListUiState> = _uiState.asStateFlow()

    init {
        loadArchives()
    }

    fun loadArchives() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            archiveRepository.getArchives()
                .onSuccess { archives ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        archives = archives
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }
}

@HiltViewModel
class ArchiveDetailViewModel @Inject constructor(
    private val archiveRepository: ArchiveRepository,
    private val mediaRepository: MediaRepository,
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveDetailUiState())
    val uiState: StateFlow<ArchiveDetailUiState> = _uiState.asStateFlow()

    private val _uploadState = MutableStateFlow(UploadState())
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    fun loadArchive(archiveId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            archiveRepository.getArchive(archiveId)
                .onSuccess { archive ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        archive = archive
                    )
                    loadMembers(archiveId)
                    loadMedia(archiveId)
                    loadMemories(archiveId)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    private fun loadMembers(archiveId: Int) {
        viewModelScope.launch {
            archiveRepository.getMembers(archiveId)
                .onSuccess { members ->
                    _uiState.value = _uiState.value.copy(members = members)
                }
        }
    }

    private fun loadMemories(archiveId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMemories = true)
            memoryRepository.getMemories(archiveId = archiveId, limit = 20)
                .onSuccess { memories ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMemories = false,
                        memories = memories
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingMemories = false)
                }
        }
    }

    fun loadMedia(archiveId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMedia = true)
            mediaRepository.getMediaAssets(archiveId = archiveId, purpose = "archive_photo")
                .onSuccess { assets ->
                    // 限制最多显示 6 张图片，避免过多并发请求
                    val limitedAssets = assets.take(6)
                    _uiState.value = _uiState.value.copy(
                        isLoadingMedia = false,
                        mediaAssets = limitedAssets
                    )
                    // 最多并发加载 3 个下载链接，其余延迟加载
                    limitedAssets.take(3).forEach { asset ->
                        fetchDownloadUrl(asset.id)
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingMedia = false)
                }
        }
    }

    private fun fetchDownloadUrl(assetId: Int) {
        viewModelScope.launch {
            try {
                val result = mediaRepository.getMediaDownloadUrl(assetId)
                if (result.isSuccess) {
                    val url = result.getOrNull()
                    if (url != null) {
                        val currentUrls = _uiState.value.mediaDownloadUrls.toMutableMap()
                        currentUrls[assetId] = url
                        _uiState.value = _uiState.value.copy(mediaDownloadUrls = currentUrls)
                    }
                }
                // 失败时不更新 map，让 UI 显示占位符（不再持续转圈）
            } catch (_: Exception) {
                // 异常时静默忽略
            }
        }
    }

    /**
     * 上传媒体文件（memberId不为空时上传为成员头像，否则上传为档案媒体）
     */
    fun uploadMedia(
        context: Context,
        uri: Uri,
        filename: String,
        archiveId: Int,
        memberId: Int? = null
    ) {
        viewModelScope.launch {
            _uploadState.value = UploadState(isUploading = true)

            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                
                // 读取文件字节
                val bytes = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                }

                if (bytes.isEmpty()) {
                    _uploadState.value = UploadState(error = "无法读取文件")
                    return@launch
                }

                val result = if (memberId != null) {
                    // 上传为成员头像
                    archiveRepository.uploadMemberAvatar(
                        archiveId = archiveId,
                        memberId = memberId,
                        filename = filename,
                        mimeType = mimeType,
                        bytes = bytes
                    )
                } else {
                    // 上传为档案媒体
                    mediaRepository.uploadMediaDirect(
                        filename = filename,
                        contentType = mimeType,
                        inputStream = bytes.inputStream(),
                        purpose = "archive_photo",
                        archiveId = archiveId,
                        memberId = null,
                        onProgress = { progress ->
                            _uploadState.value = _uploadState.value.copy(progress = progress)
                        }
                    )
                }

                result.onSuccess {
                    _uploadState.value = UploadState(isSuccess = true)
                    // 上传成功后刷新成员数据（更新头像URL）
                    if (memberId != null) {
                        loadArchive(archiveId)
                    } else {
                        loadMedia(archiveId)
                    }
                }.onFailure { e ->
                    _uploadState.value = UploadState(error = e.message)
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState(error = e.message ?: "上传失败")
            }
        }
    }

    fun deleteArchive(archiveId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            archiveRepository.deleteArchive(archiveId)
                .onSuccess { onSuccess() }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun clearUploadState() {
        _uploadState.value = UploadState()
    }
}

@HiltViewModel
class CreateArchiveViewModel @Inject constructor(
    private val archiveRepository: ArchiveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateArchiveUiState())
    val uiState: StateFlow<CreateArchiveUiState> = _uiState.asStateFlow()

    val archiveTypes = listOf(
        "family" to "家族记忆",
        "lover" to "恋人记忆",
        "friend" to "挚友记忆",
        "relative" to "至亲记忆",
        "celebrity" to "伟人记忆",
        "nation" to "国家历史"
    )

    fun createArchive(name: String, description: String?, archiveType: String) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入档案名称")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            archiveRepository.createArchive(name, description, archiveType)
                .onSuccess { archive ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        createdArchiveId = archive.id
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
