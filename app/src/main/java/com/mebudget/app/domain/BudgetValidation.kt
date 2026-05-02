package com.mebudget.app.domain

import com.mebudget.app.data.BudgetEntity
import com.mebudget.app.data.NegativeBalanceRule

data class ValidatedBudgetDefinition(
    val name: String,
    val startDateEpochDay: Long?,
    val endDateEpochDay: Long?,
    val negativeBalanceRule: NegativeBalanceRule
)

fun validateBudgetDefinition(
    name: String,
    startDateEpochDay: Long?,
    endDateEpochDay: Long?,
    negativeBalanceRule: NegativeBalanceRule
): Result<ValidatedBudgetDefinition> {
    val normalizedName = normalizeBudgetName(name, "Budget name is required.")
        ?: return Result.failure(IllegalArgumentException("Budget name is required."))
    if (startDateEpochDay != null && endDateEpochDay != null && endDateEpochDay < startDateEpochDay) {
        return Result.failure(IllegalArgumentException("End date cannot be before start date."))
    }
    return Result.success(
        ValidatedBudgetDefinition(
            name = normalizedName,
            startDateEpochDay = startDateEpochDay,
            endDateEpochDay = endDateEpochDay,
            negativeBalanceRule = negativeBalanceRule
        )
    )
}

fun validateBudgetUpdate(budget: BudgetEntity): Result<BudgetEntity> {
    return validateBudgetDefinition(
        name = budget.name,
        startDateEpochDay = budget.startDateEpochDay,
        endDateEpochDay = budget.endDateEpochDay,
        negativeBalanceRule = budget.negativeBalanceRule
    ).map { validated ->
        budget.copy(
            name = validated.name,
            startDateEpochDay = validated.startDateEpochDay,
            endDateEpochDay = validated.endDateEpochDay,
            negativeBalanceRule = validated.negativeBalanceRule
        )
    }
}

fun validateDuplicateBudgetName(name: String): Result<String> {
    val normalizedName = normalizeBudgetName(name, "Duplicate budget needs a name.")
        ?: return Result.failure(IllegalArgumentException("Duplicate budget needs a name."))
    return Result.success(normalizedName)
}

private fun normalizeBudgetName(name: String, blankMessage: String): String? {
    return name.trim().ifBlank { return null }
}
