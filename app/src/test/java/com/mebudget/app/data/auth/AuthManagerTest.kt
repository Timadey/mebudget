package com.mebudget.app.data.auth

import com.mebudget.app.data.sync.PocketBaseApi
import com.mebudget.app.data.sync.PocketBaseClient
import com.mebudget.app.data.sync.models.PocketBaseAuthResponse
import com.mebudget.app.data.sync.models.PocketBaseUserRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthManagerTest {

    private lateinit var api: PocketBaseApi
    private lateinit var client: PocketBaseClient
    private lateinit var userPreferences: UserPreferences
    private lateinit var authManager: AuthManager

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        client = spyk(PocketBaseClient("http://test"), recordPrivateCalls = true)
        every { client.api } returns api
        userPreferences = mockk(relaxed = true)
        authManager = AuthManager(
            pocketBaseClient = client,
            userPreferences = userPreferences
        )
    }

    @Test
    fun `initial state is not signed in`() {
        assertTrue(authManager.authState.value is AuthState.NotSignedIn)
    }

    @Test
    fun `sign in with email sets state and persists session`() = runTest {
        coEvery { api.authWithPassword(any()) } returns PocketBaseAuthResponse(
            token = "token123",
            record = PocketBaseUserRecord(
                id = "user1",
                email = "a@b.com",
                name = "Ada"
            )
        )

        val result = authManager.signInWithEmail("a@b.com", "password")

        assertTrue(result.isSuccess)
        val state = authManager.authState.value as AuthState.SignedIn
        assertEquals("user1", state.userId)
        assertEquals("a@b.com", state.email)
        assertEquals("Ada", state.name)
        assertEquals("token123", client.authToken)
        coVerify { userPreferences.saveAuthData("token123", "user1", "a@b.com", "Ada") }
    }

    @Test
    fun `sign in with email propagates failures`() = runTest {
        coEvery { api.authWithPassword(any()) } throws RuntimeException("boom")

        val result = authManager.signInWithEmail("a@b.com", "password")

        assertTrue(result.isFailure)
        assertTrue(authManager.authState.value is AuthState.NotSignedIn)
    }

    @Test
    fun `sign up creates user then signs in`() = runTest {
        coEvery { api.create("users", any()) } returns com.google.gson.JsonObject()
        coEvery { api.authWithPassword(any()) } returns PocketBaseAuthResponse(
            token = "token123",
            record = PocketBaseUserRecord("user1", "a@b.com", "Ada")
        )

        val result = authManager.signUpWithEmail("a@b.com", "Ada", "password1")

        assertTrue(result.isSuccess)
        val state = authManager.authState.value as AuthState.SignedIn
        assertEquals("user1", state.userId)
        assertEquals("a@b.com", state.email)
        assertEquals("Ada", state.name)
        assertEquals("token123", client.authToken)
        coVerify { api.create(collection = "users", body = any()) }
        coVerify { userPreferences.saveAuthData("token123", "user1", "a@b.com", "Ada") }
    }

    @Test
    fun `sign up propagates create failure`() = runTest {
        coEvery { api.create("users", any()) } throws RuntimeException("email already exists")

        val result = authManager.signUpWithEmail("a@b.com", "Ada", "password1")

        assertTrue(result.isFailure)
        assertTrue(authManager.authState.value is AuthState.NotSignedIn)
    }

    @Test
    fun `sign out clears state token and preferences`() = runTest {
        coEvery { api.authWithPassword(any()) } returns PocketBaseAuthResponse(
            token = "token123",
            record = PocketBaseUserRecord("user1", "a@b.com", "Ada")
        )
        coEvery { userPreferences.clearAuthData() } just runs
        authManager.signInWithEmail("a@b.com", "password")

        authManager.signOut()

        assertTrue(authManager.authState.value is AuthState.NotSignedIn)
        assertFalse(client.authToken != null)
        coVerify { userPreferences.clearAuthData() }
    }

    @Test
    fun `restore session restores signed in state when token exists`() = runTest {
        every { userPreferences.authToken } returns flowOf("stored_token")
        every { userPreferences.userId } returns flowOf("user9")
        every { userPreferences.userEmail } returns flowOf("x@y.com")
        every { userPreferences.userName } returns flowOf("Bob")

        authManager.restoreSession()

        val state = authManager.authState.value as AuthState.SignedIn
        assertEquals("user9", state.userId)
        assertEquals("stored_token", client.authToken)
    }

    @Test
    fun `restore session stays signed out when no token`() = runTest {
        every { userPreferences.authToken } returns flowOf(null)
        every { userPreferences.userId } returns flowOf(null)
        every { userPreferences.userEmail } returns flowOf(null)
        every { userPreferences.userName } returns flowOf(null)

        authManager.restoreSession()

        assertTrue(authManager.authState.value is AuthState.NotSignedIn)
    }
}
