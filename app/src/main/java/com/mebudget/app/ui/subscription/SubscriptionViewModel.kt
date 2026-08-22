package com.mebudget.app.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.mebudget.app.billing.BillingPlan
import com.mebudget.app.billing.SubscriptionManager
import com.mebudget.app.data.sync.PocketBaseClient
import com.mebudget.app.data.sync.PricingConfigManager
import com.mebudget.app.data.sync.models.CheckoutResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SubscriptionUiState(
    val plans: List<BillingPlan> = BillingPlan.DEFAULTS,
    val selectedPlan: BillingPlan? = null,
    val checkoutUrl: String? = null,
    val isActivating: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class SubscriptionViewModel(
    private val pricingConfigManager: PricingConfigManager,
    private val pocketBaseClient: PocketBaseClient,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SubscriptionUiState(plans = pricingConfigManager.plans.value)
    )
    val uiState: StateFlow<SubscriptionUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(plans = pricingConfigManager.plans.first())
            pricingConfigManager.refreshPlans()
            _uiState.value = _uiState.value.copy(plans = pricingConfigManager.plans.value)
        }
    }

    fun selectPlan(plan: BillingPlan) {
        _uiState.value = _uiState.value.copy(selectedPlan = plan)
    }

    /** Asks the server to initialize a Paystack checkout; opens the WebView on success. */
    fun startCheckout() {
        val plan = _uiState.value.selectedPlan ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = runCatching {
                pocketBaseClient.api.createCheckout(
                    JsonObject().apply { addProperty("plan", plan.id) }
                )
            }
            result.fold(
                onSuccess = { response: CheckoutResponse ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        checkoutUrl = response.authorizationUrl
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to start checkout. Try again."
                    )
                }
            )
        }
    }

    /** Confirmed by the WebView on /checkout-success. */
    fun onPaymentSucceeded() {
        _uiState.value = _uiState.value.copy(checkoutUrl = null, isActivating = true)
        viewModelScope.launch {
            // Poll until Pro activates or timeout (2 minutes).
            val maxAttempts = 24 // 24 * 5s = 120s = 2 min
            repeat(maxAttempts) {
                kotlinx.coroutines.delay(5000)
                subscriptionManager.refresh()
                if (subscriptionManager.isPro.value) {
                    _uiState.value = _uiState.value.copy(isActivating = false, isSuccess = true)
                    return@launch
                }
            }
            // Timeout — still show success but let user know status may take a moment
            _uiState.value = _uiState.value.copy(isActivating = false, isSuccess = true)
        }
    }

    /** WebView hit /checkout-cancel or the user abandoned. */
    fun onPaymentCanceled() {
        _uiState.value = _uiState.value.copy(checkoutUrl = null, error = "Payment was cancelled.")
    }

    /** Cancel the current subscription. Sets status to cancelled on server. */
    fun cancelSubscription() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = subscriptionManager.cancelSubscription()
            result.fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        isSuccess = true
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to cancel subscription."
                    )
                }
            )
        }
    }
}