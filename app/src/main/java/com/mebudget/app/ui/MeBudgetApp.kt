package com.mebudget.app.ui

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.mebudget.app.data.BudgetDetail
import com.mebudget.app.data.BudgetEntity
import com.mebudget.app.data.BudgetSummary
import com.mebudget.app.data.NegativeBalanceRule
import com.mebudget.app.data.TransactionSummary
import com.mebudget.app.data.TransactionType
import com.mebudget.app.data.WalletSummary
import com.mebudget.app.data.formatAmount
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import com.mebudget.app.ui.theme.Success
import com.mebudget.app.ui.theme.Overspend
import com.mebudget.app.ui.theme.Warning

@Composable
fun MeBudgetApp(viewModel: MeBudgetViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
            viewModel.clearError()
        }
    }

    DisposableEffect(uiState.privacyModeEnabled, view) {
        val window = view.context.findActivity()?.window
        if (uiState.privacyModeEnabled) {
            window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { }
    }

    MeBudgetRoot(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onTogglePrivacyMode = viewModel::togglePrivacyMode,
        onOpenBudget = viewModel::openBudget,
        onCloseBudget = viewModel::closeBudget,
        onCreateBudget = viewModel::createBudget,
        onDuplicateBudget = viewModel::duplicateBudget,
        onUpdateBudgetSettings = viewModel::updateBudgetSettings,
        onAddWallet = viewModel::addWallet,
        onSaveWallet = viewModel::saveWallet,
        onArchiveWallet = viewModel::archiveWallet,
        onMoveWallet = viewModel::moveWallet,
        onAddExpense = viewModel::addExpense,
        onAddTransfer = viewModel::addTransfer,
        onAddAdjustment = viewModel::addAdjustment,
        onUpdateTransaction = viewModel::updateTransaction,
        onDeleteTransaction = viewModel::deleteTransaction
    )
}

private object MeBudgetRoute {
    const val budgets = "budgets"
    const val budget = "budget"
    const val wallet = "wallet"

