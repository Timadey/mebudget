package com.mebudget.app.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mebudget.app.data.BudgetSummary
import com.mebudget.app.data.WalletSummary

@Composable
fun BudgetsScreen(
    budgets: List<BudgetSummary>,
    onOpenBudget: (Long) -> Unit,
    privacyModeEnabled: Boolean,
    onCreateBudget: (BudgetDraft) -> Unit,
    onDuplicateBudget: (Long, String) -> Unit,
    onDeleteBudget: (Long) -> Unit
) {
    var showCreateOptions by rememberSaveable { mutableStateOf(false) }
    var showBlankBudgetDialog by rememberSaveable { mutableStateOf(false) }
    var templateBudgetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingBudgetId by rememberSaveable { mutableStateOf<Long?>(null) }

    val totalBalance = budgets.sumOf { it.totalBalance }
    val activeWallets = budgets.sumOf { it.activeWalletCount }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (privacyModeEnabled) {
                item {
                    PrivacyModeBanner()
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (budgets.isEmpty()) "Start here" else "Budgets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (budgets.isEmpty()) {
                                "Create a budget before logging expenses."
                            } else {
                                "Open a budget for context, or use quick expense below."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showCreateOptions = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        Text("New")
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
            title = { Text("Delete Budget") },
            text = {
                Text("Delete this budget? All wallets and transactions will be permanently removed.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteBudget(budgetId)
                    deletingBudgetId = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingBudgetId = null }) {
                    Text("Cancel")
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Across all budgets",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = maskedAmount(totalBalance, privacyModeEnabled),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "$activeWallets active wallets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = budget.formatDateRange(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    androidx.compose.material3.IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete budget",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    androidx.compose.material3.IconButton(onClick = onDuplicate) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Duplicate budget",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${budget.activeWalletCount} active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Start blank", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Create a fresh budget and add wallets yourself.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = hasTemplates, onClick = onCreateFromTemplate),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasTemplates) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Use template", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (hasTemplates) {
                                "Copy wallet names and planned amounts from an existing budget."
                            } else {
                                "Create one budget first to unlock templates."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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


