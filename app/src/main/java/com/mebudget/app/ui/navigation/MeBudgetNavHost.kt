package com.mebudget.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mebudget.app.data.BudgetEntity
import com.mebudget.app.billing.FeatureGate
import com.mebudget.app.data.sync.LimitsConfigManager
import com.mebudget.app.ui.auth.SignInScreen
import com.mebudget.app.ui.auth.SignInViewModel
import com.mebudget.app.ui.auth.SignInViewModelFactory
import com.mebudget.app.ui.auth.SignUpScreen
import com.mebudget.app.ui.auth.authManager
import com.mebudget.app.ui.profile.ProfileScreen
import com.mebudget.app.ui.profile.ProfileViewModel
import com.mebudget.app.ui.profile.ProfileViewModelFactory
import com.mebudget.app.data.sync.syncDependencies
import com.mebudget.app.ui.subscription.SubscriptionScreen
import com.mebudget.app.ui.subscription.SubscriptionViewModel
import com.mebudget.app.ui.subscription.SubscriptionViewModelFactory
import com.mebudget.app.ui.subscription.paystackManager
import com.mebudget.app.ui.sync.MergeDialog
import com.mebudget.app.ui.sync.MergeViewModel
import com.mebudget.app.ui.sync.MergeViewModelFactory
import com.mebudget.app.ui.navigation.MeBudgetRoute
import com.mebudget.app.ui.theme.AccentBlue

