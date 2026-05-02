package com.mebudget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mebudget.app.data.BudgetEntity
import com.mebudget.app.ui.navigation.MeBudgetRoute

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MeBudgetNavHost(
    budgetsUiState: BudgetsUiState,
    budgetDetailUiState: BudgetDetailUiState,
    privacyModeEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    onTogglePrivacyMode: () -> Unit,
    onConsumePendingBudgetNavigation: () -> Unit,
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
    onDeleteTransaction: (Long) -> Unit,
    fetchWalletsForBudget: (Long) -> kotlinx.coroutines.flow.Flow<List<com.mebudget.app.data.WalletEntity>>
) {
    var showGlobalExpense by rememberSaveable { mutableStateOf(false) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val topLevelRoutes = setOf(MeBudgetRoute.budgets, MeBudgetRoute.globalInsights)

    LaunchedEffect(budgetsUiState.pendingBudgetIdToOpen, currentRoute) {
        val selectedBudgetId = budgetsUiState.pendingBudgetIdToOpen ?: return@LaunchedEffect
        if (currentRoute == MeBudgetRoute.budgets) {
            onOpenBudget(selectedBudgetId)
            navController.navigate(MeBudgetRoute.budget(selectedBudgetId))
            onConsumePendingBudgetNavigation()
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
                            privacyModeEnabled = privacyModeEnabled,
                            onTogglePrivacyMode = onTogglePrivacyMode
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == MeBudgetRoute.budgets,
                        onClick = {
                            navController.navigate(MeBudgetRoute.budgets) {
                                popUpTo(MeBudgetRoute.budgets) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Budgets") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == MeBudgetRoute.globalInsights,
                        onClick = {
                            navController.navigate(MeBudgetRoute.globalInsights) {
                                popUpTo(MeBudgetRoute.budgets) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                        label = { Text("Insights") }
                    )
                }
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
                        budgets = budgetsUiState.budgets,
                        onOpenBudget = { budgetId ->
                            onOpenBudget(budgetId)
                            navController.navigate(MeBudgetRoute.budget(budgetId))
                        },
                        privacyModeEnabled = privacyModeEnabled,
                        onCreateBudget = onCreateBudget,
                        onDuplicateBudget = onDuplicateBudget,
                        onAddGlobalExpense = { showGlobalExpense = true }
                    )
                }

                composable(MeBudgetRoute.globalInsights) {
                    GlobalInsightsScreen(
                        insights = budgetsUiState.globalInsights,
                        privacyModeEnabled = privacyModeEnabled,
                        onTogglePrivacyMode = onTogglePrivacyMode,
                        onOpenWalletInsight = { walletKey ->
                            navController.navigate(MeBudgetRoute.globalWallet(walletKey))
                        },
                        onOpenTransferInsight = { sourceKey, destinationKey ->
                            navController.navigate(MeBudgetRoute.globalTransfer(sourceKey, destinationKey))
                        },
                        showBack = false,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "${MeBudgetRoute.globalInsights}/${MeBudgetRoute.globalWallet}/{walletKey}",
                    arguments = listOf(navArgument("walletKey") { type = NavType.StringType })
                ) { backStackEntry ->
                    val walletKey = backStackEntry.arguments?.getString("walletKey") ?: return@composable
                    val walletInsight = budgetsUiState.globalInsights?.walletPatterns?.firstOrNull { it.walletKey == walletKey }
                    if (walletInsight != null) {
                        WalletHistoryDetailScreen(
                            insight = walletInsight,
                            privacyModeEnabled = privacyModeEnabled,
                            onTogglePrivacyMode = onTogglePrivacyMode,
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        LoadingState()
                    }
                }

                composable(
                    route = "${MeBudgetRoute.globalInsights}/${MeBudgetRoute.globalTransfer}/{sourceKey}/{destinationKey}",
                    arguments = listOf(
                        navArgument("sourceKey") { type = NavType.StringType },
                        navArgument("destinationKey") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val sourceKey = backStackEntry.arguments?.getString("sourceKey") ?: return@composable
                    val destinationKey = backStackEntry.arguments?.getString("destinationKey") ?: return@composable
                    val transferInsight = budgetsUiState.globalInsights?.transferPatterns?.firstOrNull {
                        it.sourceWalletKey == sourceKey && it.destinationWalletKey == destinationKey
                    }
                    if (transferInsight != null) {
                        TransferPatternDetailScreen(
                            insight = transferInsight,
                            privacyModeEnabled = privacyModeEnabled,
                            onTogglePrivacyMode = onTogglePrivacyMode,
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        LoadingState()
                    }
                }

                composable(
                    route = "${MeBudgetRoute.budget}/{budgetId}",
                    arguments = listOf(navArgument("budgetId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val budgetId = backStackEntry.arguments?.getLong("budgetId") ?: return@composable
                    LaunchedEffect(budgetId) {
                        onOpenBudget(budgetId)
                    }
                    val detail = budgetDetailUiState.selectedBudgetDetail
                    if (detail?.budget?.id == budgetId) {
                        BudgetDetailScreen(
                            detail = detail,
                            privacyModeEnabled = privacyModeEnabled,
                            onTogglePrivacyMode = onTogglePrivacyMode,
                            onBack = {
                                onCloseBudget()
                                navController.popBackStack()
                            },
                            onOpenInsights = {
                                navController.navigate(MeBudgetRoute.budgetInsights(budgetId))
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
                    route = "${MeBudgetRoute.budget}/{budgetId}/${MeBudgetRoute.insights}",
                    arguments = listOf(navArgument("budgetId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val budgetId = backStackEntry.arguments?.getLong("budgetId") ?: return@composable
                    LaunchedEffect(budgetId) {
                        onOpenBudget(budgetId)
                    }
                    val detail = budgetDetailUiState.selectedBudgetDetail
                    if (detail?.budget?.id == budgetId) {
                        BudgetInsightsScreen(
                            detail = detail,
                            privacyModeEnabled = privacyModeEnabled,
                            onTogglePrivacyMode = onTogglePrivacyMode,
                            onBack = { navController.popBackStack() }
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
                    val detail = budgetDetailUiState.selectedBudgetDetail
                    val wallet = detail?.wallets?.firstOrNull { it.id == walletId }
                    if (detail?.budget?.id == budgetId && wallet != null) {
                        WalletDetailRouteScreen(
                            detail = detail,
                            wallet = wallet,
                            privacyModeEnabled = privacyModeEnabled,
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

    if (showGlobalExpense) {
        GlobalExpenseBottomSheet(
            budgets = budgetsUiState.budgets,
            fetchWallets = fetchWalletsForBudget,
            onDismiss = { showGlobalExpense = false },
            onSave = { budgetId, draft ->
                onAddExpense(budgetId, draft)
                showGlobalExpense = false
            }
        )
    }
}
