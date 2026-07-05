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
import com.mebudget.app.ui.theme.BrutalistBudgetTheme

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
        BrutalistBudgetTheme {
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "■ BUDGETS ■",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            color = Color.Black
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showCreateOptions = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                "+ CREATE BUDGET",
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
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
            .padding(horizontal = 20.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(4.dp, Color.Black)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
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
                        color = Color.Black
                    )
                    Text(
                        text = "active wallets",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
                Text(
                    text = maskedAmount(totalBalance, privacyModeEnabled),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
            HorizontalDivider(color = Color.Black, thickness = 1.dp)
            Text(
                text = "ACROSS ALL BUDGETS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color.Black
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
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(4.dp, Color.Black)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
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
                        color = Color.Black
                    )
                    Text(
                        text = budget.formatDateRange(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                }
                val isOverspent = budget.totalBalance < 0
                val dotColor = if (isOverspent) Color(0xFFFF0000) else Color(0xFF00AA00)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(dotColor)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOverspent) "OVERSENT" else "ON TRACK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = dotColor
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${budget.activeWalletCount} active",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = maskedAmount(budget.totalBalance, privacyModeEnabled),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
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
        containerColor = Color.White,
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(4.dp, Color.Black)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("START BLANK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.Black)
                        Text(
                            "Create a fresh budget and add wallets yourself.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Black,
                            color = Color.Black.copy(alpha = 0.6f)
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
                            Color.White
                        } else {
                            Color.White.copy(alpha = 0.45f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(4.dp, Color.Black)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("USE TEMPLATE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.Black)
                        Text(
                            if (hasTemplates) {
                                "Copy wallet names and planned amounts from an existing budget."
                            } else {
                                "Create one budget first to unlock templates."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Black,
                            color = Color.Black.copy(alpha = 0.6f)
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
