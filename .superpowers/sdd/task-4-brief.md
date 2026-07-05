# Task 4: Rewrite Wallet Detail and Forms

## Context

Part of a brutalist budget redesign. Task 3 rewrote the budget overview section of `BudgetDetailScreens.kt`. This task rewrites the wallet detail section and dialog forms in the same file.

## Files

- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt`

## Composables to rewrite

Read the current file first. The following composables need brutalist styling:

### 1. WalletDetailScreen

Replace the `Scaffold` with a `Column`. No app bar. Keep all the same parameters and dialog trigger logic.

```kotlin
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
    onDeleteTransactionRequest: (TransactionSummary) -> Unit,
    onDeleteWallet: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("[<]", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
            }
            Text(
                text = wallet.name.uppercase(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
            Box {
                var expanded by remember { mutableStateOf(false) }
                TextButton(onClick = { expanded = true }) {
                    Text("[⋮]", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = Color.White,
                    border = BorderStroke(3.dp, Color.Black)
                ) {
                    DropdownMenuItem(
                        text = { Text("EDIT", fontWeight = FontWeight.Black, color = Color.Black) },
                        onClick = { onEditWallet(); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("DELETE", fontWeight = FontWeight.Black, color = Color(0xFFFF0000)) },
                        onClick = { onDeleteWallet(); expanded = false }
                    )
                }
            }
            TextButton(onClick = onTogglePrivacyMode) {
                val icon = if (privacyModeEnabled) "[P]" else "[p]"
                Text(icon, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
            }
        }
        HorizontalDivider(color = Color.Black, thickness = 2.dp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                    text = "HISTORY",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
            if (transactions.isEmpty()) {
                item { EmptyState(title = "NO HISTORY", subtitle = "Transactions will appear here.") }
            } else {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        transactions.forEachIndexed { index, transaction ->
                            if (index == 0 || transaction.dateEpochDay != transactions[index - 1].dateEpochDay) {
                                Text(
                                    text = "── ${LocalDate.ofEpochDay(transaction.dateEpochDay)} ──",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            TransactionHistoryRow(
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
    }
}
```

### 2. WalletSummaryPanel

Giant spaced-out balance with block progress bar:

```kotlin
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(4.dp, Color.Black)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spaced-out balance: "$ 8 0 0 . 0 0"
            Text(
                text = maskedAmount(wallet.balance, privacyModeEnabled)
                    .map { "$it " }
                    .joinToString("")
                    .trimEnd(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "planned ${maskedAmount(wallet.plannedAmount, privacyModeEnabled)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.Black.copy(alpha = 0.6f)
                )
                Text(
                    text = maskedPercent(progress, privacyModeEnabled),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
            BlockProgressBar(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
        }
    }
}
```

### 3. WalletActionStrip

Remove the Card wrapper — just a Row of chunky buttons:

```kotlin
@Composable
private fun WalletActionStrip(
    canTransfer: Boolean,
    onSpend: () -> Unit,
    onTransfer: () -> Unit,
    onAdjust: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onSpend,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
        ) {
            Text("ADD EXPENSE", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        OutlinedButton(
            onClick = onTransfer,
            enabled = canTransfer,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(3.dp, Color.Black),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
        ) {
            Text("MOVE MONEY", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        Box {
            var expanded by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { expanded = true },
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(3.dp, Color.Black),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("[...]", fontWeight = FontWeight.Black, color = Color.Black)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = Color.White,
                border = BorderStroke(3.dp, Color.Black)
            ) {
                DropdownMenuItem(
                    text = { Text("MANUAL ADJUSTMENT", fontWeight = FontWeight.Black) },
                    onClick = { onAdjust(); expanded = false }
                )
            }
        }
    }
}
```

### 4. Dialog Forms — ExpenseDialog, TransferDialog, AdjustmentDialog

Each bottom sheet dialog follows this structure:

```
4dp black bar at top (replaces drag handle)
Title ("ADD EXPENSE", "MOVE MONEY") in 24sp Black
Bottom-border inputs (1dp black line under label + value)
Full-width black pill "SAVE" button
Plain "cancel" text link

WalletDropdown — replace OutlinedTextField with bottom-border style
Amount input — 28sp Black
Date input — bottom-border style
Note input — bottom-border style, optional expandable
```

For the bottom-border input style, use:
```kotlin
Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
        text = "AMOUNT",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = Color.Black.copy(alpha = 0.6f)
    )
    TextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Black,
            color = Color.Black
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Black,
            unfocusedIndicatorColor = Color.Black,
            cursorColor = Color.Black
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
```

For the save button:
```kotlin
Button(
    onClick = onSave,
    modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
    shape = RoundedCornerShape(24.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = Color.Black,
        contentColor = Color.White
    )
) {
    Text("SAVE", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
}
```

For the cancel link:
```kotlin
TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
    Text(
        "cancel",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Black,
        color = Color.Black.copy(alpha = 0.5f)
    )
}
```

For the 4dp black bar at top:
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(4.dp)
        .background(Color.Black)
)
```

Replace the WalletDropdown's OutlinedTextField with the bottom-border style. Replace the modal sheet's drag handle with the black bar.

### 5. TransactionEditorDialog

Update to brutalist styling: hard black border on AlertDialog, all text in Black weight, red accent for negative amounts, black for positive.

### 6. ConfirmDeleteDialog

```kotlin
AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color.White,
    tonalElevation = 0.dp,
    title = {
        Text("DELETE?", fontWeight = FontWeight.Black, color = Color.Black)
    },
    text = {
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = Color.Black)
    },
    confirmButton = {
        TextButton(onClick = onConfirm) {
            Text("DELETE", fontWeight = FontWeight.Black, color = Color(0xFFFF0000))
        }
    },
    dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("CANCEL", fontWeight = FontWeight.Black, color = Color.Black)
        }
    }
)
```

### 7. Remove old TransactionCard composable

Now that both the Activity tab (Task 3) and WalletDetail history (this task) use `TransactionHistoryRow` instead of `TransactionCard`, the old `TransactionCard` composable can be removed entirely.

### 8. Remove unused imports

After removing `TransactionCard`, remove imports only used by it:
- `AssistChip`, `AssistChipDefaults`
- `Icons.Default.History`, `Icons.Default.SwapHoriz`, `Icons.Default.Settings`
- `Icons.Default.Edit`, `Icons.Default.MoreVert`
- `Success`, `Rust`, `Warning` from theme (check if still used by IntelliSense warnings)

## Compilation

Run: `./gradlew compileDebugKotlin` (timeout 300s). Fix issues until green.
