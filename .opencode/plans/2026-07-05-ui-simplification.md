# UI Simplification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Simplify the MeBudget interface by consolidating expense entry, hiding adjustments behind a More menu, and moving Quick Spend to a settings gear.

**Architecture:** Pure UI changes — no database or domain logic modifications. Files affected are composable screens, the NavHost, and one draft models file.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Navigation Compose

## Global Constraints

- No database schema or migration changes
- No domain logic changes (validation, commands, etc.)
- Keep all existing functionality accessible (just moved/hidden)

---

### Task 1: Remove inline spend from wallet cards

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt`

**Interfaces:**
- Consumes: `WalletCard`, `BudgetOverviewScreen`, `BudgetDetailScreen`, `WalletDetailRouteScreen` current signatures
- Produces: Modified signatures with inline spend params removed

- [ ] **Step 1: Remove inline spend from `WalletCard`**

Remove these parameters from `WalletCard` composable:
- `isInlineSpendExpanded: Boolean`
- `onSpend: () -> Unit`
- `onInlineSpendSave: (String) -> Unit`
- `onInlineSpendCancel: () -> Unit`

Remove the entire `if (isInlineSpendExpanded)` block (the inline spend form at lines ~1038-1093) including the `LaunchedEffect(isInlineSpendExpanded)` block.

Remove the "Quick spend" `FilledTonalButton` from the button row.

- [ ] **Step 2: Remove inline spend props from `BudgetOverviewScreen`**

Remove these parameters:
- `inlineSpendWalletId: Long?`
- `onInlineSpendToggle: (Long) -> Unit`
- `onInlineSpendSave: (WalletSummary, String) -> Unit`
- `onInlineSpendCancel: () -> Unit`

In the `WalletCard` call inside `BudgetOverviewScreen`, remove the corresponding arguments.

Remove the `var inlineSpendWalletId` and its logic from the section above the wallet items loop.

Simplify the `WalletCard` call in the items block to only pass remaining params.

- [ ] **Step 3: Remove inline spend props from `BudgetDetailScreen`**

Remove this state variable:
```kotlin
var inlineSpendWalletId by rememberSaveable(detail.budget.id) { mutableStateOf<Long?>(null) }
```

Remove these passed args from the `BudgetOverviewScreen` call:
```kotlin
inlineSpendWalletId = inlineSpendWalletId,
onInlineSpendToggle = { walletId ->
    inlineSpendWalletId = if (inlineSpendWalletId == walletId) null else walletId
},
onInlineSpendSave = { wallet, amount ->
    onAddExpense(
        detail.budget.id,
        ExpenseDraft(walletId = wallet.id, amount = amount)
    )
    inlineSpendWalletId = null
},
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: Remove global expense FAB and bottom sheet

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgets/BudgetsScreen.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/navigation/MeBudgetNavHost.kt`

**Interfaces:**
- Consumes: `BudgetsScreen`, `MeBudgetNavHost` current signatures
- Produces: Simpler nav host without global expense

- [ ] **Step 1: Remove FAB and unused params from `BudgetsScreen`**

Remove `onAddGlobalExpense: () -> Unit` parameter from `BudgetsScreen` signature.

Remove the entire `Column` with `ExtendedFloatingActionButton` at `Modifier.align(Alignment.BottomEnd)`.

Remove the `GlobalExpenseBottomSheet` composable (the whole function) — it's a private function defined in this file.

Remove `import com.mebudget.app.data.WalletSummary` if it's no longer used after removing the bottom sheet.

- [ ] **Step 2: Remove global expense from NavHost**

Remove these from `MeBudgetNavHost`:
```kotlin
var showGlobalExpense by rememberSaveable { mutableStateOf(false) }
```
and the entire bottom sheet block:
```kotlin
if (showGlobalExpense) {
    GlobalExpenseBottomSheet(...)
}
```

Remove `onAddGlobalExpense = { showGlobalExpense = true }` from the `BudgetsScreen` call.

Remove `fetchWalletsForBudget` parameter from `MeBudgetNavHost` if it's no longer used.

- [ ] **Step 3: Remove unused fetchWalletsForBudget wiring**

In `MeBudgetApp.kt`, remove `fetchWalletsForBudget = budgetsViewModel::fetchWalletsForBudget` from the `MeBudgetNavHost` call.

- [ ] **Step 4: Build and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: Remove Adjust buttons, add More dropdown

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt`

