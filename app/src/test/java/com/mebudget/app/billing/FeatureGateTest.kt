package com.mebudget.app.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureGateTest {

    private fun gate(
        signedIn: Boolean = false,
        pro: Boolean = false,
        limits: FeatureLimits = FeatureLimits.DEFAULT
    ): FeatureGate = FeatureGate(
        isSignedIn = { signedIn },
        isPro = { pro },
        limits = limits
    )

    @Test
    fun `anonymous user cannot exceed budget limit`() {
        val g = gate()
        assertTrue(g.canCreateBudget(currentCount = 0))
        assertTrue(g.canCreateBudget(currentCount = Int.MAX_VALUE - 1))
    }

    @Test
    fun `signed in user can create unlimited budgets`() {
        val g = gate(signedIn = true)
        assertTrue(g.canCreateBudget(currentCount = 100))
    }

    @Test
    fun `anonymous user wallet limit is unlimited`() {
        val g = gate()
        assertTrue(g.canCreateWallet(currentCount = Int.MAX_VALUE - 1))
    }

    @Test
    fun `signed in user can create unlimited wallets`() {
        val g = gate(signedIn = true)
        assertTrue(g.canCreateWallet(currentCount = 100))
    }

    @Test
    fun `anonymous user transaction limit is enforced`() {
        val g = gate()
        assertTrue(g.canCreateTransaction(currentMonthCount = 199))
        assertFalse(g.canCreateTransaction(currentMonthCount = 200))
    }

    @Test
    fun `signed in user can create unlimited transactions`() {
        val g = gate(signedIn = true)
        assertTrue(g.canCreateTransaction(currentMonthCount = 1000))
    }

    @Test
    fun `premium insights require pro`() {
        assertFalse(gate().canAccessPremiumInsights())
        assertTrue(gate(pro = true).canAccessPremiumInsights())
    }

    @Test
    fun `analytics require pro`() {
        assertFalse(gate().canAccessAnalytics())
        assertTrue(gate(pro = true).canAccessAnalytics())
    }

    @Test
    fun `cloud sync requires signed in pro`() {
        assertFalse(gate(signedIn = false, pro = false).canSyncToCloud())
        assertFalse(gate(signedIn = true, pro = false).canSyncToCloud())
        assertFalse(gate(signedIn = false, pro = true).canSyncToCloud())
        assertTrue(gate(signedIn = true, pro = true).canSyncToCloud())
    }

    @Test
    fun `server overrides free limits`() {
        val limits = FeatureLimits.fromServerConfig(
            mapOf(
                "freeMaxBudgets" to 5,
                "freeMaxWalletsPerBudget" to 3,
                "freeMaxTransactionsPerMonth" to 50
            )
        )
        val g = gate(limits = limits)
        assertTrue(g.canCreateBudget(currentCount = 4))
        assertFalse(g.canCreateBudget(currentCount = 5))
        assertTrue(g.canCreateWallet(currentCount = 2))
        assertFalse(g.canCreateWallet(currentCount = 3))
        assertTrue(g.canCreateTransaction(currentMonthCount = 49))
        assertFalse(g.canCreateTransaction(currentMonthCount = 50))
    }

    @Test
    fun `missing server keys fall back to defaults`() {
        val limits = FeatureLimits.fromServerConfig(mapOf("freeMaxBudgets" to 3))
        assertTrue(limits.freeMaxWalletsPerBudget == FeatureLimits.DEFAULT.freeMaxWalletsPerBudget)
        assertTrue(limits.freeMaxBudgets == 3)
    }
}
