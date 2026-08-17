package com.mebudget.app.data.sync

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Error(
        val message: String,
        val retryable: Boolean = true
    ) : SyncState()
    data class Pending(val count: Int) : SyncState()
    data class Paused(val reason: String) : SyncState()
}
