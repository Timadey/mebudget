package com.mebudget.app.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebudget.app.data.BudgetSummary
import com.mebudget.app.quickspend.InstalledAppSource
import com.mebudget.app.quickspend.LaunchableApp
import com.mebudget.app.quickspend.QuickSpendOverlayService
import com.mebudget.app.quickspend.QuickSpendPermissions
import com.mebudget.app.quickspend.QuickSpendSettings
import com.mebudget.app.quickspend.SharedPreferencesQuickSpendSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuickSpendUiState(
    val settings: QuickSpendSettings = QuickSpendSettings(),
    val budgets: List<BudgetSummary> = emptyList(),
    val launchableApps: List<LaunchableApp> = emptyList(),
    val overlayPermissionGranted: Boolean = false,
    val usageAccessGranted: Boolean = false
) {
    val setupComplete: Boolean
        get() = settings.isSetupComplete(
            overlayPermissionGranted = overlayPermissionGranted,
            usageAccessGranted = usageAccessGranted
        )
}

class QuickSpendViewModel(
    private val application: Application
) : ViewModel() {
    private val repository = application.budgetRepository()
    private val settingsStore = SharedPreferencesQuickSpendSettingsStore(application)
    private val installedAppSource = InstalledAppSource(application)
    private val settings = MutableStateFlow(settingsStore.load())
    private val launchableApps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    private val permissionTick = MutableStateFlow(0)

    init {
        syncOverlayService()
    }

    val uiState: StateFlow<QuickSpendUiState> = combine(
        settings,
        repository.observeBudgetSummaries(),
        launchableApps,
        permissionTick
    ) { currentSettings, budgets, apps, _ ->
        QuickSpendUiState(
            settings = currentSettings,
            budgets = budgets,
            launchableApps = apps,
            overlayPermissionGranted = QuickSpendPermissions.canDrawOverlays(application),
            usageAccessGranted = QuickSpendPermissions.hasUsageAccess(application)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = QuickSpendUiState(settings = settingsStore.load())
    )

    fun refresh() {
        permissionTick.value += 1
        syncOverlayService()
        viewModelScope.launch {
            launchableApps.value = installedAppSource.loadLaunchableApps()
        }
    }

    fun setEnabled(enabled: Boolean) {
        save(settings.value.copy(enabled = enabled))
    }

    fun selectBudget(budgetId: Long?) {
        save(settings.value.copy(selectedBudgetId = budgetId))
    }

    fun toggleApp(packageName: String) {
        val packages = settings.value.selectedAppPackages
        val updated = if (packages.contains(packageName)) {
            packages - packageName
        } else {
            packages + packageName
        }
        save(settings.value.copy(selectedAppPackages = updated))
    }

    fun overlaySettingsIntent(): Intent {
        return QuickSpendPermissions.overlaySettingsIntent(application)
    }

    fun usageAccessSettingsIntent(): Intent {
        return QuickSpendPermissions.usageAccessSettingsIntent()
    }

    fun syncOverlayService() {
        val currentSettings = settingsStore.load()
        val setupComplete = currentSettings.isSetupComplete(
            overlayPermissionGranted = QuickSpendPermissions.canDrawOverlays(application),
            usageAccessGranted = QuickSpendPermissions.hasUsageAccess(application)
        )
        val intent = Intent(application, QuickSpendOverlayService::class.java)
        if (setupComplete) {
            application.startService(intent)
        } else {
            application.stopService(intent)
        }
    }

    private fun save(updated: QuickSpendSettings) {
        settingsStore.save(updated)
        settings.value = updated
        syncOverlayService()
    }
}
