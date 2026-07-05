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
    var appSearchQuery by remember { mutableStateOf("") }
    val selectedBudget = state.budgets.firstOrNull { it.id == state.settings.selectedBudgetId }
    val selectedPackages = state.settings.selectedAppPackages
    val hasBudget = state.settings.selectedBudgetId != null
    val hasApps = state.settings.selectedAppPackages.isNotEmpty()
    val nextStep = when {
        !hasBudget -> "Choose the budget where quick spends should be recorded."
        !state.overlayPermissionGranted -> "Allow the floating button permission."
        !state.usageAccessGranted -> "Allow app detection permission."
        !hasApps -> "Select at least one bank or payment app."
        !state.settings.enabled -> "Turn on Quick Spend."
        else -> "Ready over selected apps."
    }
    val filteredApps = remember(state.launchableApps, selectedPackages, appSearchQuery) {
        val query = appSearchQuery.trim().lowercase()
        state.launchableApps
            .filter { app ->
                query.isEmpty() ||
                    app.label.lowercase().contains(query) ||
                    app.packageName.lowercase().contains(query)
            }
            .sortedWith(
                compareByDescending<com.mebudget.app.quickspend.LaunchableApp> {
                    selectedPackages.contains(it.packageName)
                }.thenBy { it.label.lowercase() }
            )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Settings", style = MaterialTheme.typography.headlineSmall)
                    Text("Record expenses while using your bank or payment app, so your budget balance stays accurate.")
                    Text(
                        "Manual entry only. MeBudget does not read your bank screen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (state.setupComplete) "Ready over selected apps" else nextStep,
                        style = MaterialTheme.typography.titleMedium
                    )
                    ChecklistRow("Choose quick-spend budget", hasBudget)
                    ChecklistRow("Allow floating button", state.overlayPermissionGranted)
                    ChecklistRow("Allow app detection", state.usageAccessGranted)
                    ChecklistRow("Select bank/payment apps", hasApps)
                    ChecklistRow("Enable Quick Spend", state.settings.enabled)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("What you gain", style = MaterialTheme.typography.titleMedium)
                    Text("Record before or after payment.")
                    Text("Avoid balance mismatch between your bank and budget.")
                    Text("Works only on apps you choose.")
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Quick Spend", style = MaterialTheme.typography.titleMedium)
                        Text("Show a small MeBudget button over selected apps.")
                    }
                    Switch(
                        checked = state.settings.enabled,
                        onCheckedChange = onToggleEnabled
                    )
                }
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bank/payment apps", style = MaterialTheme.typography.titleMedium)
                    Text("${selectedPackages.size} selected. The floating button only appears over apps you choose.")
                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        label = { Text("Search apps") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            items(filteredApps, key = { it.packageName }) { app ->
                val selected = selectedPackages.contains(app.packageName)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawableIconImage(
                        drawable = app.icon,
                        contentDescription = null
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.bodyLarge)
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
private fun ChecklistRow(
    label: String,
    complete: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (complete) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null
        )
        Text(label)
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
