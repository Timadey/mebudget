package com.mebudget.app.data.auth

import com.mebudget.app.data.sync.AuthWithPasswordRequest
import com.mebudget.app.data.sync.PocketBaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the auth session lifecycle: email/password sign-in, Google sign-in,
 * sign-out and restoring a previously persisted session.
 *
 * The resulting [AuthState] only carries identity. Whether the user has Pro is
 * decided by the subscription/feature gate, not here.
 */
class AuthManager(
    private val pocketBaseClient: PocketBaseClient,
    private val userPreferences: UserPreferences
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotSignedIn)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    suspend fun signInWithEmail(email: String, password: String): Result<AuthState> {
        return try {
            val response = pocketBaseClient.api.authWithPassword(
                AuthWithPasswordRequest(identity = email, password = password)
            )
            pocketBaseClient.authToken = response.token
            userPreferences.saveAuthData(
                token = response.token,
                userId = response.record.id,
                email = response.record.email,
                name = response.record.name
            )
            val state = AuthState.SignedIn(
                userId = response.record.id,
                email = response.record.email,
                name = response.record.name
            )
            _authState.value = state
            Result.success(state)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        pocketBaseClient.clearAuth()
        userPreferences.clearAuthData()
        _authState.value = AuthState.NotSignedIn
    }

    /** Restores a persisted session (token + profile) without a network call. */
    suspend fun restoreSession() {
        val token = userPreferences.authToken.first()
        val userId = userPreferences.userId.first()
        val email = userPreferences.userEmail.first()
        val name = userPreferences.userName.first()
        if (token != null && userId != null) {
            pocketBaseClient.authToken = token
            _authState.value = AuthState.SignedIn(
                userId = userId,
                email = email.orEmpty(),
                name = name.orEmpty()
            )
        }
    }

    /**
     * Renews the PocketBase JWT before it expires. No-op unless signed in.
     * Keeps the previous session on failure so transient network errors do not
     * sign the user out.
     */
    suspend fun refreshAuth() {
        if (_authState.value !is AuthState.SignedIn) return
        try {
            val response = pocketBaseClient.api.refreshAuth()
            pocketBaseClient.authToken = response.token
            userPreferences.saveAuthData(
                token = response.token,
                userId = response.record.id,
                email = response.record.email,
                name = response.record.name
            )
            _authState.value = AuthState.SignedIn(
                userId = response.record.id,
                email = response.record.email,
                name = response.record.name
            )
        } catch (_: Exception) {
            // Keep the existing session; the next periodic refresh will retry.
        }
    }
}
