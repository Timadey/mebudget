package com.mebudget.app.domain

import com.mebudget.app.data.WalletEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletCommandsTest {

    @Test
    fun `wallet definition trims valid name`() {
        val result = validateWalletDefinition("  Food  ", 5_000)

        assertTrue(result.isSuccess)
        assertEquals("Food", result.getOrThrow().name)
    }

    @Test
    fun `wallet reorder swaps neighboring sort orders`() {
        val reordered = planWalletReorder(
            wallets = listOf(
                wallet(id = 1, sortOrder = 0, name = "A"),
                wallet(id = 2, sortOrder = 1, name = "B"),
                wallet(id = 3, sortOrder = 2, name = "C")
            ),
            walletId = 2,
            direction = -1
        )

        assertEquals(listOf(2L, 1L, 3L), reordered.map { it.id })
        assertEquals(listOf(0, 1, 2), reordered.map { it.sortOrder })
    }

    private fun wallet(id: Long, sortOrder: Int, name: String): WalletEntity {
        return WalletEntity(
            id = id,
            budgetId = 1,
            name = name,
            plannedAmount = 1_000,
            sortOrder = sortOrder
        )
    }
}
