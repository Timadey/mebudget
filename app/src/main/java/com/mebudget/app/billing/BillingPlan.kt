package com.mebudget.app.billing

/**
 * A purchasable Pro plan. [price] is in kobo (₦1,500 = 150000).
 *
 * [DEFAULTS] are the built-in fallback prices; they can be overridden from the
 * server `config` "plans" record (see PricingConfigManager).
 */
data class BillingPlan(
    val id: String,
    val displayName: String,
    val price: Long,
    val interval: String
) {
    companion object {
        val MONTHLY = BillingPlan(
            id = "pro_monthly",
            displayName = "Pro Monthly",
            price = 150000, // ₦1,500
            interval = "monthly"
        )
        val ANNUAL = BillingPlan(
            id = "pro_annual",
            displayName = "Pro Annual",
            price = 1440000, // ₦14,400 (₦1,200/month)
            interval = "annually"
        )
        val DEFAULTS: List<BillingPlan> = listOf(MONTHLY, ANNUAL)

        fun fromId(id: String): BillingPlan? = when (id) {
            MONTHLY.id -> MONTHLY
            ANNUAL.id -> ANNUAL
            else -> null
        }
    }
}