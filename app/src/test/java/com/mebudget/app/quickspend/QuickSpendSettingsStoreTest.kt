package com.mebudget.app.quickspend

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickSpendSettingsStoreTest {

    @Test
    fun `memory store round trips settings`() {
        val store = InMemoryQuickSpendSettingsStore()
        val expected = QuickSpendSettings(
            enabled = true,
            selectedBudgetId = 42,
            selectedAppPackages = setOf("com.bank.one", "com.bank.two")
        )

        store.save(expected)

        assertEquals(expected, store.load())
    }
}

private class InMemoryQuickSpendSettingsStore : QuickSpendSettingsStore {
    private var current = QuickSpendSettings()

    override fun load(): QuickSpendSettings = current

    override fun save(settings: QuickSpendSettings) {
        current = settings
    }
}
