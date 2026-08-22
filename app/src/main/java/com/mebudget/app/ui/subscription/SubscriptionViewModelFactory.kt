package com.mebudget.app.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mebudget.app.billing.SubscriptionManager
import com.mebudget.app.data.sync.PocketBaseClient
import com.mebudget.app.data.sync.PricingConfigManager

class SubscriptionViewModelFactory(
    private val pricingConfigManager: PricingConfigManager,
    private val pocketBaseClient: PocketBaseClient,
    private val subscriptionManager: SubscriptionManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SubscriptionViewModel(pricingConfigManager, pocketBaseClient, subscriptionManager) as T
    }
}