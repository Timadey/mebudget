package com.mebudget.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Maps a local Room row to its PocketBase record and tracks push state.
 *
 * Wallets and transactions reference other local rows by their numeric id, so a
 * synced record keeps its local `id` untouched and only records the remote id.
 * Cross-references are re-mapped on pull (remote budgetId -> local budget id).
 */
@Entity(
    tableName = "sync_metadata",
    indices = [
        Index("localId"),
        Index("remoteId"),
        Index("entityType")
    ]
)
data class SyncMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String, // "budget", "wallet", "transaction"
    val localId: Long,
    val remoteId: String? = null,
    val deleted: Boolean = false,
    val lastSyncedAtMillis: Long? = null,
    val lastError: String? = null
)

object SyncEntityType {
    const val BUDGET = "budget"
    const val WALLET = "wallet"
    const val TRANSACTION = "transaction"
}
