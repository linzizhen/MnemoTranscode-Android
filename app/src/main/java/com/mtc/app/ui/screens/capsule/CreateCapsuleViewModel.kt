package com.mtc.app.ui.screens.capsule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.domain.repository.CapsuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateCapsuleUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CreateCapsuleViewModel @Inject constructor(
    private val capsuleRepository: CapsuleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateCapsuleUiState())
    val uiState: StateFlow<CreateCapsuleUiState> = _uiState.asStateFlow()

    fun createCapsule(
        memberId: Int,
        title: String,
        content: String,
        unlockDate: String
    ) {
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入标题")
            return
        }
        if (content.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入内容")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            capsuleRepository.createCapsule(memberId, title, content, unlockDate)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "创建失败"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
