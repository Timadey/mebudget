package com.mebudget.app.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaystackManagerTest {

    private val paystackManager = PaystackManager(publicKey = "pk_test_dummy")

    @Test
    fun `can get available plans`() {
        val plans = paystackManager.getAvailablePlans()
        assertEquals(2, plans.size)
    }

    @Test
    fun `plans expose pro ids`() {
        val ids = paystackManager.getAvailablePlans().map { it.id }.toSet()
        assertEquals(setOf("pro_monthly", "pro_annual"), ids)
    }

    @Test
    fun `plan lookup by id works`() {
        assertTrue(BillingPlan.fromId("pro_monthly") == BillingPlan.Monthly)
        assertTrue(BillingPlan.fromId("pro_annual") == BillingPlan.Annual)
        assertEquals(null, BillingPlan.fromId("nope"))
    }

    @Test
    fun `annual plan is discounted per month`() {
        assertEquals(BillingPlan.Annual.price / 12, 120000L)
    }
}