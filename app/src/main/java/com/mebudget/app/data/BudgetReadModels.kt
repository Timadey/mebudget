package com.mebudget.app.data

internal fun buildBudgetSummary(
    budget: BudgetEntity,
    wallets: List<WalletEntity>,
    transactions: List<TransactionEntity>
): BudgetSummary {
    val balances = computeBalances(wallets, transactions)
    return BudgetSummary(
        id = budget.id,
        name = budget.name,
        startDateEpochDay = budget.startDateEpochDay,
        endDateEpochDay = budget.endDateEpochDay,
        walletCount = wallets.size,
        activeWalletCount = wallets.count { !it.archived },
        totalBalance = balances.values.sum()
    )
}

internal fun buildBudgetDetail(
    budget: BudgetEntity,
    wallets: List<WalletEntity>,
    transactions: List<TransactionEntity>
): BudgetDetail {
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
    return BudgetDetail(
        budget = budget,
        wallets = walletSummaries,
        transactions = transactionSummaries,
        insights = computeBudgetInsights(
            wallets = wallets,
            walletSummaries = walletSummaries,
            transactions = transactions
        )
    )
}
