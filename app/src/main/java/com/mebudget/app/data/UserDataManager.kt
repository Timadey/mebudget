package com.mebudget.app.data

import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first

class UserDataManager(
    private val budgetDao: BudgetDao,
    private val walletDao: WalletDao,
    private val transactionDao: TransactionDao
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportAllData(): String {
        val budgets = budgetDao.observeBudgets().first()
        val wallets = walletDao.observeAllWallets().first()
        val transactions = transactionDao.observeAllTransactions().first()

        val export = mapOf(
            "exportDate" to System.currentTimeMillis(),
            "appVersion" to "1.0",
            "budgets" to budgets.map { b ->
                mapOf(
                    "id" to b.id, "name" to b.name,
                    "startDateEpochDay" to b.startDateEpochDay,
                    "endDateEpochDay" to b.endDateEpochDay,
                    "negativeBalanceRule" to b.negativeBalanceRule.name,
                    "createdAtMillis" to b.createdAtMillis,
                    "updatedAtMillis" to b.updatedAtMillis
                )
            },
            "wallets" to wallets.map { w ->
                mapOf(
                    "id" to w.id, "budgetId" to w.budgetId, "name" to w.name,
                    "plannedAmount" to w.plannedAmount, "sortOrder" to w.sortOrder,
                    "archived" to w.archived, "updatedAtMillis" to w.updatedAtMillis
                )
            },
            "transactions" to transactions.map { t ->
                mapOf(
                    "id" to t.id, "budgetId" to t.budgetId, "type" to t.type.name,
                    "amount" to t.amount, "dateEpochDay" to t.dateEpochDay,
                    "sourceWalletId" to t.sourceWalletId,
                    "destinationWalletId" to t.destinationWalletId,
                    "note" to t.note, "createdAtMillis" to t.createdAtMillis,
                    "updatedAtMillis" to t.updatedAtMillis
                )
            }
        )
        return gson.toJson(export)
    }

    suspend fun deleteAllLocalData() {
        transactionDao.observeAllTransactions().first().forEach { transactionDao.delete(it) }
        walletDao.observeAllWallets().first().forEach { walletDao.delete(it) }
        budgetDao.observeBudgets().first().forEach { budgetDao.delete(it) }
    }
}
