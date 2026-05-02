package com.mebudget.app.ui

import android.app.Application
import com.mebudget.app.data.AppDatabase
import com.mebudget.app.data.BudgetRepository

internal fun Application.budgetRepository(): BudgetRepository {
    return AppDatabase.getInstance(this).run {
        BudgetRepository(budgetDao(), walletDao(), transactionDao())
    }
}
