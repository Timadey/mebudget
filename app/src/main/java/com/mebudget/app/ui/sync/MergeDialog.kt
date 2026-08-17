package com.mebudget.app.ui.sync

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun MergeDialog(
    viewModel: MergeViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "SYNC YOUR DATA",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You have data on this device and in the cloud.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                CountRow("On this device", uiState.localCounts.budgets, uiState.localCounts.wallets, uiState.localCounts.transactions)
                CountRow("In the cloud", uiState.cloudCounts.budgets, uiState.cloudCounts.wallets, uiState.cloudCounts.transactions)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "How should we combine them?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.selectMergeOption(MergeOption.MERGE) },
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(0.dp),
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        "MERGE BOTH (RECOMMENDED)",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.selectMergeOption(MergeOption.KEEP_CLOUD) },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(0.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("USE CLOUD", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { viewModel.selectMergeOption(MergeOption.KEEP_LOCAL) },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(0.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("KEEP DEVICE", fontWeight = FontWeight.Bold)
                    }
                }

                if (uiState.isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Syncing…", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (uiState.isComplete) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Done — your data is now in sync.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    enabled = !uiState.isLoading
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun CountRow(label: String, budgets: Int, wallets: Int, transactions: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Column(horizontalAlignment = Alignment.End) {
            Text("$budgets budgets", style = MaterialTheme.typography.bodyMedium)
            Text("$wallets wallets", style = MaterialTheme.typography.bodyMedium)
            Text("$transactions transactions", style = MaterialTheme.typography.bodyMedium)
        }
    }
}