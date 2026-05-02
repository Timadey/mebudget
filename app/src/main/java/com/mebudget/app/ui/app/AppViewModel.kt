package com.mebudget.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppUiState(
    val privacyModeEnabled: Boolean = false
)

class AppViewModel(application: Application) : ViewModel() {
    private val privacyPrefs = application.getSharedPreferences(PRIVACY_PREFS_NAME, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(
        AppUiState(
            privacyModeEnabled = privacyPrefs.getBoolean(PRIVACY_MODE_KEY, false)
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun togglePrivacyMode() {
        val newValue = !_uiState.value.privacyModeEnabled
        _uiState.value = _uiState.value.copy(privacyModeEnabled = newValue)
        privacyPrefs.edit().putBoolean(PRIVACY_MODE_KEY, newValue).apply()
    }
}

class AppViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppViewModel(application) as T
    }
}

private const val PRIVACY_PREFS_NAME = "mebudget_privacy"
private const val PRIVACY_MODE_KEY = "privacy_mode_enabled"
