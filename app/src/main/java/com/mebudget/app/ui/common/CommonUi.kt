package com.mebudget.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.mebudget.app.data.BudgetSummary
import com.mebudget.app.data.NegativeBalanceRule
import com.mebudget.app.data.WalletSummary
import com.mebudget.app.data.formatAmount
import java.time.LocalDate

@Composable
fun QuickAmountChips(
    amounts: List<Long>,
    onAmountSelected: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        amounts.forEach { amount ->
            AssistChip(
                onClick = { onAmountSelected(amount) },
                label = { Text(formatAmount(amount)) }
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.padding(horizontal = 20.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(4.dp, Color.Black)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = Color.Black.copy(alpha = 0.6f)
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(actionLabel.uppercase(), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun PrivacyToggleButton(
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
fun PrivacyModeBanner(
    modifier: Modifier = Modifier,
    onTogglePrivacyMode: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Privacy mode is on",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Amounts are hidden across the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onTogglePrivacyMode != null) {
                TextButton(onClick = onTogglePrivacyMode) {
                    Text("Show")
                }
            }
        }
    }
}

@Composable
fun DateInputField(
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
fun BudgetDialog(
    title: String,
    initial: BudgetDraft = BudgetDraft(),
    saveLabel: String = "Create",
    onDismiss: () -> Unit,
    onSave: (BudgetDraft) -> Unit,
    onDelete: (() -> Unit)? = null
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
                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Budget")
                    }
                }
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
fun WalletDropdown(
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
                androidx.compose.material3.DropdownMenuItem(
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
fun WalletTemplateDropdown(
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
                androidx.compose.material3.DropdownMenuItem(
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
fun <T> EnumDropdown(
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
                androidx.compose.material3.DropdownMenuItem(
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

@Composable
fun LoadingState() {
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
