package com.mebudget.app.data.sync

import com.mebudget.app.data.BudgetEntity
import com.mebudget.app.data.TransactionEntity
import com.mebudget.app.data.WalletEntity

/**
 * Resolves local-vs-remote conflicts with a last-write-wins rule.
 *
 * The local row's last write is tracked as `sync_metadata.lastSyncedAtMillis`
 * (the moment the local row was last pushed); the remote row's write time is
 * the server's `updatedAtMillis` that comes back on every fetched record. When
 * the remote is newer its entity wins, but it keeps the local Room `id` so all
 * local relations (parent budget, wallet ids, transaction references) stay
 * intact.
 */
class ConflictResolver {

    /** True when the local copy is the more recent write and should win. */
    fun shouldLocalWin(
        localLastSyncedAtMillis: Long?,
        remoteUpdatedAtMillis: Long
    ): Boolean = (localLastSyncedAtMillis ?: 0L) >= remoteUpdatedAtMillis

    fun resolveBudgetConflict(
        local: BudgetEntity,
        remote: BudgetEntity,
        localLastSyncedAtMillis: Long?,
        remoteUpdatedAtMillis: Long
    ): BudgetEntity = if (shouldLocalWin(localLastSyncedAtMillis, remoteUpdatedAtMillis)) {
        local
    } else {
        remote.copy(id = local.id)
    }

    fun resolveWalletConflict(
        local: WalletEntity,
        remote: WalletEntity,
        localLastSyncedAtMillis: Long?,
        remoteUpdatedAtMillis: Long
    ): WalletEntity = if (shouldLocalWin(localLastSyncedAtMillis, remoteUpdatedAtMillis)) {
        local
    } else {
        remote.copy(id = local.id)
    }

    fun resolveTransactionConflict(
        local: TransactionEntity,
        remote: TransactionEntity,
        localLastSyncedAtMillis: Long?,
        remoteUpdatedAtMillis: Long
    ): TransactionEntity = if (shouldLocalWin(localLastSyncedAtMillis, remoteUpdatedAtMillis)) {
        local
    } else {
        remote.copy(id = local.id)
    }
}