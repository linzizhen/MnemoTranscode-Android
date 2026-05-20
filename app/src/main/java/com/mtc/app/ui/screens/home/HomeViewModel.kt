package com.mtc.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.domain.model.Archive
import com.mtc.app.domain.model.Member
import com.mtc.app.domain.model.User
import com.mtc.app.domain.repository.ArchiveRepository
import com.mtc.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val archives: List<Archive> = emptyList(),
    val membersForDialogue: List<Member> = emptyList(),
    val error: String? = null,
    val isNetworkError: Boolean = false // 网络连接失败标志
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val archiveRepository: ArchiveRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isNetworkError = false)

            // 获取用户信息
            authRepository.getCurrentUser()
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(user = user)
                }

            // 获取档案列表
            archiveRepository.getArchives()
                .onSuccess { archives ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        archives = archives,
                        isNetworkError = false
                    )
                }
                .onFailure { e ->
                    val isNetwork = e is java.net.SocketTimeoutException
                            || e is java.net.UnknownHostException
                            || e is java.net.ConnectException
                            || e.message?.contains("Unable to resolve host", ignoreCase = true) == true
                            || e.message?.contains("timeout", ignoreCase = true) == true
                            || e.message?.contains("connection", ignoreCase = true) == true
                            || e.javaClass.name.contains("Socket", ignoreCase = true)
                            || e.javaClass.name.contains("Connect", ignoreCase = true)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = if (isNetwork) {
                            "无法连接到服务器，请检查网络或服务器配置"
                        } else {
                            e.message ?: "获取数据失败"
                        },
                        isNetworkError = isNetwork
                    )
                }
        }
    }

    /**
     * 加载指定档案的成员列表（用于对话选择）
     */
    fun loadMembersForArchive(archiveId: Int) {
        viewModelScope.launch {
            archiveRepository.getMembers(archiveId)
                .onSuccess { members ->
                    _uiState.value = _uiState.value.copy(membersForDialogue = members)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(membersForDialogue = emptyList())
                }
        }
    }

    fun refresh() {
        loadData()
    }
}
