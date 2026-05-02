package com.mebudget.app.ui

import com.mebudget.app.data.BudgetDetail
import com.mebudget.app.data.BudgetSummary
import com.mebudget.app.data.GlobalInsightSummary

data class BudgetsUiState(
    val budgets: List<BudgetSummary> = emptyList(),
    val globalInsights: GlobalInsightSummary? = null,
    val pendingBudgetIdToOpen: Long? = null,
    val errorMessage: String? = null
)

data class BudgetDetailUiState(
    val selectedBudgetId: Long? = null,
    val selectedBudgetDetail: BudgetDetail? = null,
    val errorMessage: String? = null
)
