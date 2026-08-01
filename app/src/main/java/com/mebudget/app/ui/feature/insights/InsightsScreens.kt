package com.mebudget.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mebudget.app.ui.common.BlockProgressBar
import com.mebudget.app.ui.common.offsetShadow
import com.mebudget.app.ui.common.SectionHeader
import com.mebudget.app.ui.common.BudgetStatusIndicator
import com.mebudget.app.ui.common.BudgetStatus
import com.mebudget.app.ui.theme.BrutalistBudgetTheme
import com.mebudget.app.ui.theme.AccentBlue
import com.mebudget.app.ui.theme.Success
import com.mebudget.app.ui.theme.Warning
import com.mebudget.app.ui.theme.Overspend
import com.mebudget.app.data.BudgetDetail
import com.mebudget.app.data.BudgetInsightSummary
import com.mebudget.app.data.GlobalInsightSummary
import com.mebudget.app.data.InsightObservation
import com.mebudget.app.data.TransferPathHistoryInsight
import com.mebudget.app.data.WalletBudgetInsight
import com.mebudget.app.data.WalletHistoryInsight

private enum class SignalTone {
    Good, Warning, Danger
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetInsightsScreen(
    detail: BudgetDetail,
    privacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit,
    onBack: () -> Unit
) {
    val insights = detail.insights

    BrutalistBudgetTheme {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("[<]", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Budget Insights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = detail.budget.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                PrivacyToggleButton(
                    privacyModeEnabled = privacyModeEnabled,
                    onTogglePrivacyMode = onTogglePrivacyMode
                )
            }

            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.onSurface
            )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (privacyModeEnabled) {
                item { PrivacyModeBanner(onTogglePrivacyMode = onTogglePrivacyMode) }
            }

            item {
                BudgetInsightActionCard(
                    insights = insights,
                    privacyModeEnabled = privacyModeEnabled
                )
            }

            item {
                BudgetInsightHeroCard(
                    insights = insights,
                    privacyModeEnabled = privacyModeEnabled
                )
            }

            item {
                InsightDetailCard(
                    title = "Pressure points",
                    subtitle = "These signals show where the budget needed support or drifted from plan."
                ) {
                    if (insights.observations.isNotEmpty()) {
                        ObservationList(observations = insights.observations)
                    }
                    PatternSummaryRow(
                        title = "Most rescued",
                        value = insights.mostRescuedWallet?.walletName ?: "None",
                        supporting = insights.mostRescuedWallet?.let {
                            "${maskedAmount(it.transferInTotal, privacyModeEnabled)} received"
                        } ?: "No rescue transfers"
                    )
                    PatternSummaryRow(
                        title = "Top donor",
                        value = insights.topDonorWallet?.walletName ?: "None",
                        supporting = insights.topDonorWallet?.let {
                            "${maskedAmount(it.transferOutTotal, privacyModeEnabled)} moved out"
                        } ?: "No donor wallet"
                    )
                    PatternSummaryRow(
                        title = "Top transfer path",
                        value = insights.topTransferPath?.let {
                            "${it.sourceWalletName} -> ${it.destinationWalletName}"
                        } ?: "None",
                        supporting = insights.topTransferPath?.let {
                            "${maskedAmount(it.totalAmount, privacyModeEnabled)} across ${it.transferCount} moves"
                        } ?: "No transfers yet"
                    )
                }
            }

            item {
                InsightDetailCard(
                    title = "Wallet health",
                    subtitle = "Each wallet shows plan pressure, ending balance, and how often it needed support."
                ) {
                    insights.walletInsights.forEach { walletInsight ->
                        WalletHealthRow(
                            insight = walletInsight,
                            privacyModeEnabled = privacyModeEnabled
                        )
                    }
                }
            }

            item {
                InsightDetailCard(
                    title = "Money movement",
                    subtitle = "Repeated transfer routes often reveal underplanned categories or emergency funding."
                ) {
                    if (insights.transferPaths.isEmpty()) {
                        Text(
                            text = "No transfer paths yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        insights.transferPaths.forEach { path ->
                            TransferRouteRow(
                                sourceName = path.sourceWalletName,
                                destinationName = path.destinationWalletName,
                                amount = maskedAmount(path.totalAmount, privacyModeEnabled),
                                supporting = "${path.transferCount} moves"
                            )
                        }
                    }
                }
            }

            item {
                InsightDetailCard(
                    title = "Risk review",
                    subtitle = "Negative ending balances are the clearest sign that the plan needs adjustment."
                ) {
                    if (insights.overspentWallets.isEmpty()) {
                        Text(
                            text = "No wallets ended negative.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        insights.overspentWallets.forEach { walletInsight ->
                            TransferRouteRow(
                                sourceName = walletInsight.walletName,
                                destinationName = "Negative ending",
                                amount = maskedAmount(walletInsight.endingBalance, privacyModeEnabled),
                                supporting = "Needs a higher plan or less transfer pressure"
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun BudgetInsightActionCard(
    insights: BudgetInsightSummary,
    privacyModeEnabled: Boolean
) {
    val message = when {
        insights.overspentWallets.isNotEmpty() -> {
            val wallet = insights.overspentWallets.first()
            Triple(
                "Action focus",
                "Watch ${wallet.walletName}",
                "Ended ${maskedAmount(wallet.endingBalance, privacyModeEnabled)}. Consider increasing its plan or reducing pressure from other wallets."
            )
        }
        insights.mostRescuedWallet != null -> {
            val wallet = insights.mostRescuedWallet
            Triple(
                "Action focus",
                "Revisit ${wallet.walletName}",
                "It received ${maskedAmount(wallet.transferInTotal, privacyModeEnabled)} in rescue transfers."
            )
        }
        insights.topTransferPath != null -> {
            val path = insights.topTransferPath
            Triple(
                "Action focus",
                "Check ${path.sourceWalletName} -> ${path.destinationWalletName}",
                "${maskedAmount(path.totalAmount, privacyModeEnabled)} moved across ${path.transferCount} transfers."
            )
        }
        else -> Triple(
            "Action focus",
            "Budget is stable",
            "No major stress signals detected in this cycle."
        )
    }

    PriorityInsightCard(
        title = message.first,
        headline = message.second,
        detail = message.third
    )
}

@Composable
private fun BudgetInsightHeroCard(
    insights: BudgetInsightSummary,
    privacyModeEnabled: Boolean
) {
    val spentRatio = if (insights.totalPlanned > 0L) {
        (insights.totalSpent.toFloat() / insights.totalPlanned.toFloat()).coerceIn(0f, 1.5f)
    } else {
        0f
    }
    val stressLabel = when {
        insights.overspentWallets.isNotEmpty() -> "At risk"
        spentRatio > 0.9f -> "Watch closely"
        else -> "Healthy rhythm"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .offsetShadow(offset = 4.dp, color = MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.onSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Budget pulse",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = stressLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (insights.overspentWallets.isNotEmpty()) {
                            "${insights.overspentWallets.size} wallet${if (insights.overspentWallets.size == 1) "" else "s"} ended negative."
                        } else {
                            "No wallets ended negative in this cycle."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                SignalBadge(
                    tone = when {
                        insights.overspentWallets.isNotEmpty() -> SignalTone.Danger
                        spentRatio > 0.9f -> SignalTone.Warning
                        else -> SignalTone.Good
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Expense pressure",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = if (privacyModeEnabled) "••••" else "${(spentRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                }
                BlockProgressBar(
                    progress = spentRatio.coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InsightMetricCard(
                    title = "Planned",
                    value = maskedAmount(insights.totalPlanned, privacyModeEnabled),
                    supporting = "Allocated across wallets",
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    title = "Spent",
                    value = maskedAmount(insights.totalSpent, privacyModeEnabled),
                    supporting = "Expense outflow",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InsightMetricCard(
                    title = "Transferred",
                    value = maskedAmount(insights.totalTransferred, privacyModeEnabled),
                    supporting = "Internal rebalancing",
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    title = "Adjusted",
                    value = maskedAmount(insights.totalCredited, privacyModeEnabled),
                    supporting = "Manual corrections",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WalletHealthRow(
    insight: WalletBudgetInsight,
    privacyModeEnabled: Boolean
) {
    val spentRatio = if (insight.plannedAmount > 0L) {
        (insight.spentTotal.toFloat() / insight.plannedAmount.toFloat()).coerceIn(0f, 1.5f)
    } else 0f
    val tone = when {
        insight.overspent -> SignalTone.Danger
        spentRatio > 0.9f -> SignalTone.Warning
        else -> SignalTone.Good
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offsetShadow(offset = 4.dp, color = MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = insight.walletName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Ending ${maskedAmount(insight.endingBalance, privacyModeEnabled)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                SignalBadge(tone = tone)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Spent vs plan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${maskedAmount(insight.spentTotal, privacyModeEnabled)} / ${maskedAmount(insight.plannedAmount, privacyModeEnabled)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                BlockProgressBar(
                    progress = spentRatio.coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InsightMetricCard(
                    title = "Rescue in",
                    value = maskedAmount(insight.transferInTotal, privacyModeEnabled),
                    supporting = "Support received",
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    title = "Donated",
                    value = maskedAmount(insight.transferOutTotal, privacyModeEnabled),
                    supporting = "${insight.transactionCount} transactions",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalInsightsScreen(
    insights: GlobalInsightSummary?,
    privacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit,
    onOpenWalletInsight: (String) -> Unit,
    onOpenTransferInsight: (String, String) -> Unit,
    showBack: Boolean = true,
    onBack: () -> Unit
) {
    BrutalistBudgetTheme {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBack) {
                    TextButton(onClick = onBack) {
                        Text("[<]", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Patterns across all budgets",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                PrivacyToggleButton(
                    privacyModeEnabled = privacyModeEnabled,
                    onTogglePrivacyMode = onTogglePrivacyMode
                )
            }

            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.onSurface
            )

        if (insights == null) {
            LoadingState()
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (privacyModeEnabled) {
                item { PrivacyModeBanner(onTogglePrivacyMode = onTogglePrivacyMode) }
            }

            item {
                GlobalInsightActionCard(
                    insights = insights,
                    privacyModeEnabled = privacyModeEnabled
                )
            }

            item {
                GlobalInsightHeroCard(
                    insights = insights,
                    privacyModeEnabled = privacyModeEnabled
                )
            }

            item {
                InsightDetailCard(
                    title = "Recurring pressure",
                    subtitle = "These wallets show repeat patterns across multiple budgets."
                ) {
                    if (insights.walletPatterns.isEmpty()) {
                        Text(
                            text = "No wallet patterns yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        insights.walletPatterns.take(8).forEach { walletPattern ->
                            WalletHistoryVisualRow(
                                insight = walletPattern,
                                privacyModeEnabled = privacyModeEnabled,
                                onOpen = { onOpenWalletInsight(walletPattern.walletKey) }
                            )
                        }
                    }
                }
            }

            item {
                InsightDetailCard(
                    title = "Recurring transfer routes",
                    subtitle = "Repeated movement between the same wallets usually reveals structural fixes."
                ) {
                    if (insights.transferPatterns.isEmpty()) {
                        Text(
                            text = "No repeating transfer routes yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        insights.transferPatterns.take(8).forEach { transferPattern ->
                            TransferPatternVisualRow(
                                insight = transferPattern,
                                privacyModeEnabled = privacyModeEnabled,
                                onOpen = {
                                    onOpenTransferInsight(
                                        transferPattern.sourceWalletKey,
                                        transferPattern.destinationWalletKey
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun GlobalInsightActionCard(
    insights: GlobalInsightSummary,
    privacyModeEnabled: Boolean
) {
    val message = when {
        insights.mostUnderplannedWallet != null -> {
            val wallet = insights.mostUnderplannedWallet
            Triple(
                "Action focus",
                "Most often underplanned: ${wallet.displayName}",
                "Rescued ${wallet.rescueCount} times across ${wallet.budgetsAppearedIn} budgets."
            )
        }
        insights.topRepeatedTransferPath != null -> {
            val path = insights.topRepeatedTransferPath
            Triple(
                "Action focus",
                "Repeated repair route",
                "${path.sourceDisplayName} -> ${path.destinationDisplayName} moved ${maskedAmount(path.totalAmount, privacyModeEnabled)} across ${path.budgetsAppearedIn} budgets."
            )
        }
        else -> Triple(
            "Action focus",
            "Patterns still forming",
            "No major recurring issue stands out yet."
        )
    }

    PriorityInsightCard(
        title = message.first,
        headline = message.second,
        detail = message.third
    )
}

@Composable
private fun GlobalInsightHeroCard(
    insights: GlobalInsightSummary,
    privacyModeEnabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .offsetShadow(offset = 4.dp, color = MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.onSurface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Pattern pulse",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = insights.mostUnderplannedWallet?.displayName ?: "No strong pattern yet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = insights.mostUnderplannedWallet?.let {
                            "Most often rescued across ${it.budgetsAppearedIn} budgets."
                        } ?: "As more budgets are tracked, recurring signals will strengthen here.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                SignalBadge(
                    tone = if (insights.mostUnderplannedWallet != null) SignalTone.Warning else SignalTone.Good
                )
            }

            if (insights.observations.isNotEmpty()) {
                ObservationList(observations = insights.observations.take(2))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InsightMetricCard(
                    title = "Frequent rescue",
                    value = insights.mostFrequentRescueWallet?.displayName ?: "None",
                    supporting = insights.mostFrequentRescueWallet?.let {
                        maskedAmount(it.totalTransferIn, privacyModeEnabled)
                    } ?: "No rescue trend yet",
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    title = "Top donor",
                    value = insights.topDonorWallet?.displayName ?: "None",
                    supporting = insights.topDonorWallet?.let {
                        maskedAmount(it.totalTransferOut, privacyModeEnabled)
                    } ?: "No donor trend yet",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InsightMetricCard(
                    title = "Volatile wallet",
                    value = insights.mostVolatileWallet?.displayName ?: "None",
                    supporting = insights.mostVolatileWallet?.let {
                        "Variance ${maskedAmount(it.averageVarianceFromPlan, privacyModeEnabled)}"
                    } ?: "No volatility trend yet",
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    title = "Top route",
                    value = insights.topRepeatedTransferPath?.let {
                        "${it.sourceDisplayName} -> ${it.destinationDisplayName}"
                    } ?: "None",
                    supporting = insights.topRepeatedTransferPath?.let {
                        "${it.budgetsAppearedIn} budgets"
                    } ?: "No repeated route yet",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PriorityInsightCard(
    title: String,
    headline: String,
    detail: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .offsetShadow(offset = 4.dp, color = MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.onSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "[!] $title",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = headline,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SignalBadge(tone: SignalTone) {
    val status = when (tone) {
        SignalTone.Good -> BudgetStatus.OnTrack
        SignalTone.Warning -> BudgetStatus.Warning
        SignalTone.Danger -> BudgetStatus.Overspent
    }
    val text = when (tone) {
        SignalTone.Good -> "GOOD"
        SignalTone.Warning -> "WARN"
        SignalTone.Danger -> "DANGER"
    }
    val color = when (tone) {
        SignalTone.Good -> Success
        SignalTone.Warning -> Warning
        SignalTone.Danger -> Overspend
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        BudgetStatusIndicator(status = status)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
private fun PatternSummaryRow(
    title: String,
    value: String,
    supporting: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    }
}

@Composable
private fun TransferRouteRow(
    sourceName: String,
    destinationName: String,
    amount: String,
    supporting: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offsetShadow(offset = 4.dp, color = MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$sourceName → $destinationName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WalletHistoryVisualRow(
    insight: WalletHistoryInsight,
    privacyModeEnabled: Boolean,
    onOpen: () -> Unit
) {
    val tone = when {
        insight.negativeEndingCount > 0 -> SignalTone.Danger
        insight.rescueCount > 0 || insight.donorCount > 0 -> SignalTone.Warning
        else -> SignalTone.Good
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .offsetShadow(offset = 4.dp, color = MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = insight.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${insight.budgetsAppearedIn} budgets tracked",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                SignalBadge(tone = tone)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InsightMetricCard(
                    title = "Avg planned",
                    value = maskedAmount(insight.averagePlannedAmount, privacyModeEnabled),
                    supporting = "Avg spent ${maskedAmount(insight.averageSpentAmount, privacyModeEnabled)}",
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    title = "Pressure",
                    value = "${insight.rescueCount} rescue",
                    supporting = "${insight.negativeEndingCount} negative endings",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TransferPatternVisualRow(
    insight: TransferPathHistoryInsight,
    privacyModeEnabled: Boolean,
    onOpen: () -> Unit
) {
    Box(modifier = Modifier.clickable(onClick = onOpen)) {
        TransferRouteRow(
            sourceName = insight.sourceDisplayName,
            destinationName = insight.destinationDisplayName,
            amount = maskedAmount(insight.totalAmount, privacyModeEnabled),
            supporting = "${insight.budgetsAppearedIn} budgets • ${insight.transferCount} moves"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletHistoryDetailScreen(
    insight: WalletHistoryInsight,
    privacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit,
    onBack: () -> Unit
) {
    BrutalistBudgetTheme {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("[<]", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insight.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Wallet history",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                PrivacyToggleButton(
                    privacyModeEnabled = privacyModeEnabled,
                    onTogglePrivacyMode = onTogglePrivacyMode
                )
            }

            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.onSurface
            )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InsightDetailCard(
                    title = "Overview",
                    subtitle = "How this wallet behaves across budgets."
                ) {
                    ObservationList(
                        observations = buildList {
                            if (insight.rescueCount > 0) add(
                                InsightObservation(
                                    title = "Rescue pattern",
                                    message = "${insight.displayName} needed support in ${insight.rescueCount} budgets."
                                )
                            )
                            if (insight.donorCount > 0) add(
                                InsightObservation(
                                    title = "Donor pattern",
                                    message = "${insight.displayName} also funded other wallets in ${insight.donorCount} budgets."
                                )
                            )
                            if (insight.volatilityScore > 0) add(
                                InsightObservation(
                                    title = "Variability",
                                    message = "${insight.displayName} does not stay close to one stable pattern."
                                )
                            )
                        }
                    )
                    InsightMetricCard(
                        title = "Budgets appeared",
                        value = insight.budgetsAppearedIn.toString(),
                        supporting = "Tracked over time"
                    )
                    InsightMetricCard(
                        title = "Average planned",
                        value = maskedAmount(insight.averagePlannedAmount, privacyModeEnabled),
                        supporting = "Typical allocation"
                    )
                    InsightMetricCard(
                        title = "Average spent",
                        value = maskedAmount(insight.averageSpentAmount, privacyModeEnabled),
                        supporting = "Typical outflow"
                    )
                    InsightMetricCard(
                        title = "Average ending",
                        value = maskedAmount(insight.averageEndingBalance, privacyModeEnabled),
                        supporting = "How much usually remains"
                    )
                }
            }

            item {
                InsightDetailCard(
                    title = "Pressure signals",
                    subtitle = "These show whether the wallet is usually under strain or helping other wallets."
                ) {
                    TransferRouteRow(
                        sourceName = "Rescue received",
                        destinationName = "${insight.rescueCount} budgets",
                        amount = maskedAmount(insight.totalTransferIn, privacyModeEnabled),
                        supporting = "Total support received"
                    )
                    TransferRouteRow(
                        sourceName = "Donor behavior",
                        destinationName = "${insight.donorCount} budgets",
                        amount = maskedAmount(insight.totalTransferOut, privacyModeEnabled),
                        supporting = "Total moved to other wallets"
                    )
                    TransferRouteRow(
                        sourceName = "Negative endings",
                        destinationName = insight.negativeEndingCount.toString(),
                        amount = maskedAmount(insight.volatilityScore, privacyModeEnabled),
                        supporting = "Volatility score"
                    )
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferPatternDetailScreen(
    insight: TransferPathHistoryInsight,
    privacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit,
    onBack: () -> Unit
) {
    BrutalistBudgetTheme {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("[<]", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${insight.sourceDisplayName} → ${insight.destinationDisplayName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Transfer path history",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                PrivacyToggleButton(
                    privacyModeEnabled = privacyModeEnabled,
                    onTogglePrivacyMode = onTogglePrivacyMode
                )
            }

            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.onSurface
            )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InsightDetailCard(
                    title = "Route summary",
                    subtitle = "How this transfer route repeats across budgets."
                ) {
                    ObservationList(
                        observations = listOf(
                            InsightObservation(
                                title = "Repeated repair route",
                                message = "${insight.sourceDisplayName} -> ${insight.destinationDisplayName} appeared in ${insight.budgetsAppearedIn} budgets."
                            )
                        )
                    )
                    TransferRouteRow(
                        sourceName = insight.sourceDisplayName,
                        destinationName = insight.destinationDisplayName,
                        amount = maskedAmount(insight.totalAmount, privacyModeEnabled),
                        supporting = "${insight.transferCount} total moves"
                    )
                    InsightMetricCard(
                        title = "Budgets appeared",
                        value = insight.budgetsAppearedIn.toString(),
                        supporting = "Repeated across separate budgets"
                    )
                    InsightMetricCard(
                        title = "Average move",
                        value = maskedAmount(insight.averageAmount, privacyModeEnabled),
                        supporting = "Typical amount per transfer"
                    )
                }
            }
        }
    }
}
}