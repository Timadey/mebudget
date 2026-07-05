# Brutalist Budget Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the budgets list and budget detail screens with a brutalist aesthetic — hard borders, zero elevation, black/white palette, red accent, monospace Black 900 type, block-character progress bars.

**Architecture:** Override MaterialTheme color scheme locally for these screens only (other screens keep the existing organic palette). All card widgets are refactored to use 4dp black `BorderStroke` instead of elevation/shadow. Bottom-sheet forms get bare-input styling.

**Tech Stack:** Jetpack Compose (Material3), existing Outfit font (Black 900 weight), `BorderStroke(4.dp, Color.Black)` replaces all card shadows, `drawBehind` for block-style progress bars.

## Global Constraints

- No domain/data/ViewModel layer changes — UI layer only (`app/src/main/java/com/mebudget/app/ui/`)
- All changes must compile with `./gradlew compileDebugKotlin`
- Existing Outfit font remains; only weight changes (Black 900 for everything)
- Keep all existing functionality accessible
- `GradientProgressBar` in `Gradients.kt` stays for Insights screens (unchanged by this plan)

---
## File Structure

| File | Responsibility |
|------|---------------|
| `ui/theme/Color.kt` | Add `BrutalistLightColors` color scheme |
| `ui/theme/Theme.kt` | Add `BrutalistBudgetTheme` composable wrapper |
| `ui/feature/budgets/BudgetsScreen.kt` | Rewrite budgets list (header, summary, cards, FAB) |
| `ui/feature/budgetdetail/BudgetDetailScreens.kt` | Rewrite overview, wallet detail, wallet card, transaction card, forms |
| `ui/common/CommonUi.kt` | Update `EmptyState` shape/border for brutalist |

### Task 1: Theme — Add Brutalist color scheme and wrapper

**Files:**
- Modify: `ui/theme/Color.kt` — append `BrutalistLightColors`
- Modify: `ui/theme/Theme.kt` — add `BrutalistBudgetTheme` composable

**Interfaces:**
- Produces: `BrutalistLightColors` (`lightColorScheme` with white bg, black fg, red error)
- Produces: `@Composable fun BrutalistBudgetTheme(content: @Composable () -> Unit)` — wraps content in `CompositionLocalProvider(LocalColorScheme provides BrutalistLightColors)`

- [ ] Step 1: Add `BrutalistLightColors` to `Color.kt`

Add these imports and the color scheme:

```kotlin
val BrutalistLightColors = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color.White,
    onPrimaryContainer = Color.Black,
    secondary = Color.Black,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color.Black,
    error = Color(0xFFFF0000),
    errorContainer = Color(0xFFFFF0F0),
    onError = Color.White,
    onErrorContainer = Color(0xFFCC0000),
    outline = Color.Black,
    outlineVariant = Color.Black
)
```

Place after the existing `Overspend = Color(0xFFB24733)` line.

- [ ] Step 2: Add `BrutalistBudgetTheme` to `Theme.kt`

Add these imports at the top:
```kotlin
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalColorScheme
```

Add this function after the existing `MeBudgetTheme`:

```kotlin
@Composable
fun BrutalistBudgetTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColorScheme provides BrutalistLightColors,
        content = content
    )
}
```

Note: `LocalColorScheme` is deprecated in newer Compose but available. If compile fails, use `LocalContentColor` approach or simply pass color values directly. Alternative safe approach: skip the CompositionLocalProvider and instead reference the colors inline in each composable via `Color.Black`, `Color.White`, `Color(0xFFFF0000)`.

- [ ] Step 3: Verify it compiles

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] Step 4: Commit

```bash
git add app/src/main/java/com/mebudget/app/ui/theme/Color.kt app/src/main/java/com/mebudget/app/ui/theme/Theme.kt
git commit -m "feat: add BrutalistLightColors and BrutalistBudgetTheme wrapper"
```

---

### Task 2: Rewrite BudgetsScreen — header, summary card, budget rows

**Files:**
- Modify: `ui/feature/budgets/BudgetsScreen.kt` — full rewrite of `TotalSummarySection`, `BudgetSummaryCard`, header layout, empty state, and the "New" button
- Depends on: Task 1 (BrutalistBudgetTheme) — wrap the screen content in `BrutalistBudgetTheme`

**Interfaces:**
- Consumes: `BrutalistBudgetTheme`
- Produces: Rewritten `BudgetsScreen` composable with brutalist styling

