package com.mebudget.app.quickspend

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickSpendSettingsTest {

    @Test
    fun `setup is complete only when enabled budget apps and permissions are present`() {
        val incomplete = QuickSpendSettings(
            enabled = true,
            selectedBudgetId = 1,
            selectedAppPackages = setOf("com.bank.app")
        )

        assertFalse(
            incomplete.isSetupComplete(
                overlayPermissionGranted = true,
                usageAccessGranted = false
            )
        )

        assertTrue(
            incomplete.isSetupComplete(
                overlayPermissionGranted = true,
                usageAccessGranted = true
            )
        )
    }

    @Test
    fun `setup is incomplete when disabled or missing selected data`() {
        assertFalse(
            QuickSpendSettings(
                enabled = false,
                selectedBudgetId = 1,
                selectedAppPackages = setOf("com.bank.app")
            ).isSetupComplete(true, true)
        )

        assertFalse(
            QuickSpendSettings(
                enabled = true,
                selectedBudgetId = null,
                selectedAppPackages = setOf("com.bank.app")
            ).isSetupComplete(true, true)
        )

        assertFalse(
            QuickSpendSettings(
                enabled = true,
                selectedBudgetId = 1,
                selectedAppPackages = emptySet()
            ).isSetupComplete(true, true)
        )
    }

    @Test
    fun `selected foreground app match is exact package match`() {
        val settings = QuickSpendSettings(
            enabled = true,
            selectedBudgetId = 1,
            selectedAppPackages = setOf("com.bank.app")
        )

        assertTrue(settings.matchesForegroundPackage("com.bank.app"))
        assertFalse(settings.matchesForegroundPackage("com.other.app"))
        assertFalse(settings.matchesForegroundPackage(null))
    }
}
