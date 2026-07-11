package com.mebudget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mebudget.app.data.BudgetDetail
import com.mebudget.app.data.BudgetEntity
import com.mebudget.app.data.TransactionSummary
import com.mebudget.app.data.TransactionType
import com.mebudget.app.data.WalletSummary
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.mebudget.app.ui.common.BlockProgressBar
import com.mebudget.app.ui.common.offsetShadow
import com.mebudget.app.ui.common.TransactionTypeBadge
import com.mebudget.app.ui.common.SectionHeader
import com.mebudget.app.ui.theme.AccentBlue
import com.mebudget.app.ui.theme.BrutalistBudgetTheme
import java.time.LocalDate

private enum class BudgetOverviewSection(val label: String) {
    Wallets("Wallets"),
    Activity("Activity"),
    Insights("Insights")
}

@Composable
fun WalletDetailRouteScreen(
    detail: BudgetDetail,
    wallet: WalletSummary,
    privacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit,
    onBack: () -> Unit,
    onSaveWallet: (WalletDraft) -> Unit,
    onArchiveWallet: (Long, Boolean) -> Unit,
    onMoveWallet: (Long, Int) -> Unit,
    onAddExpense: (Long, ExpenseDraft) -> Unit,
    onAddTransfer: (Long, TransferDraft) -> Unit,
    onAddAdjustment: (Long, AdjustmentDraft) -> Unit,
    onUpdateTransaction: (TransactionEditorState) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onDeleteWallet: (Long) -> Unit
) {
    val activeWallets = detail.wallets.filterNot { it.archived }
    var editingWallet by remember { mutableStateOf<WalletSummary?>(null) }
    var expenseDraft by remember(wallet.id) { mutableStateOf<ExpenseDraft?>(null) }
    var transferDraft by remember(wallet.id) { mutableStateOf<TransferDraft?>(null) }
    var adjustmentDraft by remember(wallet.id) { mutableStateOf<AdjustmentDraft?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionSummary?>(null) }
    var deletingTransaction by remember { mutableStateOf<TransactionSummary?>(null) }
    var deletingWallet by remember { mutableStateOf(false) }

    BrutalistBudgetTheme {
        WalletDetailScreen(
            wallet = wallet,
            transactions = detail.transactions.filter {
                it.sourceWalletId == wallet.id || it.destinationWalletId == wallet.id
            },
            privacyModeEnabled = privacyModeEnabled,
            onTogglePrivacyMode = onTogglePrivacyMode,
            canTransfer = activeWallets.size > 1,
            onBack = onBack,
            onSpend = { expenseDraft = ExpenseDraft(walletId = wallet.id) },
            onTransfer = {
                transferDraft = TransferDraft(
                    sourceWalletId = wallet.id,
                    destinationWalletId = activeWallets.firstOrNull { it.id != wallet.id }?.id
                )
            },
            onAdjust = { adjustmentDraft = AdjustmentDraft(walletId = wallet.id) },
            onEditWallet = { editingWallet = wallet },
            onArchiveToggle = { onArchiveWallet(wallet.id, !wallet.archived) },
            onMoveUp = { onMoveWallet(wallet.id, -1) },
            onMoveDown = { onMoveWallet(wallet.id, 1) },
            onEditTransaction = { editingTransaction = it },
            onDeleteTransactionRequest = { deletingTransaction = it },
            onDeleteWallet = { deletingWallet = true }
        )

        editingWallet?.let { currentWallet ->
            WalletDialog(
                title = "Edit Wallet",
                initial = WalletDraft(
                    walletId = currentWallet.id,
                    name = currentWallet.name,
                    plannedAmount = currentWallet.plannedAmount.toString()
                ),
                saveLabel = "Save",
                onDismiss = { editingWallet = null },
                onSave = {
                    onSaveWallet(it)
                    editingWallet = null
                }
            )
        }

        expenseDraft?.let { draft ->
            ExpenseDialog(
                wallets = activeWallets,
                initial = draft,
                onDismiss = { expenseDraft = null },
                onSave = {
                    onAddExpense(detail.budget.id, it)
                    expenseDraft = null
                }
            )
        }

        transferDraft?.let { draft ->
            TransferDialog(
                wallets = activeWallets,
                initial = draft,
                onDismiss = { transferDraft = null },
                onSave = {
                    onAddTransfer(detail.budget.id, it)
                    transferDraft = null
                }
            )
        }

        adjustmentDraft?.let { draft ->
            AdjustmentDialog(
                wallets = activeWallets,
                initial = draft,
                onDismiss = { adjustmentDraft = null },
                onSave = {
                    onAddAdjustment(detail.budget.id, it)
                    adjustmentDraft = null
                }
            )
        }

        editingTransaction?.let { transaction ->
            TransactionEditorDialog(
                editorState = transaction.toEditorState(),
                wallets = detail.wallets,
                onDismiss = { editingTransaction = null },
                onSave = {
                    onUpdateTransaction(it)
                    editingTransaction = null
                }
            )
        }

        deletingTransaction?.let { transaction ->
            ConfirmDeleteDialog(
                text = "Delete this transaction? This will recalculate the wallet balances.",
                onDismiss = { deletingTransaction = null },
                onConfirm = {
                    onDeleteTransaction(transaction.id)
                    deletingTransaction = null
                }
            )
        }

        if (deletingWallet) {
            ConfirmDeleteDialog(
                text = "Delete this wallet? Transactions referencing it will no longer be linked to it.",
                onDismiss = { deletingWallet = false },
                onConfirm = {
                    onDeleteWallet(wallet.id)
                    deletingWallet = false
                }
            )
        }
    }
}

