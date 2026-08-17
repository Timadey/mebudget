package com.mebudget.app.data.sync

import com.google.gson.JsonObject
import com.mebudget.app.billing.BillingPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Loads Pro plan prices from the server `config` collection ("plans" record).
 *
 * Defaults to [BillingPlan.DEFAULTS] until the server is reachable, then caches
 * the fetched prices for [cacheDurationMillis] so the app does not hammer the
 * server on every subscription screen open.
 */
class PricingConfigManager(
    private val pocketBaseClient: PocketBaseClient
) {
    private val _plans = MutableStateFlow(BillingPlan.DEFAULTS)
    val plans: StateFlow<List<BillingPlan>> = _plans.asStateFlow()

    private var lastFetchTime: Long = 0
    private val cacheDurationMillis = 24 * 60 * 60 * 1000L

    /** Fetches fresh prices if the cache has expired. Never throws. */
    suspend fun refreshPlans() {
        val now = System.currentTimeMillis()
        if (now - lastFetchTime < cacheDurationMillis) return

        try {
            val response = pocketBaseClient.api.getList(
                collection = "config",
                page = 1,
                perPage = 1,
                filter = "key = 'plans'"
            )
            val record = response.items.firstOrNull() ?: return
            val value = record.get("value")?.takeIf { !it.isJsonNull }?.asJsonObject ?: return
            val monthly = value.getAsJsonObject("pro_monthly")
            val annual = value.getAsJsonObject("pro_annual")
            _plans.value = listOf(
                BillingPlan.MONTHLY.copy(price = monthly?.get("priceKobo")?.asLong ?: BillingPlan.MONTHLY.price),
                BillingPlan.ANNUAL.copy(price = annual?.get("priceKobo")?.asLong ?: BillingPlan.ANNUAL.price)
            )
            lastFetchTime = now
        } catch (_: Exception) {
        }
    }
}