**Interfaces:**
- Consumes: `BudgetActionStrip`, `WalletActionStrip`, `WalletDetailScreen`, `BudgetOverviewScreen`, `BudgetDetailScreen`, `WalletDetailRouteScreen`
- Produces: Simplified action strips, More dropdown in wallet detail

- [ ] **Step 1: Remove Adjust from `BudgetActionStrip`**

Remove `canAddAdjustment: Boolean` parameter.
Remove `onQuickAdjustment: () -> Unit` parameter.
Remove the "Adjust" button:
```kotlin
FilledTonalButton(onClick = onQuickAdjustment, enabled = canAddAdjustment, modifier = Modifier.weight(1f)) {
    Text("Adjust")
}
```

- [ ] **Step 2: Remove Adjust from `BudgetOverviewScreen`**

Remove `canAddAdjustment: Boolean` and `onQuickAdjustment: () -> Unit` parameters.
Remove these from the `BudgetActionStrip` call.

- [ ] **Step 3: Remove Adjust from `BudgetDetailScreen`**

Remove:
```kotlin
val canAddAdjustment = activeWallets.isNotEmpty()
```
and:
```kotlin
var showAdjustmentDialog by rememberSaveable { mutableStateOf(false) }
```
and the "Quick Adjustment" state:
```kotlin
onQuickAdjustment = { showAdjustmentDialog = true },
```
from the `BudgetOverviewScreen` call.

Remove the `showAdjustmentDialog` section:
```kotlin
if (showAdjustmentDialog) {
    AdjustmentDialog(...)
}
```

- [ ] **Step 4: Remove Adjust from `WalletDetailRouteScreen`**

Remove `onAddAdjustment` parameter from `WalletDetailRouteScreen`.
Remove `var adjustmentDraft by remember...` state.
Remove the `adjustmentDraft?.let { ... }` dialog block.
Remove the `onAdjust = { adjustmentDraft = AdjustmentDraft(walletId = wallet.id) }` from the `WalletDetailScreen` call.

- [ ] **Step 5: Remove Adjust from `WalletActionStrip`**

Remove `onAdjust: () -> Unit` parameter.
Remove the "Adjust" button:
```kotlin
OutlinedButton(onClick = onAdjust, modifier = Modifier.weight(1f)) {
    Text("Adjust")
}
```

- [ ] **Step 6: Add More dropdown to `WalletDetailScreen`**

In `WalletDetailScreen`, add an `Icons.Default.MoreVert` `IconButton` at the end of the `WalletActionStrip` button row. When tapped, show a `DropdownMenu` with one item: "Manual Adjustment" that calls `onAdjust`.

The action strip should look like:
```
[Add expense] [Move money] [⋮]
```

Where `⋮` opens dropdown containing "Manual Adjustment".

- [ ] **Step 7: Wire adjustment call in `WalletDetailRouteScreen`**

Since `onAddAdjustment` was removed from `WalletDetailRouteScreen`, the `onAdjust` callback in `WalletDetailScreen` needs to trigger the adjustment flow. Add a new `var adjustmentDraft` state variable back in `WalletDetailRouteScreen` that opens `AdjustmentDialog` when set.

The flow: long-press More → "Manual Adjustment" → sets `adjustmentDraft` → triggers `AdjustmentDialog` → user fills and saves → `onAddAdjustment` is called.

- [ ] **Step 8: Clean up NavHost wiring**

