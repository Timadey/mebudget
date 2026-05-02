package com.mebudget.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetAnalyticsTest {

    @Test
    fun `compute balances applies expense transfer and adjustment in chronological order`() {
        val wallets = listOf(
            WalletEntity(id = 1, budgetId = 1, name = "Food", plannedAmount = 10_000, sortOrder = 0),
            WalletEntity(id = 2, budgetId = 1, name = "Bills", plannedAmount = 5_000, sortOrder = 1)
        )
        val transactions = listOf(
            TransactionEntity(id = 3, budgetId = 1, type = TransactionType.TRANSFER, amount = 2_000, dateEpochDay = 3, sourceWalletId = 1, destinationWalletId = 2),
            TransactionEntity(id = 2, budgetId = 1, type = TransactionType.ADJUSTMENT, amount = -1_000, dateEpochDay = 2, sourceWalletId = 1),
            TransactionEntity(id = 1, budgetId = 1, type = TransactionType.EXPENSE, amount = 3_000, dateEpochDay = 1, sourceWalletId = 1)
        )

        val balances = computeBalances(wallets, transactions)

        assertEquals(4_000L, balances[1])
        assertEquals(7_000L, balances[2])
    }

    @Test
    fun `compute budget insights flags overspent wallet and top transfer path`() {
        val wallets = listOf(
            WalletEntity(id = 1, budgetId = 1, name = "Food", plannedAmount = 2_000, sortOrder = 0),
            WalletEntity(id = 2, budgetId = 1, name = "Buffer", plannedAmount = 5_000, sortOrder = 1)
        )
        val transactions = listOf(
            TransactionEntity(id = 1, budgetId = 1, type = TransactionType.EXPENSE, amount = 5_000, dateEpochDay = 1, sourceWalletId = 1),
            TransactionEntity(id = 2, budgetId = 1, type = TransactionType.TRANSFER, amount = 2_000, dateEpochDay = 2, sourceWalletId = 2, destinationWalletId = 1),
            TransactionEntity(id = 3, budgetId = 1, type = TransactionType.TRANSFER, amount = 500, dateEpochDay = 3, sourceWalletId = 2, destinationWalletId = 1)
        )
        val balances = computeBalances(wallets, transactions)
        val walletSummaries = wallets.map { wallet ->
            WalletSummary(
                id = wallet.id,
                budgetId = wallet.budgetId,
                name = wallet.name,
                plannedAmount = wallet.plannedAmount,
                balance = balances.getValue(wallet.id),
                sortOrder = wallet.sortOrder,
                archived = wallet.archived,
                warning = balances.getValue(wallet.id) < 0
            )
        }

        val insights = computeBudgetInsights(wallets, walletSummaries, transactions)

        assertEquals("Food", insights.mostRescuedWallet?.walletName)
        assertEquals("Buffer", insights.topDonorWallet?.walletName)
        assertEquals("Buffer", insights.topTransferPath?.sourceWalletName)
        assertEquals("Food", insights.topTransferPath?.destinationWalletName)
        assertTrue(insights.overspentWallets.any { it.walletName == "Food" })
    }
}
