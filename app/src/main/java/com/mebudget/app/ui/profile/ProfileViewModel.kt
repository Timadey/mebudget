package com.mebudget.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebudget.app.data.auth.AuthManager
import com.mebudget.app.data.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isSignedIn: Boolean = false,
    val isPro: Boolean = false,
    val isRestoringSession: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null
)

/**
 * Account/profile state for the Profile screen. Pro status is injected via
 * [isPro] so auth and billing stay decoupled (same pattern as FeatureGate).
 */
class ProfileViewModel(
    private val authManager: AuthManager,
    private val isPro: () -> Boolean = { false }
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState(isRestoringSession = true))
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        viewModelScope.launch {
            authManager.restoreSession()
            _uiState.value = _uiState.value.copy(isRestoringSession = false)
        }
        viewModelScope.launch {
            authManager.authState.collectLatest { state ->
                _uiState.value = when (state) {
                    is AuthState.SignedIn -> ProfileUiState(
                        isSignedIn = true,
                        isPro = isPro(),
                        userName = state.name,
                        userEmail = state.email
                    )
                    AuthState.NotSignedIn -> ProfileUiState(
                        isPro = isPro()
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }
}