/** PocketBase user JWTs are short-lived; renew well before they expire. */
private const val TOKEN_REFRESH_INTERVAL_MS = 45 * 60 * 1000L

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MeBudgetNavHost(
    budgetsUiState: BudgetsUiState,
    budgetDetailUiState: BudgetDetailUiState,
    quickSpendUiState: QuickSpendUiState,
    privacyModeEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    onTogglePrivacyMode: () -> Unit,
    onQuickSpendRefresh: () -> Unit,
    onQuickSpendToggleEnabled: (Boolean) -> Unit,
    onQuickSpendSelectBudget: (Long?) -> Unit,
    onQuickSpendToggleApp: (String) -> Unit,
    quickSpendOverlaySettingsIntent: () -> Intent,
    quickSpendUsageSettingsIntent: () -> Intent,
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
    onAddCredit: (Long, CreditDraft) -> Unit,
    onUpdateTransaction: (TransactionEditorState) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onDeleteBudget: (Long) -> Unit,
    onDeleteWallet: (Long) -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val topLevelRoutes = setOf(
        MeBudgetRoute.budgets,
        MeBudgetRoute.globalInsights
    )

    val syncDeps = remember { context.applicationContext.syncDependencies() }

    // Session + realtime lifecycle: restore the persisted session once, then
    // react to sign-in/out by starting/stopping the live stream and renewing
    // the PocketBase auth token while signed in. Keying on the auth state means
    // the refresh loop is cancelled automatically on sign-out.
    LaunchedEffect(Unit) { syncDeps.authManager.restoreSession() }
    val authState by syncDeps.authManager.authState.collectAsState()
    LaunchedEffect(authState) {
        if (authState.isSignedIn) {
            syncDeps.subscriptionManager.refresh()
            syncDeps.syncEngine.startRealtimeUpdates()
            while (true) {
                kotlinx.coroutines.delay(TOKEN_REFRESH_INTERVAL_MS)
                syncDeps.authManager.refreshAuth()
            }
        } else {
            syncDeps.syncEngine.stopRealtimeUpdates()
        }
    }

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
                        IconButton(onClick = {
                            navController.navigate(MeBudgetRoute.quickSpendSettings)
                        }) {
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
        },
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    NavigationBarItem(
                        selected = currentRoute == MeBudgetRoute.budgets,
                        onClick = {
                            navController.navigate(MeBudgetRoute.budgets) {
                                popUpTo(MeBudgetRoute.budgets) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Budgets",
                                modifier = Modifier.size(28.dp),
                                tint = if (currentRoute == MeBudgetRoute.budgets) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        },
                        label = {
                            Text(
                                text = "Budgets",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (currentRoute == MeBudgetRoute.budgets) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
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
                        icon = {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Insights",
                                modifier = Modifier.size(28.dp),
                                tint = if (currentRoute == MeBudgetRoute.globalInsights) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        },
                        label = {
                            Text(
                                text = "Insights",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (currentRoute == MeBudgetRoute.globalInsights) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
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
                    val syncDeps = context.applicationContext.syncDependencies()
                    val syncState by syncDeps.syncEngine.syncState.collectAsState()
                    val scope = rememberCoroutineScope()
                    val limitsConfigManager = remember { LimitsConfigManager(syncDeps.client) }
                    val limits by limitsConfigManager.limits.collectAsState()
                    val gate = remember(limits) {
                        FeatureGate(
                            isSignedIn = { syncDeps.authManager.authState.value.isSignedIn },
                            isPro = { syncDeps.subscriptionManager.isPro.value },
                            limits = limits
                        )
                    }
                    LaunchedEffect(Unit) {
                        limitsConfigManager.refreshLimits()
                        syncDeps.subscriptionManager.refresh()
                    }
                    BudgetsScreen(
                        budgets = budgetsUiState.budgets,
                        onOpenBudget = { budgetId ->
                            onOpenBudget(budgetId)
                            navController.navigate(MeBudgetRoute.budget(budgetId))
                        },
                        privacyModeEnabled = privacyModeEnabled,
                        onCreateBudget = onCreateBudget,
                        onDuplicateBudget = onDuplicateBudget,
                        onDeleteBudget = onDeleteBudget,
                        syncState = syncState,
                        onSyncRetry = {
                            scope.launch { syncDeps.syncEngine.syncNow() }
                        },
                        canCreateBudget = gate.canCreateBudget(budgetsUiState.budgets.size),
                        onUpgradeClick = {
                            navController.navigate(MeBudgetRoute.subscription)
                        }
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

                composable(MeBudgetRoute.quickSpendSettings) {
                    QuickSpendSettingsScreen(
                        state = quickSpendUiState,
                        onBack = { navController.popBackStack() },
                        onRefresh = onQuickSpendRefresh,
                        onToggleEnabled = onQuickSpendToggleEnabled,
                        onSelectBudget = onQuickSpendSelectBudget,
                        onToggleApp = onQuickSpendToggleApp,
                        onOpenOverlaySettings = {
                            context.startActivity(quickSpendOverlaySettingsIntent())
                        },
                        onOpenUsageSettings = {
                            context.startActivity(quickSpendUsageSettingsIntent())
                        }
                    )
                }

                composable(MeBudgetRoute.signIn) {
                    val signInViewModel: SignInViewModel = viewModel(
                        factory = SignInViewModelFactory(
                            context.applicationContext.authManager()
                        )
                    )
                    SignInScreen(
                        viewModel = signInViewModel,
                        onSignInSuccess = { navController.popBackStack() },
                        onSignUpClick = {
                            navController.navigate(MeBudgetRoute.signUp)
                        },
                        onContinueWithoutSignIn = { navController.popBackStack() }
                    )
                }

                composable(MeBudgetRoute.signUp) {
                    val signUpViewModel: SignInViewModel = viewModel(
                        factory = SignInViewModelFactory(
                            context.applicationContext.authManager()
                        )
                    )
                    SignUpScreen(
                        viewModel = signUpViewModel,
                        onSignUpSuccess = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(MeBudgetRoute.subscription) {
                    val subscriptionViewModel: SubscriptionViewModel = viewModel(
                        factory = SubscriptionViewModelFactory(
                            context.applicationContext.paystackManager()
                        )
                    )
                    val scope = rememberCoroutineScope()
                    SubscriptionScreen(
                        viewModel = subscriptionViewModel,
                        onSubscribeSuccess = {
                            scope.launch { syncDeps.subscriptionManager.refresh() }
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(MeBudgetRoute.syncMerge) {
                    val mergeViewModel: MergeViewModel = viewModel(
                        factory = MergeViewModelFactory(
                            context.applicationContext.syncDependencies().syncEngine
                        )
                    )
                    MergeDialog(
                        viewModel = mergeViewModel,
                        onDismiss = { navController.popBackStack() }
                    )
                }

                composable(MeBudgetRoute.profile) {
                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModelFactory(
                            authManager = context.applicationContext.authManager(),
                            isPro = { syncDeps.subscriptionManager.isPro.value }
                        )
                    )
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onSignInClick = { navController.navigate(MeBudgetRoute.signIn) },
                        onSubscriptionClick = { navController.navigate(MeBudgetRoute.subscription) }
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
                            onAddCredit = onAddCredit,
                            onUpdateTransaction = onUpdateTransaction,
                            onDeleteTransaction = onDeleteTransaction,
                            onDeleteBudget = { budgetId ->
                                onDeleteBudget(budgetId)
                                onCloseBudget()
                                navController.popBackStack()
                            },
                            onDeleteWallet = onDeleteWallet
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
                            onAddCredit = onAddCredit,
                            onUpdateTransaction = onUpdateTransaction,
                            onDeleteTransaction = onDeleteTransaction,
                            onDeleteWallet = onDeleteWallet
                        )
                    } else {
                        LoadingState()
                    }
                }
            }
        }
    }

}
