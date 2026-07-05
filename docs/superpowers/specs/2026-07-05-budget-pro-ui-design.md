# MeBudget "Budget Pro" UI Redesign

**Date:** 2026-07-05
**Status:** Approved (approach 1, gradients-only-for-progress-bars variant)

## Overview

Redesign the MeBudget Android app visual layer to feel **premium and minimal** — replacing border-based cards with elevation depth, expanding typography, adding restrained micro-interactions, and removing visual noise. The existing warm earthy palette (Pine/Moss/Sand/Canvas) is preserved.

## Foundation

### Typography (Type.kt)

| Style | Value | Weight | Notes |
|-------|-------|--------|-------|
| `headlineLarge` | **40sp** | Black 900 | `letterSpacing -0.5.sp`; used for hero amounts |
| `headlineMedium` | **34sp** | Black 900 |  |
| `titleLarge` | **28sp** | Bold 700 |  |
| `titleMedium` | **20sp** | SemiBold 600 |  |
| `titleSmall` | 15sp | SemiBold 600 | unchanged |
| `bodyMedium` | **16sp** | Normal 400 |  |
| `bodySmall` | 13sp | Normal 400 | unchanged |
| `labelMedium` | 12sp→**13sp** | Medium 500 |  |
| `labelSmall` | 11sp | Medium 500 | unchanged |

Font family remains **Outfit** (Google Font via `ui-text-google-fonts`).

### Spacing

| Context | Before | After |
|---------|--------|-------|
| Section outer padding (horizontal) | 16dp | **20dp** |
| LazyColumn vertical item spacing | 16dp | **20dp** |
| Hero card inner padding | 20dp | **24dp** |
| Section card inner padding | 16dp | **20dp** |
| Item card inner padding | 16dp | **16dp** (unchanged) |
| Column inner spacing (hero) | 12dp | **16dp** |
| Column inner spacing (items) | 8dp | **12dp** |
| Amount-label vertical gap | ~4dp | **8dp** |

### Elevation & Shapes

All `BorderStroke` card borders are removed. Cards are defined by elevation + tonal fill.

| Card Type | Elevation | Corner Radius | Container Color |
|-----------|-----------|---------------|-----------------|
| Hero / Summary (TotalSummary, BudgetStatus, WalletSummary) | **4dp** | **20dp** | `surface` |
| Section (BudgetSummary, InsightDetailCard) | **2dp** | **16dp** | `surface` |
| Item (WalletCard, TransactionCard) | **1dp** | **12dp** | `surface` |
| Sub-card (InsightMetricCard, Observation) | 0dp | `small` → **8dp** | `surfaceVariant.copy(alpha = 0.4f)` |
| Action strip | 0dp | **16dp** | `surfaceContainerLow` (Material3 token) |
| Empty state | 0dp | 16dp | `surfaceVariant.copy(alpha = 0.45f)` |

Shapes updated:
- `extraLarge`: 16dp → **20dp** (hero cards, WalletSummaryPanel)
- `large`: 12dp → **16dp** (section cards)
- `medium`: 8dp → **12dp** (WalletCard, TransactionCard)
- `small`: 4dp → **8dp** (icon surfaces, chips, status badges)

### Color

