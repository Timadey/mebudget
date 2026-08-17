package com.mebudget.app.data.sync

import com.google.gson.JsonObject
import com.mebudget.app.data.AppDatabase
import com.mebudget.app.data.BudgetDao
import com.mebudget.app.data.BudgetEntity
import com.mebudget.app.data.NegativeBalanceRule
import com.mebudget.app.data.SyncEntityType
import com.mebudget.app.data.SyncMetadataDao
import com.mebudget.app.data.SyncMetadataEntity
import com.mebudget.app.data.TransactionDao
import com.mebudget.app.data.WalletDao
import com.mebudget.app.data.auth.AuthManager
import com.mebudget.app.data.auth.AuthState
import com.mebudget.app.data.sync.models.PocketBaseListResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncEngineTest {

    private lateinit var api: PocketBaseApi
    private lateinit var client: PocketBaseClient
    private lateinit var authManager: AuthManager
    private lateinit var database: AppDatabase
    private lateinit var budgetDao: BudgetDao
    private lateinit var walletDao: WalletDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var metadataDao: SyncMetadataDao
    private lateinit var syncEngine: SyncEngine

    private val signedIn = AuthState.SignedIn("user1", "a@b.com", "Ada")

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        client = mockk(relaxed = true)
        every { client.api } returns api

        authManager = mockk(relaxed = true)
        every { authManager.authState } returns MutableStateFlow(signedIn)

        database = mockk(relaxed = true)
        budgetDao = mockk(relaxed = true)
        walletDao = mockk(relaxed = true)
        transactionDao = mockk(relaxed = true)
        metadataDao = mockk(relaxed = true)
        every { database.budgetDao() } returns budgetDao
        every { database.walletDao() } returns walletDao
        every { database.transactionDao() } returns transactionDao
        every { database.syncMetadataDao() } returns metadataDao

        syncEngine = SyncEngine(client, authManager, database, RealtimeListener(client))
    }

    @Test
    fun `initial sync state is idle`() {
        assertTrue(syncEngine.syncState.value is SyncState.Idle)
    }

    @Test
    fun `syncNow fails when not signed in`() = runTest {
        every { authManager.authState } returns MutableStateFlow(AuthState.NotSignedIn)

        val result = syncEngine.syncNow()

        assertTrue(result.isFailure)
        assertTrue(syncEngine.syncState.value is SyncState.Paused)
    }

    @Test
    fun `syncNow creates a new remote budget and saves metadata`() = runTest {
        coEvery { budgetDao.getAllBudgets() } returns listOf(
            BudgetEntity(id = 1, name = "Groceries", createdAtMillis = 100L)
        )
        coEvery { metadataDao.getByLocalId(SyncEntityType.BUDGET, 1) } returns null
        coEvery { api.create("budgets", any()) } returns JsonObject().apply { addProperty("id", "rec_1") }

        val result = syncEngine.syncNow()

        assertTrue(result.isSuccess)
        assertTrue(syncEngine.syncState.value is SyncState.Idle)
        coVerify { api.create("budgets", any()) }
        coVerify { metadataDao.insert(any()) }
    }

    @Test
    fun `syncNow updates an existing remote budget`() = runTest {
        coEvery { budgetDao.getAllBudgets() } returns listOf(
            BudgetEntity(id = 1, name = "Groceries", createdAtMillis = 100L)
        )
        coEvery { metadataDao.getByLocalId(SyncEntityType.BUDGET, 1) } returns
            SyncMetadataEntity(id = 9, entityType = SyncEntityType.BUDGET, localId = 1, remoteId = "rec_1")

        syncEngine.syncNow()

        coVerify { api.update("budgets", "rec_1", any()) }
        coVerify(exactly = 0) { api.create("budgets", any()) }
    }

    @Test
    fun `pull inserts remote budget that has no local match`() = runTest {
        coEvery { budgetDao.getAllBudgets() } returns emptyList()
        coEvery { walletDao.getAllWallets() } returns emptyList()
        coEvery { transactionDao.getAllTransactions() } returns emptyList()
        coEvery { metadataDao.getAll() } returns emptyList()
        coEvery { metadataDao.getByRemoteId(SyncEntityType.BUDGET, any()) } returns null
        coEvery { metadataDao.getByRemoteId(SyncEntityType.WALLET, any()) } returns null
        coEvery { metadataDao.getByRemoteId(SyncEntityType.TRANSACTION, any()) } returns null
        coEvery { metadataDao.getByLocalId(SyncEntityType.BUDGET, any()) } returns null
        coEvery { metadataDao.getByLocalId(SyncEntityType.WALLET, any()) } returns null
        coEvery { metadataDao.getByLocalId(SyncEntityType.TRANSACTION, any()) } returns null
        coEvery { budgetDao.insert(any()) } returns 42L

        val remote = JsonObject().apply {
            addProperty("id", "rec_remote")
            addProperty("name", "Vacation")
            addProperty("negativeBalanceRule", "ALLOW")
            addProperty("createdAtMillis", 500L)
            addProperty("updatedAtMillis", 500L)
            addProperty("deleted", false)
        }
        coEvery { api.getList(collection = "budgets", page = 1, perPage = 200, filter = any(), sort = "updatedAtMillis") } returns
            PocketBaseListResponse(page = 1, perPage = 200, totalItems = 1, totalPages = 1, items = listOf(remote))
        coEvery { api.getList(collection = "wallets", page = 1, perPage = 200, filter = any(), sort = "updatedAtMillis") } returns
            PocketBaseListResponse(1, 200, 0, 0, emptyList())
        coEvery { api.getList(collection = "transactions", page = 1, perPage = 200, filter = any(), sort = "updatedAtMillis") } returns
            PocketBaseListResponse(1, 200, 0, 0, emptyList())

        val result = syncEngine.syncNow()

        assertTrue(result.isSuccess)
        coVerify { budgetDao.insert(any()) }
        coVerify { metadataDao.insert(match { it.remoteId == "rec_remote" }) }
    }

    @Test
    fun `pull respects last write wins`() = runTest {
        coEvery { budgetDao.getAllBudgets() } returns emptyList()
        coEvery { walletDao.getAllWallets() } returns emptyList()
        coEvery { transactionDao.getAllTransactions() } returns emptyList()
        coEvery { metadataDao.getAll() } returns emptyList()
        coEvery { metadataDao.getByRemoteId(SyncEntityType.BUDGET, any()) } returns null
        coEvery { metadataDao.getByRemoteId(SyncEntityType.WALLET, any()) } returns null
        coEvery { metadataDao.getByRemoteId(SyncEntityType.TRANSACTION, any()) } returns null

        val remote = JsonObject().apply {
            addProperty("id", "rec_remote")
            addProperty("name", "RemoteName")
            addProperty("negativeBalanceRule", "WARN")
            addProperty("createdAtMillis", 100L)
            addProperty("updatedAtMillis", 900L)
            addProperty("deleted", false)
        }
        coEvery { api.getList(collection = "budgets", page = 1, perPage = 200, filter = any(), sort = "updatedAtMillis") } returns
            PocketBaseListResponse(1, 200, 1, 1, listOf(remote))
        coEvery { api.getList(collection = "wallets", page = 1, perPage = 200, filter = any(), sort = "updatedAtMillis") } returns
            PocketBaseListResponse(1, 200, 0, 0, emptyList())
        coEvery { api.getList(collection = "transactions", page = 1, perPage = 200, filter = any(), sort = "updatedAtMillis") } returns
            PocketBaseListResponse(1, 200, 0, 0, emptyList())

        syncEngine.syncNow()

        coVerify { budgetDao.insert(match { it.name == "RemoteName" }) }
    }
}
