package com.mebudget.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mebudget.app.data.auth.AuthManager

class ProfileViewModelFactory(
    private val authManager: AuthManager,
    private val isPro: () -> Boolean = { false }
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(authManager, isPro) as T
    }
}