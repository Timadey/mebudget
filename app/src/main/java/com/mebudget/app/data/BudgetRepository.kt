package com.mebudget.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val walletDao: WalletDao,
    private val transactionDao: TransactionDao
) {
    fun observeBudgetSummaries(): Flow<List<BudgetSummary>> {
        return combine(
            budgetDao.observeBudgets(),
            walletDao.observeAllWallets(),
            transactionDao.observeAllTransactions()
        ) { budgets, wallets, transactions ->
            budgets.map { budget ->
                val budgetWallets = wallets.filter { it.budgetId == budget.id }
                val budgetTransactions = transactions.filter { it.budgetId == budget.id }
                val balances = computeBalances(budgetWallets, budgetTransactions)
                BudgetSummary(
                    id = budget.id,
                    name = budget.name,
                    startDateEpochDay = budget.startDateEpochDay,
                    endDateEpochDay = budget.endDateEpochDay,
                    walletCount = budgetWallets.size,
                    activeWalletCount = budgetWallets.count { !it.archived },
                    totalBalance = balances.values.sum()
                )
            }
        }
    }

    fun observeBudgetDetail(budgetId: Long): Flow<BudgetDetail?> {
        return combine(
            budgetDao.observeBudget(budgetId),
            walletDao.observeWalletsForBudget(budgetId),
            transactionDao.observeTransactionsForBudget(budgetId)
        ) { budget, wallets, transactions ->
            budget ?: return@combine null
            val walletMap = wallets.associateBy { it.id }
            val balances = computeBalances(wallets, transactions)
            val walletSummaries = wallets.map { wallet ->
                WalletSummary(
                    id = wallet.id,
                    budgetId = wallet.budgetId,
                    name = wallet.name,
                    plannedAmount = wallet.plannedAmount,
                    balance = balances[wallet.id] ?: wallet.plannedAmount,
                    sortOrder = wallet.sortOrder,
                    archived = wallet.archived,
                    warning = (balances[wallet.id] ?: wallet.plannedAmount) < 0
                )
            }
            val transactionSummaries = transactions.map { transaction ->
                TransactionSummary(
                    id = transaction.id,
                    budgetId = transaction.budgetId,
                    type = transaction.type,
                    amount = transaction.amount,
                    dateEpochDay = transaction.dateEpochDay,
                    sourceWalletId = transaction.sourceWalletId,
                    destinationWalletId = transaction.destinationWalletId,
                    sourceWalletName = transaction.sourceWalletId?.let { walletMap[it]?.name },
                    destinationWalletName = transaction.destinationWalletId?.let { walletMap[it]?.name },
                    note = transaction.note
                )
            }
            BudgetDetail(
                budget = budget,
                wallets = walletSummaries,
                transactions = transactionSummaries
            )
        }
    }

    suspend fun createBudget(
        name: String,
        startDateEpochDay: Long?,
        endDateEpochDay: Long?,
        negativeBalanceRule: NegativeBalanceRule
    ): Long {
        return budgetDao.insert(
            BudgetEntity(
                name = name.trim(),
                startDateEpochDay = startDateEpochDay,
                endDateEpochDay = endDateEpochDay,
                negativeBalanceRule = negativeBalanceRule
            )
        )
    }

    suspend fun duplicateBudget(budgetId: Long, newName: String): Long {
        val original = budgetDao.getBudget(budgetId) ?: return 0
        val wallets = walletDao.getWalletsForBudget(budgetId)
        val newBudgetId = budgetDao.insert(
            original.copy(
                id = 0,
                name = newName.trim(),
                createdAtMillis = System.currentTimeMillis()
            )
        )
        walletDao.insertAll(
            wallets.mapIndexed { index, wallet ->
                wallet.copy(
                    id = 0,
                    budgetId = newBudgetId,
                    sortOrder = index
                )
            }
        )
        return newBudgetId
    }

    suspend fun updateBudget(budget: BudgetEntity) {
        budgetDao.update(budget)
    }

    suspend fun addWallet(budgetId: Long, name: String, plannedAmount: Long) {
        val nextSortOrder = (walletDao.getMaxSortOrder(budgetId) ?: -1) + 1
        walletDao.insert(
            WalletEntity(
                budgetId = budgetId,
                name = name.trim(),
                plannedAmount = plannedAmount,
                sortOrder = nextSortOrder
            )
        )
    }

    suspend fun updateWallet(walletId: Long, name: String, plannedAmount: Long) {
        val wallet = walletDao.getWallet(walletId) ?: return
        walletDao.update(wallet.copy(name = name.trim(), plannedAmount = plannedAmount))
    }

    suspend fun setWalletArchived(walletId: Long, archived: Boolean) {
        val wallet = walletDao.getWallet(walletId) ?: return
        walletDao.update(wallet.copy(archived = archived))
    }

    suspend fun moveWallet(walletId: Long, direction: Int) {
        val wallet = walletDao.getWallet(walletId) ?: return
        val wallets = walletDao.getWalletsForBudget(wallet.budgetId)
        val index = wallets.indexOfFirst { it.id == walletId }
        val targetIndex = index + direction
        if (index == -1 || targetIndex !in wallets.indices) return

        val mutable = wallets.toMutableList()
        val target = mutable[targetIndex]
        mutable[targetIndex] = mutable[index]
        mutable[index] = target

        mutable.forEachIndexed { updatedIndex, item ->
            walletDao.update(item.copy(sortOrder = updatedIndex))
        }
    }

    suspend fun addExpense(
        budgetId: Long,
        walletId: Long,
        amount: Long,
        dateEpochDay: Long,
        note: String?
    ): Result<Unit> {
        return validateAndPersist(
            budgetId = budgetId,
            draft = TransactionEntity(
                budgetId = budgetId,
                type = TransactionType.EXPENSE,
                amount = amount,
                dateEpochDay = dateEpochDay,
                sourceWalletId = walletId,
                note = note.nullIfBlank()
            )
        ) { transactionDao.insert(it) }
    }

    suspend fun addTransfer(
        budgetId: Long,
        sourceWalletId: Long,
        destinationWalletId: Long,
        amount: Long,
        dateEpochDay: Long,
        note: String?
    ): Result<Unit> {
        if (sourceWalletId == destinationWalletId) {
            return Result.failure(IllegalArgumentException("Choose two different wallets."))
        }
        return validateAndPersist(
            budgetId = budgetId,
            draft = TransactionEntity(
                budgetId = budgetId,
                type = TransactionType.TRANSFER,
                amount = amount,
                dateEpochDay = dateEpochDay,
                sourceWalletId = sourceWalletId,
                destinationWalletId = destinationWalletId,
                note = note.nullIfBlank()
            )
        ) { transactionDao.insert(it) }
    }

    suspend fun addAdjustment(
        budgetId: Long,
        walletId: Long,
        signedAmount: Long,
        dateEpochDay: Long,
        note: String?
    ): Result<Unit> {
        return validateAndPersist(
            budgetId = budgetId,
            draft = TransactionEntity(
                budgetId = budgetId,
                type = TransactionType.ADJUSTMENT,
                amount = signedAmount,
                dateEpochDay = dateEpochDay,
                sourceWalletId = walletId,
                note = note.nullIfBlank()
            )
        ) { transactionDao.insert(it) }
    }

    suspend fun updateTransaction(
        transactionId: Long,
        amount: Long,
        dateEpochDay: Long,
        sourceWalletId: Long?,
        destinationWalletId: Long?,
        note: String?
    ): Result<Unit> {
        val existing = transactionDao.getTransaction(transactionId)
            ?: return Result.failure(IllegalArgumentException("Transaction not found."))
        if (existing.type == TransactionType.TRANSFER && sourceWalletId == destinationWalletId) {
            return Result.failure(IllegalArgumentException("Choose two different wallets."))
        }

        val updated = existing.copy(
            amount = amount,
            dateEpochDay = dateEpochDay,
            sourceWalletId = sourceWalletId,
            destinationWalletId = destinationWalletId,
            note = note.nullIfBlank()
        )
        return validateAndPersist(
            budgetId = existing.budgetId,
            draft = updated,
            replacingId = existing.id
        ) { transactionDao.update(it) }
    }

    suspend fun deleteTransaction(transactionId: Long) {
        val existing = transactionDao.getTransaction(transactionId) ?: return
        transactionDao.delete(existing)
    }

    private suspend fun validateAndPersist(
        budgetId: Long,
        draft: TransactionEntity,
        replacingId: Long? = null,
        persist: suspend (TransactionEntity) -> Unit
    ): Result<Unit> {
        if (draft.type != TransactionType.ADJUSTMENT && draft.amount <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero."))
        }
        if (draft.type == TransactionType.ADJUSTMENT && draft.amount == 0L) {
            return Result.failure(IllegalArgumentException("Adjustment cannot be zero."))
        }

        val budget = budgetDao.getBudget(budgetId)
            ?: return Result.failure(IllegalArgumentException("Budget not found."))
        val wallets = walletDao.getWalletsForBudget(budgetId)
        val walletIds = wallets.mapTo(mutableSetOf()) { it.id }
        val referencedWalletIds = listOfNotNull(draft.sourceWalletId, draft.destinationWalletId)
        if (!walletIds.containsAll(referencedWalletIds)) {
            return Result.failure(IllegalArgumentException("Choose wallets from this budget."))
        }
        val currentTransactions = transactionDao.getTransactionsForBudget(budgetId)
            .filterNot { it.id == replacingId }
        val projectedTransactions = currentTransactions + draft
        val balances = computeBalances(wallets, projectedTransactions)

        val violated = when (budget.negativeBalanceRule) {
            NegativeBalanceRule.ALLOW -> null
            NegativeBalanceRule.WARN -> null
            NegativeBalanceRule.BLOCK -> balances.entries.firstOrNull { it.value < 0 }
        }
        if (violated != null) {
            val walletName = wallets.firstOrNull { it.id == violated.key }?.name ?: "wallet"
            return Result.failure(IllegalArgumentException("$walletName cannot go below zero."))
        }

        persist(draft)
        return Result.success(Unit)
    }

    private fun computeBalances(
        wallets: List<WalletEntity>,
        transactions: List<TransactionEntity>
    ): Map<Long, Long> {
        val balances = wallets.associate { it.id to it.plannedAmount }.toMutableMap()
        transactions.sortedWith(compareBy<TransactionEntity> { it.dateEpochDay }.thenBy { it.id })
            .forEach { transaction ->
                when (transaction.type) {
                    TransactionType.EXPENSE -> {
                        transaction.sourceWalletId?.let { walletId ->
                            balances[walletId] = (balances[walletId] ?: 0L) - transaction.amount
                        }
                    }

                    TransactionType.TRANSFER -> {
                        transaction.sourceWalletId?.let { walletId ->
                            balances[walletId] = (balances[walletId] ?: 0L) - transaction.amount
                        }
                        transaction.destinationWalletId?.let { walletId ->
                            balances[walletId] = (balances[walletId] ?: 0L) + transaction.amount
                        }
                    }

                    TransactionType.ADJUSTMENT -> {
                        transaction.sourceWalletId?.let { walletId ->
                            balances[walletId] = (balances[walletId] ?: 0L) + transaction.amount
                        }
                    }
                }
            }
        return balances
    }
}

private fun String?.nullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

fun formatAmount(amount: Long): String = "₦%,d".format(amount)