@Composable
fun BudgetDetailScreen(
    detail: BudgetDetail,
    privacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit,
    onBack: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenWallet: (WalletSummary) -> Unit,
    onUpdateBudgetSettings: (BudgetEntity) -> Unit,
    onAddWallet: (Long, WalletDraft) -> Unit,
    onSaveWallet: (WalletDraft) -> Unit,
    onArchiveWallet: (Long, Boolean) -> Unit,
    onMoveWallet: (Long, Int) -> Unit,
    onAddExpense: (Long, ExpenseDraft) -> Unit,
    onAddTransfer: (Long, TransferDraft) -> Unit,
    onAddAdjustment: (Long, AdjustmentDraft) -> Unit,
    onUpdateTransaction: (TransactionEditorState) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onDeleteBudget: (Long) -> Unit,
    onDeleteWallet: (Long) -> Unit
) {
    val activeWallets = detail.wallets.filterNot { it.archived }
    val canAddTransfer = activeWallets.size > 1
    var showArchived by rememberSaveable(detail.budget.id) { mutableStateOf(false) }
    var showWalletDialog by rememberSaveable { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<WalletSummary?>(null) }
    var showTransferDialog by rememberSaveable { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionSummary?>(null) }
    var deletingTransaction by remember { mutableStateOf<TransactionSummary?>(null) }
    var showBudgetSettings by rememberSaveable { mutableStateOf(false) }
    var showDeleteBudgetConfirm by rememberSaveable { mutableStateOf(false) }

    BrutalistBudgetTheme {
        BudgetOverviewScreen(
        detail = detail,
        privacyModeEnabled = privacyModeEnabled,
        onTogglePrivacyMode = onTogglePrivacyMode,
        visibleWallets = detail.wallets.filter { showArchived || !it.archived },
        canAddTransfer = canAddTransfer,
        showArchived = showArchived,
        onBack = onBack,
        onToggleArchived = { showArchived = it },
        onOpenSettings = { showBudgetSettings = true },
        onOpenInsights = onOpenInsights,
        onQuickTransfer = { showTransferDialog = true },
        onAddWalletRequest = { showWalletDialog = true },
        onOpenWallet = onOpenWallet,
        onEditWallet = { editingWallet = it },
        onArchiveWallet = { onArchiveWallet(it.id, !it.archived) },
        onMoveWalletUp = { onMoveWallet(it.id, -1) },
        onMoveWalletDown = { onMoveWallet(it.id, 1) },
        onEditTransaction = { editingTransaction = it },
        onDeleteTransaction = { deletingTransaction = it },
        onDeleteWallet = { onDeleteWallet(it.id) }
    )

        if (showBudgetSettings) {
            BudgetDialog(
                title = "Budget Settings",
                initial = BudgetDraft(
                    name = detail.budget.name,
                    startDate = detail.budget.startDateEpochDay?.let(LocalDate::ofEpochDay)?.toString().orEmpty(),
                    endDate = detail.budget.endDateEpochDay?.let(LocalDate::ofEpochDay)?.toString().orEmpty(),
                    negativeBalanceRule = detail.budget.negativeBalanceRule
                ),
                saveLabel = "Save",
                onDismiss = { showBudgetSettings = false },
                onSave = {
                    onUpdateBudgetSettings(
                        detail.budget.copy(
                            name = it.name.trim(),
                            startDateEpochDay = it.startDate.parseDateOrNull(),
                            endDateEpochDay = it.endDate.parseDateOrNull(),
                            negativeBalanceRule = it.negativeBalanceRule
                        )
                    )
                    showBudgetSettings = false
                },
                onDelete = { showDeleteBudgetConfirm = true }
            )
        }

        if (showDeleteBudgetConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteBudgetConfirm = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                title = { Text("Delete Budget", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Text("Delete this budget? All wallets and transactions will be permanently removed.", color = MaterialTheme.colorScheme.onSurface)
                },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteBudget(detail.budget.id)
                        showDeleteBudgetConfirm = false
                    }) {
                        Text("DELETE", fontWeight = FontWeight.Black, color = Color(0xFFFF0000))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteBudgetConfirm = false }) {
                        Text("CANCEL", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }

        if (showWalletDialog) {
            WalletDialog(
                title = "Add Wallet",
                onDismiss = { showWalletDialog = false },
                onSave = {
                    onAddWallet(detail.budget.id, it)
                    showWalletDialog = false
                }
            )
        }

        editingWallet?.let { wallet ->
            WalletDialog(
                title = "Edit Wallet",
                initial = WalletDraft(
                    walletId = wallet.id,
                    name = wallet.name,
                    plannedAmount = wallet.plannedAmount.toString()
                ),
                saveLabel = "Save",
                onDismiss = { editingWallet = null },
                onSave = {
                    onSaveWallet(it)
                    editingWallet = null
                }
            )
        }

        if (showTransferDialog) {
            TransferDialog(
                wallets = activeWallets,
                onDismiss = { showTransferDialog = false },
                onSave = {
                    onAddTransfer(detail.budget.id, it)
                    showTransferDialog = false
                }
            )
        }

        editingTransaction?.let { transaction ->
            TransactionEditorDialog(
                editorState = transaction.toEditorState(),
                wallets = detail.wallets,
                onDismiss = { editingTransaction = null },
                onSave = {
                    onUpdateTransaction(it)
                    editingTransaction = null
                }
            )
        }

        deletingTransaction?.let { transaction ->
            ConfirmDeleteDialog(
                text = "Delete this transaction? This will recalculate the wallet balances.",
                onDismiss = { deletingTransaction = null },
                onConfirm = {
                    onDeleteTransaction(transaction.id)
                    deletingTransaction = null
                }
            )
        }
    }
}

