package com.mebudget.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebudget.app.data.GlobalInsightSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetsViewModel(application: Application) : ViewModel() {
    private val repository = application.budgetRepository()
    private val transientError = MutableStateFlow<String?>(null)
    private val pendingBudgetIdToOpen = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<BudgetsUiState> = combine(
        repository.observeBudgetSummaries(),
        repository.observeGlobalInsights(),
        pendingBudgetIdToOpen,
        transientError
    ) { budgets, globalInsights: GlobalInsightSummary, pendingBudgetId, error ->
        BudgetsUiState(
            budgets = budgets,
            globalInsights = globalInsights,
            pendingBudgetIdToOpen = pendingBudgetId,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetsUiState()
    )

    fun consumePendingBudgetNavigation() {
        pendingBudgetIdToOpen.value = null
    }

    fun clearError() {
        transientError.value = null
    }

    fun createBudget(draft: BudgetDraft) {
        viewModelScope.launch {
            runCatching {
                repository.createBudget(
                    name = draft.name,
                    startDateEpochDay = draft.startDate.parseOptionalDate(),
                    endDateEpochDay = draft.endDate.parseOptionalDate(),
                    negativeBalanceRule = draft.negativeBalanceRule
                )
            }.onSuccess { newBudgetId ->
                pendingBudgetIdToOpen.value = newBudgetId
            }.onFailure(::emitError)
        }
    }

    fun duplicateBudget(sourceBudgetId: Long, newName: String) {
        viewModelScope.launch {
            runCatching {
                repository.duplicateBudget(sourceBudgetId, newName)
            }.onSuccess { duplicatedBudgetId ->
                if (duplicatedBudgetId != 0L) {
                    pendingBudgetIdToOpen.value = duplicatedBudgetId
                }
            }.onFailure(::emitError)
        }
    }

    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch {
            repository.deleteBudget(budgetId)
        }
    }

    fun addExpense(budgetId: Long, draft: ExpenseDraft) {
        val walletId = draft.walletId ?: return emitError(IllegalArgumentException("Choose a wallet."))
        viewModelScope.launch {
            repository.addExpense(
                budgetId = budgetId,
                walletId = walletId,
                amount = draft.amount.parseAmount(),
                dateEpochDay = draft.date.parseRequiredDate(),
                note = draft.note
            ).onFailure(::emitError)
        }
    }

    private fun emitError(throwable: Throwable) {
        transientError.value = throwable.message ?: "Something went wrong."
    }
}
