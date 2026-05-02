package com.mebudget.app.ui

import com.mebudget.app.data.NegativeBalanceRule
import com.mebudget.app.data.TransactionSummary
import com.mebudget.app.data.TransactionType
import java.time.LocalDate

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
