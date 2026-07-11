package com.mebudget.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mebudget.app.data.BudgetInsightSummary
import com.mebudget.app.data.InsightObservation
import com.mebudget.app.ui.common.offsetShadow
import com.mebudget.app.ui.common.SectionHeader

@Composable
fun BudgetInsightSection(
    insights: BudgetInsightSummary,
    privacyModeEnabled: Boolean,
    onOpenInsights: () -> Unit
) {
    val previewObservations = insights.observations.take(2)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${insights.walletInsights.size} wallets reviewed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onOpenInsights,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("View Budget Insights")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InsightMetricCard(
                    title = "Most rescued",
                    value = insights.mostRescuedWallet?.walletName ?: "None",
                    supporting = insights.mostRescuedWallet?.let {
                        "${maskedAmount(it.transferInTotal, privacyModeEnabled)} received"
                    } ?: "No rescue transfers",
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    title = "Top donor",
                    value = insights.topDonorWallet?.walletName ?: "None",
                    supporting = insights.topDonorWallet?.let {
                        "${maskedAmount(it.transferOutTotal, privacyModeEnabled)} moved out"
                    } ?: "No donor wallet",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InsightMetricCard(
                    title = "Top transfer path",
                    value = insights.topTransferPath?.let {
                        "${it.sourceWalletName} -> ${it.destinationWalletName}"
                    } ?: "None",
                    supporting = insights.topTransferPath?.let {
                        "${maskedAmount(it.totalAmount, privacyModeEnabled)} across ${it.transferCount} moves"
                    } ?: "No transfers yet",
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    title = "Overspent wallets",
                    value = insights.overspentWallets.size.toString(),
                    supporting = if (insights.overspentWallets.isEmpty()) {
                        "No negative balances"
                    } else {
                        insights.overspentWallets.joinToString(limit = 2) { it.walletName }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            if (previewObservations.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ObservationList(observations = previewObservations)
            }
        }
    }
}

@Composable
fun InsightMetricCard(
    title: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ObservationList(observations: List<InsightObservation>) {
    if (observations.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        observations.forEach { observation ->
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = observation.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = observation.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InsightDetailCard(
    title: String,
    subtitle: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offsetShadow(offset = 4.dp, color = MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = title,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
fun TransferPathRow(
    sourceName: String,
    destinationName: String,
    supporting: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$sourceName -> $destinationName",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    }
}
