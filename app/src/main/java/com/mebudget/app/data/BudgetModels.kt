package com.mebudget.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class NegativeBalanceRule {
    ALLOW,
    WARN,
    BLOCK
}

enum class TransactionType {
    EXPENSE,
    TRANSFER,
    ADJUSTMENT
}

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startDateEpochDay: Long? = null,
    val endDateEpochDay: Long? = null,
    val negativeBalanceRule: NegativeBalanceRule = NegativeBalanceRule.WARN,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "wallets",
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("budgetId")]
)
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val budgetId: Long,
    val name: String,
    val plannedAmount: Long,
    val sortOrder: Int,
    val archived: Boolean = false
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("budgetId"), Index("sourceWalletId"), Index("destinationWalletId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val budgetId: Long,
    val type: TransactionType,
    val amount: Long,
    val dateEpochDay: Long,
    val sourceWalletId: Long? = null,
    val destinationWalletId: Long? = null,
    val note: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class BudgetSummary(
    val id: Long,
    val name: String,
    val startDateEpochDay: Long?,
    val endDateEpochDay: Long?,
    val walletCount: Int,
    val activeWalletCount: Int,
    val totalBalance: Long
)

data class WalletSummary(
    val id: Long,
    val budgetId: Long,
    val name: String,
    val plannedAmount: Long,
    val balance: Long,
    val sortOrder: Int,
    val archived: Boolean,
    val warning: Boolean
)

data class TransactionSummary(
    val id: Long,
    val budgetId: Long,
    val type: TransactionType,
    val amount: Long,
    val dateEpochDay: Long,
    val sourceWalletId: Long?,
    val destinationWalletId: Long?,
    val sourceWalletName: String?,
    val destinationWalletName: String?,
    val note: String?
)

data class BudgetDetail(
    val budget: BudgetEntity,
    val wallets: List<WalletSummary>,
    val transactions: List<TransactionSummary>
)

