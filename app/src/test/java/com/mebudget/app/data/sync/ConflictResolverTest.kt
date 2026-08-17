package com.mebudget.app.data.sync

import com.mebudget.app.data.BudgetEntity
import com.mebudget.app.data.NegativeBalanceRule
import com.mebudget.app.data.TransactionEntity
import com.mebudget.app.data.WalletEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ConflictResolverTest {

    private val resolver = ConflictResolver()

    @Test
    fun `local wins when it was synced after the remote update`() {
        val local = BudgetEntity(id = 7, name = "Local", createdAtMillis = 100L)
        val remote = BudgetEntity(id = 0, name = "Remote", createdAtMillis = 50L)

        val resolved = resolver.resolveBudgetConflict(
            local = local,
            remote = remote,
            localLastSyncedAtMillis = 1_000L,
            remoteUpdatedAtMillis = 900L
        )

        assertEquals(local, resolved)
        assertEquals("Local", resolved.name)
    }

    @Test
    fun `local wins on equal timestamps`() {
        val local = BudgetEntity(id = 7, name = "Local", createdAtMillis = 100L)
        val remote = BudgetEntity(id = 0, name = "Remote", createdAtMillis = 100L)

        val resolved = resolver.resolveBudgetConflict(local, remote, 1_000L, 1_000L)

        assertEquals(local, resolved)
    }

    @Test
    fun `remote wins when updated after local sync and keeps local id`() {
        val local = BudgetEntity(id = 7, name = "Local", createdAtMillis = 100L)
        val remote = BudgetEntity(id = 0, name = "Remote", negativeBalanceRule = NegativeBalanceRule.ALLOW)

        val resolved = resolver.resolveBudgetConflict(local, remote, 900L, 1_000L)

        assertEquals(7L, resolved.id)
        assertEquals("Remote", resolved.name)
        assertEquals(NegativeBalanceRule.ALLOW, resolved.negativeBalanceRule)
    }

    @Test
    fun `wallet conflict keeps local id when remote wins`() {
        val local = WalletEntity(id = 3, budgetId = 1, name = "Cash", plannedAmount = 100L, sortOrder = 0)
        val remote = WalletEntity(id = 0, budgetId = 1, name = "M-Pesa", plannedAmount = 500L, sortOrder = 1)

        val resolved = resolver.resolveWalletConflict(local, remote, 900L, 1_000L)

        assertEquals(3L, resolved.id)
        assertEquals("M-Pesa", resolved.name)
        assertEquals(500L, resolved.plannedAmount)
    }

    @Test
    fun `transaction conflict keeps local id when remote wins`() {
        val local = TransactionEntity(id = 5, budgetId = 1, type = com.mebudget.app.data.TransactionType.EXPENSE, amount = 10L, dateEpochDay = 1L, createdAtMillis = 100L)
        val remote = TransactionEntity(id = 0, budgetId = 1, type = com.mebudget.app.data.TransactionType.CREDIT, amount = 42L, dateEpochDay = 1L, createdAtMillis = 200L)

        val resolved = resolver.resolveTransactionConflict(local, remote, 900L, 1_000L)

        assertEquals(5L, resolved.id)
        assertEquals(com.mebudget.app.data.TransactionType.CREDIT, resolved.type)
        assertEquals(42L, resolved.amount)
    }

    @Test
    fun `missing local sync timestamp counts as oldest so remote wins`() {
        val local = BudgetEntity(id = 7, name = "Local", createdAtMillis = 100L)
        val remote = BudgetEntity(id = 0, name = "Remote", createdAtMillis = 50L)

        val resolved = resolver.resolveBudgetConflict(local, remote, null, 1L)

        assertEquals("Remote", resolved.name)
    }

    @Test
    fun `remote updated at zero is oldest so local wins`() {
        val local = BudgetEntity(id = 7, name = "Local", createdAtMillis = 100L)
        val remote = BudgetEntity(id = 0, name = "Remote", createdAtMillis = 50L)

        val resolved = resolver.resolveBudgetConflict(local, remote, 500L, 0L)

        assertEquals("Local", resolved.name)
    }
}