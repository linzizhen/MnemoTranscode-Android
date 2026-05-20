package com.mtc.app.ui.screens.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.domain.model.Memory
import com.mtc.app.domain.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoryDetailUiState(
    val isLoading: Boolean = false,
    val memory: Memory? = null,
    val error: String? = null
)

data class CreateMemoryUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

data class MemoryListUiState(
    val isLoading: Boolean = false,
    val memories: List<Memory> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class MemoryDetailViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryDetailUiState())
    val uiState: StateFlow<MemoryDetailUiState> = _uiState.asStateFlow()

    fun loadMemory(memoryId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            memoryRepository.getMemory(memoryId)
                .onSuccess { memory ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        memory = memory
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

    fun deleteMemory(memoryId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(memoryId)
                .onSuccess { onSuccess() }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }
}

@HiltViewModel
class CreateMemoryViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateMemoryUiState())
    val uiState: StateFlow<CreateMemoryUiState> = _uiState.asStateFlow()

    val emotionLabels = listOf(
        "joy" to "😊 喜悦",
        "love" to "❤️ 爱",
        "sadness" to "😢 悲伤",
        "anger" to "😠 愤怒",
        "fear" to "😨 恐惧",
        "surprise" to "😮 惊讶",
        "nostalgia" to "🥹 怀念",
        "gratitude" to "🙏 感激",
        "regret" to "😔 遗憾",
        "peaceful" to "😌 平静"
    )

    fun createMemory(
        memberId: Int,
        title: String,
        contentText: String,
        timestamp: String?,
        location: String?,
        emotionLabel: String?
    ) {
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入记忆标题")
            return
        }

        if (contentText.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入记忆内容")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            memoryRepository.createMemory(
                archiveId = null,
                memberId = memberId,
                title = title,
                contentText = contentText,
                timestamp = timestamp,
                location = location,
                emotionLabel = emotionLabel
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
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

@HiltViewModel
class MemoryListViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryListUiState())
    val uiState: StateFlow<MemoryListUiState> = _uiState.asStateFlow()

    fun loadMemories(memberId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            memoryRepository.getMemories(memberId = memberId)
                .onSuccess { memories ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        memories = memories
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
