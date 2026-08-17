package com.mebudget.app.ui.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebudget.app.billing.BillingPlan
import com.mebudget.app.billing.PaystackManager
import co.paystack.android.model.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SubscriptionUiState(
    val plans: List<BillingPlan> = emptyList(),
    val selectedPlan: BillingPlan? = null,
    val email: String = "",
    val cardNumber: String = "",
    val expiryMonth: String = "",
    val expiryYear: String = "",
    val cvv: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class SubscriptionViewModel(
    private val paystackManager: PaystackManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SubscriptionUiState(plans = paystackManager.getAvailablePlans())
    )
    val uiState: StateFlow<SubscriptionUiState> = _uiState

    fun selectPlan(plan: BillingPlan) {
        _uiState.value = _uiState.value.copy(selectedPlan = plan)
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onCardNumberChanged(cardNumber: String) {
        _uiState.value = _uiState.value.copy(cardNumber = cardNumber)
    }

    fun onExpiryMonthChanged(value: String) {
        _uiState.value = _uiState.value.copy(expiryMonth = value)
    }

    fun onExpiryYearChanged(value: String) {
        _uiState.value = _uiState.value.copy(expiryYear = value)
    }

    fun onCvvChanged(value: String) {
        _uiState.value = _uiState.value.copy(cvv = value)
    }

    fun subscribe(activity: Activity) {
        val state = _uiState.value
        val plan = state.selectedPlan ?: return

        val card = try {
            Card.Builder(
                state.cardNumber,
                state.expiryMonth.toInt(),
                state.expiryYear.toInt(),
                state.cvv
            ).build()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Check the card details and try again.")
            return
        }

        if (!card.isValid) {
            _uiState.value = _uiState.value.copy(error = "That card looks invalid. Check the details and try again.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = paystackManager.chargeCard(
                activity = activity,
                amount = plan.price,
                email = state.email.trim(),
                card = card
            )
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Subscription failed. Try again."
                    )
                }
            )
        }
    }
}