    fun budget(budgetId: Long): String = "$budget/$budgetId"
    fun wallet(budgetId: Long, walletId: Long): String = "$budget/$budgetId/$wallet/$walletId"
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MeBudgetRoot(
    uiState: MeBudgetUiState,
    snackbarHostState: SnackbarHostState,
    onTogglePrivacyMode: () -> Unit,
    onOpenBudget: (Long) -> Unit,
    onCloseBudget: () -> Unit,
    onCreateBudget: (BudgetDraft) -> Unit,
    onDuplicateBudget: (Long, String) -> Unit,
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
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(uiState.selectedBudgetId, currentRoute) {
        val selectedBudgetId = uiState.selectedBudgetId ?: return@LaunchedEffect
        if (currentRoute == MeBudgetRoute.budgets) {
            navController.navigate(MeBudgetRoute.budget(selectedBudgetId))
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (currentRoute == MeBudgetRoute.budgets) {
                CenterAlignedTopAppBar(
                    title = { Text("MeBudget", style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        PrivacyToggleButton(
                            privacyModeEnabled = uiState.privacyModeEnabled,
                            onTogglePrivacyMode = onTogglePrivacyMode
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavHost(
                navController = navController,
                startDestination = MeBudgetRoute.budgets
            ) {
                composable(MeBudgetRoute.budgets) {
                    BudgetsScreen(
                        budgets = uiState.budgets,
                        onOpenBudget = { budgetId ->
                            onOpenBudget(budgetId)
                            navController.navigate(MeBudgetRoute.budget(budgetId))
                        },
                        privacyModeEnabled = uiState.privacyModeEnabled,
                        onCreateBudget = onCreateBudget,
                        onDuplicateBudget = onDuplicateBudget
                    )
                }

                composable(
                    route = "${MeBudgetRoute.budget}/{budgetId}",
                    arguments = listOf(navArgument("budgetId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val budgetId = backStackEntry.arguments?.getLong("budgetId") ?: return@composable
                    LaunchedEffect(budgetId) {
                        onOpenBudget(budgetId)
                    }
                    val detail = uiState.selectedBudgetDetail
                    if (detail?.budget?.id == budgetId) {
                        BudgetDetailScreen(
                            detail = detail,
                            privacyModeEnabled = uiState.privacyModeEnabled,
                            onTogglePrivacyMode = onTogglePrivacyMode,
                            onBack = {
                                onCloseBudget()
                                navController.popBackStack()
                            },
                            onOpenWallet = { wallet ->
                                navController.navigate(MeBudgetRoute.wallet(budgetId, wallet.id))
                            },
                            onUpdateBudgetSettings = onUpdateBudgetSettings,
                            onAddWallet = onAddWallet,
                            onSaveWallet = onSaveWallet,
                            onArchiveWallet = onArchiveWallet,
                            onMoveWallet = onMoveWallet,
                            onAddExpense = onAddExpense,
                            onAddTransfer = onAddTransfer,
                            onAddAdjustment = onAddAdjustment,
                            onUpdateTransaction = onUpdateTransaction,
                            onDeleteTransaction = onDeleteTransaction
                        )
                    } else {
                        LoadingState()
                    }
                }

                composable(
                    route = "${MeBudgetRoute.budget}/{budgetId}/${MeBudgetRoute.wallet}/{walletId}",
                    arguments = listOf(
                        navArgument("budgetId") { type = NavType.LongType },
                        navArgument("walletId") { type = NavType.LongType }
                    )
                ) { backStackEntry ->
                    val budgetId = backStackEntry.arguments?.getLong("budgetId") ?: return@composable
                    val walletId = backStackEntry.arguments?.getLong("walletId") ?: return@composable
                    LaunchedEffect(budgetId) {
                        onOpenBudget(budgetId)
                    }
                    val detail = uiState.selectedBudgetDetail
                    val wallet = detail?.wallets?.firstOrNull { it.id == walletId }
                    if (detail?.budget?.id == budgetId && wallet != null) {
                        WalletDetailRouteScreen(
                            detail = detail,
                            wallet = wallet,
                            privacyModeEnabled = uiState.privacyModeEnabled,
                            onTogglePrivacyMode = onTogglePrivacyMode,
                            onBack = { navController.popBackStack() },
                            onSaveWallet = onSaveWallet,
                            onArchiveWallet = onArchiveWallet,
                            onMoveWallet = onMoveWallet,
                            onAddExpense = onAddExpense,
                            onAddTransfer = onAddTransfer,
                            onAddAdjustment = onAddAdjustment,
                            onUpdateTransaction = onUpdateTransaction,
                            onDeleteTransaction = onDeleteTransaction
                        )
                    } else {
                        LoadingState()
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletDetailRouteScreen(
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
private fun BudgetsScreen(
    budgets: List<BudgetSummary>,
    onOpenBudget: (Long) -> Unit,
    privacyModeEnabled: Boolean,
    onCreateBudget: (BudgetDraft) -> Unit,
    onDuplicateBudget: (Long, String) -> Unit
) {
    var showCreateOptions by rememberSaveable { mutableStateOf(false) }
    var showBlankBudgetDialog by rememberSaveable { mutableStateOf(false) }
    var templateBudgetId by rememberSaveable { mutableStateOf<Long?>(null) }

    val totalBalance = budgets.sumOf { it.totalBalance }
    val activeWallets = budgets.sumOf { it.activeWalletCount }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TotalSummarySection(
                    totalBalance = totalBalance,
                    activeWallets = activeWallets,
                    privacyModeEnabled = privacyModeEnabled
                )
            }

            if (budgets.isEmpty()) {
                item {
                    EmptyState(
                        title = "Start your financial journey",
                        subtitle = "Create your first budget sheet to organize your spending and savings."
                    )
                }
            } else {
                items(budgets, key = { it.id }) { budget ->
                    BudgetSummaryCard(
                        budget = budget,
                        privacyModeEnabled = privacyModeEnabled,
                        onOpen = { onOpenBudget(budget.id) },
                        onDuplicate = { templateBudgetId = budget.id }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateOptions = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Budget")
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
}

@Composable
private fun TotalSummarySection(totalBalance: Long, activeWallets: Int, privacyModeEnabled: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        shape = MaterialTheme.shapes.extraLarge
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
                color = MaterialTheme.colorScheme.primary
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
    budget: BudgetSummary,
    privacyModeEnabled: Boolean,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                IconButton(onClick = onDuplicate) {
                    Icon(
                        Icons.Default.SettingsApplications,
                        contentDescription = "Templates",
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
            
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BudgetDetailScreen(
    detail: BudgetDetail,
    privacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit,
    onBack: () -> Unit,
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
    val canAddExpense = activeWallets.isNotEmpty()
    val canAddTransfer = activeWallets.size > 1
    val canAddAdjustment = activeWallets.isNotEmpty()
    var inlineSpendWalletId by rememberSaveable(detail.budget.id) { mutableStateOf<Long?>(null) }
    var showArchived by rememberSaveable(detail.budget.id) { mutableStateOf(false) }
    var showWalletDialog by rememberSaveable { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<WalletSummary?>(null) }
    var expenseDraft by remember(detail.budget.id) { mutableStateOf<ExpenseDraft?>(null) }
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
        canAddExpense = canAddExpense,
        canAddTransfer = canAddTransfer,
        canAddAdjustment = canAddAdjustment,
        showArchived = showArchived,
        onBack = onBack,
        onToggleArchived = { showArchived = it },
        onOpenSettings = { showBudgetSettings = true },
        onQuickExpense = { expenseDraft = ExpenseDraft(walletId = activeWallets.firstOrNull()?.id) },
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
        onSpendFromWallet = { expenseDraft = ExpenseDraft(walletId = it.id) },
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

    expenseDraft?.let { initialDraft ->
        ExpenseDialog(
            wallets = activeWallets,
            initial = initialDraft,
            onDismiss = { expenseDraft = null },
            onSave = {
                onAddExpense(detail.budget.id, it)
                expenseDraft = null
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
    canAddExpense: Boolean,
    canAddTransfer: Boolean,
    canAddAdjustment: Boolean,
    showArchived: Boolean,
    onBack: () -> Unit,
    onToggleArchived: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onQuickExpense: () -> Unit,
    onQuickTransfer: () -> Unit,
    onQuickAdjustment: () -> Unit,
    onAddWalletRequest: () -> Unit,
    inlineSpendWalletId: Long?,
    onInlineSpendToggle: (Long) -> Unit,
    onInlineSpendSave: (WalletSummary, String) -> Unit,
    onOpenWallet: (WalletSummary) -> Unit,
    onSpendFromWallet: (WalletSummary) -> Unit,
    onEditWallet: (WalletSummary) -> Unit,
    onArchiveWallet: (WalletSummary) -> Unit,
    onMoveWalletUp: (WalletSummary) -> Unit,
    onMoveWalletDown: (WalletSummary) -> Unit,
    onEditTransaction: (TransactionSummary) -> Unit,
    onDeleteTransaction: (TransactionSummary) -> Unit
) {
    val recentTransactions = detail.transactions.take(5)

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
                BudgetActionStrip(
                    canAddExpense = canAddExpense,
                    canAddTransfer = canAddTransfer,
                    canAddAdjustment = canAddAdjustment,
                    onQuickExpense = onQuickExpense,
                    onQuickTransfer = onQuickTransfer,
                    onQuickAdjustment = onQuickAdjustment
                )
            }

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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Archived", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = showArchived,
                            onCheckedChange = onToggleArchived,
                            modifier = Modifier.scale(0.8f)
                        )
                        FilledTonalButton(onClick = onAddWalletRequest) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                            Text("Add")
                        }
                    }
                }
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
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
    canAddExpense: Boolean,
    canAddTransfer: Boolean,
    canAddAdjustment: Boolean,
    onQuickExpense: () -> Unit,
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
                Button(onClick = onQuickExpense, enabled = canAddExpense, modifier = Modifier.weight(1f)) {
                    Text("Spend")
                }
                OutlinedButton(onClick = onQuickTransfer, enabled = canAddTransfer, modifier = Modifier.weight(1f)) {
                    Text("Move")
                }
                OutlinedButton(onClick = onQuickAdjustment, enabled = canAddAdjustment, modifier = Modifier.weight(1f)) {
                    Text("Adjust")
                }
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
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
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasTemplates) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (hasTemplates) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                    )
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
                Text("Spend")
            }
            OutlinedButton(onClick = onTransfer, enabled = canTransfer, modifier = Modifier.weight(1f)) {
                Text("Move")
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
            .padding(horizontal = 16.dp)
            .clickable(onClick = onOpen),
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
                    when {
                        wallet.archived -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            AssistChip(onClick = onArchiveToggle, label = { Text("Hidden") })
                        }

                        isOverspent -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            AssistChip(
                                onClick = {},
                                label = { Text("Over budget") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    labelColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                    }
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
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (isOverspent) Overspend else Success,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onSpend, modifier = Modifier.weight(1f)) {
                    Text(if (isInlineSpendExpanded) "Close" else "Spend")
                }
                OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
                    Text("Details")
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
                        text = "Quick spend",
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
            containerColor = when (transaction.type) {
                TransactionType.EXPENSE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
                TransactionType.TRANSFER -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                TransactionType.ADJUSTMENT -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = accentColor.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title(focusWalletId),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = transaction.subtitle(focusWalletId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                transaction.note?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = amountColor
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PrivacyToggleButton(
    privacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit
) {
    IconButton(onClick = onTogglePrivacyMode) {
        Icon(
            imageVector = if (privacyModeEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (privacyModeEnabled) "Show amounts" else "Hide amounts"
        )
    }
}

@Composable
private fun DateInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    allowClear: Boolean = true
) {
    var showPicker by remember { mutableStateOf(false) }
    val selectedDateMillis = remember(value) { value.toPickerMillis() }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                placeholder = { Text("Select date") },
                trailingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showPicker = true }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { onValueChange(LocalDate.now().toString()) },
                label = { Text("Today") }
            )
            if (allowClear && value.isNotBlank()) {
                AssistChip(
                    onClick = { onValueChange("") },
                    label = { Text("Clear") }
                )
            }
        }
    }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
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
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun BudgetDialog(
    title: String,
    initial: BudgetDraft = BudgetDraft(),
    saveLabel: String = "Create",
    onDismiss: () -> Unit,
    onSave: (BudgetDraft) -> Unit
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
                    label = { Text("Budget name") },
                    modifier = Modifier.fillMaxWidth()
                )
                DateInputField(
                    value = draft.startDate,
                    onValueChange = { draft = draft.copy(startDate = it) },
                    label = "Start date"
                )
                DateInputField(
                    value = draft.endDate,
                    onValueChange = { draft = draft.copy(endDate = it) },
                    label = "End date",
                    allowClear = true
                )
                EnumDropdown(
                    label = "Negative balance rule",
                    options = NegativeBalanceRule.entries,
                    selected = draft.negativeBalanceRule,
                    optionLabel = { option ->
                        option.name.lowercase().replaceFirstChar { char -> char.titlecase() }
                    },
                    onSelected = { draft = draft.copy(negativeBalanceRule = it) }
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

@Composable
private fun ExpenseDialog(
    wallets: List<WalletSummary>,
    initial: ExpenseDraft = ExpenseDraft(),
    onDismiss: () -> Unit,
    onSave: (ExpenseDraft) -> Unit
) {
    var draft by remember(initial, wallets) {
        mutableStateOf(
            initial.copy(walletId = initial.walletId ?: wallets.firstOrNull()?.id)
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    modifier = Modifier.fillMaxWidth()
                )
                DateInputField(
                    value = draft.date,
                    onValueChange = { draft = draft.copy(date = it) },
                    label = "Date"
                )
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) },
                    label = { Text("Optional note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move Money") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    modifier = Modifier.fillMaxWidth()
                )
                DateInputField(
                    value = draft.date,
                    onValueChange = { draft = draft.copy(date = it) },
                    label = "Date"
                )
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) },
                    label = { Text("Optional note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AdjustmentDialog(
    wallets: List<WalletSummary>,
    initial: AdjustmentDraft = AdjustmentDraft(),
    onDismiss: () -> Unit,
    onSave: (AdjustmentDraft) -> Unit
) {
    var draft by remember(initial, wallets) {
        mutableStateOf(
            initial.copy(walletId = initial.walletId ?: wallets.firstOrNull()?.id)
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Adjustment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    modifier = Modifier.fillMaxWidth()
                )
                DateInputField(
                    value = draft.date,
                    onValueChange = { draft = draft.copy(date = it) },
                    label = "Date"
                )
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) },
                    label = { Text("Optional note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
                DateInputField(
                    value = draft.date,
                    onValueChange = { draft = draft.copy(date = it) },
                    label = "Date"
                )
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) },
                    label = { Text("Optional note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
        title = { Text("Confirm Delete") },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
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
private fun WalletDropdown(
    label: String,
    wallets: List<WalletSummary>,
    selectedWalletId: Long?,
    onSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedWallet = wallets.firstOrNull { it.id == selectedWalletId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedWallet?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = wallets.isNotEmpty()
                )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            wallets.forEach { wallet ->
                DropdownMenuItem(
                    text = { Text(wallet.name) },
                    onClick = {
                        onSelected(wallet.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletTemplateDropdown(
    label: String,
    budgets: List<BudgetSummary>,
    selectedBudgetId: Long?,
    onSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedBudget = budgets.firstOrNull { it.id == selectedBudgetId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedBudget?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = budgets.isNotEmpty()
                )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            budgets.forEach { budget ->
                DropdownMenuItem(
                    text = { Text(budget.name) },
                    onClick = {
                        onSelected(budget.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable
                )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun String.filterNumericInput(): String = filter { it.isDigit() || it == ',' }

private fun String.filterSignedNumericInput(): String {
    return buildString {
        forEachIndexed { index, char ->
            if (char.isDigit() || char == ',') append(char)
            if (char == '-' && index == 0) append(char)
        }
    }
}

private fun BudgetSummary.formatDateRange(): String {
    val start = startDateEpochDay?.let(LocalDate::ofEpochDay)?.toString()
    val end = endDateEpochDay?.let(LocalDate::ofEpochDay)?.toString()
    return when {
        start != null && end != null -> "$start to $end"
        start != null -> "Starts $start"
        else -> "No date range"
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

private fun TransactionSummary.typeLabel(): String {
    return when (type) {
        TransactionType.EXPENSE -> "Expense"
        TransactionType.TRANSFER -> "Transfer"
        TransactionType.ADJUSTMENT -> "Adjust"
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
        TransactionType.EXPENSE -> "-${formatAmount(amount)}"
        TransactionType.TRANSFER -> when {
            focusWalletId != null && sourceWalletId == focusWalletId -> "-${formatAmount(amount)}"
            focusWalletId != null && destinationWalletId == focusWalletId -> "+${formatAmount(amount)}"
            else -> formatAmount(amount)
        }
        TransactionType.ADJUSTMENT -> if (amount > 0L) "+${formatAmount(amount)}" else formatAmount(amount)
    }
}

private fun String.parseDateOrNull(): Long? {
    if (isBlank()) return null
    return runCatching { LocalDate.parse(trim()).toEpochDay() }.getOrNull()
}

private fun String.toPickerMillis(): Long? {
    return parseDateOrNull()?.let { epochDay ->
        LocalDate.ofEpochDay(epochDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}

private fun Long.toStoredDate(): String {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toString()
}

private fun maskedAmount(amount: Long, privacyModeEnabled: Boolean): String {
    if (!privacyModeEnabled) return formatAmount(amount)
    return if (amount < 0L) "-••••" else "••••"
}

private fun maskedPercent(progress: Float, privacyModeEnabled: Boolean): String {
    return if (privacyModeEnabled) "••••" else "${(progress * 100).toInt()}%"
}

private tailrec fun android.content.Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