@Composable
private fun BudgetOverviewScreen(
    detail: BudgetDetail,
    privacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit,
    visibleWallets: List<WalletSummary>,
    canAddTransfer: Boolean,
    showArchived: Boolean,
    onBack: () -> Unit,
    onToggleArchived: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInsights: () -> Unit,
    onQuickTransfer: () -> Unit,
    onAddWalletRequest: () -> Unit,
    onOpenWallet: (WalletSummary) -> Unit,
    onEditWallet: (WalletSummary) -> Unit,
    onArchiveWallet: (WalletSummary) -> Unit,
    onMoveWalletUp: (WalletSummary) -> Unit,
    onMoveWalletDown: (WalletSummary) -> Unit,
    onEditTransaction: (TransactionSummary) -> Unit,
    onDeleteTransaction: (TransactionSummary) -> Unit,
    onDeleteWallet: (WalletSummary) -> Unit
) {
    val recentTransactions = detail.transactions.take(3)
    var selectedSection by rememberSaveable(detail.budget.id) {
        mutableStateOf(BudgetOverviewSection.Wallets)
    }

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
                        text = detail.budget.name.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        detail.budget.formatDateRange(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                TextButton(onClick = onOpenSettings) {
                    Text("[S]", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                TextButton(onClick = onTogglePrivacyMode) {
                    val icon = if (privacyModeEnabled) "[P]" else "[p]"
                    Text(icon, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface, thickness = 2.dp)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    BudgetStatusCard(detail = detail, privacyModeEnabled = privacyModeEnabled)
                }

                item {
                    BudgetSectionSwitcher(
                        selectedSection = selectedSection,
                        onSectionSelected = { selectedSection = it }
                    )
                }

                if (selectedSection == BudgetOverviewSection.Wallets) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WALLETS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onToggleArchived(!showArchived) },
                                    shape = RoundedCornerShape(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (showArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        contentColor = if (showArchived) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(if (showArchived) "Archived visible" else "Archived hidden", fontWeight = FontWeight.Black)
                                }
                                Button(
                                    onClick = onAddWalletRequest,
                                    shape = RoundedCornerShape(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("+ Add Wallet", fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    item {
                        BudgetActionStrip(
                            canAddTransfer = canAddTransfer,
                            onQuickTransfer = onQuickTransfer
                        )
                    }

                    if (visibleWallets.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No wallets found",
                                subtitle = "Add your first wallet to start tracking your budget."
                            )
                        }
                    } else {
                        items(visibleWallets, key = { "wallet-${it.id}" }) { wallet ->
                            WalletCard(
                                modifier = Modifier.animateItem(),
                                wallet = wallet,
                                privacyModeEnabled = privacyModeEnabled,
                                onOpen = { onOpenWallet(wallet) },
                                onEdit = { onEditWallet(wallet) },
                                onArchiveToggle = { onArchiveWallet(wallet) },
                                onMoveUp = { onMoveWalletUp(wallet) },
                                onMoveDown = { onMoveWalletDown(wallet) },
                                onDelete = { onDeleteWallet(wallet) }
                            )
                        }
                    }
                }

                if (selectedSection == BudgetOverviewSection.Activity) {
                    item {
                        Text(
                            text = "RECENT ACTIVITY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    if (recentTransactions.isEmpty()) {
                        item { EmptyState(title = "NO ACTIVITY", subtitle = "Transactions will appear here.") }
                    } else {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                recentTransactions.forEachIndexed { index, transaction ->
                                    if (index == 0 || transaction.dateEpochDay != recentTransactions[index - 1].dateEpochDay) {
                                        Text(
                                            text = "── ${LocalDate.ofEpochDay(transaction.dateEpochDay)} ──",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    TransactionHistoryRow(
                                        transaction = transaction,
                                        privacyModeEnabled = privacyModeEnabled,
                                        focusWalletId = null,
                                        onEdit = { onEditTransaction(transaction) },
                                        onDelete = { onDeleteTransaction(transaction) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedSection == BudgetOverviewSection.Insights) {
                    item {
                        BudgetInsightSection(
                            insights = detail.insights,
                            privacyModeEnabled = privacyModeEnabled,
                            onOpenInsights = onOpenInsights
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetSectionSwitcher(
    selectedSection: BudgetOverviewSection,
    onSectionSelected: (BudgetOverviewSection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BudgetOverviewSection.entries.forEach { section ->
            val isSelected = section == selectedSection
            Button(
                onClick = { onSectionSelected(section) },
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = section.label.uppercase(),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun BudgetStatusCard(detail: BudgetDetail, privacyModeEnabled: Boolean) {
    val totalPlanned = detail.wallets.sumOf { it.plannedAmount }
    val currentBalance = detail.wallets.sumOf { it.balance }
    val progress = if (totalPlanned > 0) (currentBalance.toFloat() / totalPlanned.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "REMAINING",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        maskedAmount(currentBalance, privacyModeEnabled),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "planned ${maskedAmount(totalPlanned, privacyModeEnabled)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        maskedPercent(progress, privacyModeEnabled),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            BlockProgressBar(
                progress = progress,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
        }
    }
}

@Composable
private fun BudgetActionStrip(
    canAddTransfer: Boolean,
    onQuickTransfer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onQuickTransfer,
            enabled = canAddTransfer,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) {
            Text("MOVE MONEY", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletDetailScreen(
    wallet: WalletSummary,
    transactions: List<TransactionSummary>,
    privacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit,
    canTransfer: Boolean,
    onBack: () -> Unit,
    onSpend: () -> Unit,
    onTransfer: () -> Unit,
    onAdjust: () -> Unit,
    onEditWallet: () -> Unit,
    onArchiveToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditTransaction: (TransactionSummary) -> Unit,
    onDeleteTransactionRequest: (TransactionSummary) -> Unit,
    onDeleteWallet: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("[<]", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = wallet.name.uppercase(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box {
                var expanded by remember { mutableStateOf(false) }
                TextButton(onClick = { expanded = true }) {
                    Text("[⋮]", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline)
                ) {
                    DropdownMenuItem(
                        text = { Text("EDIT", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface) },
                        onClick = { onEditWallet(); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("DELETE", fontWeight = FontWeight.Black, color = Color(0xFFFF0000)) },
                        onClick = { onDeleteWallet(); expanded = false }
                    )
                }
            }
            TextButton(onClick = onTogglePrivacyMode) {
                val icon = if (privacyModeEnabled) "[P]" else "[p]"
                Text(icon, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface, thickness = 2.dp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                WalletSummaryPanel(
                    wallet = wallet,
                    privacyModeEnabled = privacyModeEnabled,
                    onEditWallet = onEditWallet,
                    onArchiveToggle = onArchiveToggle,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown
                )
            }
            item {
                WalletActionStrip(
                    canTransfer = canTransfer,
                    onSpend = onSpend,
                    onTransfer = onTransfer,
                    onAdjust = onAdjust
                )
            }
            item {
                Text(
                    text = "HISTORY",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (transactions.isEmpty()) {
                item { EmptyState(title = "NO HISTORY", subtitle = "Transactions will appear here.") }
            } else {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        transactions.forEachIndexed { index, transaction ->
                            if (index == 0 || transaction.dateEpochDay != transactions[index - 1].dateEpochDay) {
                                Text(
                                    text = "── ${LocalDate.ofEpochDay(transaction.dateEpochDay)} ──",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            TransactionHistoryRow(
                                transaction = transaction,
                                privacyModeEnabled = privacyModeEnabled,
                                focusWalletId = wallet.id,
                                onEdit = { onEditTransaction(transaction) },
                                onDelete = { onDeleteTransactionRequest(transaction) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletActionStrip(
    canTransfer: Boolean,
    onSpend: () -> Unit,
    onTransfer: () -> Unit,
    onAdjust: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onSpend,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) {
            Text("ADD EXPENSE", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        OutlinedButton(
            onClick = onTransfer,
            enabled = canTransfer,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Text("MOVE MONEY", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        Box {
            var expanded by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { expanded = true },
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("[...]", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surface,
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline)
            ) {
                DropdownMenuItem(
                    text = { Text("MANUAL ADJUSTMENT", fontWeight = FontWeight.Black) },
                    onClick = { onAdjust(); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun WalletSummaryPanel(
    wallet: WalletSummary,
    privacyModeEnabled: Boolean,
    onEditWallet: () -> Unit,
    onArchiveToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val progress = if (wallet.plannedAmount > 0) (wallet.balance.toFloat() / wallet.plannedAmount.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = maskedAmount(wallet.balance, privacyModeEnabled)
                    .map { "$it " }
                    .joinToString("")
                    .trimEnd(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "planned ${maskedAmount(wallet.plannedAmount, privacyModeEnabled)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = maskedPercent(progress, privacyModeEnabled),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            BlockProgressBar(
                progress = progress,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
        }
    }
}

@Composable
private fun WalletCard(
    modifier: Modifier = Modifier,
    wallet: WalletSummary,
    privacyModeEnabled: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onArchiveToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (wallet.plannedAmount > 0) (wallet.balance.toFloat() / wallet.plannedAmount.toFloat()).coerceIn(0f, 1f) else 0f
    val isOverspent = wallet.warning

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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOverspent) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(Color(0xFFFF0000))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            text = wallet.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "planned ${maskedAmount(wallet.plannedAmount, privacyModeEnabled)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Text(
                    text = maskedAmount(wallet.balance, privacyModeEnabled),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            BlockProgressBar(
                progress = progress,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (progress * 100).toInt().toString() + "%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val statusLabel = when {
                    wallet.archived -> "HIDDEN"
                    isOverspent -> "OVERSENT"
                    progress < 0.2f -> "LOW"
                    else -> "OK"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = if (isOverspent) Color(0xFFFF0000) else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(BorderStroke(3.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(0.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(">", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionHistoryRow(
    transaction: TransactionSummary,
    privacyModeEnabled: Boolean,
    focusWalletId: Long?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = when (transaction.type) {
        TransactionType.EXPENSE -> Color(0xFFFF0000)
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
        TransactionType.ADJUSTMENT -> Color(0xFFFF8800)
    }
    val amountText = transaction.amountText(focusWalletId, privacyModeEnabled)
    val typeTag = when (transaction.type) {
        TransactionType.EXPENSE -> "[EXP]"
        TransactionType.TRANSFER -> "[TRF]"
        TransactionType.ADJUSTMENT -> "[ADJ]"
    }

    var showActions by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showActions = !showActions }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showActions) {
            TextButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Text("[EDIT]", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }
            TextButton(onClick = onDelete) {
                Text("[DEL]", fontWeight = FontWeight.Black, color = Color(0xFFFF0000))
            }
        } else {
            Text(
                text = transaction.title(focusWalletId),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = amountText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = typeTag,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
    transaction.note?.takeIf { it.isNotBlank() }?.let { note ->
        if (!showActions) {
            Text(
                text = note,
                modifier = Modifier.padding(start = 0.dp, bottom = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                maxLines = 1
            )
        }
    }
}

private fun TransactionSummary.typeLabel(): String {
    return when (type) {
        TransactionType.EXPENSE -> "Expense"
        TransactionType.TRANSFER -> "Transfer"
        TransactionType.ADJUSTMENT -> "Adjustment"
    }
}

private fun TransactionSummary.dateLabel(): String {
    return LocalDate.ofEpochDay(dateEpochDay).toString()
}

@Composable
private fun WalletDialog(
    title: String,
    initial: WalletDraft = WalletDraft(),
    saveLabel: String = "Add",
    onDismiss: () -> Unit,
    onSave: (WalletDraft) -> Unit
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = { Text(title, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BrutalistTextField(
                    label = "WALLET NAME",
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) }
                )
                BrutalistTextField(
                    label = "PLANNED AMOUNT",
                    value = draft.plannedAmount,
                    onValueChange = { draft = draft.copy(plannedAmount = it.filterNumericInput()) },
                    keyboardType = KeyboardType.Number
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text(saveLabel, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDialog(
    wallets: List<WalletSummary>,
    initial: ExpenseDraft = ExpenseDraft(),
    onDismiss: () -> Unit,
    onSave: (ExpenseDraft) -> Unit
) {
    var draft by remember(initial, wallets) {
        mutableStateOf(initial.copy(walletId = initial.walletId ?: wallets.firstOrNull()?.id))
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "ADD EXPENSE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                BrutalistWalletDropdown(
                    label = "WALLET",
                    wallets = wallets,
                    selectedWalletId = draft.walletId,
                    onSelected = { draft = draft.copy(walletId = it) }
                )
                BrutalistTextField(
                    label = "AMOUNT",
                    value = draft.amount,
                    onValueChange = { draft = draft.copy(amount = it.filterNumericInput()) },
                    keyboardType = KeyboardType.Number
                )
                BrutalistDateField(
                    label = "DATE",
                    value = draft.date,
                    onValueChange = { draft = draft.copy(date = it) }
                )
                BrutalistTextField(
                    label = "NOTE",
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) }
                )
                Button(
                    onClick = { onSave(draft) },
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("SAVE", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "cancel",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferDialog(
    wallets: List<WalletSummary>,
    initial: TransferDraft = TransferDraft(),
    onDismiss: () -> Unit,
    onSave: (TransferDraft) -> Unit
) {
    var draft by remember(initial, wallets) {
        mutableStateOf(
            initial.copy(
                sourceWalletId = initial.sourceWalletId ?: wallets.firstOrNull()?.id,
                destinationWalletId = initial.destinationWalletId
                    ?: wallets.firstOrNull { it.id != (initial.sourceWalletId ?: wallets.firstOrNull()?.id) }?.id
                    ?: wallets.drop(1).firstOrNull()?.id
                    ?: wallets.firstOrNull()?.id
            )
        )
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "MOVE MONEY",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                BrutalistWalletDropdown(
                    label = "FROM WALLET",
                    wallets = wallets,
                    selectedWalletId = draft.sourceWalletId,
                    onSelected = { draft = draft.copy(sourceWalletId = it) }
                )
                BrutalistWalletDropdown(
                    label = "TO WALLET",
                    wallets = wallets,
                    selectedWalletId = draft.destinationWalletId,
                    onSelected = { draft = draft.copy(destinationWalletId = it) }
                )
                BrutalistTextField(
                    label = "AMOUNT",
                    value = draft.amount,
                    onValueChange = { draft = draft.copy(amount = it.filterNumericInput()) },
                    keyboardType = KeyboardType.Number
                )
                BrutalistDateField(
                    label = "DATE",
                    value = draft.date,
                    onValueChange = { draft = draft.copy(date = it) }
                )
                BrutalistTextField(
                    label = "NOTE",
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) }
                )
                Button(
                    onClick = { onSave(draft) },
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("SAVE", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "cancel",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdjustmentDialog(
    wallets: List<WalletSummary>,
    initial: AdjustmentDraft = AdjustmentDraft(),
    onDismiss: () -> Unit,
    onSave: (AdjustmentDraft) -> Unit
) {
    var draft by remember(initial, wallets) {
        mutableStateOf(initial.copy(walletId = initial.walletId ?: wallets.firstOrNull()?.id))
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "MANUAL ADJUSTMENT",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                BrutalistWalletDropdown(
                    label = "WALLET",
                    wallets = wallets,
                    selectedWalletId = draft.walletId,
                    onSelected = { draft = draft.copy(walletId = it) }
                )
                BrutalistTextField(
                    label = "SIGNED AMOUNT (+/-)",
                    value = draft.signedAmount,
                    onValueChange = { draft = draft.copy(signedAmount = it.filterSignedNumericInput()) },
                    keyboardType = KeyboardType.Number
                )
                BrutalistDateField(
                    label = "DATE",
                    value = draft.date,
                    onValueChange = { draft = draft.copy(date = it) }
                )
                BrutalistTextField(
                    label = "NOTE",
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) }
                )
                Button(
                    onClick = { onSave(draft) },
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("SAVE", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "cancel",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionEditorDialog(
    editorState: TransactionEditorState,
    wallets: List<WalletSummary>,
    onDismiss: () -> Unit,
    onSave: (TransactionEditorState) -> Unit
) {
    var draft by remember(editorState) { mutableStateOf(editorState) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Text(
                "EDIT TRANSACTION",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (draft.type) {
                    TransactionType.EXPENSE -> {
                        BrutalistWalletDropdown(
                            label = "WALLET",
                            wallets = wallets,
                            selectedWalletId = draft.sourceWalletId,
                            onSelected = { draft = draft.copy(sourceWalletId = it) }
                        )
                    }
                    TransactionType.TRANSFER -> {
                        BrutalistWalletDropdown(
                            label = "FROM WALLET",
                            wallets = wallets,
                            selectedWalletId = draft.sourceWalletId,
                            onSelected = { draft = draft.copy(sourceWalletId = it) }
                        )
                        BrutalistWalletDropdown(
                            label = "TO WALLET",
                            wallets = wallets,
                            selectedWalletId = draft.destinationWalletId,
                            onSelected = { draft = draft.copy(destinationWalletId = it) }
                        )
                    }
                    TransactionType.ADJUSTMENT -> {
                        BrutalistWalletDropdown(
                            label = "WALLET",
                            wallets = wallets,
                            selectedWalletId = draft.sourceWalletId,
                            onSelected = { draft = draft.copy(sourceWalletId = it) }
                        )
                    }
                }
                BrutalistTextField(
                    label = if (draft.type == TransactionType.ADJUSTMENT) "SIGNED AMOUNT (+/-)" else "AMOUNT",
                    value = draft.amount,
                    onValueChange = {
                        draft = draft.copy(
                            amount = when (draft.type) {
                                TransactionType.ADJUSTMENT -> it.filterSignedNumericInput()
                                else -> it.filterNumericInput()
                            }
                        )
                    },
                    keyboardType = if (draft.type == TransactionType.ADJUSTMENT) null else KeyboardType.Number
                )
                BrutalistDateField(
                    label = "DATE",
                    value = draft.date,
                    onValueChange = { draft = draft.copy(date = it) }
                )
                BrutalistTextField(
                    label = "NOTE",
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text("SAVE", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}

@Composable
private fun ConfirmDeleteDialog(
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Text("DELETE?", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("DELETE", fontWeight = FontWeight.Black, color = Color(0xFFFF0000))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}

private fun String.filterSignedNumericInput(): String {
    return buildString {
        forEachIndexed { index, char ->
            if (char.isDigit() || char == ',') append(char)
            if (char == '-' && index == 0) append(char)
        }
    }
}

private fun BudgetEntity.formatDateRange(): String {
    val start = startDateEpochDay?.let(LocalDate::ofEpochDay)?.toString()
    val end = endDateEpochDay?.let(LocalDate::ofEpochDay)?.toString()
    return when {
        start != null && end != null -> "$start to $end"
        start != null -> "Starts $start"
        else -> "No date range"
    }
}

private fun TransactionSummary.title(focusWalletId: Long? = null): String {
    return when (type) {
        TransactionType.EXPENSE -> "Spent from ${sourceWalletName.orEmpty()}"
        TransactionType.TRANSFER -> when {
            focusWalletId != null && sourceWalletId == focusWalletId -> "Moved to ${destinationWalletName.orEmpty()}"
            focusWalletId != null && destinationWalletId == focusWalletId -> "Moved from ${sourceWalletName.orEmpty()}"
            else -> "Moved from ${sourceWalletName.orEmpty()} to ${destinationWalletName.orEmpty()}"
        }
        TransactionType.ADJUSTMENT -> "Adjusted ${sourceWalletName.orEmpty()}"
    }
}

private fun TransactionSummary.subtitle(focusWalletId: Long? = null): String {
    return when (type) {
        TransactionType.EXPENSE -> "Expense"
        TransactionType.TRANSFER -> when {
            focusWalletId != null && sourceWalletId == focusWalletId -> "Transfer out"
            focusWalletId != null && destinationWalletId == focusWalletId -> "Transfer in"
            else -> "Transfer"
        }
        TransactionType.ADJUSTMENT -> if (amount < 0L) "Manual decrease" else "Manual increase"
    }
}

private fun TransactionSummary.amountText(
    focusWalletId: Long? = null,
    privacyModeEnabled: Boolean = false
): String {
    if (privacyModeEnabled) {
        return when (type) {
            TransactionType.EXPENSE -> "-••••"
            TransactionType.TRANSFER -> when {
                focusWalletId != null && sourceWalletId == focusWalletId -> "-••••"
                focusWalletId != null && destinationWalletId == focusWalletId -> "+••••"
                else -> "••••"
            }
            TransactionType.ADJUSTMENT -> if (amount > 0L) "+••••" else "-••••"
        }
    }
    return when (type) {
        TransactionType.EXPENSE -> "-${com.mebudget.app.data.formatAmount(amount)}"
        TransactionType.TRANSFER -> when {
            focusWalletId != null && sourceWalletId == focusWalletId -> "-${com.mebudget.app.data.formatAmount(amount)}"
            focusWalletId != null && destinationWalletId == focusWalletId -> "+${com.mebudget.app.data.formatAmount(amount)}"
            else -> com.mebudget.app.data.formatAmount(amount)
        }
        TransactionType.ADJUSTMENT -> if (amount > 0L) "+${com.mebudget.app.data.formatAmount(amount)}" else com.mebudget.app.data.formatAmount(amount)
    }
}

private fun String.parseDateOrNull(): Long? {
    if (isBlank()) return null
    return runCatching { LocalDate.parse(trim()).toEpochDay() }.getOrNull()
}

private fun maskedPercent(progress: Float, privacyModeEnabled: Boolean): String {
    return if (privacyModeEnabled) "••••" else "${(progress * 100).toInt()}%"
}

@Composable
private fun BrutalistTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = if (keyboardType != null) KeyboardOptions(keyboardType = keyboardType) else KeyboardOptions.Default,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrutalistWalletDropdown(
    label: String,
    wallets: List<WalletSummary>,
    selectedWalletId: Long?,
    onSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedWallet = wallets.firstOrNull { it.id == selectedWalletId }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = selectedWallet?.name.orEmpty(),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = wallets.isNotEmpty())
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
            ) {
                wallets.forEach { wallet ->
                    DropdownMenuItem(
                        text = { Text(wallet.name, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            onSelected(wallet.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrutalistDateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Box {
            TextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Select date", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showPicker = true }
            )
        }
    }
    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toPickerMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { pickedMillis ->
                            onValueChange(pickedMillis.toStoredDate())
                        }
                        showPicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("CANCEL", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}


