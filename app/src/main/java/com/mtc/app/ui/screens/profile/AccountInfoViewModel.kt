package com.mtc.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.domain.model.User
import com.mtc.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountInfoUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class AccountInfoViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountInfoUiState())
    val uiState: StateFlow<AccountInfoUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            authRepository.getCurrentUser()
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user
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

    fun updateUsername(newUsername: String) {
        // TODO: 调用后端 API 更新用户名
        _uiState.value = _uiState.value.copy(
            error = "用户名修改功能开发中"
        )
    }

    fun updateEmail(newEmail: String) {
        // TODO: 调用后端 API 更新邮箱
        _uiState.value = _uiState.value.copy(
            error = "邮箱修改功能开发中"
        )
    }

    fun updatePassword(oldPassword: String, newPassword: String) {
        // TODO: 调用后端 API 更新密码
        _uiState.value = _uiState.value.copy(
            error = "密码修改功能开发中"
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}
