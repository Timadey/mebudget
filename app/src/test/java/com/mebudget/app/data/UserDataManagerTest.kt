package com.mebudget.app.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserDataManagerTest {
    private lateinit var budgetDao: BudgetDao
    private lateinit var walletDao: WalletDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var userDataManager: UserDataManager

    @Before
    fun setup() {
        budgetDao = mockk(relaxed = true)
        walletDao = mockk(relaxed = true)
        transactionDao = mockk(relaxed = true)
        userDataManager = UserDataManager(
            budgetDao = budgetDao,
            walletDao = walletDao,
            transactionDao = transactionDao
        )
    }

    @Test
    fun `exportAllData returns valid JSON with expected keys`() = runTest {
        every { budgetDao.observeBudgets() } returns flowOf(emptyList())
        every { walletDao.observeAllWallets() } returns flowOf(emptyList())
        every { transactionDao.observeAllTransactions() } returns flowOf(emptyList())

        val result = userDataManager.exportAllData()

        assertTrue(result.contains("exportDate"))
        assertTrue(result.contains("appVersion"))
        assertTrue(result.contains("budgets"))
        assertTrue(result.contains("wallets"))
        assertTrue(result.contains("transactions"))
    }

    @Test
    fun `deleteAllLocalData clears all tables`() = runTest {
        every { transactionDao.observeAllTransactions() } returns flowOf(emptyList())
        every { walletDao.observeAllWallets() } returns flowOf(emptyList())
        every { budgetDao.observeBudgets() } returns flowOf(emptyList())

        userDataManager.deleteAllLocalData()

        coVerify(exactly = 0) { transactionDao.delete(any()) }
        coVerify(exactly = 0) { walletDao.delete(any()) }
        coVerify(exactly = 0) { budgetDao.delete(any()) }
    }
}
