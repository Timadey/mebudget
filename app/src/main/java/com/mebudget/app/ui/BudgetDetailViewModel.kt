package com.mebudget.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mebudget.app.data.BudgetDetail
import com.mebudget.app.data.BudgetEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetDetailViewModel(application: Application) : ViewModel() {
    private val repository = application.budgetRepository()
    private val selectedBudgetId = MutableStateFlow<Long?>(null)
    private val transientError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BudgetDetailUiState> = combine(
        selectedBudgetId,
        selectedBudgetId.flatMapLatest { budgetId ->
            if (budgetId == null) {
                flowOf<BudgetDetail?>(null)
            } else {
                repository.observeBudgetDetail(budgetId)
            }
        },
        transientError
    ) { currentBudgetId, detail, error ->
        BudgetDetailUiState(
            selectedBudgetId = currentBudgetId,
            selectedBudgetDetail = detail,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetDetailUiState()
    )

    fun openBudget(budgetId: Long) {
        selectedBudgetId.value = budgetId
    }

    fun closeBudget() {
        selectedBudgetId.value = null
    }

    fun clearError() {
        transientError.value = null
    }

    fun updateBudgetSettings(budget: BudgetEntity) {
        viewModelScope.launch {
            runCatching {
                repository.updateBudget(budget)
            }.onFailure(::emitError)
        }
    }

    fun addWallet(budgetId: Long, draft: WalletDraft) {
        viewModelScope.launch {
            runCatching {
                repository.addWallet(
                    budgetId = budgetId,
                    name = draft.name.requireNotBlank("Wallet name is required."),
                    plannedAmount = draft.plannedAmount.parseAmount()
                )
            }.onFailure(::emitError)
        }
    }

    fun saveWallet(draft: WalletDraft) {
        val walletId = draft.walletId ?: return
        viewModelScope.launch {
            runCatching {
                repository.updateWallet(
                    walletId = walletId,
                    name = draft.name.requireNotBlank("Wallet name is required."),
                    plannedAmount = draft.plannedAmount.parseAmount()
                )
            }.onFailure(::emitError)
        }
    }

    fun archiveWallet(walletId: Long, archived: Boolean) {
        viewModelScope.launch {
            repository.setWalletArchived(walletId, archived)
        }
    }

    fun moveWallet(walletId: Long, direction: Int) {
        viewModelScope.launch {
            repository.moveWallet(walletId, direction)
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

    fun addTransfer(budgetId: Long, draft: TransferDraft) {
        val sourceWalletId = draft.sourceWalletId
            ?: return emitError(IllegalArgumentException("Choose a source wallet."))
        val destinationWalletId = draft.destinationWalletId
            ?: return emitError(IllegalArgumentException("Choose a destination wallet."))
        viewModelScope.launch {
            repository.addTransfer(
                budgetId = budgetId,
                sourceWalletId = sourceWalletId,
                destinationWalletId = destinationWalletId,
                amount = draft.amount.parseAmount(),
                dateEpochDay = draft.date.parseRequiredDate(),
                note = draft.note
            ).onFailure(::emitError)
        }
    }

    fun addAdjustment(budgetId: Long, draft: AdjustmentDraft) {
        val walletId = draft.walletId ?: return emitError(IllegalArgumentException("Choose a wallet."))
        viewModelScope.launch {
            repository.addAdjustment(
                budgetId = budgetId,
                walletId = walletId,
                signedAmount = draft.signedAmount.parseSignedAmount(),
                dateEpochDay = draft.date.parseRequiredDate(),
                note = draft.note
            ).onFailure(::emitError)
        }
    }

    fun updateTransaction(editorState: TransactionEditorState) {
        viewModelScope.launch {
            repository.updateTransaction(
                transactionId = editorState.transactionId,
                amount = editorState.amount.parseAmountAllowSigned(editorState.type),
                dateEpochDay = editorState.date.parseRequiredDate(),
                sourceWalletId = editorState.sourceWalletId,
                destinationWalletId = editorState.destinationWalletId,
                note = editorState.note
            ).onFailure(::emitError)
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
        }
    }

    private fun emitError(throwable: Throwable) {
        transientError.value = throwable.message ?: "Something went wrong."
    }
}
