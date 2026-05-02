package com.mebudget.app.domain

import com.mebudget.app.data.TransactionEntity
import com.mebudget.app.data.TransactionType

fun createExpenseTransaction(
    budgetId: Long,
    walletId: Long,
    amount: Long,
    dateEpochDay: Long,
    note: String?
): TransactionEntity {
    return TransactionEntity(
        budgetId = budgetId,
        type = TransactionType.EXPENSE,
        amount = amount,
        dateEpochDay = dateEpochDay,
        sourceWalletId = walletId,
        note = note.normalizedNote()
    )
}

fun createTransferTransaction(
    budgetId: Long,
    sourceWalletId: Long,
    destinationWalletId: Long,
    amount: Long,
    dateEpochDay: Long,
    note: String?
): TransactionEntity {
    return TransactionEntity(
        budgetId = budgetId,
        type = TransactionType.TRANSFER,
        amount = amount,
        dateEpochDay = dateEpochDay,
        sourceWalletId = sourceWalletId,
        destinationWalletId = destinationWalletId,
        note = note.normalizedNote()
    )
}

fun createAdjustmentTransaction(
    budgetId: Long,
    walletId: Long,
    signedAmount: Long,
    dateEpochDay: Long,
    note: String?
): TransactionEntity {
    return TransactionEntity(
        budgetId = budgetId,
        type = TransactionType.ADJUSTMENT,
        amount = signedAmount,
        dateEpochDay = dateEpochDay,
        sourceWalletId = walletId,
        note = note.normalizedNote()
    )
}

fun updateTransactionCommand(
    existing: TransactionEntity,
    amount: Long,
    dateEpochDay: Long,
    sourceWalletId: Long?,
    destinationWalletId: Long?,
    note: String?
): TransactionEntity {
    return existing.copy(
        amount = amount,
        dateEpochDay = dateEpochDay,
        sourceWalletId = sourceWalletId,
        destinationWalletId = destinationWalletId,
        note = note.normalizedNote()
    )
}

private fun String?.normalizedNote(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
