package com.mebudget.app.billing

/**
 * Feature limits for the free tier. Hardcoded defaults are overridden by the
 * server `config` collection when reachable (see [fromServerConfig]).
 */
data class FeatureLimits(
    val freeMaxBudgets: Int = 2,
    val freeMaxWalletsPerBudget: Int = 5,
    val freeMaxTransactionsPerMonth: Int = 100
) {
    companion object {
        val DEFAULT = FeatureLimits()

        fun fromServerConfig(config: Map<String, Any>): FeatureLimits {
            return FeatureLimits(
                freeMaxBudgets = (config["freeMaxBudgets"] as? Number)?.toInt() ?: DEFAULT.freeMaxBudgets,
                freeMaxWalletsPerBudget = (config["freeMaxWalletsPerBudget"] as? Number)?.toInt() ?: DEFAULT.freeMaxWalletsPerBudget,
                freeMaxTransactionsPerMonth = (config["freeMaxTransactionsPerMonth"] as? Number)?.toInt() ?: DEFAULT.freeMaxTransactionsPerMonth
            )
        }
    }
}