package com.mebudget.app.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaystackManagerTest {

    @Test
    fun `billing plan defaults match legacy prices`() {
        assertEquals("pro_monthly", BillingPlan.MONTHLY.id)
        assertEquals("monthly", BillingPlan.MONTHLY.interval)
        assertEquals(150000L, BillingPlan.MONTHLY.price)
        assertEquals("pro_annual", BillingPlan.ANNUAL.id)
        assertEquals("annually", BillingPlan.ANNUAL.interval)
        assertEquals(1440000L, BillingPlan.ANNUAL.price)
    }

    @Test
    fun `fromId resolves known plans and rejects unknown`() {
        assertEquals(BillingPlan.MONTHLY, BillingPlan.fromId("pro_monthly"))
        assertEquals(BillingPlan.ANNUAL, BillingPlan.fromId("pro_annual"))
        assertNull(BillingPlan.fromId("nope"))
    }

    @Test
    fun `defaults list contains both plans`() {
        assertEquals(listOf(BillingPlan.MONTHLY, BillingPlan.ANNUAL), BillingPlan.DEFAULTS)
    }

    @Test
    fun `annual per-month equivalent is 120000`() {
        assertEquals(120000L, BillingPlan.ANNUAL.price / 12)
    }
}