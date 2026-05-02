package com.mebudget.app.domain

import com.mebudget.app.data.WalletEntity

data class ValidatedWalletDefinition(
    val name: String,
    val plannedAmount: Long
)

fun validateWalletDefinition(
    name: String,
    plannedAmount: Long
): Result<ValidatedWalletDefinition> {
    val normalizedName = name.trim()
    if (normalizedName.isEmpty()) {
        return Result.failure(IllegalArgumentException("Wallet name is required."))
    }
    if (plannedAmount <= 0L) {
        return Result.failure(IllegalArgumentException("Enter a valid amount."))
    }
    return Result.success(
        ValidatedWalletDefinition(
            name = normalizedName,
            plannedAmount = plannedAmount
        )
    )
}

fun planWalletInsert(
    budgetId: Long,
    nextSortOrder: Int,
    definition: ValidatedWalletDefinition
): WalletEntity {
    return WalletEntity(
        budgetId = budgetId,
        name = definition.name,
        plannedAmount = definition.plannedAmount,
        sortOrder = nextSortOrder
    )
}

fun planWalletUpdate(
    existing: WalletEntity,
    definition: ValidatedWalletDefinition
): WalletEntity {
    return existing.copy(
        name = definition.name,
        plannedAmount = definition.plannedAmount
    )
}

fun planWalletArchive(
    existing: WalletEntity,
    archived: Boolean
): WalletEntity {
    return existing.copy(archived = archived)
}

fun planWalletReorder(
    wallets: List<WalletEntity>,
    walletId: Long,
    direction: Int
): List<WalletEntity> {
    val index = wallets.indexOfFirst { it.id == walletId }
    val targetIndex = index + direction
    if (index == -1 || targetIndex !in wallets.indices) return emptyList()

    val mutable = wallets.toMutableList()
    val target = mutable[targetIndex]
    mutable[targetIndex] = mutable[index]
    mutable[index] = target

    return mutable.mapIndexed { updatedIndex, item ->
        item.copy(sortOrder = updatedIndex)
    }
}
