package com.mebudget.app.data.auth

/**
 * Authentication state only. Pro/free tier is decided by the subscription
 * manager (FeatureGate), keeping auth and billing concerns decoupled.
 */
sealed class AuthState {
    object NotSignedIn : AuthState()

    data class SignedIn(
        val userId: String,
        val email: String,
        val name: String
    ) : AuthState()

    val isSignedIn: Boolean
        get() = this is SignedIn
}
