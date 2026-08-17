package com.mebudget.app.ui.auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mebudget.app.data.auth.AuthManager
import com.mebudget.app.data.auth.UserPreferences
import com.mebudget.app.data.sync.PocketBaseConfig
import com.mebudget.app.data.sync.PocketBaseClient

internal fun Context.authManager(): AuthManager {
    return AuthManager(
        pocketBaseClient = PocketBaseClient(PocketBaseConfig.DEFAULT_DEV_URL),
        userPreferences = UserPreferences(this)
    )
}

class SignInViewModelFactory(
    private val authManager: AuthManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SignInViewModel(authManager) as T
    }
}