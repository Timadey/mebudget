package com.mebudget.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mebudget.app.data.BudgetSummary
import com.mebudget.app.data.sync.SyncState
import com.mebudget.app.ui.theme.BrutalistBudgetTheme
import androidx.compose.ui.text.style.TextAlign
import com.mebudget.app.ui.common.offsetShadow
import com.mebudget.app.ui.common.SectionHeader
import com.mebudget.app.ui.common.BudgetStatusIndicator
import com.mebudget.app.ui.common.BudgetStatus
import com.mebudget.app.ui.common.SyncStatusBanner
import com.mebudget.app.ui.common.UpgradePrompt
import com.mebudget.app.ui.theme.AccentBlue

@Composable
fun BudgetsScreen(
    budgets: List<BudgetSummary>,
    onOpenBudget: (Long) -> Unit,
    privacyModeEnabled: Boolean,
    onCreateBudget: (BudgetDraft) -> Unit,
    onDuplicateBudget: (Long, String) -> Unit,
    onDeleteBudget: (Long) -> Unit,
    syncState: SyncState = SyncState.Idle,
    onSyncRetry: () -> Unit = {},
    onSyncPausedClick: () -> Unit = {},
    canCreateBudget: Boolean = true,
    onUpgradeClick: () -> Unit = {}
) {
    var showCreateOptions by rememberSaveable { mutableStateOf(false) }
    var showBlankBudgetDialog by rememberSaveable { mutableStateOf(false) }
    var templateBudgetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingBudgetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showUpgradePrompt by rememberSaveable { mutableStateOf(false) }

    val totalBalance = budgets.sumOf { it.totalBalance }
    val activeWallets = budgets.sumOf { it.activeWalletCount }

    Box(modifier = Modifier.fillMaxSize()) {
        BrutalistBudgetTheme {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (privacyModeEnabled) {
                    item {
                        PrivacyModeBanner()
                    }
                }

                if (syncState !is SyncState.Idle) {
                    item {
                        SyncStatusBanner(
                            syncState = syncState,
                            onRetryClick = onSyncRetry,
                            onPausedClick = onSyncPausedClick
                        )
                    }
                }

                item {
                    TotalSummarySection(
                        totalBalance = totalBalance,
                        activeWallets = activeWallets,
                        privacyModeEnabled = privacyModeEnabled
                    )
                }

                item {
                    SectionHeader(
                        title = "BUDGETS",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                if (canCreateBudget) {
                                    showCreateOptions = true
                                } else {
                                    showUpgradePrompt = true
                                }
                            },
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                        ) {
                            Text(
                                "+ CREATE BUDGET",
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                if (budgets.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Start your financial journey",
                            subtitle = "Create your first budget sheet to organize your spending and savings.",
                            actionLabel = "Create Budget",
                            onAction = { showCreateOptions = true }
                        )
                    }
                } else {
                    items(budgets, key = { it.id }) { budget ->
                        BudgetSummaryCard(
                            modifier = Modifier.animateItem(),
                            budget = budget,
                            privacyModeEnabled = privacyModeEnabled,
                            onOpen = { onOpenBudget(budget.id) },
                            onDuplicate = { templateBudgetId = budget.id },
                            onDelete = { deletingBudgetId = budget.id }
                        )
                    }
                }
            }
        }
    }

    if (showCreateOptions) {
        BudgetCreationChoiceDialog(
            hasTemplates = budgets.isNotEmpty(),
            onDismiss = { showCreateOptions = false },
            onCreateBlank = {
                showCreateOptions = false
                showBlankBudgetDialog = true
            },
            onCreateFromTemplate = {
                showCreateOptions = false
                templateBudgetId = budgets.firstOrNull()?.id
            }
        )
    }

    if (showUpgradePrompt) {
        UpgradePrompt(
            featureName = "Unlimited Budgets",
            onUpgradeClick = {
                showUpgradePrompt = false
                onUpgradeClick()
            },
            onDismiss = { showUpgradePrompt = false }
        )
    }

    if (showBlankBudgetDialog) {
        BudgetDialog(
            title = "New Budget",
            onDismiss = { showBlankBudgetDialog = false },
            onSave = {
                onCreateBudget(it)
                showBlankBudgetDialog = false
            }
        )
    }

    templateBudgetId?.let { selectedTemplateId ->
        TemplateBudgetDialog(
            budgets = budgets,
            initialTemplateBudgetId = selectedTemplateId,
            onDismiss = { templateBudgetId = null },
            onCreateFromTemplate = { sourceBudgetId, name ->
                onDuplicateBudget(sourceBudgetId, name)
                templateBudgetId = null
            }
        )
    }

    deletingBudgetId?.let { budgetId ->
        AlertDialog(
            onDismissRequest = { deletingBudgetId = null },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            title = {
                Text(
                    "DELETE BUDGET",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "Delete this budget? All wallets and transactions will be permanently removed.",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBudget(budgetId)
                        deletingBudgetId = null
                    },
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("DELETE", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingBudgetId = null }) {
                    Text("CANCEL", fontWeight = FontWeight.Black)
                }
            }
        )
    }
}

