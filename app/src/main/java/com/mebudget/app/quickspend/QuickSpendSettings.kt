package com.mebudget.app.quickspend

data class QuickSpendSettings(
    val enabled: Boolean = false,
    val selectedBudgetId: Long? = null,
    val selectedAppPackages: Set<String> = emptySet()
) {
    fun isSetupComplete(
        overlayPermissionGranted: Boolean,
        usageAccessGranted: Boolean
    ): Boolean {
        return enabled &&
            selectedBudgetId != null &&
            selectedAppPackages.isNotEmpty() &&
            overlayPermissionGranted &&
            usageAccessGranted
    }

    fun matchesForegroundPackage(packageName: String?): Boolean {
        return packageName != null && selectedAppPackages.contains(packageName)
    }
}
