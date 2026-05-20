package com.mtc.app.ui.screens.capsule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.domain.model.Archive
import com.mtc.app.domain.model.Capsule
import com.mtc.app.domain.model.Member
import com.mtc.app.domain.repository.ArchiveRepository
import com.mtc.app.domain.repository.CapsuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CapsuleListUiState(
    val isLoading: Boolean = false,
    val capsules: List<Capsule> = emptyList(),
    val selectedCapsule: Capsule? = null,
    val archives: List<Archive> = emptyList(),
    val isCreating: Boolean = false,
    val createSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CapsuleListViewModel @Inject constructor(
    private val capsuleRepository: CapsuleRepository,
    private val archiveRepository: ArchiveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CapsuleListUiState())
    val uiState: StateFlow<CapsuleListUiState> = _uiState.asStateFlow()

    init {
        loadCapsules()
    }

    fun loadCapsules(memberId: Int? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            capsuleRepository.getCapsules(memberId)
                .onSuccess { capsules ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        capsules = capsules
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

    fun loadArchives() {
        viewModelScope.launch {
            archiveRepository.getArchives()
                .onSuccess { archives ->
                    _uiState.value = _uiState.value.copy(archives = archives)
                }
        }
    }

    fun loadMembersForArchive(archiveId: Int, onLoaded: (List<Member>) -> Unit) {
        viewModelScope.launch {
            archiveRepository.getMembers(archiveId)
                .onSuccess { members ->
                    onLoaded(members)
                }
                .onFailure {
                    onLoaded(emptyList())
                }
        }
    }

    fun selectCapsule(capsuleId: Int) {
        viewModelScope.launch {
            capsuleRepository.getCapsule(capsuleId)
                .onSuccess { capsule ->
                    _uiState.value = _uiState.value.copy(selectedCapsule = capsule)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun clearSelectedCapsule() {
        _uiState.value = _uiState.value.copy(selectedCapsule = null)
    }

    fun createCapsule(
        memberId: Int,
        title: String,
        content: String,
        unlockDate: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, error = null)
            capsuleRepository.createCapsule(memberId, title, content, unlockDate)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        createSuccess = true
                    )
                    loadCapsules()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        error = e.message
                    )
                }
        }
    }

    fun resetCreateSuccess() {
        _uiState.value = _uiState.value.copy(createSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
