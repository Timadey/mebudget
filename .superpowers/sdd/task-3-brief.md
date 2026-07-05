# Task 3: Rewrite Budget Detail Overview

## Context

Part of a brutalist budget redesign. This task rewrites the budget detail screen (the overview showing budget status, wallet list, section switcher, activity tab). The same file also contains `WalletDetailScreen` — leave that for Task 4.

## Files

- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt`

## Composable rules for ALL cards

Every Card in this task should follow these rules:
```
shape = RoundedCornerShape(0.dp),
colors = CardDefaults.cardColors(containerColor = Color.White),
elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
border = BorderStroke(4.dp, Color.Black)
```

## Composables to rewrite in this file

Read the file first to identify the exact functions.

### 1. Top bar (inside BudgetOverviewScreen)

Replace the `Scaffold` with `Column`. No app bar — use a raw Row:

```kotlin
Column {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text("[<]", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = detail.budget.name.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
            Text(
                detail.budget.formatDateRange(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Black,
                color = Color.Black.copy(alpha = 0.6f)
            )
        }
        TextButton(onClick = onOpenSettings) {
            Text("[S]", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
        }
        TextButton(onClick = onTogglePrivacyMode) {
            val icon = if (privacyModeEnabled) "[P]" else "[p]"
            Text(icon, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
        }
    }
    HorizontalDivider(color = Color.Black, thickness = 2.dp)
}
```

### 2. BudgetSectionSwitcher

```kotlin
@Composable
private fun BudgetSectionSwitcher(
    selectedSection: BudgetOverviewSection,
    onSectionSelected: (BudgetOverviewSection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BudgetOverviewSection.entries.forEach { section ->
            val isSelected = section == selectedSection
            Button(
                onClick = { onSectionSelected(section) },
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color.Black else Color.White,
                    contentColor = if (isSelected) Color.White else Color.Black
                ),
                border = BorderStroke(4.dp, Color.Black),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    section.label.uppercase(),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
```

### 3. BudgetStatusCard

```kotlin
@Composable
private fun BudgetStatusCard(detail: BudgetDetail, privacyModeEnabled: Boolean) {
    val totalPlanned = detail.wallets.sumOf { it.plannedAmount }
    val currentBalance = detail.wallets.sumOf { it.balance }
    val progress = if (totalPlanned > 0) (currentBalance.toFloat() / totalPlanned.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(4.dp, Color.Black)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "REMAINING",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Color.Black
                    )
                    Text(
                        maskedAmount(currentBalance, privacyModeEnabled),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "planned ${maskedAmount(totalPlanned, privacyModeEnabled)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                    Text(
                        maskedPercent(progress, privacyModeEnabled),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
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

### 4. BlockProgressBar (NEW composable)

Add this at the bottom of the file (before the private helper functions):

```kotlin
@Composable
private fun BlockProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    filledColor: Color = Color.Black,
    trackColor: Color = Color.Black.copy(alpha = 0.15f)
) {
    val segments = 12
    val filledSegments = (progress * segments).toInt().coerceIn(0, segments)

    Box(
        modifier = modifier
            .clipToBounds()
            .drawBehind {
                val segmentWidth = size.width / segments
                for (i in 0 until segments) {
                    val left = segmentWidth * i
                    val color = if (i < filledSegments) filledColor else trackColor
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(left, 0f),
                        size = androidx.compose.ui.geometry.Size(
                            segmentWidth - 2.dp.toPx(),
                            size.height
                        )
                    )
                }
            }
    )
}
```

Needs imports: `import androidx.compose.ui.draw.clipToBounds`

### 5. WalletCard

```kotlin
@Composable
private fun WalletCard(
    modifier: Modifier = Modifier,
    wallet: WalletSummary,
    privacyModeEnabled: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onArchiveToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (wallet.plannedAmount > 0) (wallet.balance.toFloat() / wallet.plannedAmount.toFloat()).coerceIn(0f, 1f) else 0f
    val isOverspent = wallet.warning

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(4.dp, Color.Black)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOverspent) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(Color(0xFFFF0000))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            text = wallet.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                        Text(
                            text = "planned ${maskedAmount(wallet.plannedAmount, privacyModeEnabled)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Black,
                            color = Color.Black.copy(alpha = 0.6f)
                        )
                    }
                }
                Text(
                    text = maskedAmount(wallet.balance, privacyModeEnabled),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
            BlockProgressBar(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (progress * 100).toInt().toString() + "%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                val statusLabel = when {
                    wallet.archived -> "HIDDEN"
                    isOverspent -> "OVERSENT"
                    progress < 0.2f -> "LOW"
                    else -> "OK"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = if (isOverspent) Color(0xFFFF0000) else Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(BorderStroke(3.dp, Color.Black), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(">", fontWeight = FontWeight.Black, color = Color.Black)
                    }
                }
            }
        }
    }
}
```

### 6. BudgetActionStrip

```kotlin
@Composable
private fun BudgetActionStrip(
    canAddTransfer: Boolean,
    onQuickTransfer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onQuickTransfer,
            enabled = canAddTransfer,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
        ) {
            Text("MOVE MONEY", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}
```

### 7. Activity section — replace TransactionCard usage with bare rows

Inside the Activity section of `BudgetOverviewScreen`, replace the `TransactionCard` calls. Create a new `ActivitySection` composable or inline the logic:

```kotlin
// Replace the if (selectedSection == BudgetOverviewSection.Activity) block with:
if (selectedSection == BudgetOverviewSection.Activity) {
    item {
        Text(
            text = "RECENT ACTIVITY",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
    if (recentTransactions.isEmpty()) {
        item { EmptyState(title = "NO ACTIVITY", subtitle = "Transactions will appear here.") }
    } else {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                recentTransactions.forEachIndexed { index, transaction ->
                    if (index == 0 || transaction.dateEpochDay != recentTransactions[index - 1].dateEpochDay) {
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
                        focusWalletId = null,
                        onEdit = { onEditTransaction(transaction) },
                        onDelete = { onDeleteTransaction(transaction) }
                    )
                }
            }
        }
    }
}
```

### 8. TransactionHistoryRow (NEW composable)

```kotlin
@Composable
private fun TransactionHistoryRow(
    transaction: TransactionSummary,
    privacyModeEnabled: Boolean,
    focusWalletId: Long?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = when (transaction.type) {
        TransactionType.EXPENSE -> Color(0xFFFF0000)
        TransactionType.TRANSFER -> Color.Black
        TransactionType.ADJUSTMENT -> Color(0xFFFF8800)
    }
    val amountText = transaction.amountText(focusWalletId, privacyModeEnabled)
    val typeTag = when (transaction.type) {
        TransactionType.EXPENSE -> "[EXP]"
        TransactionType.TRANSFER -> "[TRF]"
        TransactionType.ADJUSTMENT -> "[ADJ]"
    }

    var showActions by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showActions = !showActions }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showActions) {
            TextButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Text("[EDIT]", fontWeight = FontWeight.Black, color = Color.Black)
            }
            TextButton(onClick = onDelete) {
                Text("[DEL]", fontWeight = FontWeight.Black, color = Color(0xFFFF0000))
            }
        } else {
            Text(
                text = transaction.title(focusWalletId),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = amountText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = typeTag,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Black,
                color = Color.Black.copy(alpha = 0.5f)
            )
        }
    }
    transaction.note?.takeIf { it.isNotBlank() }?.let { note ->
        if (!showActions) {
            Text(
                text = note,
                modifier = Modifier.padding(start = 0.dp, bottom = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Black,
                color = Color.Black.copy(alpha = 0.4f),
                maxLines = 1
            )
        }
    }
}
```

## Important

- The `BudgetsScreen.kt` now uses `BrutalistBudgetTheme` — this file should also use it. Wrap the content inside `Scaffold` with `BrutalistBudgetTheme { ... }`
- `WalletDetailScreen` and related composables (WalletSummaryPanel, WalletActionStrip, dialogs, TransactionCard) are in this same file but should NOT be modified — leave them for Task 4
- Add imports: `BorderStroke`, `Color`, `Canvas`, `clipToBounds`, `HorizontalDivider`
- Only remove imports that are genuinely unused after your changes. Let compilation errors guide you — add what's missing, remove what's unused. Be careful not to remove imports needed by composables you didn't modify.
- Keep `GradientProgressBar` import if it's still used in WalletSummaryPanel (Task 4's area) — leave that for Task 4

## Compilation

Run: `./gradlew compileDebugKotlin` — must pass.
