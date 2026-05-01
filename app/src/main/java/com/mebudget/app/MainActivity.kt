package com.mebudget.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mebudget.app.ui.MeBudgetApp
import com.mebudget.app.ui.MeBudgetViewModel
import com.mebudget.app.ui.MeBudgetViewModelFactory
import com.mebudget.app.ui.theme.MeBudgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeBudgetTheme {
                val viewModel: MeBudgetViewModel = viewModel(
                    factory = MeBudgetViewModelFactory(application)
                )
                MeBudgetApp(viewModel = viewModel)
            }
        }
    }
}

