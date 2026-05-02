package com.mebudget.app.domain

import com.mebudget.app.data.TransactionEntity
import com.mebudget.app.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionCommandsTest {

    @Test
    fun `create transfer transaction normalizes note`() {
        val transaction = createTransferTransaction(
            budgetId = 1,
            sourceWalletId = 10,
            destinationWalletId = 11,
            amount = 2_000,
            dateEpochDay = 5,
            note = "  move funds  "
        )

        assertEquals(TransactionType.TRANSFER, transaction.type)
        assertEquals("move funds", transaction.note)
    }

    @Test
    fun `update transaction command clears blank note`() {
        val updated = updateTransactionCommand(
            existing = TransactionEntity(
                id = 1,
                budgetId = 1,
                type = TransactionType.EXPENSE,
                amount = 500,
                dateEpochDay = 1,
                sourceWalletId = 10,
                note = "old"
            ),
            amount = 600,
            dateEpochDay = 2,
            sourceWalletId = 10,
            destinationWalletId = null,
            note = "   "
        )

        assertEquals(600L, updated.amount)
        assertNull(updated.note)
    }
}
