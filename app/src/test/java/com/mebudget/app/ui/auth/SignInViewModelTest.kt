package com.mebudget.app.ui.auth

import com.mebudget.app.data.auth.AuthManager
import com.mebudget.app.data.auth.AuthState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var authManager: AuthManager

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        authManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `blank fields show validation error`() = runTest(dispatcher) {
        val viewModel = SignInViewModel(authManager)

        viewModel.signIn()

        assertEquals("Email and password are required.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun `successful sign in sets isSignedIn`() = runTest(dispatcher) {
        coEvery { authManager.signInWithEmail("a@b.com", "secret") } returns Result.success(
            AuthState.SignedIn("u1", "a@b.com", "Ada")
        )
        val viewModel = SignInViewModel(authManager)

        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("secret")
        viewModel.signIn()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSignedIn)
        assertEquals(null, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `failed sign in surfaces error`() = runTest(dispatcher) {
        coEvery { authManager.signInWithEmail(any(), any()) } returns Result.failure(
            RuntimeException("Invalid credentials")
        )
        val viewModel = SignInViewModel(authManager)

        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("wrong")
        viewModel.signIn()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSignedIn)
        assertEquals("Invalid credentials", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `blank sign up fields show validation error`() = runTest(dispatcher) {
        val viewModel = SignInViewModel(authManager)

        viewModel.signUp()

        assertEquals("All fields are required.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun `short sign up password shows validation error`() = runTest(dispatcher) {
        val viewModel = SignInViewModel(authManager)
        viewModel.onNameChanged("Ada")
        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("short")
        viewModel.onConfirmPasswordChanged("short")

        viewModel.signUp()

        assertEquals("Password must be at least 8 characters.", viewModel.uiState.value.error)
    }

    @Test
    fun `mismatched sign up passwords show validation error`() = runTest(dispatcher) {
        val viewModel = SignInViewModel(authManager)
        viewModel.onNameChanged("Ada")
        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("password1")
        viewModel.onConfirmPasswordChanged("password2")

        viewModel.signUp()

        assertEquals("Passwords do not match.", viewModel.uiState.value.error)
    }

    @Test
    fun `successful sign up signs in`() = runTest(dispatcher) {
        coEvery { authManager.signUpWithEmail("a@b.com", "Ada", "password1") } returns Result.success(
            AuthState.SignedIn("u1", "a@b.com", "Ada")
        )
        val viewModel = SignInViewModel(authManager)
        viewModel.onNameChanged("Ada")
        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("password1")
        viewModel.onConfirmPasswordChanged("password1")

        viewModel.signUp()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSignedIn)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `failed sign up surfaces error`() = runTest(dispatcher) {
        coEvery { authManager.signUpWithEmail(any(), any(), any()) } returns Result.failure(
            RuntimeException("email already exists")
        )
        val viewModel = SignInViewModel(authManager)
        viewModel.onNameChanged("Ada")
        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("password1")
        viewModel.onConfirmPasswordChanged("password1")

        viewModel.signUp()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSignedIn)
        assertEquals("email already exists", viewModel.uiState.value.error)
    }
}