Key visual changes:
- No `Card` elevation or tonal fills anywhere
- Every card gets `border = BorderStroke(4.dp, Color.Black)`, `elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)`, `shape = RoundedCornerShape(0.dp)`
- Header is raw text (no card): `■ BUDGETS ■` label, count + total on one line
- TotalSummarySection becomes a hard-bordered card with block progress
- BudgetSummaryCard name is uppercase `FontWeight.Black`, status dot with colored circle
- Delete/duplicate icons removed from card; only `[>]` button remains
- "New" button is a full-width pill at the bottom: black fill, white text
- `BudgetCreationChoiceDialog` gets hard 4dp border, no rounded corners
- `TemplateBudgetDialog` same treatment

- [ ] Step 1: Read the current file to verify understanding

- [ ] Step 2: Rewrite `BudgetsScreen` — wrap entire content in `BrutalistBudgetTheme`

The outer `Box` content is wrapped:
```kotlin
BrutalistBudgetTheme {
    Box(modifier = Modifier.fillMaxSize()) {
        // existing LazyColumn content
    }
}
```

- [ ] Step 3: Rewrite `TotalSummarySection`

Remove the Card elevation/rounded corners. Apply 4dp black border, 0dp elevation, 0dp shape:

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
                // Left: count (huge) + "active wallets" label
                Column {
                    // big number
                    Text(
                        text = "${activeWallets}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                    Text(
                        text = "active wallets",
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
                // Right: total balance
                Text(
                    text = maskedAmount(totalBalance, privacyModeEnabled),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
            // thin divider line
            Spacer(modifier = Modifier.height(4.dp))
            Divider(color = Color.Black, thickness = 1.dp)
            Spacer(modifier = Modifier.height(4.dp))
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

Add imports needed:
```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
```

- [ ] Step 4: Rewrite `BudgetSummaryCard`

Each budget card: hard border, uppercase name, status dot, balance, `[>]` button:

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
                // Status dot + label
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

Add the needed imports:
```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
```

- [ ] Step 5: Update header section in the LazyColumn

Replace the current header Row with:
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

Remove the subtitle text ("Open a budget for context..."). Keep the "New" FilledTonalButton changed to a text-based approach matching the spec.

- [ ] Step 6: Replace "New" button with brutalist full-width pill

Replace the FilledTonalButton create section with:
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

- [ ] Step 7: Update `EmptyState` for brutalist style (if used on budgets page)

The budgets list calls `EmptyState` which is in `CommonUi.kt`. We'll update it in Task 5. For now, ensure the budgets list empty state works.

- [ ] Step 8: Update `BudgetCreationChoiceDialog` and `TemplateBudgetDialog`

Both AlertDialogs need:
```kotlin
containerColor = Color.White,
tonalElevation = 0.dp
```

And the inner option cards:
```kotlin
shape = RoundedCornerShape(0.dp),
border = BorderStroke(4.dp, Color.Black),
elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
```

- [ ] Step 9: Verify compilation

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] Step 10: Commit

```bash
git add app/src/main/java/com/mebudget/app/ui/feature/budgets/BudgetsScreen.kt
git commit -m "feat: rewrite BudgetsScreen with brutalist styling"
```

---

### Task 3: Rewrite Budget Detail — overview screen, section switcher, wallet rows, activity tab

**Files:**
- Modify: `ui/feature/budgetdetail/BudgetDetailScreens.kt` — full visual rewrite of `BudgetOverviewScreen`, `BudgetStatusCard`, `BudgetSectionSwitcher`, `WalletCard`, transaction rows (activity tab), `BudgetActionStrip`

**Interfaces:**
- Consumes: `BrutalistBudgetTheme` wrapper (wrap the scaffold content)
- Produces: Brutalist-styled budget detail composables

- [ ] Step 1: Wrap `BudgetDetailScreen` content in `BrutalistBudgetTheme`

- [ ] Step 2: Rewrite `BudgetSectionSwitcher`

Replace FilterChip/Button/OutlinedButton with chunky toggles:
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

- [ ] Step 3: Rewrite `BudgetStatusCard`

Hard 4dp border, 0dp elevation, block progress bar:
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
            // Block-style progress bar
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

- [ ] Step 4: Create `BlockProgressBar` composable

Add this helper composable at the bottom of the file (or in a new small file — inline in the same file is fine for now):

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

Add the import:
```kotlin
import androidx.compose.ui.draw.clipToBounds
```

- [ ] Step 5: Rewrite `WalletCard`

Hard border, uppercase name, status dot, balance, progress bar, the `[>]` expand button:

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

- [ ] Step 6: Rewrite the top bar

Replace the Scaffold's `topBar` with a simpler layout. Remove `MediumTopAppBar`. Use a `Column` with a `Row` for back/title/actions:

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
                detail.budget.name.uppercase(),
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
        val privacyIcon = if (privacyModeEnabled) "[P]" else "[p]"
        TextButton(onClick = onTogglePrivacyMode) {
            Text(privacyIcon, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
        }
    }
    HorizontalDivider(color = Color.Black, thickness = 2.dp)
}
```

- [ ] Step 7: Rewrite `BudgetActionStrip` — remove the card, make it just a row of chunky buttons

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

- [ ] Step 8: Rewrite activity tab transaction rows

In the activity section, replace `TransactionCard` with bare rows. Create a `HistoryRow` composable (or inline it):

```kotlin
@Composable
private fun ActivitySection(
    transactions: List<TransactionSummary>,
    privacyModeEnabled: Boolean,
    focusWalletId: Long?,
    onEdit: (TransactionSummary) -> Unit,
    onDelete: (TransactionSummary) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = "RECENT ACTIVITY",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        if (transactions.isEmpty()) {
            EmptyState(
                title = "NO ACTIVITY",
                subtitle = "Transactions will appear here."
            )
        } else {
            transactions.forEachIndexed { index, transaction ->
                // date divider
                if (index == 0 || transaction.dateEpochDay != transactions[index - 1].dateEpochDay) {
                    Text(
                        text = "── ${LocalDate.ofEpochDay(transaction.dateEpochDay)} ──",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = Color.Black.copy(alpha = 0.4f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                // transaction row (no card, no border)
                TransactionHistoryRow(
                    transaction = transaction,
                    privacyModeEnabled = privacyModeEnabled,
                    focusWalletId = focusWalletId,
                    onEdit = { onEdit(transaction) },
                    onDelete = { onDelete(transaction) }
                )
            }
        }
    }
}
```

- [ ] Step 9: Create `TransactionHistoryRow`

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
    // transaction note if present
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

- [ ] Step 10: Verify compilation

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] Step 11: Commit

```bash
git add app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt
git commit -m "feat: rewrite BudgetDetail screens with brutalist styling"
```

---

### Task 4: Rewrite Wallet Detail — wallet detail screen, summary panel, action strip, history rows, forms

**Files:**
- Modify: `ui/feature/budgetdetail/BudgetDetailScreens.kt` — rewrite `WalletDetailScreen`, `WalletSummaryPanel`, `WalletActionStrip`, inline history rows
- Modify: same file — rewrite `ExpenseDialog`, `TransferDialog`, `AdjustmentDialog`, `TransactionEditorDialog`, `ConfirmDeleteDialog`

**Interfaces:**
- Consumes: `BrutalistBudgetTheme`, `BlockProgressBar`, `TransactionHistoryRow` (from Task 3)

- [ ] Step 1: Rewrite `WalletDetailScreen` — no app bar, chunky back button, privacy toggle as text

Replace the `Scaffold` with `Column` layout:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletDetailScreen(
    // ... same params as current
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header row
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
                WalletSummaryPanel(...)  // to be rewritten in step 2
            }
            item {
                WalletActionStrip(...)  // to be rewritten in step 3
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
                item {
                    EmptyState(title = "NO HISTORY", subtitle = "Transactions will appear here.")
                }
            } else {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        transactions.forEachIndexed { index, transaction ->
                            // Date divider
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

Note: The `WalletDetailRouteScreen` wrapper composable stays the same (it manages state/dialogs). Only `WalletDetailScreen` visual layout changes.

- [ ] Step 2: Rewrite `WalletSummaryPanel`

Giant spaced-out balance, block progress bar:

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Spaced-out characters like "$ 8 0 0 . 0 0"
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

For the spaced-out balance, use:
```kotlin
Text(
    text = maskedAmount(wallet.balance, privacyModeEnabled)
        .map { "$it " }
        .joinToString("")
        .trimEnd(),
    style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 0.sp),
    fontWeight = FontWeight.Black,
    color = Color.Black
)
```

This takes something like `$800.00` and renders it as `$ 8 0 0 . 0 0`.

- [ ] Step 3: Rewrite `WalletActionStrip`

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

- [ ] Step 4: Rewrite dialog forms — `ExpenseDialog`, `TransferDialog`, `AdjustmentDialog`, `TransactionEditorDialog`, `ConfirmDeleteDialog`

Key changes for all bottom sheets:
- Replace drag handle with a 4dp black bar at top
- Replace OutlinedTextField with bottom-border style (1dp black line under label + value)
- Title becomes all-caps 24sp Black
- Save button is full-width black pill with white text
- Cancel is plain text link

Example for the expense dialog header and input style:

```kotlin
// Replace the bottom sheet drag handle:
Column {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color.Black)
    )
    // ... rest of content
}

