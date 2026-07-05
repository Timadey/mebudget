# Task 2: Rewrite BudgetsScreen

## Context

Part of a brutalist budget redesign. This task rewrites the budgets list screen with a hard-bordered, zero-elevation, black/white/red aesthetic. Task 1 added `BrutalistBudgetTheme` — use it here.

## Files

- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgets/BudgetsScreen.kt`

## Current file to read first

Read `app/src/main/java/com/mebudget/app/ui/feature/budgets/BudgetsScreen.kt` before editing.

## Changes

### 1. Wrap the entire LazyColumn content in BrutalistBudgetTheme

Replace the content of the outer `Box(modifier = Modifier.fillMaxSize())` with:

```kotlin
BrutalistBudgetTheme {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ... items stay the same structure but with new composables
    }
}
```

Add the import: `import com.mebudget.app.ui.theme.BrutalistBudgetTheme`

### 2. Replace the header section

Replace the current `item { Row(...) { "Start here" / "Budgets" } }` section with:

```kotlin
item {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = "■ BUDGETS ■",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            color = Color.Black
        )
    }
}
```

### 3. Replace the "New" FilledTonalButton

Replace the FilledTonalButton section with a full-width black pill:

```kotlin
item {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { showCreateOptions = true },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text(
                "+ CREATE BUDGET",
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}
```

### 4. Rewrite TotalSummarySection

```kotlin
@Composable
private fun TotalSummarySection(totalBalance: Long, activeWallets: Int, privacyModeEnabled: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(4.dp, Color.Black)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "${activeWallets}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                    Text(
                        text = "active wallets",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
                Text(
                    text = maskedAmount(totalBalance, privacyModeEnabled),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
            HorizontalDivider(color = Color.Black, thickness = 1.dp)
            Text(
                text = "ACROSS ALL BUDGETS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color.Black
            )
        }
    }
}
```

### 5. Rewrite BudgetSummaryCard

```kotlin
@Composable
private fun BudgetSummaryCard(
    modifier: Modifier = Modifier,
    budget: BudgetSummary,
    privacyModeEnabled: Boolean,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
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
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.name.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                    Text(
                        text = budget.formatDateRange(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                }
                val isOverspent = budget.totalBalance < 0
                val dotColor = if (isOverspent) Color(0xFFFF0000) else Color(0xFF00AA00)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(dotColor)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOverspent) "OVERSENT" else "ON TRACK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = dotColor
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${budget.activeWalletCount} active",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = maskedAmount(budget.totalBalance, privacyModeEnabled),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }
    }
}
```

### 6. Update BudgetCreationChoiceDialog

Replace the inner option Cards with brutalist style:

```kotlin
Card(
    modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onCreateBlank),
    shape = RoundedCornerShape(0.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border = BorderStroke(4.dp, Color.Black)
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("START BLANK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.Black)
        Text(
            "Create a fresh budget and add wallets yourself.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Black,
            color = Color.Black.copy(alpha = 0.6f)
        )
    }
}
```

And similarly for the template option card. Also set `containerColor = Color.White` and `tonalElevation = 0.dp` on the outer AlertDialog.

### 7. Also remove unused imports

Remove imports that are no longer used:
- `Icons.Default.ContentCopy`
- `Icons.Default.Delete`
- `Icons.Default.Add` (if no longer used)
- `rememberModalBottomSheetState` (if used nowhere else)
- `ModalBottomSheet` (if used nowhere else)
- `Icons.Default.AccountBalanceWallet` (if no longer used in this file)

Keep: `BorderStroke`, `Color`, `Canvas`, `HorizontalDivider` — these are newly needed.

### 8. Compilation

Run: `./gradlew compileDebugKotlin` — must pass. Fix any issues and re-run until green.
