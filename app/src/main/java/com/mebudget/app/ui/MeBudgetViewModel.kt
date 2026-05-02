package com.mebudget.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mebudget.app.data.AppDatabase
import com.mebudget.app.data.BudgetDetail
import com.mebudget.app.data.BudgetEntity
import com.mebudget.app.data.BudgetRepository
import com.mebudget.app.data.BudgetSummary
import com.mebudget.app.data.GlobalInsightSummary
import com.mebudget.app.data.NegativeBalanceRule
import com.mebudget.app.data.TransactionSummary
import com.mebudget.app.data.TransactionType
import com.mebudget.app.data.WalletSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

data class MeBudgetUiState(
    val budgets: List<BudgetSummary> = emptyList(),
    val globalInsights: GlobalInsightSummary? = null,
    val selectedBudgetId: Long? = null,
    val selectedBudgetDetail: BudgetDetail? = null,
    val errorMessage: String? = null,
    val privacyModeEnabled: Boolean = false
)

data class BudgetDraft(
    val name: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val negativeBalanceRule: NegativeBalanceRule = NegativeBalanceRule.WARN
)

data class WalletDraft(
    val walletId: Long? = null,
    val name: String = "",
    val plannedAmount: String = ""
)

data class ExpenseDraft(
    val walletId: Long? = null,
    val amount: String = "",
    val date: String = LocalDate.now().toString(),
    val note: String = ""
)

data class TransferDraft(
    val sourceWalletId: Long? = null,
    val destinationWalletId: Long? = null,
    val amount: String = "",
    val date: String = LocalDate.now().toString(),
    val note: String = ""
)

data class AdjustmentDraft(
    val walletId: Long? = null,
    val signedAmount: String = "",
    val date: String = LocalDate.now().toString(),
    val note: String = ""
)

data class TransactionEditorState(
    val transactionId: Long,
    val type: TransactionType,
    val amount: String,
    val date: String,
    val sourceWalletId: Long?,
    val destinationWalletId: Long?,
    val note: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class MeBudgetViewModel(application: Application) : ViewModel() {
    private val privacyPrefs = application.getSharedPreferences(PRIVACY_PREFS_NAME, Context.MODE_PRIVATE)
    private val repository = AppDatabase.getInstance(application).run {
        BudgetRepository(budgetDao(), walletDao(), transactionDao())
    }

    private val selectedBudgetId = MutableStateFlow<Long?>(null)
    private val transientError = MutableStateFlow<String?>(null)
    private val privacyModeEnabled = MutableStateFlow(
        privacyPrefs.getBoolean(PRIVACY_MODE_KEY, false)
    )

    private val budgetSelectionState = combine(
        selectedBudgetId,
        selectedBudgetId.flatMapLatest { budgetId ->
            if (budgetId == null) flowOf(null) else repository.observeBudgetDetail(budgetId)
        },
        transientError
    ) { currentBudgetId, detail, error ->
        Triple(currentBudgetId, detail, error)
    }

    val uiState: StateFlow<MeBudgetUiState> = combine(
        repository.observeBudgetSummaries(),
        repository.observeGlobalInsights(),
        budgetSelectionState,
        privacyModeEnabled
    ) { budgets, globalInsights, budgetSelection, privacyEnabled ->
        MeBudgetUiState(
            budgets = budgets,
            globalInsights = globalInsights,
            selectedBudgetId = budgetSelection.first,
            selectedBudgetDetail = budgetSelection.second,
            errorMessage = budgetSelection.third,
            privacyModeEnabled = privacyEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MeBudgetUiState()
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

    fun togglePrivacyMode() {
        val newValue = !privacyModeEnabled.value
        privacyModeEnabled.value = newValue
        privacyPrefs.edit().putBoolean(PRIVACY_MODE_KEY, newValue).apply()
    }

    fun fetchWalletsForBudget(budgetId: Long): kotlinx.coroutines.flow.Flow<List<com.mebudget.app.data.WalletEntity>> {
        return repository.observeWalletsForBudget(budgetId)
    }

    fun createBudget(draft: BudgetDraft) {
        viewModelScope.launch {
            runCatching {
                require(draft.name.isNotBlank()) { "Budget name is required." }
                repository.createBudget(
                    name = draft.name,
                    startDateEpochDay = draft.startDate.parseOptionalDate(),
                    endDateEpochDay = draft.endDate.parseOptionalDate(),
                    negativeBalanceRule = draft.negativeBalanceRule
                )
            }.onSuccess { newBudgetId ->
                selectedBudgetId.value = newBudgetId
            }.onFailure(::emitError)
        }
    }

    fun duplicateBudget(sourceBudgetId: Long, newName: String) {
        viewModelScope.launch {
            runCatching {
                require(newName.isNotBlank()) { "Duplicate budget needs a name." }
                repository.duplicateBudget(sourceBudgetId, newName)
            }.onSuccess { duplicatedBudgetId ->
                if (duplicatedBudgetId != 0L) {
                    selectedBudgetId.value = duplicatedBudgetId
                }
            }.onFailure(::emitError)
        }
    }

    fun updateBudgetSettings(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.updateBudget(budget)
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

private const val PRIVACY_PREFS_NAME = "mebudget_privacy"
private const val PRIVACY_MODE_KEY = "privacy_mode_enabled"

class MeBudgetViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MeBudgetViewModel(application) as T
    }
}

private fun String.requireNotBlank(message: String): String {
    return trim().ifBlank { throw IllegalArgumentException(message) }
}

private fun String.parseAmount(): Long {
    val cleaned = replace(",", "").trim()
    return cleaned.toLongOrNull()?.takeIf { it > 0 }
        ?: throw IllegalArgumentException("Enter a valid amount.")
}

private fun String.parseSignedAmount(): Long {
    val cleaned = replace(",", "").trim()
    return cleaned.toLongOrNull()?.takeIf { it != 0L }
        ?: throw IllegalArgumentException("Enter a valid signed amount.")
}

private fun String.parseAmountAllowSigned(type: TransactionType): Long {
    return when (type) {
        TransactionType.ADJUSTMENT -> parseSignedAmount()
        else -> parseAmount()
    }
}

private fun String.parseRequiredDate(): Long {
    return runCatching { LocalDate.parse(trim()) }.getOrElse {
        throw IllegalArgumentException("Use date format YYYY-MM-DD.")
    }.toEpochDay()
}

private fun String.parseOptionalDate(): Long? {
    if (isBlank()) return null
    return parseRequiredDate()
}

fun TransactionSummary.toEditorState(): TransactionEditorState {
    return TransactionEditorState(
        transactionId = id,
        type = type,
        amount = amount.toString(),
        date = LocalDate.ofEpochDay(dateEpochDay).toString(),
        sourceWalletId = sourceWalletId,
        destinationWalletId = destinationWalletId,
        note = note.orEmpty()
    )
}
