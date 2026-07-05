# Task 2: Rewrite BudgetsScreen

**Status:** DONE

**Commit hash:** 018f3e7

**Test result:** `./gradlew compileDebugKotlin` — BUILD SUCCESSFUL in 11s

**Changes made:**
- Wrapped `LazyColumn` content in `BrutalistBudgetTheme`
- Replaced header section with "■ BUDGETS ■" title block
- Replaced "New" `FilledTonalButton` with full-width black pill "CREATE BUDGET" button
- Rewrote `TotalSummarySection` with zero-elevation, black border, black/white/red brutalist aesthetic
- Rewrote `BudgetSummaryCard` with hard border, dot indicator (red/green), overspent/on-track label, uppercase name, removed delete/duplicate icons
- Rewrote `BudgetCreationChoiceDialog` inner cards with `RoundedCornerShape(0.dp)`, black 4dp border, black text
- Removed unused imports: `ContentCopy`, `Delete`, `Add`, `AccountBalanceWallet`, `rememberModalBottomSheetState`, `ModalBottomSheet`, `ExperimentalMaterial3Api`, `FilledTonalButton`, `Icon`
- Added imports: `BrutalistBudgetTheme`, `BorderStroke`, `Color`, `Canvas`, `HorizontalDivider`

**Concerns:** None.
