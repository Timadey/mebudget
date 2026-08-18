package com.mebudget.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebudget.app.data.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignedIn: Boolean = false
)

class SignInViewModel(
    private val authManager: AuthManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirmPassword)
    }

    fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Email and password are required.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authManager.signInWithEmail(state.email.trim(), state.password)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSignedIn = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Sign in failed. Check your details and try again."
                    )
                }
            )
        }
    }

    fun signUp() {
        val state = _uiState.value
        val name = state.name.trim()
        val email = state.email.trim()
        if (name.isBlank() || email.isBlank() || state.password.isBlank() || state.confirmPassword.isBlank()) {
            _uiState.value = state.copy(error = "All fields are required.")
            return
        }
        if (state.password.length < 8) {
            _uiState.value = state.copy(error = "Password must be at least 8 characters.")
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(error = "Passwords do not match.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authManager.signUpWithEmail(email, name, state.password)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSignedIn = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Sign up failed. Check the details and try again."
                    )
                }
            )
        }
    }
}