// Replace OutlinedTextField with bottom-border style:
Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
        text = "AMOUNT",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = Color.Black.copy(alpha = 0.6f)
    )
    TextField(
        value = draft.amount,
        onValueChange = { draft = draft.copy(amount = it.filterNumericInput()) },
        textStyle = MaterialTheme.typography.headlineMedium.copy(
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

Replace the Save button:
```kotlin
Button(
    onClick = { onSave(draft) },
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

Replace the Cancel button:
```kotlin
TextButton(
    onClick = onDismiss,
    modifier = Modifier.fillMaxWidth()
) {
    Text(
        "cancel",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Black,
        color = Color.Black.copy(alpha = 0.5f)
    )
}
```

For `ConfirmDeleteDialog`:
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

Note: `AlertDialog` doesn't support `border` directly. The 4dp black border for AlertDialogs can be approximated by wrapping a `Card` inside, or skipped if too complex. The `containerColor = Color.White, tonalElevation = 0.dp` gives a flat white dialog.

- [ ] Step 5: Verify compilation

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] Step 6: Commit

```bash
git add app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt
git commit -m "feat: rewrite WalletDetail and forms with brutalist styling"
```

---

### Task 5: Update CommonUi.kt — EmptyState, PrivacyModeBanner, and shared dialogs

**Files:**
- Modify: `ui/common/CommonUi.kt` — update `EmptyState` to use brutalist card style (hard border, 0dp elevation)

**Interfaces:**
- Consumes: `BrutalistBudgetTheme` (indirect — shared components used inside themed wrappers)
- Produces: Brutalist-styled `EmptyState` composable

- [ ] Step 1: Update `EmptyState`

```kotlin
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
```

Add the needed imports:
```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
```

Note: `Color.White` and `Color.Black` are from `androidx.compose.ui.graphics.Color` which is already imported via the file's existing `import` of Material3.

- [ ] Step 2: Verify compilation

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] Step 3: Commit

```bash
git add app/src/main/java/com/mebudget/app/ui/common/CommonUi.kt
git commit -m "feat: update EmptyState with brutalist styling"
```

---

### Task 6: Cleanup — remove unused imports and old references

**Files:**
- Modify: `ui/feature/budgets/BudgetsScreen.kt` — remove unused imports (e.g., `Icons.Default.ContentCopy`, `Icons.Default.Delete`, `Icons.Default.Add`, rememberModalBottomSheetState, ModalBottomSheet)
- Modify: `ui/feature/budgetdetail/BudgetDetailScreens.kt` — remove unused imports
- Check: Ensure `GradientProgressBar` is only referenced in `InsightsScreens.kt` and `InsightsUi.kt` (it should be — those screens aren't redesigned here)

- [ ] Step 1: Run the build to check for import warnings

```bash
./gradlew compileDebugKotlin 2>&1 | grep -E "warn|unused|Unused"
```

- [ ] Step 2: Fix any unused imports found

- [ ] Step 3: Verify final build

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] Step 4: Commit

```bash
git commit -am "chore: remove unused imports after brutalist refactor"
```

---
## Spec Coverage

| Spec Requirement | Task(s) |
|---|---|
| BrutalistLightColors + BrutalistBudgetTheme | Task 1 |
| Budgets list header (■ BUDGETS ■, count, total) | Task 2 |
| TotalSummarySection (hard border, block progress) | Task 2 |
| BudgetSummaryCard (hard border, status dot, [>]) | Task 2 |
| EmptyState (hard border) | Task 5 |
| Budget detail top bar ([<], [S], [P]/[p]) | Task 3 |
| Section switcher (chunky black borders, instant swap) | Task 3 |
| BudgetStatusCard (hard border, block progress) | Task 3 |
| WalletCard (hard border, block progress, status dot) | Task 3 |
| Activity tab (bare rows, date dividers, [EXP]/[TRF]/[ADJ]) | Task 3 |
| BudgetActionStrip (chunky MOVE MONEY) | Task 3 |
| Wallet detail (giant spaced balance, block progress) | Task 4 |
| WalletActionStrip (chunky ADD EXPENSE / MOVE MONEY) | Task 4 |
| History rows (bare TransactionHistoryRow) | Task 4 |
| Expense/Transfer/Adjustment bottom sheets (brutalist inputs, save) | Task 4 |
| AlertDialogs (flat white, hard border where possible) | Task 4 |
| Import cleanup | Task 6 |
