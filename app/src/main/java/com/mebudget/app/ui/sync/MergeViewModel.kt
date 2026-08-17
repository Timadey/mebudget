package com.mebudget.app.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebudget.app.data.sync.SyncDataCounts
import com.mebudget.app.data.sync.SyncEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MergeUiState(
    val localCounts: SyncDataCounts = SyncDataCounts(0, 0, 0),
    val cloudCounts: SyncDataCounts = SyncDataCounts(0, 0, 0),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false
)

enum class MergeOption {
    KEEP_LOCAL,
    KEEP_CLOUD,
    MERGE
}

class MergeViewModel(
    private val syncEngine: SyncEngine
) : ViewModel() {
    private val _uiState = MutableStateFlow(MergeUiState())
    val uiState: StateFlow<MergeUiState> = _uiState

    init {
        loadDataCounts()
    }

    private fun loadDataCounts() {
        viewModelScope.launch {
            val local = syncEngine.getLocalDataCounts()
            val cloud = syncEngine.getCloudDataCounts()
            _uiState.value = _uiState.value.copy(
                localCounts = local,
                cloudCounts = cloud
            )
        }
    }

    fun selectMergeOption(option: MergeOption) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = when (option) {
                MergeOption.KEEP_LOCAL -> syncEngine.uploadLocalData()
                MergeOption.KEEP_CLOUD -> syncEngine.downloadCloudData()
                MergeOption.MERGE -> syncEngine.mergeLocalAndCloud()
            }
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isComplete = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isComplete = false,
                        error = e.message ?: "Sync failed"
                    )
                }
            )
        }
    }

    fun dismiss() {
        _uiState.value = _uiState.value.copy(error = null, isComplete = false)
    }
}