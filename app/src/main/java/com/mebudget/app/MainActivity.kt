package com.mebudget.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebudget.app.ui.AppViewModel
import com.mebudget.app.ui.AppViewModelFactory
import com.mebudget.app.ui.BudgetDetailViewModel
import com.mebudget.app.ui.BudgetDetailViewModelFactory
import com.mebudget.app.ui.BudgetsViewModel
import com.mebudget.app.ui.BudgetsViewModelFactory
import com.mebudget.app.ui.MeBudgetApp
import com.mebudget.app.ui.theme.MeBudgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeBudgetTheme {
                val appViewModel: AppViewModel = viewModel(
                    factory = AppViewModelFactory(application)
                )
                val budgetsViewModel: BudgetsViewModel = viewModel(
                    factory = BudgetsViewModelFactory(application)
                )
                val budgetDetailViewModel: BudgetDetailViewModel = viewModel(
                    factory = BudgetDetailViewModelFactory(application)
                )
                MeBudgetApp(
                    appViewModel = appViewModel,
                    budgetsViewModel = budgetsViewModel,
                    budgetDetailViewModel = budgetDetailViewModel
                )
            }
        }
    }
}
