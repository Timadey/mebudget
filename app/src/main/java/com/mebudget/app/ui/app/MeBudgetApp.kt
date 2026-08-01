@file:JvmName("MeBudgetEntry")

package com.mebudget.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlin.jvm.JvmName

@Composable
fun MeBudgetApp(
    appViewModel: AppViewModel,
    budgetsViewModel: BudgetsViewModel,
    budgetDetailViewModel: BudgetDetailViewModel,
    quickSpendViewModel: QuickSpendViewModel
) {
    val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
    val budgetsUiState by budgetsViewModel.uiState.collectAsStateWithLifecycle()
    val budgetDetailUiState by budgetDetailViewModel.uiState.collectAsStateWithLifecycle()
    val quickSpendUiState by quickSpendViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    LaunchedEffect(budgetsUiState.errorMessage) {
        budgetsUiState.errorMessage?.let { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
            budgetsViewModel.clearError()
        }
    }

    LaunchedEffect(budgetDetailUiState.errorMessage) {
        budgetDetailUiState.errorMessage?.let { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
            budgetDetailViewModel.clearError()
        }
    }

    DisposableEffect(appUiState.privacyModeEnabled, view) {
        val window = view.context.findActivity()?.window
        if (appUiState.privacyModeEnabled) {
            window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { }
    }

    MeBudgetNavHost(
        budgetsUiState = budgetsUiState,
        budgetDetailUiState = budgetDetailUiState,
        quickSpendUiState = quickSpendUiState,
        privacyModeEnabled = appUiState.privacyModeEnabled,
        snackbarHostState = snackbarHostState,
        onTogglePrivacyMode = appViewModel::togglePrivacyMode,
        onQuickSpendRefresh = quickSpendViewModel::refresh,
        onQuickSpendToggleEnabled = quickSpendViewModel::setEnabled,
        onQuickSpendSelectBudget = quickSpendViewModel::selectBudget,
        onQuickSpendToggleApp = quickSpendViewModel::toggleApp,
        quickSpendOverlaySettingsIntent = quickSpendViewModel::overlaySettingsIntent,
        quickSpendUsageSettingsIntent = quickSpendViewModel::usageAccessSettingsIntent,
        onConsumePendingBudgetNavigation = budgetsViewModel::consumePendingBudgetNavigation,
        onOpenBudget = budgetDetailViewModel::openBudget,
        onCloseBudget = budgetDetailViewModel::closeBudget,
        onCreateBudget = budgetsViewModel::createBudget,
        onDuplicateBudget = budgetsViewModel::duplicateBudget,
        onUpdateBudgetSettings = budgetDetailViewModel::updateBudgetSettings,
        onAddWallet = budgetDetailViewModel::addWallet,
        onSaveWallet = budgetDetailViewModel::saveWallet,
        onArchiveWallet = budgetDetailViewModel::archiveWallet,
        onMoveWallet = budgetDetailViewModel::moveWallet,
        onAddExpense = budgetDetailViewModel::addExpense,
        onAddTransfer = budgetDetailViewModel::addTransfer,
        onAddCredit = budgetDetailViewModel::addCredit,
        onUpdateTransaction = budgetDetailViewModel::updateTransaction,
        onDeleteTransaction = budgetDetailViewModel::deleteTransaction,
        onDeleteBudget = budgetsViewModel::deleteBudget,
        onDeleteWallet = budgetDetailViewModel::deleteWallet
    )
}
