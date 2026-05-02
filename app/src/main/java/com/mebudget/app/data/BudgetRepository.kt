package com.mebudget.app.data

import com.mebudget.app.domain.validateBudgetDefinition
import com.mebudget.app.domain.validateBudgetUpdate
import com.mebudget.app.domain.validateDuplicateBudgetName
import com.mebudget.app.domain.TransactionValidationContext
import com.mebudget.app.domain.createAdjustmentTransaction
import com.mebudget.app.domain.createExpenseTransaction
import com.mebudget.app.domain.createTransferTransaction
import com.mebudget.app.domain.planWalletArchive
import com.mebudget.app.domain.planWalletInsert
import com.mebudget.app.domain.planWalletReorder
import com.mebudget.app.domain.planWalletUpdate
import com.mebudget.app.domain.updateTransactionCommand
import com.mebudget.app.domain.validateWalletDefinition
import com.mebudget.app.domain.validateTransactionChange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val walletDao: WalletDao,
    private val transactionDao: TransactionDao
) {
    fun observeWalletsForBudget(budgetId: Long): Flow<List<WalletEntity>> {
        return walletDao.observeWalletsForBudget(budgetId)
    }

    fun observeBudgetSummaries(): Flow<List<BudgetSummary>> {
        return combine(
            budgetDao.observeBudgets(),
            walletDao.observeAllWallets(),
            transactionDao.observeAllTransactions()
        ) { budgets, wallets, transactions ->
            budgets.map { budget ->
                val budgetWallets = wallets.filter { it.budgetId == budget.id }
                val budgetTransactions = transactions.filter { it.budgetId == budget.id }
                buildBudgetSummary(budget, budgetWallets, budgetTransactions)
            }
        }
    }

    fun observeBudgetDetail(budgetId: Long): Flow<BudgetDetail?> {
        return combine(
            budgetDao.observeBudget(budgetId),
            walletDao.observeWalletsForBudget(budgetId),
            transactionDao.observeTransactionsForBudget(budgetId)
        ) { budget, wallets, transactions ->
            budget?.let { buildBudgetDetail(it, wallets, transactions) }
        }
    }

    fun observeGlobalInsights(): Flow<GlobalInsightSummary> {
        return combine(
            budgetDao.observeBudgets(),
            walletDao.observeAllWallets(),
            transactionDao.observeAllTransactions()
        ) { budgets, wallets, transactions ->
            computeGlobalInsights(
                budgets = budgets,
                wallets = wallets,
                transactions = transactions
            )
        }
    }

    suspend fun createBudget(
        name: String,
        startDateEpochDay: Long?,
        endDateEpochDay: Long?,
        negativeBalanceRule: NegativeBalanceRule
    ): Long {
        val validated = validateBudgetDefinition(
            name = name,
            startDateEpochDay = startDateEpochDay,
            endDateEpochDay = endDateEpochDay,
            negativeBalanceRule = negativeBalanceRule
        ).getOrThrow()
        return budgetDao.insert(
            BudgetEntity(
                name = validated.name,
                startDateEpochDay = validated.startDateEpochDay,
                endDateEpochDay = validated.endDateEpochDay,
                negativeBalanceRule = validated.negativeBalanceRule
            )
        )
    }

    suspend fun duplicateBudget(budgetId: Long, newName: String): Long {
        val validatedName = validateDuplicateBudgetName(newName).getOrThrow()
        val original = budgetDao.getBudget(budgetId) ?: return 0
        val wallets = walletDao.getWalletsForBudget(budgetId)
        val newBudgetId = budgetDao.insert(
            original.copy(
                id = 0,
                name = validatedName,
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
        budgetDao.update(validateBudgetUpdate(budget).getOrThrow())
    }

    suspend fun addWallet(budgetId: Long, name: String, plannedAmount: Long) {
        val definition = validateWalletDefinition(name, plannedAmount).getOrThrow()
        val nextSortOrder = (walletDao.getMaxSortOrder(budgetId) ?: -1) + 1
        walletDao.insert(planWalletInsert(budgetId, nextSortOrder, definition))
    }

    suspend fun updateWallet(walletId: Long, name: String, plannedAmount: Long) {
        val wallet = walletDao.getWallet(walletId) ?: return
        val definition = validateWalletDefinition(name, plannedAmount).getOrThrow()
        walletDao.update(planWalletUpdate(wallet, definition))
    }

    suspend fun setWalletArchived(walletId: Long, archived: Boolean) {
        val wallet = walletDao.getWallet(walletId) ?: return
        walletDao.update(planWalletArchive(wallet, archived))
    }

    suspend fun moveWallet(walletId: Long, direction: Int) {
        val wallet = walletDao.getWallet(walletId) ?: return
        val wallets = walletDao.getWalletsForBudget(wallet.budgetId)
        planWalletReorder(wallets, walletId, direction)
            .forEach { reorderedWallet ->
                walletDao.update(reorderedWallet)
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
            draft = createExpenseTransaction(
                budgetId = budgetId,
                walletId = walletId,
                amount = amount,
                dateEpochDay = dateEpochDay,
                note = note
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
        return validateAndPersist(
            budgetId = budgetId,
            draft = createTransferTransaction(
                budgetId = budgetId,
                sourceWalletId = sourceWalletId,
                destinationWalletId = destinationWalletId,
                amount = amount,
                dateEpochDay = dateEpochDay,
                note = note
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
            draft = createAdjustmentTransaction(
                budgetId = budgetId,
                walletId = walletId,
                signedAmount = signedAmount,
                dateEpochDay = dateEpochDay,
                note = note
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

        val updated = updateTransactionCommand(
            existing = existing,
            amount = amount,
            dateEpochDay = dateEpochDay,
            sourceWalletId = sourceWalletId,
            destinationWalletId = destinationWalletId,
            note = note
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
        val currentTransactions = transactionDao.getTransactionsForBudget(budgetId)
        validateTransactionChange(
            context = TransactionValidationContext(
                budget = budget,
                wallets = wallets,
                currentTransactions = currentTransactions
            ),
            draft = draft,
            replacingId = replacingId
        ).getOrElse { return Result.failure(it) }

        persist(draft)
        return Result.success(Unit)
    }
}

fun formatAmount(amount: Long): String = "₦%,d".format(amount)
