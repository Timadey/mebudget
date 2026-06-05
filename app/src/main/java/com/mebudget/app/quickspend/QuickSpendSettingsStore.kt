package com.mebudget.app.quickspend

import android.content.Context
import android.content.SharedPreferences

interface QuickSpendSettingsStore {
    fun load(): QuickSpendSettings
    fun save(settings: QuickSpendSettings)
}

class SharedPreferencesQuickSpendSettingsStore(
    context: Context
) : QuickSpendSettingsStore {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "quick_spend_settings",
        Context.MODE_PRIVATE
    )

    override fun load(): QuickSpendSettings {
        val budgetId = preferences.getLong(KEY_BUDGET_ID, NO_BUDGET_ID)
            .takeIf { it != NO_BUDGET_ID }
        return QuickSpendSettings(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            selectedBudgetId = budgetId,
            selectedAppPackages = preferences.getStringSet(KEY_PACKAGES, emptySet()).orEmpty()
        )
    }

    override fun save(settings: QuickSpendSettings) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putLong(KEY_BUDGET_ID, settings.selectedBudgetId ?: NO_BUDGET_ID)
            .putStringSet(KEY_PACKAGES, settings.selectedAppPackages)
            .apply()
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_BUDGET_ID = "selected_budget_id"
        const val KEY_PACKAGES = "selected_app_packages"
        const val NO_BUDGET_ID = -1L
    }
}
