package com.mebudget.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebudget.app.data.auth.AuthManager
import com.mebudget.app.data.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isSignedIn: Boolean = false,
    val isPro: Boolean = false,
    val isRestoringSession: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null
)

/**
 * Account/profile state for the Profile screen. Pro status is collected
 * reactively from [isProFlow] so UI updates immediately when subscription
 * status changes.
 */
class ProfileViewModel(
    private val authManager: AuthManager,
    private val isProFlow: StateFlow<Boolean> = MutableStateFlow(false)
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState(isRestoringSession = true))
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        viewModelScope.launch {
            authManager.restoreSession()
            _uiState.value = _uiState.value.copy(isRestoringSession = false)
        }
        viewModelScope.launch {
            combine(authManager.authState, isProFlow) { auth, isPro ->
                when (auth) {
                    is AuthState.SignedIn -> ProfileUiState(
                        isSignedIn = true,
                        isPro = isPro,
                        userName = auth.name,
                        userEmail = auth.email
                    )
                    AuthState.NotSignedIn -> ProfileUiState(
                        isPro = isPro
                    )
                }
            }.collectLatest { state ->
                _uiState.value = state
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }
}