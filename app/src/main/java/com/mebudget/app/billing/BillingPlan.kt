package com.mebudget.app.billing

sealed class BillingPlan(
    val id: String,
    val displayName: String,
    val price: Long, // Amount in kobo (₦1,500 = 150000)
    val interval: String
) {
    object Monthly : BillingPlan(
        id = "pro_monthly",
        displayName = "Pro Monthly",
        price = 150000, // ₦1,500
        interval = "monthly"
    )

    object Annual : BillingPlan(
        id = "pro_annual",
        displayName = "Pro Annual",
        price = 1440000, // ₦14,400 (₦1,200/month)
        interval = "annually"
    )

    companion object {
        fun fromId(id: String): BillingPlan? = when (id) {
            Monthly.id -> Monthly
            Annual.id -> Annual
            else -> null
        }
    }
}