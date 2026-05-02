package com.mebudget.app.domain

import com.mebudget.app.data.BudgetEntity
import com.mebudget.app.data.NegativeBalanceRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetValidationTest {

    @Test
    fun `budget definition trims valid name`() {
        val result = validateBudgetDefinition(
            name = "  May Budget  ",
            startDateEpochDay = 10,
            endDateEpochDay = 20,
            negativeBalanceRule = NegativeBalanceRule.WARN
        )

        assertTrue(result.isSuccess)
        assertEquals("May Budget", result.getOrThrow().name)
    }

    @Test
    fun `budget definition rejects reversed date range`() {
        val result = validateBudgetDefinition(
            name = "May",
            startDateEpochDay = 20,
            endDateEpochDay = 10,
            negativeBalanceRule = NegativeBalanceRule.WARN
        )

        assertTrue(result.isFailure)
        assertEquals("End date cannot be before start date.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `budget update returns normalized entity`() {
        val result = validateBudgetUpdate(
            BudgetEntity(
                id = 1,
                name = "  Trim Me  ",
                startDateEpochDay = 1,
                endDateEpochDay = 2,
                negativeBalanceRule = NegativeBalanceRule.BLOCK
            )
        )

        assertTrue(result.isSuccess)
        assertEquals("Trim Me", result.getOrThrow().name)
    }
}
