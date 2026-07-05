# UI Simplification Design

## Overview

Simplify the MeBudget application interface by reducing redundant entry points, hiding rarely-used features, and moving configuration out of the primary navigation.

## Changes

### 1. Consolidate Expense Entry

**Problem:** Three competing ways to add an expense — FAB on budgets list (global expense bottom sheet), inline spend on wallet cards, and expense button in wallet detail.

**Solution:** Remove the least-used paths and keep one clear primary path.

- Remove the "Quick spend" inline expand/collapse from `WalletCard` in `BudgetOverviewScreen`
- Remove the FAB ("Add expense") and its `GlobalExpenseBottomSheet` from `BudgetsScreen`
- Remove `showGlobalExpense` state and its bottom sheet from `MeBudgetNavHost`
- Keep expense entry solely in the wallet detail screen via `WalletActionStrip`'s "Add expense" button, where notes are available
- Keep "Move Money" (transfer) in both `BudgetActionStrip` (budget overview) and `WalletActionStrip` (wallet detail)

**Files affected:**
- `BudgetDetailScreens.kt` — `WalletCard`: remove inline spend expand/collapse, `onSpend`, `onInlineSpendSave`, `onInlineSpendCancel` params. Remove `inlineSpendWalletId`, `onInlineSpendToggle`, `onInlineSpendSave` from `BudgetOverviewScreen`
- `BudgetsScreen.kt` — remove FAB and `GlobalExpenseBottomSheet`
- `MeBudgetNavHost.kt` — remove `showGlobalExpense` state and bottom sheet composable
- `BudgetDraftModels.kt` — remove `GlobalExpenseBottomSheet`'s `fetchWallets` callback type (no longer used from nav level)

### 2. Hide Adjustments

**Problem:** Adjustments are a transaction type the user has never used, adding visual noise as a dedicated button in both action strips.

**Solution:** Remove the prominent "Adjust" buttons from action strips. Move the option behind a "More" dropdown on the wallet detail screen only.

- Remove "Adjust" button from `BudgetActionStrip` (budget overview screen)
- Remove "Adjust" button from `WalletActionStrip` (wallet detail screen)
- Add a three-dot "More" `IconButton` in `WalletDetailScreen`'s action strip area (or top bar) that reveals a dropdown with "Manual Adjustment" option
- Keep `onAddAdjustment`, `AdjustmentDialog`, and all domain logic intact — only UI visibility changes
- Existing `ADJUSTMENT` records in the database remain readable

**Files affected:**
- `BudgetDetailScreens.kt`:
  - `BudgetActionStrip`: remove `canAddAdjustment` param and "Adjust" button
  - `BudgetOverviewScreen`: remove `canAddAdjustment` param from callers
  - `WalletActionStrip`: remove "Adjust" button
  - `WalletDetailScreen`: add "More" dropdown with "Manual Adjustment" option
  - `BudgetDetailScreen`, `WalletDetailRouteScreen`: remove `canAddAdjustment` / `onAddAdjustment` wiring

### 3. Move Quick Spend to Settings

**Problem:** Quick Spend configuration occupies a bottom-nav slot but is a one-time settings screen, not a primary app function.

**Solution:** Remove from bottom nav. Add a gear icon on the budgets list top bar to access settings.

- Remove "Quick" tab from bottom `NavigationBar`
- Add a gear (`Icons.Default.Settings`) `IconButton` to the budgets list top bar (next to the privacy toggle)
- Tapping gear navigates to `MeBudgetRoute.quickSpendSettings`
- Rename screen title from "Quick Spend" to "Settings" in the quick spend screen
- Hide the bottom nav bar on the settings route (current behavior keeps it visible since it's a top-level route; need to handle showing/hiding appropriately)
- Keep all Quick Spend settings functionality unchanged

**Files affected:**
- `MeBudgetNavHost.kt`: remove "Quick" from bottom nav items, add gear icon to budgets list top bar, update `topLevelRoutes` set
- `QuickSpendSettingsScreen.kt`: change title from "Quick Spend" to "Settings"

### 4. Privacy Mode — No Change

The privacy mode toggle remains in every screen's top bar as-is.

## Files Summary

| File | Changes |
|---|---|
| `BudgetDetailScreens.kt` | Remove inline spend from WalletCard; remove Adjust from action strips; add More dropdown for adjustments |
| `BudgetsScreen.kt` | Remove FAB and GlobalExpenseBottomSheet |
| `BudgetDraftModels.kt` | Remove unused types/callbacks |
| `MeBudgetNavHost.kt` | Remove FAB/showGlobalExpense; remove Quick from bottom nav; add gear icon to budgets list |
| `QuickSpendSettingsScreen.kt` | Title change to "Settings" |

## Non-Goals

- No database schema or migration changes
- No domain logic changes (validation, commands, etc.)
- No changes to Insights screens
- No changes to privacy mode
- No removal of adjustment capability — only UI hiding
