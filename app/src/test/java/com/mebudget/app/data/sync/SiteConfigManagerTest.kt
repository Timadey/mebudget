package com.mebudget.app.data.sync

import com.google.gson.JsonObject
import com.mebudget.app.data.sync.models.PocketBaseListResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SiteConfigManagerTest {

    private lateinit var api: PocketBaseApi
    private lateinit var client: PocketBaseClient
    private lateinit var manager: SiteConfigManager

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        client = mockk(relaxed = true)
        every { client.api } returns api
        manager = SiteConfigManager(client)
    }

    private fun siteUrlRecord(url: String = "https://mebudget.blackshade.site"): JsonObject {
        val value = JsonObject()
        value.addProperty("siteUrl", url)
        val record = JsonObject()
        record.addProperty("key", "site_url")
        record.add("value", value)
        return record
    }

    @Test
    fun `starts with default site URL`() {
        assertEquals(SiteConfigManager.DEFAULT_SITE_URL, manager.siteUrl.value)
    }

    @Test
    fun `privacyPolicyUrl appends privacy path`() {
        assertEquals(
            "https://mebudget.blackshade.site/privacy",
            manager.privacyPolicyUrl
        )
    }

    @Test
    fun `refreshSiteUrl fetches server URL`() = runTest {
        coEvery { api.getList(collection = "config", page = 1, perPage = 1, filter = "key = 'site_url'") } returns
            PocketBaseListResponse(1, 1, 1, 1, listOf(siteUrlRecord("https://custom.domain.com")))

        manager.refreshSiteUrl()

        assertEquals("https://custom.domain.com", manager.siteUrl.value)
        assertEquals("https://custom.domain.com/privacy", manager.privacyPolicyUrl)
    }

    @Test
    fun `refreshSiteUrl keeps defaults when server returns nothing`() = runTest {
        coEvery { api.getList(collection = "config", page = 1, perPage = 1, filter = "key = 'site_url'") } returns
            PocketBaseListResponse(1, 1, 0, 0, emptyList())

        manager.refreshSiteUrl()

        assertEquals(SiteConfigManager.DEFAULT_SITE_URL, manager.siteUrl.value)
    }

    @Test
    fun `refreshSiteUrl swallows server errors and keeps defaults`() = runTest {
        coEvery { api.getList(collection = "config", page = 1, perPage = 1, filter = "key = 'site_url'") } throws
            RuntimeException("offline")

        manager.refreshSiteUrl()

        assertEquals(SiteConfigManager.DEFAULT_SITE_URL, manager.siteUrl.value)
    }

    @Test
    fun `refreshSiteUrl only fetches once within cache window`() = runTest {
        coEvery { api.getList(collection = "config", page = 1, perPage = 1, filter = "key = 'site_url'") } returns
            PocketBaseListResponse(1, 1, 1, 1, listOf(siteUrlRecord()))

        manager.refreshSiteUrl()
        manager.refreshSiteUrl()

        coVerify(exactly = 1) { api.getList(collection = "config", page = 1, perPage = 1, filter = "key = 'site_url'") }
    }

    @Test
    fun `refreshSiteUrl trims trailing slash`() = runTest {
        coEvery { api.getList(collection = "config", page = 1, perPage = 1, filter = "key = 'site_url'") } returns
            PocketBaseListResponse(1, 1, 1, 1, listOf(siteUrlRecord("https://example.com/")))

        manager.refreshSiteUrl()

        assertEquals("https://example.com", manager.siteUrl.value)
        assertEquals("https://example.com/privacy", manager.privacyPolicyUrl)
    }
}
