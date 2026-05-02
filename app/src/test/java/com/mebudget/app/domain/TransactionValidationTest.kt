package com.mebudget.app.domain

import com.mebudget.app.data.BudgetEntity
import com.mebudget.app.data.NegativeBalanceRule
import com.mebudget.app.data.TransactionEntity
import com.mebudget.app.data.TransactionType
import com.mebudget.app.data.WalletEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionValidationTest {

    @Test
    fun `block rule rejects transaction that drives wallet below zero`() {
        val result = validateTransactionChange(
            context = context(
                budget = BudgetEntity(id = 1, name = "May", negativeBalanceRule = NegativeBalanceRule.BLOCK),
                wallets = listOf(wallet(id = 10, plannedAmount = 5_000))
            ),
            draft = expense(walletId = 10, amount = 6_000)
        )

        assertTrue(result.isFailure)
        assertEquals("Food cannot go below zero.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `warn rule allows negative projected balance`() {
        val result = validateTransactionChange(
            context = context(
                budget = BudgetEntity(id = 1, name = "May", negativeBalanceRule = NegativeBalanceRule.WARN),
                wallets = listOf(wallet(id = 10, plannedAmount = 5_000))
            ),
            draft = expense(walletId = 10, amount = 6_000)
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `transfer requires two different wallets from same budget`() {
        val result = validateTransactionChange(
            context = context(
                wallets = listOf(wallet(id = 10), wallet(id = 11, name = "Buffer"))
            ),
            draft = transfer(sourceWalletId = 10, destinationWalletId = 10, amount = 1_000)
        )

        assertTrue(result.isFailure)
        assertEquals("Choose two different wallets.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validation rejects wallet references outside budget`() {
        val result = validateTransactionChange(
            context = context(
                wallets = listOf(wallet(id = 10))
            ),
            draft = transfer(sourceWalletId = 10, destinationWalletId = 99, amount = 1_000)
        )

        assertTrue(result.isFailure)
        assertEquals("Choose wallets from this budget.", result.exceptionOrNull()?.message)
    }

    private fun context(
        budget: BudgetEntity = BudgetEntity(id = 1, name = "May", negativeBalanceRule = NegativeBalanceRule.BLOCK),
        wallets: List<WalletEntity>,
        currentTransactions: List<TransactionEntity> = emptyList()
    ): TransactionValidationContext {
        return TransactionValidationContext(
            budget = budget,
            wallets = wallets,
            currentTransactions = currentTransactions
        )
    }

    private fun wallet(
        id: Long,
        budgetId: Long = 1,
        plannedAmount: Long = 10_000,
        name: String = "Food"
    ): WalletEntity {
        return WalletEntity(
            id = id,
            budgetId = budgetId,
            name = name,
            plannedAmount = plannedAmount,
            sortOrder = 0
        )
    }

    private fun expense(walletId: Long, amount: Long): TransactionEntity {
        return TransactionEntity(
            budgetId = 1,
            type = TransactionType.EXPENSE,
            amount = amount,
            dateEpochDay = 1,
            sourceWalletId = walletId
        )
    }

    private fun transfer(sourceWalletId: Long, destinationWalletId: Long, amount: Long): TransactionEntity {
        return TransactionEntity(
            budgetId = 1,
            type = TransactionType.TRANSFER,
            amount = amount,
            dateEpochDay = 1,
            sourceWalletId = sourceWalletId,
            destinationWalletId = destinationWalletId
        )
    }
}
