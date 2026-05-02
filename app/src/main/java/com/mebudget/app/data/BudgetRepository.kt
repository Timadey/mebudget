package com.mebudget.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private const val MIN_BUDGET_ROUTE_REPEAT_COUNT = 2
private const val MIN_GLOBAL_PATTERN_BUDGET_COUNT = 2
private const val MIN_GLOBAL_ROUTE_BUDGET_COUNT = 2

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
            val insights = computeBudgetInsights(
                wallets = wallets,
                walletSummaries = walletSummaries,
                transactions = transactions
            )
            BudgetDetail(
                budget = budget,
                wallets = walletSummaries,
                transactions = transactionSummaries,
                insights = insights
            )
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

    private fun computeBudgetInsights(
        wallets: List<WalletEntity>,
        walletSummaries: List<WalletSummary>,
        transactions: List<TransactionEntity>
    ): BudgetInsightSummary {
        val walletById = wallets.associateBy { it.id }
        val walletInsights = walletSummaries.map { walletSummary ->
            val walletTransactions = transactions.filter {
                it.sourceWalletId == walletSummary.id || it.destinationWalletId == walletSummary.id
            }
            val spentTotal = transactions
                .filter { it.type == TransactionType.EXPENSE && it.sourceWalletId == walletSummary.id }
                .sumOf { it.amount }
            val transferInTotal = transactions
                .filter { it.type == TransactionType.TRANSFER && it.destinationWalletId == walletSummary.id }
                .sumOf { it.amount }
            val transferInCount = transactions.count {
                it.type == TransactionType.TRANSFER && it.destinationWalletId == walletSummary.id
            }
            val transferOutTotal = transactions
                .filter { it.type == TransactionType.TRANSFER && it.sourceWalletId == walletSummary.id }
                .sumOf { it.amount }
            val transferOutCount = transactions.count {
                it.type == TransactionType.TRANSFER && it.sourceWalletId == walletSummary.id
            }
            val adjustmentTotal = transactions
                .filter { it.type == TransactionType.ADJUSTMENT && it.sourceWalletId == walletSummary.id }
                .sumOf { it.amount }

            WalletBudgetInsight(
                walletId = walletSummary.id,
                walletKey = walletSummary.name.toWalletKey(),
                walletName = walletSummary.name,
                plannedAmount = walletSummary.plannedAmount,
                spentTotal = spentTotal,
                transferInTotal = transferInTotal,
                transferInCount = transferInCount,
                transferOutTotal = transferOutTotal,
                transferOutCount = transferOutCount,
                adjustmentTotal = adjustmentTotal,
                endingBalance = walletSummary.balance,
                varianceFromPlan = walletSummary.balance - walletSummary.plannedAmount,
                overspent = walletSummary.balance < 0,
                transactionCount = walletTransactions.size,
                transferCount = walletTransactions.count { it.type == TransactionType.TRANSFER }
            )
        }.sortedBy { it.walletName.lowercase() }

        val transferPaths = transactions
            .filter { it.type == TransactionType.TRANSFER && it.sourceWalletId != null && it.destinationWalletId != null }
            .groupBy { it.sourceWalletId!! to it.destinationWalletId!! }
            .mapNotNull { (path, pathTransactions) ->
                val sourceWallet = walletById[path.first] ?: return@mapNotNull null
                val destinationWallet = walletById[path.second] ?: return@mapNotNull null
                TransferPathInsight(
                    sourceWalletId = sourceWallet.id,
                    destinationWalletId = destinationWallet.id,
                    sourceWalletName = sourceWallet.name,
                    destinationWalletName = destinationWallet.name,
                    transferCount = pathTransactions.size,
                    totalAmount = pathTransactions.sumOf { it.amount }
                )
            }
            .sortedWith(
                compareByDescending<TransferPathInsight> { it.transferCount }
                    .thenByDescending { it.totalAmount }
            )

        val mostRescuedWallet = walletInsights
            .filter { it.transferInCount > 0 || it.overspent }
            .maxWithOrNull(budgetRescueComparator())
        val topDonorWallet = walletInsights
            .filter { it.transferOutCount > 0 }
            .maxWithOrNull(budgetDonorComparator())

        return BudgetInsightSummary(
            totalPlanned = wallets.sumOf { it.plannedAmount },
            totalSpent = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            totalTransferred = transactions.filter { it.type == TransactionType.TRANSFER }.sumOf { it.amount },
            totalAdjusted = transactions.filter { it.type == TransactionType.ADJUSTMENT }.sumOf { kotlin.math.abs(it.amount) },
            totalEndingBalance = walletSummaries.sumOf { it.balance },
            overspentWallets = walletInsights.filter { it.overspent },
            mostRescuedWallet = mostRescuedWallet,
            topDonorWallet = topDonorWallet,
            topTransferPath = transferPaths.firstOrNull(),
            observations = buildBudgetObservations(
                walletInsights = walletInsights,
                mostRescuedWallet = mostRescuedWallet,
                topDonorWallet = topDonorWallet,
                topTransferPath = transferPaths.firstOrNull()
            ),
            walletInsights = walletInsights,
            transferPaths = transferPaths
        )
    }

    private fun computeGlobalInsights(
        budgets: List<BudgetEntity>,
        wallets: List<WalletEntity>,
        transactions: List<TransactionEntity>
    ): GlobalInsightSummary {
        val walletPatterns = budgets.flatMap { budget ->
            val budgetWallets = wallets.filter { it.budgetId == budget.id }
            if (budgetWallets.isEmpty()) return@flatMap emptyList()
            val budgetTransactions = transactions.filter { it.budgetId == budget.id }
            val balances = computeBalances(budgetWallets, budgetTransactions)
            val walletSummaries = budgetWallets.map { wallet ->
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
            computeBudgetInsights(
                wallets = budgetWallets,
                walletSummaries = walletSummaries,
                transactions = budgetTransactions
            ).walletInsights
        }
            .groupBy { it.walletKey }
            .map { (walletKey, entries) ->
                val displayName = entries
                    .groupingBy { it.walletName }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?.key
                    ?: entries.first().walletName
                val plannedValues = entries.map { it.plannedAmount }
                val spentValues = entries.map { it.spentTotal }
                val endingValues = entries.map { it.endingBalance }
                val varianceValues = entries.map { kotlin.math.abs(it.varianceFromPlan) }

                WalletHistoryInsight(
                    walletKey = walletKey,
                    displayName = displayName,
                    budgetsAppearedIn = entries.size,
                    averagePlannedAmount = plannedValues.averageLong(),
                    averageSpentAmount = spentValues.averageLong(),
                    averageEndingBalance = endingValues.averageLong(),
                    totalTransferIn = entries.sumOf { it.transferInTotal },
                    totalTransferOut = entries.sumOf { it.transferOutTotal },
                    overspendCount = entries.count { it.spentTotal > it.plannedAmount },
                    negativeEndingCount = entries.count { it.overspent },
                    rescueCount = entries.count { it.transferInTotal > 0 },
                    donorCount = entries.count { it.transferOutTotal > 0 },
                    averageVarianceFromPlan = entries.map { it.varianceFromPlan }.averageLong(),
                    volatilityScore = varianceValues.averageLong()
                )
            }
            .sortedWith(
                compareByDescending<WalletHistoryInsight> { it.budgetsAppearedIn }
                    .thenBy { it.displayName.lowercase() }
            )

        val walletNameByKey = walletPatterns.associate { it.walletKey to it.displayName }
        val transferPatterns = budgets.flatMap { budget ->
            val budgetWallets = wallets.filter { it.budgetId == budget.id }.associateBy { it.id }
            val budgetTransfers = transactions.filter { it.budgetId == budget.id && it.type == TransactionType.TRANSFER }
            budgetTransfers.mapNotNull { transfer ->
                val sourceWallet = transfer.sourceWalletId?.let { budgetWallets[it] } ?: return@mapNotNull null
                val destinationWallet = transfer.destinationWalletId?.let { budgetWallets[it] } ?: return@mapNotNull null
                TransferPathHistoryInsight(
                    sourceWalletKey = sourceWallet.name.toWalletKey(),
                    destinationWalletKey = destinationWallet.name.toWalletKey(),
                    sourceDisplayName = sourceWallet.name,
                    destinationDisplayName = destinationWallet.name,
                    transferCount = 1,
                    budgetsAppearedIn = 1,
                    totalAmount = transfer.amount,
                    averageAmount = transfer.amount
                )
            }
        }
            .groupBy { it.sourceWalletKey to it.destinationWalletKey }
            .map { (pathKey, entries) ->
                val groupedBudgetCount = entries.size
                val totalAmount = entries.sumOf { it.totalAmount }
                TransferPathHistoryInsight(
                    sourceWalletKey = pathKey.first,
                    destinationWalletKey = pathKey.second,
                    sourceDisplayName = walletNameByKey[pathKey.first] ?: entries.first().sourceDisplayName,
                    destinationDisplayName = walletNameByKey[pathKey.second] ?: entries.first().destinationDisplayName,
                    transferCount = entries.sumOf { it.transferCount },
                    budgetsAppearedIn = groupedBudgetCount,
                    totalAmount = totalAmount,
                    averageAmount = if (entries.isEmpty()) 0L else totalAmount / entries.size
                )
            }
            .sortedWith(
                compareByDescending<TransferPathHistoryInsight> { it.budgetsAppearedIn }
                    .thenByDescending { it.transferCount }
                    .thenByDescending { it.totalAmount }
            )

        val mostUnderplannedWallet = walletPatterns
            .filter { it.isGlobalUnderplannedCandidate() }
            .maxWithOrNull(globalUnderplannedComparator())
        val mostFrequentRescueWallet = walletPatterns
            .filter { it.rescueCount >= MIN_GLOBAL_PATTERN_BUDGET_COUNT }
            .maxWithOrNull(globalRescueComparator())
        val topDonorWallet = walletPatterns
            .filter { it.donorCount >= MIN_GLOBAL_PATTERN_BUDGET_COUNT }
            .maxWithOrNull(globalDonorComparator())
        val mostVolatileWallet = walletPatterns
            .filter { it.budgetsAppearedIn >= MIN_GLOBAL_PATTERN_BUDGET_COUNT && it.volatilityScore > 0L }
            .maxWithOrNull(globalVolatilityComparator())
        val topRepeatedTransferPath = transferPatterns
            .firstOrNull { it.budgetsAppearedIn >= MIN_GLOBAL_ROUTE_BUDGET_COUNT }

        return GlobalInsightSummary(
            mostUnderplannedWallet = mostUnderplannedWallet,
            mostFrequentRescueWallet = mostFrequentRescueWallet,
            topDonorWallet = topDonorWallet,
            mostVolatileWallet = mostVolatileWallet,
            topRepeatedTransferPath = topRepeatedTransferPath,
            observations = buildGlobalObservations(
                walletPatterns = walletPatterns,
                transferPatterns = transferPatterns
            ),
            walletPatterns = walletPatterns,
            transferPatterns = transferPatterns
        )
    }

    private fun buildBudgetObservations(
        walletInsights: List<WalletBudgetInsight>,
        mostRescuedWallet: WalletBudgetInsight?,
        topDonorWallet: WalletBudgetInsight?,
        topTransferPath: TransferPathInsight?
    ): List<InsightObservation> {
        val observations = mutableListOf<InsightObservation>()

        mostRescuedWallet?.takeIf { it.transferInCount > 0 || it.overspent }?.let {
            observations += InsightObservation(
                title = "Budget pressure",
                message = if (it.overspent) {
                    "${it.walletName} ended below zero and needed the most repair."
                } else if (it.transferInCount >= MIN_BUDGET_ROUTE_REPEAT_COUNT) {
                    "${it.walletName} needed repeated rescue transfers in this budget."
                } else {
                    "${it.walletName} received the strongest rescue support in this budget."
                }
            )
        }

        topDonorWallet?.takeIf { it.transferOutCount > 0 }?.let {
            observations += InsightObservation(
                title = "Primary donor",
                message = if (it.transferOutCount >= MIN_BUDGET_ROUTE_REPEAT_COUNT) {
                    "${it.walletName} repeatedly funded other wallets in this budget."
                } else {
                    "${it.walletName} funded other wallets more than any other wallet."
                }
            )
        }

        topTransferPath?.takeIf { it.transferCount >= MIN_BUDGET_ROUTE_REPEAT_COUNT }?.let {
            observations += InsightObservation(
                title = "Repeated repair route",
                message = "${it.sourceWalletName} -> ${it.destinationWalletName} repeated ${it.transferCount} times."
            )
        }

        walletInsights
            .filter { it.spentTotal > it.plannedAmount }
            .maxWithOrNull(
                compareByDescending<WalletBudgetInsight> { it.spentTotal - it.plannedAmount }
                    .thenByDescending { it.transferInCount }
            )?.let {
            observations += InsightObservation(
                title = "Underplanned spending",
                message = "${it.walletName} spent beyond its original allocation."
            )
        }

        return observations.take(4)
    }

    private fun buildGlobalObservations(
        walletPatterns: List<WalletHistoryInsight>,
        transferPatterns: List<TransferPathHistoryInsight>
    ): List<InsightObservation> {
        val observations = mutableListOf<InsightObservation>()

        walletPatterns
            .filter { it.rescueCount >= MIN_GLOBAL_PATTERN_BUDGET_COUNT }
            .maxWithOrNull(globalRescueComparator())?.let {
            observations += InsightObservation(
                title = "Frequent rescue wallet",
                message = "${it.displayName} regularly needs support from other wallets."
            )
        }

        walletPatterns
            .filter { it.donorCount >= MIN_GLOBAL_PATTERN_BUDGET_COUNT }
            .maxWithOrNull(globalDonorComparator())?.let {
            observations += InsightObservation(
                title = "Consistent donor",
                message = "${it.displayName} often funds other wallets across budgets."
            )
        }

        walletPatterns
            .filter { it.budgetsAppearedIn >= MIN_GLOBAL_PATTERN_BUDGET_COUNT && it.volatilityScore > 0L }
            .maxWithOrNull(globalVolatilityComparator())?.let {
            observations += InsightObservation(
                title = "Most volatile wallet",
                message = "${it.displayName} changes more than most wallets from budget to budget."
            )
        }

        transferPatterns.firstOrNull { it.budgetsAppearedIn >= MIN_GLOBAL_ROUTE_BUDGET_COUNT }?.let {
            observations += InsightObservation(
                title = "Repeated transfer route",
                message = "${it.sourceDisplayName} -> ${it.destinationDisplayName} keeps appearing across budgets."
            )
        }

        walletPatterns
            .filter { it.isGlobalUnderplannedCandidate() }
            .maxWithOrNull(globalUnderplannedComparator())?.let {
            observations += InsightObservation(
                title = "Likely underplanned",
                message = "${it.displayName} often bends away from the original plan."
            )
        }

        return observations.take(5)
    }
}

