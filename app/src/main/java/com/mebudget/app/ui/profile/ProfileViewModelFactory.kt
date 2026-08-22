package com.mebudget.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mebudget.app.data.auth.AuthManager
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModelFactory(
    private val authManager: AuthManager,
    private val isProFlow: StateFlow<Boolean>
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(authManager, isProFlow) as T
    }
}