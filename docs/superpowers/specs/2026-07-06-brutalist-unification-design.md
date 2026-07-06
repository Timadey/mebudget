# Brutalist Unification Design

**Date:** 2026-07-06
**Status:** Approved
**Goal:** Convert all remaining screens (4 insight screens, 5 shared insight components, 7 common UI components) to the brutalist style for complete visual coherence.

## Problem

The app has a dual aesthetic:
- **Brutalist** — budget screens (BudgetsScreen, BudgetOverviewScreen, WalletDetailScreen): black/white palette, `RoundedCornerShape(0.dp)`, 4dp black borders, `BlockProgressBar`, 0 elevation
- **Original Material** — insight screens (BudgetInsightsScreen, GlobalInsightsScreen, WalletHistoryDetailScreen, TransferPatternDetailScreen): rounded corners (8-16dp), elevation shadows, gradient progress bars, nature-inspired container colors

Common UI components (CommonUi.kt) are also split — `EmptyState` is partially brutalist, while `PrivacyModeBanner`, `BudgetDialog`, `DateInputField`, `WalletDropdown`, `WalletTemplateDropdown`, and `EnumDropdown` remain original Material.

## Scope

All screens and all common components. No domain/data/ViewModel changes.

## Files Modified

| File | Changes |
|------|---------|
| `Gradients.kt` | Extract `BlockProgressBar` from `BudgetDetailScreens.kt` with added `color: Color` parameter. Remove `GradientProgressBar` (dead code after conversion). |
| `InsightsScreens.kt` | Convert 4 screens + all sub-composables to brutalist |
| `InsightsUi.kt` | Convert 5 shared insight components to brutalist |
| `CommonUi.kt` | Convert 7 components to brutalist; remove `QuickAmountChips` + `WalletDropdown` (dead code) |
| `BudgetsScreen.kt` | Wrap `BudgetDialog` + `TemplateBudgetDialog` calls in `BrutalistBudgetTheme` |
| `BudgetDetailScreens.kt` | Remove private `BlockProgressBar` (moved to Gradients.kt); remove unused color constants |

## Detailed Design

### A. BlockProgressBar Extraction (Gradients.kt)

```kotlin
@Composable
fun BlockProgressBar(
    progress: Float,
    color: Color = MaterialTheme.colorScheme.primary,
    segments: Int = 12
)
```

- Same 12-segment block rendering as current implementation in BudgetDetailScreens
- `color` parameter lets insight callers pass red/green/yellow for health signaling
- Imported by both budget detail screens and insight screens

### B. Insights Screens (InsightsScreens.kt)

**Header replacement** — Each of the 4 screens currently uses:
```kotlin
Scaffold(topBar = MediumTopAppBar(...))
```

Replace with brutalist header pattern matching `BudgetOverviewScreen`:
```kotlin
Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    TextButton(onClick = onBack) { Text("[<]", Black 900, 18.sp) }
    Column(Modifier.weight(1f)) {
        Text(title, titleLarge, Black 900)
        Text(subtitle, bodySmall, onSurface.copy(0.6f))
    }
    TextButton(onClick = onTogglePrivacy) { Text("[P]", Black 900) }
}
```

**Card conversion** — Every `Card` in these screens:
- `RoundedCornerShape(x.dp)` → `RoundedCornerShape(0.dp)`
- `elevation = 2.dp` (or any) → `elevation = 0.dp`
- Add `border = BorderStroke(4.dp, MaterialTheme.colorScheme.outline)`
- Background stays `MaterialTheme.colorScheme.surface`

**SignalBadge** — Replace `AssistChip` with:
```kotlin
Surface(
    shape = RoundedCornerShape(0.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    color = when(signal) {
        Signal.Good -> Color(0xFF00AA00)
        Signal.Warning -> Color(0xFFFF8800)
        Signal.Danger -> Color(0xFFFF0000)
    }
) {
    Text(signal.label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
}
```

**Progress bars** — All `GradientProgressBar` calls replaced with `BlockProgressBar`:
- Green `0xFF00AA00` for healthy/on-track
- Orange `0xFFFF8800` for warning/low
- Red `0xFFFF0000` for overspent/danger

### C. Shared Insight Components (InsightsUi.kt)

| Component | Change |
|-----------|--------|
| `BudgetInsightSection` | Card corners 16→0, elevation 2→0, add 4dp black border |
| `InsightDetailCard` | Same card conversion; `content` slot unchanged |
| `InsightMetricCard` | Card corners 8→0, `surfaceVariant` bg → `surface` bg + border |
| `ObservationList` | Each observation `Card(8.dp)` → `Card(0.dp)` + 1dp black border |
| `TransferPathRow` | No visual change (text-only) |

### D. Common Components (CommonUi.kt)

| Component | Current | Brutalist |
|-----------|---------|-----------|
| `EmptyState` | Pill button, hardcoded colors | `RoundedCornerShape(0.dp)`, theme tokens |
| `PrivacyModeBanner` | `Card(16.dp)`, `secondaryContainer` | `Card(0.dp)` + 4dp border, `surface` bg |
| `BudgetDialog` | `OutlinedTextField`, rounded | `BrutalistTextField`, `RoundedCornerShape(0.dp)` |
| `DateInputField` | `OutlinedTextField`, rounded chips | `BrutalistTextField`, sharp black-border chips |
| `WalletTemplateDropdown` | `OutlinedTextField` dropdown | Sharp corners, black border |
| `EnumDropdown` | Same | Same conversion |
| `QuickAmountChips` | Dead code | **Remove** |
| `WalletDropdown` | Dead code (never called) | **Remove** |

### E. BudgetsScreen Theme Wraps

- `BudgetDialog` call (line 166): wrap in `BrutalistBudgetTheme { }`
- `TemplateBudgetDialog` (line 437): wrap `AlertDialog` content in `BrutalistBudgetTheme { }`

### F. BudgetDetailScreens Cleanup

- Remove private `BlockProgressBar`
- Remove unused color constants: `Color(0xFF00AA00)`, `Color(0xFFCC0000)` (if not used elsewhere)

## Build Order

1. **Gradients.kt** — Extract `BlockProgressBar`, add `color` param, remove `GradientProgressBar`
2. **BudgetDetailScreens.kt** — Remove private `BlockProgressBar`, unused constants
3. **InsightsUi.kt** — Convert 5 components
4. **InsightsScreens.kt** — Convert 4 screens + sub-composables
5. **CommonUi.kt** — Convert 7 components, remove dead code
6. **BudgetsScreen.kt** — Wrap dialog calls in `BrutalistBudgetTheme`
7. `./gradlew compileDebugKotlin` + `./gradlew test` — verify

## What Stays Unchanged

- `MeBudgetTheme` (root app wrapper in MainActivity)
- Navigation, ViewModels, data layer
- Existing brutalist budget screens
- Typography (Outfit Black 900)
- Private brutalist helpers in BudgetDetailScreens (BrutalistTextField, BrutalistWalletDropdown, BrutalistDateField)

## Risk Mitigation

- **Theme leakage**: Dialogs outside `BrutalistBudgetTheme` get wrapped explicitly in `BudgetsScreen.kt`
- **System DatePickerDialog**: Keeps its Material style (ephemeral, acceptable)
- **Dead code removal**: Verified zero call sites via grep — safe to remove