private fun String?.nullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
private fun String.toWalletKey(): String = trim().lowercase()
private fun List<Long>.averageLong(): Long = if (isEmpty()) 0L else sum() / size
private fun WalletBudgetInsight.budgetRescueScore(): Long =
    (transferInCount.toLong() * 1_000_000_000L) +
        (if (overspent) 100_000_000L else 0L) +
        transferInTotal

private fun WalletBudgetInsight.budgetDonorScore(): Long =
    (transferOutCount.toLong() * 1_000_000_000L) + transferOutTotal

private fun budgetRescueComparator(): Comparator<WalletBudgetInsight> =
    compareByDescending<WalletBudgetInsight> { it.budgetRescueScore() }
        .thenByDescending { it.spentTotal - it.plannedAmount }

private fun budgetDonorComparator(): Comparator<WalletBudgetInsight> =
    compareByDescending<WalletBudgetInsight> { it.budgetDonorScore() }
        .thenByDescending { it.endingBalance }

private fun WalletHistoryInsight.isGlobalUnderplannedCandidate(): Boolean {
    if (budgetsAppearedIn < MIN_GLOBAL_PATTERN_BUDGET_COUNT) return false
    return rescueCount >= MIN_GLOBAL_PATTERN_BUDGET_COUNT ||
        overspendCount >= MIN_GLOBAL_PATTERN_BUDGET_COUNT ||
        negativeEndingCount > 0 ||
        averageSpentAmount > averagePlannedAmount
}

