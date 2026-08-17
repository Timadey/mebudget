package com.mebudget.app.billing

import com.google.gson.JsonObject
import com.mebudget.app.data.auth.AuthManager
import com.mebudget.app.data.auth.AuthState
import com.mebudget.app.data.sync.PocketBaseApi
import com.mebudget.app.data.sync.PocketBaseClient
import com.mebudget.app.data.sync.models.PocketBaseListResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionManagerTest {

    private lateinit var api: PocketBaseApi
    private lateinit var client: PocketBaseClient
    private lateinit var authManager: AuthManager
    private lateinit var authFlow: MutableStateFlow<AuthState>
    private lateinit var cache: InMemorySubscriptionCache
    private lateinit var manager: SubscriptionManager

    private var currentTime = 1_000_000L

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        client = mockk(relaxed = true)
        every { client.api } returns api
        authFlow = MutableStateFlow(AuthState.NotSignedIn)
        authManager = mockk(relaxed = true)
        every { authManager.authState } returns authFlow
        cache = InMemorySubscriptionCache()
        manager = SubscriptionManager(
            pocketBaseClient = client,
            authManager = authManager,
            cache = cache,
            now = { currentTime },
            scope = CoroutineScope(UnconfinedTestDispatcher())
        )
    }

    private fun subscriptionRecord(endDate: String): JsonObject = JsonObject().apply {
        addProperty("id", "sub1")
        addProperty("status", "active")
        addProperty("endDate", endDate)
    }

    private fun signedIn() {
        authFlow.value = AuthState.SignedIn("u1", "a@b.com", "Ada")
    }

    private fun signedOut() {
        authFlow.value = AuthState.NotSignedIn
    }

    @Test
    fun `starts free when not signed in`() = runTest {
        signedOut()
        assertFalse(manager.isPro.value)
    }

    @Test
    fun `refresh marks pro when an active subscription is in the future`() = runTest {
        signedIn()
        coEvery {
            api.getList(collection = "subscriptions", page = 1, perPage = 1, filter = "status = 'active'", sort = "-endDate")
        } returns PocketBaseListResponse(
            1, 1, 1, 1,
            listOf(subscriptionRecord("2027-01-01 00:00:00.000Z"))
        )

        manager.refresh()

        assertTrue(manager.isPro.value)
    }

    @Test
    fun `refresh marks free when the active subscription already expired`() = runTest {
        signedIn()
        currentTime = TimeUnit.MILLISECONDS.toMillis(1_800_000_000_000L) // far future
        coEvery {
            api.getList(collection = "subscriptions", page = 1, perPage = 1, filter = "status = 'active'", sort = "-endDate")
        } returns PocketBaseListResponse(
            1, 1, 1, 1,
            listOf(subscriptionRecord("2020-01-01 00:00:00.000Z"))
        )

        manager.refresh()

        assertFalse(manager.isPro.value)
    }

    @Test
    fun `refresh falls back to cached expiry on network failure`() = runTest {
        signedIn()
        cache.saveExpiryMillis(TimeUnit.MILLISECONDS.toMillis(2_000_000_000_000L))
        manager = SubscriptionManager(
            pocketBaseClient = client,
            authManager = authManager,
            cache = cache,
            now = { currentTime },
            scope = CoroutineScope(UnconfinedTestDispatcher())
        )
        coEvery {
            api.getList(collection = "subscriptions", page = 1, perPage = 1, filter = "status = 'active'", sort = "-endDate")
        } throws RuntimeException("offline")

        manager.refresh()

        assertTrue(manager.isPro.value)
    }

    @Test
    fun `refresh clears pro when signed out`() = runTest {
        signedIn()
        cache.saveExpiryMillis(TimeUnit.MILLISECONDS.toMillis(2_000_000_000_000L))
        signedOut()

        manager.refresh()

        assertFalse(manager.isPro.value)
    }

    @Test
    fun `refresh persists the last known expiry for offline grace`() = runTest {
        signedIn()
        coEvery {
            api.getList(collection = "subscriptions", page = 1, perPage = 1, filter = "status = 'active'", sort = "-endDate")
        } returns PocketBaseListResponse(
            1, 1, 1, 1,
            listOf(subscriptionRecord("2027-01-01 00:00:00.000Z"))
        )

        manager.refresh()

        assertEquals(
            java.util.Date.from(
                java.time.Instant.parse("2027-01-01T00:00:00Z")
            ).time,
            cache.savedExpiryMillis
        )
    }

    @Test
    fun `offline grace keeps pro after restart until expiry passes`() = runTest {
        signedIn()
        cache.saveExpiryMillis(TimeUnit.MILLISECONDS.toMillis(2_000_000_000_000L))

        val restarted = SubscriptionManager(
            pocketBaseClient = client,
            authManager = authManager,
            cache = cache,
            now = { currentTime },
            scope = CoroutineScope(UnconfinedTestDispatcher())
        )

        assertTrue(restarted.isPro.value)
    }
}

private class InMemorySubscriptionCache(
    var savedExpiryMillis: Long? = null
) : SubscriptionCache {
    override suspend fun loadExpiryMillis(): Long? = savedExpiryMillis
    override suspend fun saveExpiryMillis(expiryMillis: Long?) {
        savedExpiryMillis = expiryMillis
    }
}