package com.mebudget.app.data.sync

import com.google.gson.JsonObject
import com.mebudget.app.data.AppDatabase
import com.mebudget.app.data.BudgetEntity
import com.mebudget.app.data.NegativeBalanceRule
import com.mebudget.app.data.TransactionEntity
import com.mebudget.app.data.WalletEntity
import com.mebudget.app.data.SyncEntityType
import com.mebudget.app.data.SyncMetadataEntity
import com.mebudget.app.data.auth.AuthManager
import com.mebudget.app.data.sync.models.PocketBaseListResponse
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Offline-first sync engine for budgets, wallets and transactions.
 *
 * Local Room rows keep their numeric ids; [SyncMetadataEntity] maps each local
 * row to its PocketBase record id. Push runs in dependency order so wallets and
 * transactions can reference the remote ids of their parents. Pull remaps
 * remote relation ids back to local ids and applies last-write-wins based on
 * the remote `updatedAtMillis` vs the last time the row was synced.
 */
class SyncEngine(
    private val pocketBaseClient: PocketBaseClient,
    private val authManager: AuthManager,
    private val database: AppDatabase,
    private val realtimeListener: RealtimeListener,
    private val conflictResolver: ConflictResolver = ConflictResolver()
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val budgetDao get() = database.budgetDao()
    private val walletDao get() = database.walletDao()
    private val transactionDao get() = database.transactionDao()
    private val metadataDao get() = database.syncMetadataDao()

    /** Starts live push updates from the server. Safe to call at any time. */
    fun startRealtimeUpdates() {
        realtimeListener.startListening()
    }

    /** Stops the live stream until [startRealtimeUpdates] is called again. */
    fun stopRealtimeUpdates() {
        realtimeListener.stopListening()
    }

    /** True while the realtime SSE stream is connected. */
    val isListening get() = realtimeListener.isListening

    // ------------------------------------------------------------------
    // First-sync merge support
    // ------------------------------------------------------------------

    /** Counts of budgets/wallets/transactions stored locally on this device. */
    suspend fun getLocalDataCounts(): SyncDataCounts = SyncDataCounts(
        budgets = budgetDao.count(),
        wallets = walletDao.count(),
        transactions = transactionDao.count()
    )

    /** Counts of budgets/wallets/transactions currently on the server. */
    suspend fun getCloudDataCounts(): SyncDataCounts {
        val auth = authManager.authState.first()
        if (auth !is com.mebudget.app.data.auth.AuthState.SignedIn) {
            return SyncDataCounts(0, 0, 0)
        }
        return SyncDataCounts(
            budgets = fetchAll("budgets").size,
            wallets = fetchAll("wallets").size,
            transactions = fetchAll("transactions").size
        )
    }

    /** Push this device's data to the cloud, leaving nothing else to merge. */
    suspend fun uploadLocalData(): Result<Unit> = runMerge {
        pushLocalChanges()
    }

    /** Replace local data with whatever is on the server. */
    suspend fun downloadCloudData(): Result<Unit> = runMerge {
        budgetDao.clearAll()
        walletDao.clearAll()
        transactionDao.clearAll()
        metadataDao.clearAll()
        pullRemoteChanges()
    }

    /** Combine local and cloud data using last-write-wins conflict resolution. */
    suspend fun mergeLocalAndCloud(): Result<Unit> = runMerge {
        pushLocalChanges()
        pullRemoteChanges()
    }

    private suspend fun runMerge(block: suspend () -> Unit): Result<Unit> {
        _syncState.value = SyncState.Syncing
        return try {
            block()
            _syncState.value = SyncState.Idle
            Result.success(Unit)
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Merge failed")
            Result.failure(e)
        }
    }

    suspend fun syncNow(): Result<Unit> {
        val auth = authManager.authState.first()
        if (auth !is com.mebudget.app.data.auth.AuthState.SignedIn) {
            _syncState.value = SyncState.Paused(reason = "Not signed in")
            return Result.failure(IllegalStateException("Not signed in"))
        }

        _syncState.value = SyncState.Syncing

        return try {
            pushLocalChanges()
            pullRemoteChanges()
            _syncState.value = SyncState.Idle
            Result.success(Unit)
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Push
    // ------------------------------------------------------------------

    private suspend fun pushLocalChanges() {
        val auth = authManager.authState.first()
        val userId = (auth as? com.mebudget.app.data.auth.AuthState.SignedIn)?.userId ?: return
        pushBudgets(userId)
        pushWallets(userId)
        pushTransactions(userId)
        cleanupDeletedLocally()
    }

    private suspend fun pushBudgets(userId: String) {
        for (budget in budgetDao.getAllBudgets()) {
            val meta = metadataDao.getByLocalId(SyncEntityType.BUDGET, budget.id)
            val body = budgetToBody(budget, userId)
            val remoteId = if (meta?.remoteId != null) {
                pocketBaseClient.api.update("budgets", meta.remoteId, body).get("id").asString
            } else {
                pocketBaseClient.api.create("budgets", body).get("id").asString
            }
            saveMetadata(SyncEntityType.BUDGET, budget.id, remoteId)
        }
    }

    private suspend fun pushWallets(userId: String) {
        val budgetRemoteByLocal = remoteBudgetMap()
        for (wallet in walletDao.getAllWallets()) {
            val remoteBudgetId = budgetRemoteByLocal[wallet.budgetId] ?: continue
            val meta = metadataDao.getByLocalId(SyncEntityType.WALLET, wallet.id)
            val body = walletToBody(wallet, remoteBudgetId, userId)
            val remoteId = if (meta?.remoteId != null) {
                pocketBaseClient.api.update("wallets", meta.remoteId, body).get("id").asString
            } else {
                pocketBaseClient.api.create("wallets", body).get("id").asString
            }
            saveMetadata(SyncEntityType.WALLET, wallet.id, remoteId)
        }
    }

    private suspend fun pushTransactions(userId: String) {
        val budgetRemoteByLocal = remoteBudgetMap()
        val walletRemoteByLocal = remoteWalletMap()
        for (transaction in transactionDao.getAllTransactions()) {
            val remoteBudgetId = budgetRemoteByLocal[transaction.budgetId] ?: continue
            val meta = metadataDao.getByLocalId(SyncEntityType.TRANSACTION, transaction.id)
            val body = transactionToBody(transaction, remoteBudgetId, walletRemoteByLocal, userId)
            val remoteId = if (meta?.remoteId != null) {
                pocketBaseClient.api.update("transactions", meta.remoteId, body).get("id").asString
            } else {
                pocketBaseClient.api.create("transactions", body).get("id").asString
            }
            saveMetadata(SyncEntityType.TRANSACTION, transaction.id, remoteId)
        }
    }

    /** Remote rows whose local row no longer exists are deleted from the server. */
    private suspend fun cleanupDeletedLocally() {
        for (meta in metadataDao.getAll()) {
            val stillExists = when (meta.entityType) {
                SyncEntityType.BUDGET -> budgetDao.getBudget(meta.localId) != null
                SyncEntityType.WALLET -> walletDao.getWallet(meta.localId) != null
                SyncEntityType.TRANSACTION -> transactionDao.getTransaction(meta.localId) != null
                else -> true
            }
            if (!stillExists) {
                if (meta.remoteId != null) {
                    pocketBaseClient.api.delete(meta.entityType.pluralize(), meta.remoteId)
                }
                metadataDao.delete(meta)
            }
        }
    }

    // ------------------------------------------------------------------
    // Pull
    // ------------------------------------------------------------------

    private suspend fun pullRemoteChanges() {
        pullBudgets()
        pullWallets()
        pullTransactions()
    }

    private suspend fun pullBudgets() {
        for (rec in fetchAll("budgets")) {
            val meta = metadataDao.getByRemoteId(SyncEntityType.BUDGET, rec.remoteId)
            val deleted = rec.safeGetBool("deleted")
            if (deleted) {
                val localId = meta?.localId ?: continue
                budgetDao.getBudget(localId)?.let { budgetDao.delete(it) }
                meta?.let { metadataDao.delete(it) }
                continue
            }
            val remoteUpdated = rec.safeGetLong("updatedAtMillis") ?: 0L
            val localId = if (meta != null) {
                val existing = budgetDao.getBudget(meta.localId)
                if (existing != null) {
                    budgetDao.update(
                        conflictResolver.resolveBudgetConflict(
                            local = existing,
                            remote = bodyToBudget(rec, null),
                            localLastSyncedAtMillis = meta.lastSyncedAtMillis,
                            remoteUpdatedAtMillis = remoteUpdated
                        )
                    )
                } else {
                    budgetDao.insert(bodyToBudget(rec, meta.localId))
                }
                meta.localId
            } else {
                budgetDao.insert(bodyToBudget(rec, null))
            }
            saveMetadata(SyncEntityType.BUDGET, localId, rec.remoteId)
        }
    }

    private suspend fun pullWallets() {
        val budgetLocalByRemote = localBudgetByRemoteMap()
        for (rec in fetchAll("wallets")) {
            val meta = metadataDao.getByRemoteId(SyncEntityType.WALLET, rec.remoteId)
            val deleted = rec.safeGetBool("deleted")
            if (deleted) {
                val localId = meta?.localId ?: continue
                walletDao.getWallet(localId)?.let { walletDao.delete(it) }
                meta?.let { metadataDao.delete(it) }
                continue
            }
            val remoteBudgetId = rec.safeGetString("budgetId") ?: continue
            val localBudgetId = budgetLocalByRemote[remoteBudgetId] ?: continue
            val remoteUpdated = rec.safeGetLong("updatedAtMillis") ?: 0L
            val localId = if (meta != null) {
                val existing = walletDao.getWallet(meta.localId)
                if (existing != null) {
                    walletDao.update(
                        conflictResolver.resolveWalletConflict(
                            local = existing,
                            remote = bodyToWallet(rec, null, localBudgetId),
                            localLastSyncedAtMillis = meta.lastSyncedAtMillis,
                            remoteUpdatedAtMillis = remoteUpdated
                        )
                    )
                } else {
                    walletDao.insert(bodyToWallet(rec, null, localBudgetId))
                }
                meta.localId
            } else {
                walletDao.insert(bodyToWallet(rec, null, localBudgetId))
            }
            saveMetadata(SyncEntityType.WALLET, localId, rec.remoteId)
        }
    }

    private suspend fun pullTransactions() {
        val budgetLocalByRemote = localBudgetByRemoteMap()
        val walletLocalByRemote = localWalletByRemoteMap()
        for (rec in fetchAll("transactions")) {
            val meta = metadataDao.getByRemoteId(SyncEntityType.TRANSACTION, rec.remoteId)
            val deleted = rec.safeGetBool("deleted")
            if (deleted) {
                val localId = meta?.localId ?: continue
                transactionDao.getTransaction(localId)?.let { transactionDao.delete(it) }
                meta?.let { metadataDao.delete(it) }
                continue
            }
            val remoteBudgetId = rec.safeGetString("budgetId") ?: continue
            val localBudgetId = budgetLocalByRemote[remoteBudgetId] ?: continue
            val remoteUpdated = rec.safeGetLong("updatedAtMillis") ?: 0L
            val localId = if (meta != null) {
                val existing = transactionDao.getTransaction(meta.localId)
                if (existing != null) {
                    transactionDao.update(
                        conflictResolver.resolveTransactionConflict(
                            local = existing,
                            remote = bodyToTransaction(rec, null, localBudgetId, walletLocalByRemote),
                            localLastSyncedAtMillis = meta.lastSyncedAtMillis,
                            remoteUpdatedAtMillis = remoteUpdated
                        )
                    )
                } else {
                    transactionDao.insert(bodyToTransaction(rec, null, localBudgetId, walletLocalByRemote))
                }
                meta.localId
            } else {
                transactionDao.insert(bodyToTransaction(rec, null, localBudgetId, walletLocalByRemote))
            }
            saveMetadata(SyncEntityType.TRANSACTION, localId, rec.remoteId)
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private suspend fun saveMetadata(entityType: String, localId: Long, remoteId: String) {
        val existing = metadataDao.getByLocalId(entityType, localId)
        val updated = (existing ?: SyncMetadataEntity(entityType = entityType, localId = localId))
            .copy(
                remoteId = remoteId,
                deleted = false,
                lastSyncedAtMillis = System.currentTimeMillis(),
                lastError = null
            )
        if (existing != null) metadataDao.update(updated) else metadataDao.insert(updated)
    }

    private suspend fun fetchAll(collection: String): List<JsonObject> {
        val items = mutableListOf<JsonObject>()
        var page = 1
        while (true) {
            val response: PocketBaseListResponse<JsonObject> = pocketBaseClient.api.getList(
                collection = collection,
                page = page,
                perPage = 200,
                sort = "updatedAtMillis"
            )
            items += response.items
            if (page >= response.totalPages) break
            page++
        }
        return items
    }

    private suspend fun remoteBudgetMap(): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        for (budget in budgetDao.getAllBudgets()) {
            metadataDao.getByLocalId(SyncEntityType.BUDGET, budget.id)
                ?.remoteId?.let { map[budget.id] = it }
        }
        return map
    }

    private suspend fun remoteWalletMap(): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        for (wallet in walletDao.getAllWallets()) {
            metadataDao.getByLocalId(SyncEntityType.WALLET, wallet.id)
                ?.remoteId?.let { map[wallet.id] = it }
        }
        return map
    }

    private suspend fun localBudgetByRemoteMap(): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        for (meta in metadataDao.getByEntityType(SyncEntityType.BUDGET)) {
            meta.remoteId?.let { map[it] = meta.localId }
        }
        return map
    }

    private suspend fun localWalletByRemoteMap(): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        for (meta in metadataDao.getByEntityType(SyncEntityType.WALLET)) {
            meta.remoteId?.let { map[it] = meta.localId }
        }
        return map
    }

    private fun budgetToBody(budget: BudgetEntity, userId: String): JsonObject = JsonObject().apply {
        addProperty("userId", userId)
        addProperty("name", budget.name)
        budget.startDateEpochDay?.let { addProperty("startDateEpochDay", it) }
        budget.endDateEpochDay?.let { addProperty("endDateEpochDay", it) }
        addProperty("negativeBalanceRule", budget.negativeBalanceRule.name)
        addProperty("createdAtMillis", budget.createdAtMillis)
        addProperty("updatedAtMillis", System.currentTimeMillis())
        addProperty("deleted", false)
    }

    private fun walletToBody(wallet: WalletEntity, remoteBudgetId: String, userId: String): JsonObject = JsonObject().apply {
        addProperty("userId", userId)
        addProperty("budgetId", remoteBudgetId)
        addProperty("name", wallet.name)
        addProperty("plannedAmount", wallet.plannedAmount)
        addProperty("sortOrder", wallet.sortOrder)
        addProperty("archived", wallet.archived)
        addProperty("updatedAtMillis", System.currentTimeMillis())
        addProperty("deleted", false)
    }

    private fun transactionToBody(
        transaction: TransactionEntity,
        remoteBudgetId: String,
        walletRemoteByLocal: Map<Long, String>,
        userId: String
    ): JsonObject = JsonObject().apply {
        addProperty("userId", userId)
        addProperty("budgetId", remoteBudgetId)
        addProperty("type", transaction.type.name)
        addProperty("amount", transaction.amount)
        addProperty("dateEpochDay", transaction.dateEpochDay)
        transaction.sourceWalletId?.let { walletRemoteByLocal[it]?.let { w -> addProperty("sourceWalletId", w) } }
        transaction.destinationWalletId?.let { walletRemoteByLocal[it]?.let { w -> addProperty("destinationWalletId", w) } }
        transaction.note?.let { addProperty("note", it) }
        addProperty("createdAtMillis", transaction.createdAtMillis)
        addProperty("updatedAtMillis", System.currentTimeMillis())
        addProperty("deleted", false)
    }

    private fun bodyToBudget(rec: JsonObject, localId: Long?): BudgetEntity = BudgetEntity(
        id = localId ?: 0L,
        name = rec.safeGetString("name") ?: "",
        startDateEpochDay = rec.safeGetLong("startDateEpochDay"),
        endDateEpochDay = rec.safeGetLong("endDateEpochDay"),
        negativeBalanceRule = rec.safeGetString("negativeBalanceRule")
            ?.let { runCatching { NegativeBalanceRule.valueOf(it) }.getOrNull() }
            ?: NegativeBalanceRule.WARN,
        createdAtMillis = rec.safeGetLong("createdAtMillis") ?: System.currentTimeMillis()
    )

    private fun bodyToWallet(rec: JsonObject, localId: Long?, localBudgetId: Long): WalletEntity = WalletEntity(
        id = localId ?: 0L,
        budgetId = localBudgetId,
        name = rec.safeGetString("name") ?: "",
        plannedAmount = rec.safeGetLong("plannedAmount") ?: 0L,
        sortOrder = rec.safeGetInt("sortOrder") ?: 0,
        archived = rec.safeGetBool("archived")
    )

    private fun bodyToTransaction(
        rec: JsonObject,
        localId: Long?,
        localBudgetId: Long,
        walletLocalByRemote: Map<String, Long>
    ): TransactionEntity = TransactionEntity(
        id = localId ?: 0L,
        budgetId = localBudgetId,
        type = rec.safeGetString("type")
            ?.let { runCatching { com.mebudget.app.data.TransactionType.valueOf(it) }.getOrNull() }
            ?: com.mebudget.app.data.TransactionType.EXPENSE,
        amount = rec.safeGetLong("amount") ?: 0L,
        dateEpochDay = rec.safeGetLong("dateEpochDay") ?: 0L,
        sourceWalletId = rec.safeGetString("sourceWalletId")?.let { walletLocalByRemote[it] },
        destinationWalletId = rec.safeGetString("destinationWalletId")?.let { walletLocalByRemote[it] },
        note = rec.safeGetString("note"),
        createdAtMillis = rec.safeGetLong("createdAtMillis") ?: System.currentTimeMillis()
    )

    private fun JsonObject.safeGetString(key: String): String? =
        if (has(key) && !get(key).isJsonNull) get(key).asString else null

    private val JsonObject.remoteId: String
        get() = safeGetString("id") ?: ""

    private fun JsonObject.safeGetLong(key: String): Long? =
        if (has(key) && !get(key).isJsonNull) get(key).asLong else null

    private fun JsonObject.safeGetInt(key: String): Int? =
        if (has(key) && !get(key).isJsonNull) get(key).asInt else null

    private fun JsonObject.safeGetBool(key: String): Boolean =
        has(key) && !get(key).isJsonNull && get(key).asBoolean

    private fun String.pluralize(): String = when (this) {
        SyncEntityType.BUDGET -> "budgets"
        SyncEntityType.WALLET -> "wallets"
        SyncEntityType.TRANSACTION -> "transactions"
        else -> this
    }
}

/** Counts of synced rows on one side (device or cloud) of a first-time merge. */
data class SyncDataCounts(
    val budgets: Int,
    val wallets: Int,
    val transactions: Int
)