private fun WalletHistoryInsight.globalUnderplannedScore(): Long {
    val averageGap = (averageSpentAmount - averagePlannedAmount).coerceAtLeast(0L)
    return (rescueCount.toLong() * 1_000_000_000L) +
        (negativeEndingCount.toLong() * 100_000_000L) +
        (overspendCount.toLong() * 10_000_000L) +
        averageGap
}

private fun WalletHistoryInsight.globalRescueScore(): Long =
    (rescueCount.toLong() * 1_000_000_000L) +
        (negativeEndingCount.toLong() * 100_000_000L) +
        totalTransferIn

private fun WalletHistoryInsight.globalDonorScore(): Long =
    (donorCount.toLong() * 1_000_000_000L) +
        (budgetsAppearedIn.toLong() * 100_000_000L) +
        totalTransferOut

private fun WalletHistoryInsight.globalVolatilitySignalScore(): Long =
    (budgetsAppearedIn.toLong() * 1_000_000_000L) +
        (negativeEndingCount.toLong() * 100_000_000L) +
        volatilityScore

private fun globalUnderplannedComparator(): Comparator<WalletHistoryInsight> =
    compareByDescending<WalletHistoryInsight> { it.globalUnderplannedScore() }
        .thenByDescending { it.budgetsAppearedIn }

private fun globalRescueComparator(): Comparator<WalletHistoryInsight> =
    compareByDescending<WalletHistoryInsight> { it.globalRescueScore() }
        .thenByDescending { it.budgetsAppearedIn }

private fun globalDonorComparator(): Comparator<WalletHistoryInsight> =
    compareByDescending<WalletHistoryInsight> { it.globalDonorScore() }
        .thenByDescending { it.averageEndingBalance }

private fun globalVolatilityComparator(): Comparator<WalletHistoryInsight> =
    compareByDescending<WalletHistoryInsight> { it.globalVolatilitySignalScore() }
        .thenByDescending { it.averageVarianceFromPlan }

fun formatAmount(amount: Long): String = "₦%,d".format(amount)
