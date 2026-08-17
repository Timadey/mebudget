package com.mebudget.app.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mebudget.app.data.sync.SyncEngine

class MergeViewModelFactory(
    private val syncEngine: SyncEngine
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MergeViewModel(syncEngine) as T
    }
}