- **Palette**: unchanged (Pine, Moss, Lagoon, Sand, Canvas, Shell, Bark, Rust, Charcoal, Fog, etc.)
- **Amounts**: change from `primary` (Pine) → **`onSurface`** (Charcoal). Primary is preserved for progress bars, accent buttons, icons.
- **Progress bars**: only place gradients are used.
  - Healthy: Pine (#1F4D43) → Moss (#5E7B62)
  - Warning: Amber (#C48A1D) → Rust (#9E4E3D)
  - Overspend: Rust (#9E4E3D) → Overspend (#B24733)
- **Overspent wallet**: replace error border with a **3dp colored dot** on the left edge of the WalletCard.

### Motion & Micro-interactions

- **Card press**: `animateFloatAsState` driven by `MutableInteractionSource` — spring scale 0.97x on press, spring back on release.
- **List item entrance**: `AnimatedVisibility` with `fadeIn + slideInVertically(8dp)` staggered on first appearance.
- **Progress bar**: `animateFloatAsState` with spring animation for smooth value transitions.
- **Reduced motion**: all animations gated behind `LocalViewConfiguration.value.hasFocusEnabled` or system-level reduced motion check.

## Screen-Specific Changes

### BudgetsScreen

| Element | Change |
|---------|--------|
| TotalSummaryCard | 4dp elevation, 20dp radius, 24dp inner padding. Amount in 40sp Black `onSurface`. |
| BudgetSummaryCard | 2dp elevation, 16dp radius, no border. Name in `titleMedium` Bold. Amount in `headlineSmall` (28sp) Black, right-aligned. |
| Section header "Budgets" | Larger bottom margin, `headlineSmall` Bold. "New" button uses `FilledTonalButton`. |
| Bottom clearance | Reduce from 80dp to 72dp (nav bar is shorter now). |
| Navigation bar | Icons 28dp, no labels. Active = filled `primary`, inactive = outlined `onSurfaceVariant`. |

### BudgetDetailScreen

| Element | Change |
|---------|--------|
| BudgetStatusCard | 4dp elevation, 20dp radius, no border. Amount in `headlineLarge` (40sp) Black. Progress bar: 8dp height, gradient Pine→Moss (or Rust→Overspend). |
| Section switcher | Replace `FilterChip` row with `FilledTonalButton` toggle group (Wallets / Activity / Insights). |
| "Quick actions" | Use `surfaceContainerLow`, 16dp radius. |
| Section header "Wallets" | `headlineSmall` Bold, larger bottom margin. |
| WalletCard | 1dp elevation, 12dp radius, no border. Overspend: 3dp colored dot (Rust) on left edge instead of error border. Amount in `titleLarge` Black `onSurface`. Progress bar: 4dp height, gradient. Status chip kept. |
| WalletActionStrip | Use `surfaceContainerLow`, 16dp radius. Add expense button → `Button`, Move money → `OutlinedButton`, More → `IconButton` with `DropdownMenu`. |
| BudgetActionStrip (Move Money) | `surfaceContainerLow`, single `Button` full-width. |
| Insight preview section | `2dp` elevation, 16dp radius. Metric sub-cards → 0dp elevation, tonal fill 0.4f alpha. |

### WalletDetailScreen

| Element | Change |
|---------|--------|
| WalletSummaryPanel | 4dp elevation, 20dp radius, no border. Amount in `headlineLarge` (40sp) Black. Progress bar: 12dp height, gradient. |
| TransactionCard | 0dp elevation (flat), 12dp radius, `surfaceVariant.copy(alpha = 0.25f)` fill instead of border. Icon surface: 40dp, 8dp radius. |
| Transaction amount | `bodyLarge` (16sp) Black `onSurface`. Color coding by type preserved (expense=Rust, income=Pine, transfer=Fog). |

### Insights Screens

| Element | Change |
|---------|--------|
| Hero cards (Budget pulse / Pattern pulse) | 2dp elevation, 16dp radius, no border. Amounts in `headlineMedium` (34sp) Black. Progress bar: 10dp height, gradient. |
| InsightMetricCard | 0dp elevation, 8dp radius, `surfaceVariant.copy(alpha = 0.4f)` fill. |
| Observation cards | 0dp elevation, 8dp radius, tonal fill. |

### QuickSpendSettingsScreen

Minimal changes — title updated to "Settings" already. Keep layout as-is but update card elevation/shapes to match new system.

## Out of Scope

- No database or domain model changes
- No navigation or route changes
- No new screens or removal of existing screens
- No changes to bottom sheet content (sheets get the Material3 default styling)
- No changes to dialogs beyond tonal alignment
- No changes to privacy mode behavior
- Quick Spend overlay (separate system service) unaffected

## Implementation Plan

See `../plans/2026-07-05-budget-pro-ui-plan.md` for the step-by-step implementation plan.