In `MeBudgetNavHost.kt`, remove `onAddAdjustment` from the `WalletDetailRouteScreen` call if it's still passed.

Wait — `onAddAdjustment` is still needed by `BudgetDetailScreen` (for the budget overview's transfer dialog). Only remove it from `WalletDetailRouteScreen`.

In `MeBudgetApp.kt`, make sure `onAddAdjustment` is still wired — it's still used by `BudgetDetailScreen` for the transfer dialog and by the new hidden adjustment path in wallet detail.

- [ ] **Step 9: Build and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 4: Move Quick Spend to settings gear

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/navigation/MeBudgetNavHost.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/quickspend/QuickSpendSettingsScreen.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/app/MeBudgetApp.kt` (if wiring changes needed)
- Possibly: `app/src/main/java/com/mebudget/app/ui/navigation/MeBudgetNavigation.kt` (if route changes)

**Interfaces:**
- Consumes: `MeBudgetNavHost`, `QuickSpendSettingsScreen` current signatures
- Produces: 2-tab bottom nav, settings via gear icon

- [ ] **Step 1: Update `QuickSpendSettingsScreen` title**

Change the title from "Quick Spend" to "Settings" in the top bar.

- [ ] **Step 2: Remove Quick tab from bottom nav**

In `MeBudgetNavHost.kt`:
- Remove the `NavigationBarItem` for Quick Spend from the `NavigationBar`
- Remove `MeBudgetRoute.quickSpendSettings` from `topLevelRoutes` set (so bottom nav hides when on settings screen)

- [ ] **Step 3: Add gear icon to budgets list top bar**

In `MeBudgetNavHost.kt`, in the `if (currentRoute == MeBudgetRoute.budgets)` top bar section, add a gear `IconButton` before the privacy toggle:
```kotlin
IconButton(onClick = {
    navController.navigate(MeBudgetRoute.quickSpendSettings)
}) {
    Icon(Icons.Default.Settings, contentDescription = "Settings")
}
```

Note: Need `import androidx.compose.material.icons.filled.Settings` if not already present.

- [ ] **Step 4: Build and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 5: Remove unused imports and dead code

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgets/BudgetsScreen.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/navigation/MeBudgetNavHost.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/app/MeBudgetApp.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/BudgetDetailViewModel.kt` — remove `addAdjustment` if no longer called from anywhere

**Interfaces:**
- Consumes: All modified files from Tasks 1-4
- Produces: Clean, compilable code

- [ ] **Step 1: Remove unused imports in each file**

Open each modified file and remove any now-unused imports (the compiler will flag them as warnings):
- `FilterChip` — might be unused if inline spend removed
- `WalletSummary` — might be unused in some files
- `Icons.AutoMirrored.Filled.ArrowBack` — still used
- `Icons.Default.SwapHoriz` — still used by transfers
- `Icons.Default.History` — still used by transactions
- `Icons.Default.Edit` — still used

- [ ] **Step 2: Clean up ViewModel**

If `addAdjustment` is no longer called from `BudgetDetailViewModel`, check `BudgetDetailViewModel.kt` — it's still called from `BudgetDetailScreen` (for the budget overview's "Adjust" action). So it should stay.

Check `WalletDetailRouteScreen` — if it no longer passes `onAddAdjustment`, make sure the ViewModel method is still wired from `BudgetDetailScreen`.

- [ ] **Step 3: Final build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL with no errors

---

## Verification

After all tasks:
1. `./gradlew compileDebugKotlin` — must pass
2. Manual check: open a budget → wallet detail → only [Add expense] [Move money] buttons visible, no inline spend on wallet cards
3. Manual check: wallet detail → More ⋮ → "Manual Adjustment" appears in dropdown
4. Manual check: budgets list → gear icon in top bar → opens Settings screen (no Quick tab in bottom nav)
5. Manual check: budgets list → no FAB
