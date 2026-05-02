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

data class WalletBudgetInsight(
    val walletId: Long,
    val walletKey: String,
    val walletName: String,
    val plannedAmount: Long,
    val spentTotal: Long,
    val transferInTotal: Long,
    val transferInCount: Int,
    val transferOutTotal: Long,
    val transferOutCount: Int,
    val adjustmentTotal: Long,
    val endingBalance: Long,
    val varianceFromPlan: Long,
    val overspent: Boolean,
    val transactionCount: Int,
    val transferCount: Int
)

data class TransferPathInsight(
    val sourceWalletId: Long,
    val destinationWalletId: Long,
    val sourceWalletName: String,
    val destinationWalletName: String,
    val transferCount: Int,
    val totalAmount: Long
)

data class InsightObservation(
    val title: String,
    val message: String
)

data class BudgetInsightSummary(
    val totalPlanned: Long,
    val totalSpent: Long,
    val totalTransferred: Long,
    val totalAdjusted: Long,
    val totalEndingBalance: Long,
    val overspentWallets: List<WalletBudgetInsight>,
    val mostRescuedWallet: WalletBudgetInsight?,
    val topDonorWallet: WalletBudgetInsight?,
    val topTransferPath: TransferPathInsight?,
    val observations: List<InsightObservation>,
    val walletInsights: List<WalletBudgetInsight>,
    val transferPaths: List<TransferPathInsight>
)

data class WalletHistoryInsight(
    val walletKey: String,
    val displayName: String,
    val budgetsAppearedIn: Int,
    val averagePlannedAmount: Long,
    val averageSpentAmount: Long,
    val averageEndingBalance: Long,
    val totalTransferIn: Long,
    val totalTransferOut: Long,
    val overspendCount: Int,
    val negativeEndingCount: Int,
    val rescueCount: Int,
    val donorCount: Int,
    val averageVarianceFromPlan: Long,
    val volatilityScore: Long
)

data class TransferPathHistoryInsight(
    val sourceWalletKey: String,
    val destinationWalletKey: String,
    val sourceDisplayName: String,
    val destinationDisplayName: String,
    val transferCount: Int,
    val budgetsAppearedIn: Int,
    val totalAmount: Long,
    val averageAmount: Long
)

data class GlobalInsightSummary(
    val mostUnderplannedWallet: WalletHistoryInsight?,
    val mostFrequentRescueWallet: WalletHistoryInsight?,
    val topDonorWallet: WalletHistoryInsight?,
    val mostVolatileWallet: WalletHistoryInsight?,
    val topRepeatedTransferPath: TransferPathHistoryInsight?,
    val observations: List<InsightObservation>,
    val walletPatterns: List<WalletHistoryInsight>,
    val transferPatterns: List<TransferPathHistoryInsight>
)

data class BudgetDetail(
    val budget: BudgetEntity,
    val wallets: List<WalletSummary>,
    val transactions: List<TransactionSummary>,
    val insights: BudgetInsightSummary
)
