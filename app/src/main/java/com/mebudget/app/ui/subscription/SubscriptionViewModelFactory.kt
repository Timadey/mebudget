package com.mebudget.app.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mebudget.app.billing.PaystackManager

class SubscriptionViewModelFactory(
    private val paystackManager: PaystackManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SubscriptionViewModel(paystackManager) as T
    }
}