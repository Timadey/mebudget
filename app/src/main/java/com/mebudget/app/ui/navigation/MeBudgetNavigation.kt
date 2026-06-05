package com.mebudget.app.ui.navigation

import android.net.Uri

object MeBudgetRoute {
    const val budgets = "budgets"
    const val budget = "budget"
    const val wallet = "wallet"
    const val insights = "insights"
    const val globalInsights = "global-insights"
    const val globalWallet = "global-wallet"
    const val globalTransfer = "global-transfer"
    const val quickSpendSettings = "quick-spend-settings"

    fun budget(budgetId: Long): String = "$budget/$budgetId"
    fun wallet(budgetId: Long, walletId: Long): String = "$budget/$budgetId/$wallet/$walletId"
    fun budgetInsights(budgetId: Long): String = "$budget/$budgetId/$insights"
    fun globalWallet(walletKey: String): String = "$globalInsights/$globalWallet/${Uri.encode(walletKey)}"
    fun globalTransfer(sourceKey: String, destinationKey: String): String =
        "$globalInsights/$globalTransfer/${Uri.encode(sourceKey)}/${Uri.encode(destinationKey)}"
}
