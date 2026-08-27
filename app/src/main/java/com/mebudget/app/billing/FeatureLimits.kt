package com.mebudget.app.billing

/**
 * Feature limits for the free tier. Hardcoded defaults are overridden by the
 * server `config` collection when reachable (see [fromServerConfig]).
 */
data class FeatureLimits(
    val freeMaxBudgets: Int = Int.MAX_VALUE,
    val freeMaxWalletsPerBudget: Int = Int.MAX_VALUE,
    val freeMaxTransactionsPerMonth: Int = 200
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