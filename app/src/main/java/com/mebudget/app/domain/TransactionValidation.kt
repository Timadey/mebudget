package com.mebudget.app.domain

import com.mebudget.app.data.BudgetEntity
import com.mebudget.app.data.NegativeBalanceRule
import com.mebudget.app.data.TransactionEntity
import com.mebudget.app.data.TransactionType
import com.mebudget.app.data.WalletEntity
import com.mebudget.app.data.computeBalances

data class TransactionValidationContext(
    val budget: BudgetEntity,
    val wallets: List<WalletEntity>,
    val currentTransactions: List<TransactionEntity>
)

fun validateTransactionChange(
    context: TransactionValidationContext,
    draft: TransactionEntity,
    replacingId: Long? = null
): Result<Unit> {
    if (draft.type != TransactionType.ADJUSTMENT && draft.amount <= 0) {
        return Result.failure(IllegalArgumentException("Amount must be greater than zero."))
    }
    if (draft.type == TransactionType.ADJUSTMENT && draft.amount == 0L) {
        return Result.failure(IllegalArgumentException("Adjustment cannot be zero."))
    }
    if (draft.type == TransactionType.TRANSFER && draft.sourceWalletId == draft.destinationWalletId) {
        return Result.failure(IllegalArgumentException("Choose two different wallets."))
    }

    val walletIds = context.wallets.mapTo(mutableSetOf()) { it.id }
    val referencedWalletIds = listOfNotNull(draft.sourceWalletId, draft.destinationWalletId)
    if (!walletIds.containsAll(referencedWalletIds)) {
        return Result.failure(IllegalArgumentException("Choose wallets from this budget."))
    }

    val projectedTransactions = context.currentTransactions.filterNot { it.id == replacingId } + draft
    val balances = computeBalances(context.wallets, projectedTransactions)
    val violated = when (context.budget.negativeBalanceRule) {
        NegativeBalanceRule.ALLOW -> null
        NegativeBalanceRule.WARN -> null
        NegativeBalanceRule.BLOCK -> balances.entries.firstOrNull { it.value < 0 }
    }
    if (violated != null) {
        val walletName = context.wallets.firstOrNull { it.id == violated.key }?.name ?: "wallet"
        return Result.failure(IllegalArgumentException("$walletName cannot go below zero."))
    }

    return Result.success(Unit)
}
