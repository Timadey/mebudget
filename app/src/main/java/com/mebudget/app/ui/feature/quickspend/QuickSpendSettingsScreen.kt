package com.mebudget.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun QuickSpendSettingsScreen(
    state: QuickSpendUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectBudget: (Long?) -> Unit,
    onToggleApp: (String) -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenUsageSettings: () -> Unit
) {
    LaunchedEffect(Unit) { onRefresh() }
    var budgetMenuExpanded by remember { mutableStateOf(false) }
    val selectedBudget = state.budgets.firstOrNull { it.id == state.settings.selectedBudgetId }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quick Spend") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Floating quick spend", style = MaterialTheme.typography.titleMedium)
                        Text("Manual expense entry over selected payment apps.")
                    }
                    Switch(
                        checked = state.settings.enabled,
                        onCheckedChange = onToggleEnabled
                    )
                }
            }

            item {
                Text(
                    text = if (state.setupComplete) {
                        "Ready over selected apps"
                    } else {
                        "Complete setup to enable the floating button"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = budgetMenuExpanded,
                    onExpandedChange = { budgetMenuExpanded = !budgetMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBudget?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Quick-spend budget") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(budgetMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = budgetMenuExpanded,
                        onDismissRequest = { budgetMenuExpanded = false }
                    ) {
                        state.budgets.forEach { budget ->
                            DropdownMenuItem(
                                text = { Text(budget.name) },
                                onClick = {
                                    onSelectBudget(budget.id)
                                    budgetMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                PermissionRow(
                    title = "Display over other apps",
                    granted = state.overlayPermissionGranted,
                    buttonLabel = "Allow Floating Button",
                    onClick = onOpenOverlaySettings
                )
            }

            item {
                PermissionRow(
                    title = "Usage Access",
                    granted = state.usageAccessGranted,
                    buttonLabel = "Allow App Detection",
                    onClick = onOpenUsageSettings
                )
            }

            item {
                Text("Selected bank/payment apps", style = MaterialTheme.typography.titleMedium)
            }

            items(state.launchableApps, key = { it.packageName }) { app ->
                val selected = state.settings.selectedAppPackages.contains(app.packageName)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.label)
                        Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onToggleApp(app.packageName) }) {
                        Icon(
                            imageVector = if (selected) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.RadioButtonUnchecked
                            },
                            contentDescription = if (selected) "Selected" else "Not selected"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(if (granted) "Allowed" else "Required")
        }
        if (!granted) {
            Button(onClick = onClick) {
                Text(buttonLabel)
            }
        }
    }
}
