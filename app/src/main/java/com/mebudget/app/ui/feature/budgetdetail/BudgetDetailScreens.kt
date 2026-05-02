package com.mebudget.app.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.mebudget.app.data.BudgetDetail
import com.mebudget.app.data.BudgetEntity
import com.mebudget.app.data.TransactionSummary
import com.mebudget.app.data.TransactionType
import com.mebudget.app.data.WalletSummary
import com.mebudget.app.ui.theme.Overspend
import com.mebudget.app.ui.theme.Success
import com.mebudget.app.ui.theme.Warning
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
    onDeleteTransaction: (Long) -> Unit
) {
    val activeWallets = detail.wallets.filterNot { it.archived }
    var editingWallet by remember { mutableStateOf<WalletSummary?>(null) }
    var expenseDraft by remember(wallet.id) { mutableStateOf<ExpenseDraft?>(null) }
    var transferDraft by remember(wallet.id) { mutableStateOf<TransferDraft?>(null) }
    var adjustmentDraft by remember(wallet.id) { mutableStateOf<AdjustmentDraft?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionSummary?>(null) }
    var deletingTransaction by remember { mutableStateOf<TransactionSummary?>(null) }

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
        onDeleteTransactionRequest = { deletingTransaction = it }
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
    onDeleteTransaction: (Long) -> Unit
) {
    val activeWallets = detail.wallets.filterNot { it.archived }
    val canAddTransfer = activeWallets.size > 1
    val canAddAdjustment = activeWallets.isNotEmpty()
    var inlineSpendWalletId by rememberSaveable(detail.budget.id) { mutableStateOf<Long?>(null) }
    var showArchived by rememberSaveable(detail.budget.id) { mutableStateOf(false) }
    var showWalletDialog by rememberSaveable { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<WalletSummary?>(null) }
    var showTransferDialog by rememberSaveable { mutableStateOf(false) }
    var showAdjustmentDialog by rememberSaveable { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionSummary?>(null) }
    var deletingTransaction by remember { mutableStateOf<TransactionSummary?>(null) }
    var showBudgetSettings by rememberSaveable { mutableStateOf(false) }

    BudgetOverviewScreen(
        detail = detail,
        privacyModeEnabled = privacyModeEnabled,
        onTogglePrivacyMode = onTogglePrivacyMode,
        visibleWallets = detail.wallets.filter { showArchived || !it.archived },
        canAddTransfer = canAddTransfer,
        canAddAdjustment = canAddAdjustment,
        showArchived = showArchived,
        onBack = onBack,
        onToggleArchived = { showArchived = it },
        onOpenSettings = { showBudgetSettings = true },
        onOpenInsights = onOpenInsights,
        onQuickTransfer = { showTransferDialog = true },
        onQuickAdjustment = { showAdjustmentDialog = true },
        onAddWalletRequest = { showWalletDialog = true },
        inlineSpendWalletId = inlineSpendWalletId,
        onInlineSpendToggle = { walletId ->
            inlineSpendWalletId = if (inlineSpendWalletId == walletId) null else walletId
        },
        onInlineSpendSave = { wallet, amount ->
            onAddExpense(
                detail.budget.id,
                ExpenseDraft(walletId = wallet.id, amount = amount)
            )
            inlineSpendWalletId = null
        },
        onOpenWallet = onOpenWallet,
        onEditWallet = { editingWallet = it },
        onArchiveWallet = { onArchiveWallet(it.id, !it.archived) },
        onMoveWalletUp = { onMoveWallet(it.id, -1) },
        onMoveWalletDown = { onMoveWallet(it.id, 1) },
        onEditTransaction = { editingTransaction = it },
        onDeleteTransaction = { deletingTransaction = it }
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

    if (showAdjustmentDialog) {
        AdjustmentDialog(
            wallets = activeWallets,
            onDismiss = { showAdjustmentDialog = false },
            onSave = {
                onAddAdjustment(detail.budget.id, it)
                showAdjustmentDialog = false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetOverviewScreen(
    detail: BudgetDetail,
    privacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit,
    visibleWallets: List<WalletSummary>,
    canAddTransfer: Boolean,
    canAddAdjustment: Boolean,
    showArchived: Boolean,
    onBack: () -> Unit,
    onToggleArchived: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInsights: () -> Unit,
    onQuickTransfer: () -> Unit,
    onQuickAdjustment: () -> Unit,
    onAddWalletRequest: () -> Unit,
    inlineSpendWalletId: Long?,
    onInlineSpendToggle: (Long) -> Unit,
    onInlineSpendSave: (WalletSummary, String) -> Unit,
    onOpenWallet: (WalletSummary) -> Unit,
    onEditWallet: (WalletSummary) -> Unit,
    onArchiveWallet: (WalletSummary) -> Unit,
    onMoveWalletUp: (WalletSummary) -> Unit,
    onMoveWalletDown: (WalletSummary) -> Unit,
    onEditTransaction: (TransactionSummary) -> Unit,
    onDeleteTransaction: (TransactionSummary) -> Unit
) {
    val recentTransactions = detail.transactions.take(3)
    var selectedSection by rememberSaveable(detail.budget.id) {
        mutableStateOf(BudgetOverviewSection.Wallets)
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Column {
                        Text(detail.budget.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            detail.budget.formatDateRange(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    PrivacyToggleButton(
                        privacyModeEnabled = privacyModeEnabled,
                        onTogglePrivacyMode = onTogglePrivacyMode
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wallets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = showArchived,
                                onClick = { onToggleArchived(!showArchived) },
                                label = { Text(if (showArchived) "Archived visible" else "Archived hidden") }
                            )
                            FilledTonalButton(onClick = onAddWalletRequest) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                Spacer(modifier = Modifier.height(0.dp))
                                Text("Add Wallet")
                            }
                        }
                    }
                }

                item {
                    BudgetActionStrip(
                        canAddTransfer = canAddTransfer,
                        canAddAdjustment = canAddAdjustment,
                        onQuickTransfer = onQuickTransfer,
                        onQuickAdjustment = onQuickAdjustment
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
                            wallet = wallet,
                            privacyModeEnabled = privacyModeEnabled,
                            isInlineSpendExpanded = inlineSpendWalletId == wallet.id,
                            onOpen = { onOpenWallet(wallet) },
                            onSpend = { onInlineSpendToggle(wallet.id) },
                            onInlineSpendSave = { amount -> onInlineSpendSave(wallet, amount) },
                            onInlineSpendCancel = {
                                if (inlineSpendWalletId == wallet.id) {
                                    onInlineSpendToggle(wallet.id)
                                }
                            },
                            onEdit = { onEditWallet(wallet) },
                            onArchiveToggle = { onArchiveWallet(wallet) },
                            onMoveUp = { onMoveWalletUp(wallet) },
                            onMoveDown = { onMoveWalletDown(wallet) }
                        )
                    }
                }
            }

            if (selectedSection == BudgetOverviewSection.Activity) {
                item {
                    Text(
                        text = "Recent activity",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (recentTransactions.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Quiet for now",
                            subtitle = "Your recent expenses and transfers will show up here."
                        )
                    }
                } else {
                    items(recentTransactions, key = { "tx-${it.id}" }) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            privacyModeEnabled = privacyModeEnabled,
                            onEdit = { onEditTransaction(transaction) },
                            onDelete = { onDeleteTransaction(transaction) }
                        )
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

@Composable
private fun BudgetSectionSwitcher(
    selectedSection: BudgetOverviewSection,
    onSectionSelected: (BudgetOverviewSection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BudgetOverviewSection.entries.forEach { section ->
            FilterChip(
                selected = selectedSection == section,
                onClick = { onSectionSelected(section) },
                label = { Text(section.label) }
            )
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Remaining Balance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        maskedAmount(currentBalance, privacyModeEnabled),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "planned ${maskedAmount(totalPlanned, privacyModeEnabled)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        maskedPercent(progress, privacyModeEnabled),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (progress < 0.2f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (progress < 0.2f) Overspend else Success,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetActionStrip(
    canAddTransfer: Boolean,
    canAddAdjustment: Boolean,
    onQuickTransfer: () -> Unit,
    onQuickAdjustment: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = onQuickTransfer, enabled = canAddTransfer, modifier = Modifier.weight(1f)) {
                    Text("Move Money")
                }
                FilledTonalButton(onClick = onQuickAdjustment, enabled = canAddAdjustment, modifier = Modifier.weight(1f)) {
                    Text("Adjust")
                }
            }
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
    onDeleteTransactionRequest: (TransactionSummary) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(wallet.name, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditWallet) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    PrivacyToggleButton(
                        privacyModeEnabled = privacyModeEnabled,
                        onTogglePrivacyMode = onTogglePrivacyMode
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    text = "History",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyState(
                        title = "No history yet",
                        subtitle = "Recorded expenses and transfers for this wallet will appear here."
                    )
                }
            } else {
                items(transactions, key = { "tx-${it.id}" }) { transaction ->
                    TransactionCard(
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

@Composable
private fun WalletActionStrip(
    canTransfer: Boolean,
    onSpend: () -> Unit,
    onTransfer: () -> Unit,
    onAdjust: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onSpend, modifier = Modifier.weight(1f)) {
                Text("Add expense")
            }
            OutlinedButton(onClick = onTransfer, enabled = canTransfer, modifier = Modifier.weight(1f)) {
                Text("Move money")
            }
            OutlinedButton(onClick = onAdjust, modifier = Modifier.weight(1f)) {
                Text("Adjust")
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
    val isOverspent = wallet.warning

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isOverspent) MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Available Balance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = maskedAmount(wallet.balance, privacyModeEnabled),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = if (isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Planned: ${maskedAmount(wallet.plannedAmount, privacyModeEnabled)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = maskedPercent(progress, privacyModeEnabled),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = if (isOverspent) Overspend else Success,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
private fun WalletCard(
    wallet: WalletSummary,
    privacyModeEnabled: Boolean,
    isInlineSpendExpanded: Boolean,
    onOpen: () -> Unit,
    onSpend: () -> Unit,
    onInlineSpendSave: (String) -> Unit,
    onInlineSpendCancel: () -> Unit,
    onEdit: () -> Unit,
    onArchiveToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val progress = if (wallet.plannedAmount > 0) (wallet.balance.toFloat() / wallet.plannedAmount.toFloat()).coerceIn(0f, 1f) else 0f
    val isOverspent = wallet.warning
    val statusLabel = when {
        wallet.archived -> "Hidden"
        isOverspent -> "Needs attention"
        progress < 0.2f -> "Low runway"
        else -> "On track"
    }
    var inlineAmount by remember(wallet.id, isInlineSpendExpanded) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isInlineSpendExpanded) {
        if (isInlineSpendExpanded) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isInlineSpendExpanded) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isInlineSpendExpanded) 2.dp else 1.dp),
        border = BorderStroke(
            1.dp,
            when {
                isOverspent -> MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                isInlineSpendExpanded -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Planned ${maskedAmount(wallet.plannedAmount, privacyModeEnabled)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AssistChip(
                        onClick = if (wallet.archived) onArchiveToggle else ({ }),
                        label = { Text(statusLabel) },
                        leadingIcon = if (isOverspent) {
                            {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                                )
                            }
                        } else {
                            null
                        },
                        colors = if (isOverspent) {
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else {
                            AssistChipDefaults.assistChipColors()
                        }
                    )
                }
                Text(
                    text = maskedAmount(wallet.balance, privacyModeEnabled),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = if (isOverspent) Overspend else Success,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = onSpend, modifier = Modifier.weight(1f)) {
                    Text("Quick spend")
                }
                Button(onClick = onOpen, modifier = Modifier.weight(1f)) {
                    Text("Open")
                }
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit wallet") },
                            onClick = {
                                onEdit()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (wallet.archived) "Unhide" else "Hide") },
                            onClick = {
                                onArchiveToggle()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move Up") },
                            onClick = {
                                onMoveUp()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move Down") },
                            onClick = {
                                onMoveDown()
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (isInlineSpendExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), MaterialTheme.shapes.medium)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Quick spend for ${wallet.name}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inlineAmount,
                        onValueChange = { inlineAmount = it.filterNumericInput() },
                        label = { Text("Amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                    QuickAmountChips(
                        amounts = listOf(500L, 1_000L, 2_000L, 5_000L),
                        onAmountSelected = { amount -> inlineAmount = amount.toString() }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                inlineAmount = ""
                                keyboardController?.hide()
                                onInlineSpendCancel()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onInlineSpendSave(inlineAmount)
                                inlineAmount = ""
                                keyboardController?.hide()
                            },
                            enabled = inlineAmount.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: TransactionSummary,
    privacyModeEnabled: Boolean,
    focusWalletId: Long? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = when (transaction.type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
        TransactionType.ADJUSTMENT -> Warning
    }
    val icon = when (transaction.type) {
        TransactionType.EXPENSE -> Icons.Default.History
        TransactionType.TRANSFER -> Icons.Default.SwapHoriz
        TransactionType.ADJUSTMENT -> Icons.Default.Settings
    }
    val amountText = transaction.amountText(focusWalletId, privacyModeEnabled)
    val amountColor = when {
        transaction.type == TransactionType.TRANSFER &&
            focusWalletId != null &&
            transaction.destinationWalletId == focusWalletId -> Success
        transaction.type == TransactionType.ADJUSTMENT && transaction.amount > 0L -> Success
        transaction.type == TransactionType.ADJUSTMENT && transaction.amount < 0L -> MaterialTheme.colorScheme.error
        else -> accentColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = transaction.title(focusWalletId),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${transaction.subtitle(focusWalletId)} • ${transaction.dateLabel()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = amountColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(transaction.typeLabel()) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = accentColor.copy(alpha = 0.12f),
                            labelColor = accentColor
                        )
                    )
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Transaction options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    onEdit()
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    onDelete()
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                transaction.note?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
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
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    label = { Text("Wallet name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.plannedAmount,
                    onValueChange = { draft = draft.copy(plannedAmount = it.filterNumericInput()) },
                    label = { Text("Planned amount") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text(saveLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add Expense", style = MaterialTheme.typography.titleLarge)
            WalletDropdown(
                label = "Wallet",
                wallets = wallets,
                selectedWalletId = draft.walletId,
                onSelected = { draft = draft.copy(walletId = it) }
            )
            OutlinedTextField(
                value = draft.amount,
                onValueChange = { draft = draft.copy(amount = it.filterNumericInput()) },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            DateInputField(value = draft.date, onValueChange = { draft = draft.copy(date = it) }, label = "Date")
            OutlinedTextField(
                value = draft.note,
                onValueChange = { draft = draft.copy(note = it) },
                label = { Text("Optional note") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = { onSave(draft) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Move Money", style = MaterialTheme.typography.titleLarge)
            WalletDropdown(
                label = "From wallet",
                wallets = wallets,
                selectedWalletId = draft.sourceWalletId,
                onSelected = { draft = draft.copy(sourceWalletId = it) }
            )
            WalletDropdown(
                label = "To wallet",
                wallets = wallets,
                selectedWalletId = draft.destinationWalletId,
                onSelected = { draft = draft.copy(destinationWalletId = it) }
            )
            OutlinedTextField(
                value = draft.amount,
                onValueChange = { draft = draft.copy(amount = it.filterNumericInput()) },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            DateInputField(value = draft.date, onValueChange = { draft = draft.copy(date = it) }, label = "Date")
            OutlinedTextField(
                value = draft.note,
                onValueChange = { draft = draft.copy(note = it) },
                label = { Text("Optional note") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = { onSave(draft) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Manual Adjustment", style = MaterialTheme.typography.titleLarge)
            WalletDropdown(
                label = "Wallet",
                wallets = wallets,
                selectedWalletId = draft.walletId,
                onSelected = { draft = draft.copy(walletId = it) }
            )
            OutlinedTextField(
                value = draft.signedAmount,
                onValueChange = { draft = draft.copy(signedAmount = it.filterSignedNumericInput()) },
                label = { Text("Signed amount (+/-)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            DateInputField(value = draft.date, onValueChange = { draft = draft.copy(date = it) }, label = "Date")
            OutlinedTextField(
                value = draft.note,
                onValueChange = { draft = draft.copy(note = it) },
                label = { Text("Optional note") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = { onSave(draft) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
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
        title = { Text("Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (draft.type) {
                    TransactionType.EXPENSE -> {
                        WalletDropdown(
                            label = "Wallet",
                            wallets = wallets,
                            selectedWalletId = draft.sourceWalletId,
                            onSelected = { draft = draft.copy(sourceWalletId = it) }
                        )
                    }
                    TransactionType.TRANSFER -> {
                        WalletDropdown(
                            label = "From wallet",
                            wallets = wallets,
                            selectedWalletId = draft.sourceWalletId,
                            onSelected = { draft = draft.copy(sourceWalletId = it) }
                        )
                        WalletDropdown(
                            label = "To wallet",
                            wallets = wallets,
                            selectedWalletId = draft.destinationWalletId,
                            onSelected = { draft = draft.copy(destinationWalletId = it) }
                        )
                    }
                    TransactionType.ADJUSTMENT -> {
                        WalletDropdown(
                            label = "Wallet",
                            wallets = wallets,
                            selectedWalletId = draft.sourceWalletId,
                            onSelected = { draft = draft.copy(sourceWalletId = it) }
                        )
                    }
                }
                OutlinedTextField(
                    value = draft.amount,
                    onValueChange = {
                        draft = draft.copy(
                            amount = when (draft.type) {
                                TransactionType.ADJUSTMENT -> it.filterSignedNumericInput()
                                else -> it.filterNumericInput()
                            }
                        )
                    },
                    label = { Text(if (draft.type == TransactionType.ADJUSTMENT) "Signed amount (+/-)" else "Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                DateInputField(value = draft.date, onValueChange = { draft = draft.copy(date = it) }, label = "Date")
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) },
                    label = { Text("Optional note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
        title = { Text("Confirm Delete") },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
