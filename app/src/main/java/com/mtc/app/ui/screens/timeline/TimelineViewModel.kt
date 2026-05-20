package com.mtc.app.ui.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.domain.model.Archive
import com.mtc.app.domain.model.Memory
import com.mtc.app.domain.model.TimelineEntry
import com.mtc.app.domain.repository.ArchiveRepository
import com.mtc.app.domain.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimelineUiState(
    val isLoading: Boolean = false,
    val archives: List<Archive> = emptyList(),
    val selectedArchiveId: Int? = null,
    val timeline: List<TimelineEntry> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val archiveRepository: ArchiveRepository,
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

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
                    if (archives.isNotEmpty() && _uiState.value.selectedArchiveId == null) {
                        selectArchive(archives.first().id)
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

    fun selectArchive(archiveId: Int) {
        _uiState.value = _uiState.value.copy(selectedArchiveId = archiveId)
        loadTimeline(archiveId)
    }

    private fun loadTimeline(archiveId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            archiveRepository.getMembers(archiveId)
                .onSuccess { members ->
                    val allMemories = mutableListOf<Pair<String, Memory>>()
                    for (member in members) {
                        memoryRepository.getMemories(memberId = member.id)
                            .onSuccess { memories ->
                                allMemories.addAll(memories.map { memory ->
                                    val year = memory.timestamp?.take(4) ?: memory.createdAt.take(4)
                                    year to memory
                                })
                            }
                    }

                    val timelineByYear = allMemories
                        .groupBy { it.first }
                        .map { (year, memories) ->
                            TimelineEntry(
                                title = "${year}年",
                                year = year.toIntOrNull(),
                                description = "${memories.size} 条记忆",
                                memoryIds = memories.map { it.second.id }
                            )
                        }
                        .sortedBy { it.year }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        timeline = timelineByYear
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
