package com.mtc.app.ui.screens.profile

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.domain.model.User
import com.mtc.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isUploadingAvatar: Boolean = false,
    val avatarUploadError: String? = null,
    val avatarUploadSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

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

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            val filename = getFileName(uri) ?: "avatar.jpg"
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: run {
                    _uiState.value = _uiState.value.copy(
                        avatarUploadError = "无法读取文件",
                        isUploadingAvatar = false
                    )
                    return@launch
                }

            _uiState.value = _uiState.value.copy(
                isUploadingAvatar = true,
                avatarUploadError = null,
                avatarUploadSuccess = false
            )

            authRepository.uploadAvatar(filename, mimeType, bytes)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingAvatar = false,
                        user = user,
                        avatarUploadSuccess = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingAvatar = false,
                        avatarUploadError = e.message ?: "上传失败"
                    )
                }
        }
    }

    fun deleteAvatar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUploadingAvatar = true,
                avatarUploadError = null,
                avatarUploadSuccess = false
            )

            authRepository.deleteAvatar()
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingAvatar = false,
                        user = user,
                        avatarUploadSuccess = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingAvatar = false,
                        avatarUploadError = e.message ?: "删除失败"
                    )
                }
        }
    }

    fun clearAvatarUploadState() {
        _uiState.value = _uiState.value.copy(
            avatarUploadError = null,
            avatarUploadSuccess = false
        )
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }

    private fun getFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }
}
