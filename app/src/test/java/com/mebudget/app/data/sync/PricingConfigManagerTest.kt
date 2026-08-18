package com.mebudget.app.data.sync

import com.google.gson.JsonObject
import com.mebudget.app.billing.BillingPlan
import com.mebudget.app.data.sync.models.PocketBaseListResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PricingConfigManagerTest {

    private lateinit var api: PocketBaseApi
    private lateinit var client: PocketBaseClient
    private lateinit var manager: PricingConfigManager

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        client = mockk(relaxed = true)
        every { client.api } returns api
        manager = PricingConfigManager(client)
    }

    private fun plansRecord(monthlyKobo: Long = 150000, annualKobo: Long = 1440000): JsonObject {
        val value = JsonObject()
        value.add("pro_monthly", JsonObject().apply { addProperty("priceKobo", monthlyKobo) })
        value.add("pro_annual", JsonObject().apply { addProperty("priceKobo", annualKobo) })
        val record = JsonObject()
        record.addProperty("key", "plans")
        record.add("value", value)
        return record
    }

    @Test
    fun `starts with default plans`() {
        assertEquals(BillingPlan.DEFAULTS, manager.plans.value)
    }

    @Test
    fun `refreshPlans fetches server prices`() = runTest {
        coEvery { api.getList(collection = "config", page = 1, perPage = 1, filter = "key = 'plans'") } returns
            PocketBaseListResponse(1, 1, 1, 1, listOf(plansRecord(monthlyKobo = 200000, annualKobo = 2400000)))

        manager.refreshPlans()

        assertEquals(200000L, manager.plans.value.first { it.id == "pro_monthly" }.price)
        assertEquals(2400000L, manager.plans.value.first { it.id == "pro_annual" }.price)
    }

    @Test
    fun `refreshPlans keeps defaults when server returns nothing`() = runTest {
        coEvery { api.getList(collection = "config", page = 1, perPage = 1, filter = "key = 'plans'") } returns
            PocketBaseListResponse(1, 1, 0, 0, emptyList())

        manager.refreshPlans()

        assertEquals(BillingPlan.DEFAULTS, manager.plans.value)
    }

    @Test
    fun `refreshPlans swallows server errors and keeps defaults`() = runTest {
        coEvery { api.getList(collection = "config", page = 1, perPage = 1, filter = "key = 'plans'") } throws
            RuntimeException("offline")

        manager.refreshPlans()

        assertEquals(BillingPlan.DEFAULTS, manager.plans.value)
    }

    @Test
    fun `refreshPlans only fetches once within cache window`() = runTest {
        coEvery { api.getList(collection = "config", page = 1, perPage = 1, filter = "key = 'plans'") } returns
            PocketBaseListResponse(1, 1, 1, 1, listOf(plansRecord(monthlyKobo = 200000)))

        manager.refreshPlans()
        manager.refreshPlans()

        coVerify(exactly = 1) { api.getList(collection = "config", page = 1, perPage = 1, filter = "key = 'plans'") }
    }
}