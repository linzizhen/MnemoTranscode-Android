package com.mtc.app.ui.screens.member

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.data.model.ChatImportConfig
import com.mtc.app.data.model.ChatImportRequest
import com.mtc.app.data.remote.MtcApiService
import com.mtc.app.domain.model.GraphEdge
import com.mtc.app.domain.model.GraphNode
import com.mtc.app.domain.model.MediaAsset
import com.mtc.app.domain.model.Member
import com.mtc.app.domain.model.Memory
import com.mtc.app.domain.repository.ArchiveRepository
import com.mtc.app.domain.repository.MediaRepository
import com.mtc.app.domain.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class MemberDetailUiState(
    val isLoading: Boolean = false,
    val member: Member? = null,
    val memories: List<Memory> = emptyList(),
    val isLoadingMemories: Boolean = false,
    val graphNodes: List<GraphNode> = emptyList(),
    val graphEdges: List<GraphEdge> = emptyList(),
    val isLoadingGraph: Boolean = false,
    val graphError: String? = null,
    val error: String? = null,
    // 媒体相关
    val mediaAssets: List<MediaAsset> = emptyList(),
    val mediaDownloadUrls: Map<Int, String> = emptyMap(),
    val isLoadingMedia: Boolean = false
)

data class MemberUploadState(
    val isUploading: Boolean = false,
    val progress: Float = 0f,
    val isSuccess: Boolean = false,
    val error: String? = null
)

