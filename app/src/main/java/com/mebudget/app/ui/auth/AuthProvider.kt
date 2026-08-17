package com.mebudget.app.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mebudget.app.data.auth.AuthManager
import com.mebudget.app.data.sync.syncDependencies

/**
 * Returns the app's single [AuthManager] (shared with the sync graph) so the
 * sign-in token lands on the same PocketBaseClient the sync/realtime workers
 * use. Never build a second PocketBaseClient here — entities must share one.
 */
internal fun Context.authManager(): AuthManager {
    return syncDependencies().authManager
}

class SignInViewModelFactory(
    private val authManager: AuthManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SignInViewModel(authManager) as T
    }
}