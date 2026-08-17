package com.mebudget.app.ui.profile

import com.mebudget.app.data.auth.AuthManager
import com.mebudget.app.data.auth.AuthState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var authManager: AuthManager
    private val authFlow = MutableStateFlow<AuthState>(AuthState.NotSignedIn)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        authManager = mockk(relaxed = true)
        every { authManager.authState } returns authFlow
        coEvery { authManager.restoreSession() } answers {
            authFlow.value = AuthState.SignedIn("u1", "a@b.com", "Ada")
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signed out user sees empty profile`() = runTest(dispatcher) {
        coEvery { authManager.restoreSession() } answers { Unit }
        val viewModel = ProfileViewModel(authManager, isPro = { false })
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSignedIn)
        assertFalse(viewModel.uiState.value.isPro)
        assertNull(viewModel.uiState.value.userName)
        assertNull(viewModel.uiState.value.userEmail)
    }

    @Test
    fun `restores session and shows signed in profile`() = runTest(dispatcher) {
        val viewModel = ProfileViewModel(authManager, isPro = { false })
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSignedIn)
        assertEquals("Ada", viewModel.uiState.value.userName)
        assertEquals("a@b.com", viewModel.uiState.value.userEmail)
        assertFalse(viewModel.uiState.value.isPro)
        assertFalse(viewModel.uiState.value.isRestoringSession)
    }

    @Test
    fun `pro status is read from injected lambda`() = runTest(dispatcher) {
        val viewModel = ProfileViewModel(authManager, isPro = { true })
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSignedIn)
        assertTrue(viewModel.uiState.value.isPro)
    }

    @Test
    fun `sign out clears auth state`() = runTest(dispatcher) {
        coEvery { authManager.signOut() } answers {
            authFlow.value = AuthState.NotSignedIn
        }
        val viewModel = ProfileViewModel(authManager, isPro = { false })
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSignedIn)

        viewModel.signOut()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { authManager.signOut() }
        assertFalse(viewModel.uiState.value.isSignedIn)
    }
}