data class CreateMemberUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val createdMemberId: Int? = null,
    val error: String? = null
)

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    private val archiveRepository: ArchiveRepository,
    private val memoryRepository: MemoryRepository,
    private val mediaRepository: MediaRepository,
    private val apiService: MtcApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberDetailUiState())
    val uiState: StateFlow<MemberDetailUiState> = _uiState.asStateFlow()

    // 独立的 StateFlow：只给媒体库使用，URL 变化只触发媒体库 recompose
    private val _mediaDownloadUrls = MutableStateFlow<Map<Int, String>>(emptyMap())
    val mediaDownloadUrls: StateFlow<Map<Int, String>> = _mediaDownloadUrls.asStateFlow()

    private val _uploadState = MutableStateFlow(MemberUploadState())
    val uploadState: StateFlow<MemberUploadState> = _uploadState.asStateFlow()

    // 追踪各加载任务，避免重复启动或竞态
    private var loadMemberJob: Job? = null
    private var loadMemoriesJob: Job? = null
    private var loadGraphJob: Job? = null
    private var loadMediaJob: Job? = null

    fun loadMember(archiveId: Int, memberId: Int) {
        loadMemberJob?.cancel()
        loadMemoriesJob?.cancel()
        loadGraphJob?.cancel()
        loadMediaJob?.cancel()

        loadMemberJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = withTimeoutOrNull(15_000L) {
                archiveRepository.getMember(archiveId, memberId)
            }
            when {
                result == null -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "无法连接服务器，请检查网络"
                    )
                }
                result.isSuccess -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        member = result.getOrNull()
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "加载失败"
                    )
                }
            }
        }
        loadMemoriesJob = launchLoadMemories(memberId)
        loadGraphJob = launchLoadMnemoGraph(archiveId, memberId)
        loadMediaJob = launchLoadMedia(memberId)
        viewModelScope.launch {
            delay(20_000L)
            if (_uiState.value.isLoading) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (_uiState.value.member != null) null else "加载超时，请检查网络"
                )
            }
        }
    }

    private fun launchLoadMemories(memberId: Int): Job = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoadingMemories = true)
        val result = withTimeoutOrNull(15_000L) {
            memoryRepository.getMemories(memberId = memberId)
        }
        _uiState.value = _uiState.value.copy(
            isLoadingMemories = false,
            memories = result?.getOrNull() ?: emptyList()
        )
    }

    private fun launchLoadMnemoGraph(archiveId: Int, memberId: Int): Job = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoadingGraph = true, graphError = null)
        val result = withTimeoutOrNull(15_000L) {
            memoryRepository.getMnemoGraph(archiveId, memberId)
        }
        when {
            result == null -> {
                _uiState.value = _uiState.value.copy(
                    isLoadingGraph = false,
                    graphError = "关系网络加载超时"
                )
            }
            result.isSuccess -> {
                val (nodes, edges) = result.getOrNull() ?: (emptyList<GraphNode>() to emptyList<GraphEdge>())
                _uiState.value = _uiState.value.copy(
                    isLoadingGraph = false,
                    graphNodes = nodes,
                    graphEdges = edges
                )
            }
            else -> {
                _uiState.value = _uiState.value.copy(
                    isLoadingGraph = false,
                    graphError = result.exceptionOrNull()?.message ?: "加载失败"
                )
            }
        }
    }

    /**
     * 公开方法：供 UI 层重新加载关系图使用
     */
    fun loadMnemoGraph(archiveId: Int, memberId: Int) {
        loadGraphJob?.cancel()
        loadGraphJob = launchLoadMnemoGraph(archiveId, memberId)
    }

    private fun launchLoadMedia(memberId: Int): Job = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoadingMedia = true)
        val result = withTimeoutOrNull(15_000L) {
            mediaRepository.getMediaAssets(memberId = memberId)
        }
        when {
            result == null -> {
                _uiState.value = _uiState.value.copy(isLoadingMedia = false)
            }
            result.isSuccess -> {
                val assets = result.getOrNull() ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    isLoadingMedia = false,
                    mediaAssets = assets
                )
                // 在这里统一加载所有 URL，由 _mediaDownloadUrls 独立管理
                assets.forEach { asset ->
                    launchFetchMediaUrl(asset.id)
                }
            }
            else -> {
                _uiState.value = _uiState.value.copy(isLoadingMedia = false)
            }
        }
    }

    private fun launchFetchMediaUrl(assetId: Int): Job = viewModelScope.launch {
        if (_mediaDownloadUrls.value.containsKey(assetId)) return@launch
        val url = withContext(Dispatchers.IO) {
            mediaRepository.getMediaDownloadUrl(assetId).getOrNull()
        }
        if (url != null) {
            _mediaDownloadUrls.value = _mediaDownloadUrls.value + (assetId to url)
        }
    }

    fun loadMedia(memberId: Int) {
        loadMediaJob?.cancel()
        loadMediaJob = launchLoadMedia(memberId)
    }

    fun loadMemories(memberId: Int) {
        loadMemoriesJob?.cancel()
        loadMemoriesJob = launchLoadMemories(memberId)
    }

    fun fetchMediaUrl(assetId: Int) {
        launchFetchMediaUrl(assetId)
    }

    fun uploadMedia(
        context: Context,
        uri: Uri,
        filename: String,
        memberId: Int
    ) {
        viewModelScope.launch {
            _uploadState.value = MemberUploadState(isUploading = true)
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                }
                if (bytes.isEmpty()) {
                    _uploadState.value = MemberUploadState(error = "无法读取文件")
                    return@launch
                }
                mediaRepository.uploadMediaDirect(
                    filename = filename,
                    contentType = mimeType,
                    inputStream = bytes.inputStream(),
                    purpose = "archive_photo",
                    memberId = memberId
                ).onSuccess {
                    _uploadState.value = MemberUploadState(isSuccess = true)
                    loadMedia(memberId)
                }.onFailure { e ->
                    _uploadState.value = MemberUploadState(error = e.message)
                }
            } catch (e: Exception) {
                _uploadState.value = MemberUploadState(error = e.message)
            }
        }
    }

    fun deleteMediaAsset(asset: MediaAsset) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMedia = true)
            mediaRepository.deleteMediaAsset(asset.id)
                .onSuccess {
                    val updatedAssets = _uiState.value.mediaAssets.filter { it.id != asset.id }
                    val updatedUrls = _uiState.value.mediaDownloadUrls.toMutableMap().apply { remove(asset.id) }
                    _uiState.value = _uiState.value.copy(
                        isLoadingMedia = false,
                        mediaAssets = updatedAssets,
                        mediaDownloadUrls = updatedUrls
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMedia = false,
                        error = "删除失败：${e.message}"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, graphError = null)
    }

    fun deleteMember(archiveId: Int, memberId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            archiveRepository.deleteMember(archiveId, memberId)
                .onSuccess { onSuccess() }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun deleteMemory(memoryId: Int) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(memoryId)
                .onSuccess {
                    _uiState.value.member?.let { member ->
                        loadMemoriesJob?.cancel()
                        loadMemoriesJob = launchLoadMemories(member.id)
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun uploadMemberAvatar(
        context: Context,
        uri: Uri,
        filename: String,
        archiveId: Int,
        memberId: Int
    ) {
        viewModelScope.launch {
            _uploadState.value = MemberUploadState(isUploading = true)
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                }
                if (bytes.isEmpty()) {
                    _uploadState.value = MemberUploadState(error = "无法读取文件")
                    return@launch
                }
                archiveRepository.uploadMemberAvatar(
                    archiveId = archiveId,
                    memberId = memberId,
                    filename = filename,
                    mimeType = mimeType,
                    bytes = bytes
                ).onSuccess { updatedMember ->
                    _uploadState.value = MemberUploadState(isSuccess = true)
                    _uiState.value = _uiState.value.copy(member = updatedMember)
                }.onFailure { e ->
                    _uploadState.value = MemberUploadState(error = e.message)
                }
            } catch (e: Exception) {
                _uploadState.value = MemberUploadState(error = e.message)
            }
        }
    }

    fun clearUploadState() {
        _uploadState.value = MemberUploadState()
    }

    /**
     * 导入聊天记录
     * @param navigateToProgress 是否跳转到进度页（AI 多批处理）
     */
    fun importChatFromText(
        rawText: String,
        config: ChatImportConfig,
        archiveId: Int,
        memberId: Int,
        navigateToProgress: Boolean,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (rawText.isBlank()) {
                    onError("聊天内容不能为空")
                    return@launch
                }
                val source = when (config.parseMode) {
                    com.mtc.app.data.model.ParseMode.AUTO -> "auto"
                    com.mtc.app.data.model.ParseMode.WECHAT -> "wechat"
                    com.mtc.app.data.model.ParseMode.PLAIN -> "plain"
                }
                val request = ChatImportRequest(
                    memberId = memberId,
                    rawText = rawText,
                    source = source,
                    buildGraph = config.buildRelationNetwork
                )
                val response = apiService.chatImport(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        onSuccess(body.createdCount)
                    } else {
                        onError("服务器返回空响应")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    onError("导入失败: HTTP ${response.code()}${if (!errorBody.isNullOrBlank()) " - $errorBody" else ""}")
                }
            } catch (e: Exception) {
                onError(e.message ?: "未知错误")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadMemberJob?.cancel()
        loadMemoriesJob?.cancel()
        loadGraphJob?.cancel()
        loadMediaJob?.cancel()
    }
}

@HiltViewModel
class CreateMemberViewModel @Inject constructor(
    private val archiveRepository: ArchiveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateMemberUiState())
    val uiState: StateFlow<CreateMemberUiState> = _uiState.asStateFlow()

    fun createMember(
        archiveId: Int,
        name: String,
        relationshipType: String,
        birthYear: Int?,
        endYear: Int?,
        status: String,
        bio: String?
    ) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入成员姓名")
            return
        }

        if (relationshipType.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入关系")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            archiveRepository.createMember(
                archiveId = archiveId,
                name = name,
                relationshipType = relationshipType,
                birthYear = birthYear,
                endYear = if (status == "passed") endYear else null,
                status = status,
                bio = bio
            )
                .onSuccess { member ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        createdMemberId = member.id
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