@Composable
private fun TotalSummarySection(totalBalance: Long, activeWallets: Int, privacyModeEnabled: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .offsetShadow(offset = 4.dp, color = MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "$activeWallets",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ACTIVE WALLETS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = maskedAmount(totalBalance, privacyModeEnabled),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface, thickness = 1.dp)
            Text(
                text = "ACROSS ALL BUDGETS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BudgetSummaryCard(
    modifier: Modifier = Modifier,
    budget: BudgetSummary,
    privacyModeEnabled: Boolean,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .offsetShadow(offset = 4.dp, color = MaterialTheme.colorScheme.outline)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.name.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = budget.formatDateRange(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val isOverspent = budget.totalBalance < 0
                val status = if (isOverspent) BudgetStatus.Overspent else BudgetStatus.OnTrack
                val statusText = if (isOverspent) "OVERSPENT" else "ON TRACK"
                val statusColor = if (isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BudgetStatusIndicator(status = status)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${budget.activeWalletCount} active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = maskedAmount(budget.totalBalance, privacyModeEnabled),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BudgetCreationChoiceDialog(
    hasTemplates: Boolean,
    onDismiss: () -> Unit,
    onCreateBlank: () -> Unit,
    onCreateFromTemplate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = { Text("Create Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Choose how you want to start this budget.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCreateBlank),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(4.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("START BLANK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Create a fresh budget and add wallets yourself.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = hasTemplates, onClick = onCreateFromTemplate),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasTemplates) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(4.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("USE TEMPLATE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (hasTemplates) {
                                "Copy wallet names and planned amounts from an existing budget."
                            } else {
                                "Create one budget first to unlock templates."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun TemplateBudgetDialog(
    budgets: List<BudgetSummary>,
    initialTemplateBudgetId: Long?,
    onDismiss: () -> Unit,
    onCreateFromTemplate: (Long, String) -> Unit
) {
    var selectedBudgetId by remember(initialTemplateBudgetId) { mutableStateOf(initialTemplateBudgetId) }
    var templateName by remember(initialTemplateBudgetId, budgets) {
        mutableStateOf(
            initialTemplateBudgetId?.let { budgetId ->
                budgets.firstOrNull { it.id == budgetId }?.name?.plus(" Copy")
            }.orEmpty()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = { Text("Use Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WalletTemplateDropdown(
                    label = "Template budget",
                    budgets = budgets,
                    selectedBudgetId = selectedBudgetId,
                    onSelected = { budgetId ->
                        selectedBudgetId = budgetId
                        if (templateName.isBlank() || templateName.endsWith(" Copy")) {
                            templateName = budgets.firstOrNull { it.id == budgetId }?.name?.plus(" Copy").orEmpty()
                        }
                    }
                )
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("New budget name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "This copies wallet names and planned amounts from the selected budget.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedBudgetId?.let { onCreateFromTemplate(it, templateName) } },
                enabled = selectedBudgetId != null && templateName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
