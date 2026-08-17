package com.mebudget.app.data.sync

import com.google.gson.JsonObject
import com.mebudget.app.billing.FeatureLimits
import com.mebudget.app.data.sync.models.PocketBaseListResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class LimitsConfigManagerTest {

    private lateinit var api: PocketBaseApi
    private lateinit var client: PocketBaseClient
    private lateinit var manager: LimitsConfigManager

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        client = mockk(relaxed = true)
        every { client.api } returns api
        manager = LimitsConfigManager(client)
    }

    private fun configRecord(overrides: Map<String, Int> = emptyMap()): JsonObject {
        val record = JsonObject()
        featureLimitsKeys().forEach { key ->
            record.addProperty(key, overrides[key] ?: FeatureLimits.DEFAULT.run {
                when (key) {
                    "freeMaxBudgets" -> freeMaxBudgets
                    "freeMaxWalletsPerBudget" -> freeMaxWalletsPerBudget
                    else -> freeMaxTransactionsPerMonth
                }
            })
        }
        return record
    }

    private fun featureLimitsKeys() = listOf(
        "freeMaxBudgets",
        "freeMaxWalletsPerBudget",
        "freeMaxTransactionsPerMonth"
    )

    @Test
    fun `starts with default limits`() {
        assertEquals(FeatureLimits.DEFAULT, manager.limits.value)
    }

    @Test
    fun `refreshLimits fetches config and updates limits`() = runTest {
        coEvery { api.getList(collection = "config", page = 1, perPage = 1) } returns
            PocketBaseListResponse(1, 1, 1, 1, listOf(configRecord(mapOf("freeMaxBudgets" to 3))))

        manager.refreshLimits()

        assertEquals(3, manager.limits.value.freeMaxBudgets)
        assertEquals(FeatureLimits.DEFAULT.freeMaxWalletsPerBudget, manager.limits.value.freeMaxWalletsPerBudget)
        coVerify { api.getList(collection = "config", page = 1, perPage = 1) }
    }

    @Test
    fun `refreshLimits keeps defaults when server returns nothing`() = runTest {
        coEvery { api.getList(collection = "config", page = 1, perPage = 1) } returns
            PocketBaseListResponse(1, 1, 0, 0, emptyList())

        manager.refreshLimits()

        assertEquals(FeatureLimits.DEFAULT, manager.limits.value)
    }

    @Test
    fun `refreshLimits swallows server errors and keeps current limits`() = runTest {
        val custom = FeatureLimits.DEFAULT
        coEvery { api.getList(collection = "config", page = 1, perPage = 1) } throws RuntimeException("offline")

        manager.refreshLimits()

        assertEquals(custom, manager.limits.value)
        assertNotNull(manager.limits.value)
    }

    @Test
    fun `refreshLimits only fetches once within cache window`() = runTest {
        coEvery { api.getList(collection = "config", page = 1, perPage = 1) } returns
            PocketBaseListResponse(1, 1, 1, 1, listOf(configRecord(mapOf("freeMaxBudgets" to 4))))

        manager.refreshLimits()
        manager.refreshLimits()

        coVerify(exactly = 1) { api.getList(collection = "config", page = 1, perPage = 1) }
    }
}