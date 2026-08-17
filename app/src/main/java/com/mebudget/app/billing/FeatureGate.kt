package com.mebudget.app.billing

/**
 * Enforces free/pro tier limits based on injected sources of truth.
 *
 * Auth and subscription status are kept decoupled: [isSignedIn] and [isPro]
 * are provided as lambdas so the gate is pure and trivially testable, and the
 * caller decides where the state comes from (AuthManager, SubscriptionManager).
 */
class FeatureGate(
    private val isSignedIn: () -> Boolean,
    private val isPro: () -> Boolean,
    private val limits: FeatureLimits = FeatureLimits.DEFAULT
) {

    fun canCreateBudget(currentCount: Int): Boolean {
        if (isPro()) return true
        return currentCount < limits.freeMaxBudgets
    }

    fun canCreateWallet(currentCount: Int): Boolean {
        if (isPro()) return true
        return currentCount < limits.freeMaxWalletsPerBudget
    }

    fun canCreateTransaction(currentMonthCount: Int): Boolean {
        if (isPro()) return true
        return currentMonthCount < limits.freeMaxTransactionsPerMonth
    }

    fun canAccessPremiumInsights(): Boolean = isPro()

    fun canSyncToCloud(): Boolean = isSignedIn() && isPro()
}