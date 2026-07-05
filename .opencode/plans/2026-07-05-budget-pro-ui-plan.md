# MeBudget "Budget Pro" UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Elevate the MeBudget Android app from a functional Material3 design to a premium-minimal visual experience through typography expansion, elevation-based cards, restrained gradients, and refined spacing.

**Architecture:** Pure Compose UI layer changes — no domain, database, or navigation logic touched. Changes isolated to theme files (`Type.kt`) and screen composable files. Each task touches one bounded part of the UI (typography, spacing, cards, gradients, per-screen details, navigation).

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2026.04.01), Material3, Compose Navigation

## Global Constraints

- Do NOT modify any `.kt` files outside `app/src/main/java/com/mebudget/app/ui/` (no domain, data, or infrastructure changes)
- Do NOT change database schema, migrations, or entity classes
- Do NOT change ViewModel or business logic
- Outfit font must remain the typeface (no font library changes)
- Color palette (Color.kt, Theme.kt) must remain unchanged — only token *usage* changes in components
- Gradients only in `LinearProgressIndicator` composables — no gradient backgrounds, gradient strips on cards, or gradient icon fills
- All animations must honor system reduced-motion settings (`LocalViewConfiguration`)

---

## File Structure

### Files to Modify

| File | Path | Changes |
|------|------|---------|
| **Type.kt** | `app/src/main/java/com/mebudget/app/ui/theme/Type.kt` | Update font sizes: headlineLarge 34→40sp, headlineMedium 30→34sp, titleMedium 18→20sp, bodyMedium 15→16sp. Add letterSpacing(-0.5.sp) to headline styles. |
| **BudgetsScreen.kt** | `app/src/main/java/com/mebudget/app/ui/feature/budgets/BudgetsScreen.kt` | TotalSummaryCard: elevation 2→4dp, shape extraLarge→20dp, amounts onSurface not primary. BudgetSummaryCard: elevation 2→2dp, shape large→16dp, amounts onSurface, padding adjusted. Remove borders. |
| **BudgetDetailScreens.kt** | `app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt` | BudgetStatusCard: elevation 2→4dp, gradients on progress bar, amounts onSurface. SectionSwitcher: FilterChip→FilledTonalButton toggle. WalletCard: remove border, add overspent dot. WalletActionStrip: use surfaceContainerLow. WalletSummaryPanel: elevation→4dp, no border, gradient progress. TransactionCard: tonal fill instead of border. All cards: remove BorderStroke, update shapes, update padding. |
| **InsightsScreens.kt** | `app/src/main/java/com/mebudget/app/ui/feature/insights/InsightsScreens.kt` | Hero cards: elevation→2dp, shapes→16dp, gradient progress bars, amounts onSurface. InsightMetricCard: tonal fill→0.4f alpha, shape→8dp. |
| **MeBudgetNavHost.kt** | `app/src/main/java/com/mebudget/app/ui/navigation/MeBudgetNavHost.kt` | Bottom nav: remove labels, icon size 24→28dp, active=filled primary, inactive=outlined. Budgets LazyColumn bottom padding 80→72dp. |
| **CommonUi.kt** | `app/src/main/java/com/mebudget/app/ui/common/CommonUi.kt` | Update EmptyState card styling to match new elevation/shape system. PrivacyModeBanner: remove border. |
| **InsightsUi.kt** | `app/src/main/java/com/mebudget/app/ui/common/InsightsUi.kt` | Update InsightMetricCard, BudgetInsightSection, InsightDetailCard to match new elevation/shape system. |

### No files created

All changes are modifications to existing files.

---

### Motion Design Details (for all tasks below)

The following motion principles apply to every composable task:

- **Card press**: use `Modifier.clickable` with `MutableInteractionSource` + `animateFloatAsState` for spring 0.97x scale on press, spring back on release. Wrap cards in a reusable `animatePress` modifier.
- **List items**: wrap in `AnimatedVisibility` with `fadeIn + slideInVertically(8.dp)`, staggered via `animationDelay` based on item index.
- **Progress bars**: wrap `progress` param with `animateFloatAsState` for smooth transitions.
- **Reduced motion**: all animations gated behind `LocalViewConfiguration` or the system-level reduced motion flag.

Shared utility to add in `Gradients.kt` (Task 3) or a new `common/Animations.kt`:

```kotlin
@Composable
fun Modifier.animatePress(): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = DampingRatioMediumBouncy, stiffness = StiffnessMedium)
    )
    graphicsLayer(scaleX = scale, scaleY = scale)
        .clickable(interactionSource = interactionSource, indication = null) { }
}
```

---

## Tasks

### Task 1: Update Typography (Type.kt)

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/theme/Type.kt`

**Interfaces:**
- Consumes: none (first task)
- Produces: updated `Typography` object consumed by `Theme.kt` and all screen composables via `MaterialTheme.typography`

- [ ] **Step 1: Update headlineLarge**

Change `headlineLarge` from 34sp/38sp to 40sp/44sp, add `letterSpacing = (-0.5).sp`:

```kotlin
headlineLarge = TextStyle(
    fontFamily = MainFontFamily,
    fontWeight = FontWeight.Black,
    fontSize = 40.sp,
    lineHeight = 44.sp,
    letterSpacing = (-0.5).sp
),
```

- [ ] **Step 2: Update headlineMedium**

Change 30sp/34sp to 34sp/38sp:

```kotlin
headlineMedium = TextStyle(
    fontFamily = MainFontFamily,
    fontWeight = FontWeight.Black,
    fontSize = 34.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.5).sp
),
```

- [ ] **Step 3: Update titleLarge**

Change 26sp/30sp to 28sp/32sp:

```kotlin
titleLarge = TextStyle(
    fontFamily = MainFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 32.sp
),
```

- [ ] **Step 4: Update titleMedium**

Change 18sp/22sp to 20sp/24sp:

```kotlin
titleMedium = TextStyle(
    fontFamily = MainFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 24.sp
),
```

- [ ] **Step 5: Update labelMedium**

Change 12sp/16sp to 13sp/18sp:

```kotlin
labelMedium = TextStyle(
    fontFamily = MainFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp
),
```

- [ ] **Step 6: Update bodyMedium**

Change 15sp/21sp to 16sp/22sp:

```kotlin
bodyMedium = TextStyle(
    fontFamily = MainFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 22.sp
),
```

- [ ] **Step 7: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/theme/Type.kt
git commit -m "refactor(ui): expand typography for premium-minimal feel"
```

---

### Task 2: Update BudgetsScreen Cards

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgets/BudgetsScreen.kt`

**Interfaces:**
- Consumes: updated `Typography` from Task 1 (larger headlineMedium, headlineLarge)
- Produces: refactored `TotalSummarySection`, `BudgetSummaryCard` composables

- [ ] **Step 1: Remove unused imports**

Remove `BorderStroke` import (no borders used anywhere in this file after changes).

- [ ] **Step 2: Refactor TotalSummarySection**

Current code (lines 224-264) — make these changes:

1. Elevation: `defaultElevation = 2.dp` → `defaultElevation = 4.dp`
2. Increase inner padding: `Modifier.padding(24.dp)` already correct (stays)
3. Amount color: `color = MaterialTheme.colorScheme.primary` → `color = MaterialTheme.colorScheme.onSurface`
4. Ensure text style uses MaterialTheme already (no change needed)

```kotlin
Card(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    shape = RoundedCornerShape(20.dp)
)
```

The shape `RoundedCornerShape(20.dp)` replaces `MaterialTheme.shapes.extraLarge` (was 16dp).
The horizontal padding changes from `16.dp` to `20.dp`.

- [ ] **Step 3: Refactor BudgetSummaryCard**

Current code (lines 268-358) — make these changes:

1. Remove the `clickable` modifier wrapper approach: currently clickable on the Card directly
2. Elevation stays `2.dp`
3. Shape: `MaterialTheme.shapes.large` → `RoundedCornerShape(16.dp)`
4. Remove the `HorizontalDivider` (lines 326) — no dividers
5. Inner padding: `Modifier.padding(16.dp)` → `Modifier.padding(20.dp)`
6. Amount color: `primary` → `onSurface`
7. Amount style: `titleMedium` → `headlineSmall` (now 28sp)

```kotlin
Card(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .clickable(onClick = onOpen),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ... same Row with name/date/action icons ...
        // REMOVE the HorizontalDivider entirely
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ... wallet icon + count ...
            Text(
                text = maskedAmount(budget.totalBalance, privacyModeEnabled),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
```

- [ ] **Step 4: Update outer padding and spacing in BudgetsScreen**

Change the LazyColumn's outer padding:
```kotlin
contentPadding = PaddingValues(bottom = 72.dp),
verticalArrangement = Arrangement.spacedBy(20.dp)
```
(80dp→72dp bottom, 16dp→20dp spacing)

- [ ] **Step 5: Update section header row padding**

Change `Modifier.padding(horizontal = 16.dp)` to `Modifier.padding(horizontal = 20.dp)` in the section header Row (line 100-102).

- [ ] **Step 6: Update BudgetCreationChoiceDialog**

Remove `BorderStroke` from inner cards (lines 385, 410-414). Replace with subtle elevation or tonal fill only. Use `RoundedCornerShape(12.dp)` for inner selection cards.

- [ ] **Step 7: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/feature/budgets/BudgetsScreen.kt
git commit -m "refactor(ui): update budgets screen cards - elevation, spacing, border removal"
```

---

### Task 3: Create Gradient Utility and Update Progress Bars

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt` (add a private gradient function)
- OR create: `app/src/main/java/com/mebudget/app/ui/common/Gradients.kt` (if shared across files)

**Decision:** Since gradients are used in BudgetStatusCard, WalletCard, WalletSummaryPanel (BudgetDetailScreens) AND in InsightsScreens.kt, add a shared utility in the `ui/common/` package.

- [ ] **Step 1: Create `Gradients.kt`**

Create `app/src/main/java/com/mebudget/app/ui/common/Gradients.kt`:

```kotlin
package com.mebudget.app.ui.common

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.mebudget.app.ui.theme.Moss
import com.mebudget.app.ui.theme.Overspend
import com.mebudget.app.ui.theme.Pine
import com.mebudget.app.ui.theme.Rust
import com.mebudget.app.ui.theme.Success
import com.mebudget.app.ui.theme.Warning

fun progressBarBrush(progress: Float, isOverspent: Boolean): Brush {
    val gradientStart: Color
    val gradientEnd: Color
    when {
        isOverspent -> {
            gradientStart = Rust
            gradientEnd = Overspend
        }
        progress < 0.2f -> {
            gradientStart = Warning
            gradientEnd = Rust
        }
        else -> {
            gradientStart = Pine
            gradientEnd = Moss
        }
    }
    return Brush.horizontalGradient(listOf(gradientStart, gradientEnd))
}
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/common/Gradients.kt
git commit -m "feat(ui): add gradient utility for progress bars"
```

---

### Task 4: Update Budget Detail Screen — Status Card, Section Switcher, Action Strips

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt`

**Interfaces:**
- Consumes: `progressBarBrush()` from Task 3, updated Typography from Task 1
- Produces: refactored `BudgetStatusCard`, `BudgetSectionSwitcher`, `BudgetActionStrip`

- [ ] **Step 1: Refactor BudgetStatusCard (lines 574-627)**

1. Elevation: `2.dp` → `4.dp`
2. Shape: `MaterialTheme.shapes.large` → `RoundedCornerShape(20.dp)`
3. Amount color: no color override → `MaterialTheme.colorScheme.onSurface`
4. Progress bar: replace solid color with `progressBarBrush()`:
   ```kotlin
   LinearProgressIndicator(
       progress = { progress },
       modifier = Modifier.fillMaxWidth().height(8.dp),
       brush = progressBarBrush(progress, progress < 0.2f),
       strokeCap = StrokeCap.Round,
       trackColor = MaterialTheme.colorScheme.surfaceVariant
   )
   ```
5. Remove `import` for `Overspend` and `Success` if they become unused in this file (they're reused in WalletCard/WalletSummaryPanel, so keep them).
6. Outer padding: `horizontal = 16.dp` → `horizontal = 20.dp`

- [ ] **Step 2: Refactor BudgetSectionSwitcher (lines 553-571)**

Replace `FilterChip` row with `FilledTonalButton` toggle group:

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
            if (selectedSection == section) {
                Button(
                    onClick = { onSectionSelected(section) },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(section.label)
                }
            } else {
                OutlinedButton(
                    onClick = { onSectionSelected(section) },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(section.label)
                }
            }
        }
    }
}
```

Add imports: `PaddingValues`, `Button`, `OutlinedButton` (if not already imported).

- [ ] **Step 3: Refactor BudgetActionStrip (lines 630-644)**

1. Container color: `surfaceVariant.copy(alpha = 0.45f)` → use `surfaceContainerLow` via a R.color or just use the Material3 token. Material3 may not expose `surfaceContainerLow` as a named token in Compose. Check if it's available:
   - If `MaterialTheme.colorScheme.surfaceContainerLow` compiles, use it.
   - Otherwise keep current approach but increase alpha: `SurfaceTonalElevation` approach, or use `surfaceVariant.copy(alpha = 0.30f)` with a small elevation of `1.dp`.
2. Shape: `MaterialTheme.shapes.large` → `RoundedCornerShape(16.dp)`
3. Outer padding: `horizontal = 16.dp` → `horizontal = 20.dp`

- [ ] **Step 4: Update Wallets section header row**

Outer padding: `Modifier.padding(horizontal = 16.dp)` → `Modifier.padding(horizontal = 20.dp)` (lines 451-453).

Section title text: update to `headlineSmall` Bold or keep `titleMedium` (Task 1 bumped it to 20sp which is fine as-is).

- [ ] **Step 5: Update BudgetOverviewScreen LazyColumn**

Change `contentPadding = PaddingValues(bottom = 80.dp)` → `bottom = 72.dp` (line 434).
Change `verticalArrangement = Arrangement.spacedBy(16.dp)` → `spacedBy(20.dp)` (line 435).

- [ ] **Step 6: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt
git commit -m "refactor(ui): update budget detail - status card, section switcher, action strip"
```

---

### Task 5: Update Wallet Detail Screen — WalletCard, WalletActionStrip, WalletSummaryPanel, TransactionCard

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt`

- [ ] **Step 1: Refactor WalletCard (lines 905-1055)**

1. Remove `border = BorderStroke(...)` entirely (lines 933-937)
2. Add overspent indicator: a 3dp colored dot on the left edge of the card:

```kotlin
Card(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .clip(RoundedCornerShape(12.dp))  // needed for the overspent dot,
                                          // but Card clips internally
        .then(if (isOverspent) {
            Modifier.drawBehind {
                drawCircle(
                    color = Rust,
                    radius = 3.dp.toPx(),
                    center = Offset(x = 3.dp.toPx(), y = size.height / 2f)
                )
            }
        } else Modifier),
    ...
)
```

Actually, a cleaner approach: wrap the Card in a `Box` and place the dot as a `Box` modifier on the left edge:

```kotlin
Box {
    Card(...) { ... }
    if (isOverspent) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-6).dp)  // half outside card
                .size(6.dp)
                .background(Rust, CircleShape)
        )
    }
}
```

Even simpler: since `Card` clips to shape by default, draw a small Box inside the Card content aligned left:

```kotlin
Row(modifier = Modifier.padding(16.dp)) {
    if (isOverspent) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(32.dp)
                .background(Rust, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
    }
    // existing content column...
}
```

**Simplest approach:** Use the Card's own `border` parameter but only to add a subtle left border when overspent is true. Actually the spec says no BorderStroke at all. Let me use the `drawBehind` approach or just a colored dot inside the content.

I recommend: Put the colored dot INSIDE the card content as a small Box before the content Row, drawn only when `isOverspent`.

3. Shape: `MaterialTheme.shapes.medium` → `RoundedCornerShape(12.dp)`
4. Inner padding: `Modifier.padding(16.dp)` stays (items keep 16dp)
5. Amount color: `primary` → `onSurface`
6. Progress bar: use `progressBarBrush` from Task 3
7. Outer padding: `horizontal = 16.dp` → `horizontal = 20.dp`

- [ ] **Step 2: Refactor WalletSummaryPanel (lines 830-902)**

1. Remove `border = BorderStroke(...)` (lines 849-853)
2. Elevation: add `elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)`
3. Shape: `MaterialTheme.shapes.extraLarge` → `RoundedCornerShape(20.dp)`
4. Amount color: `primary` → `onSurface` (line 869)
5. Progress bar: use `progressBarBrush` from Task 3
6. Outer padding: `horizontal = 16.dp` → `horizontal = 20.dp`

- [ ] **Step 3: Refactor WalletActionStrip (lines 784-827)**

1. Container color: use same approach as BudgetActionStrip (surfaceContainerLow or tonal + small elevation)
2. Shape: `MaterialTheme.shapes.large` → `RoundedCornerShape(16.dp)`
3. Outer padding: `horizontal = 16.dp` → `horizontal = 20.dp`

- [ ] **Step 4: Refactor TransactionCard (lines 1058-1100+)**

1. Remove `border = BorderStroke(...)` — replace with tonal fill:
   ```kotlin
   colors = CardDefaults.cardColors(
       containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
   ),
   ```
2. Elevation stays `0.dp`
3. Shape: `MaterialTheme.shapes.medium` → `RoundedCornerShape(12.dp)`
4. Outer padding: `horizontal = 16.dp` → `horizontal = 20.dp`

- [ ] **Step 5: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt
git commit -m "refactor(ui): update wallet detail - cards, panel, transaction card styling"
```

---

### Task 6: Update Insights Screens

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/insights/InsightsScreens.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/common/InsightsUi.kt`

- [ ] **Step 1: Update hero cards in InsightsScreens.kt**

Find `BudgetInsightHeroCard` / `GlobalInsightHeroCard` (search for `fun BudgetInsightHeroCard` and `fun GlobalInsightHeroCard`):

1. Shape: `MaterialTheme.shapes.large` → `RoundedCornerShape(16.dp)`
2. Remove border strokes
3. Add elevation: `elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)` if not present
4. Progress bar: use `progressBarBrush` from Task 3 (import `com.mebudget.app.ui.common.progressBarBrush`)
5. Amount colors: `primary` → `onSurface` where applicable
6. Outer padding: `horizontal = 16.dp` → `horizontal = 20.dp`

- [ ] **Step 2: Update InsightMetricCard in InsightsUi.kt**

1. Container color: `surfaceVariant.copy(alpha = 0.35f)` → `surfaceVariant.copy(alpha = 0.4f)`
2. Remove border stroke
3. Shape: `MaterialTheme.shapes.small` → `RoundedCornerShape(8.dp)`
4. Inner padding: `PaddingValues(12.dp)` stays

- [ ] **Step 3: Update BudgetInsightSection in InsightsUi.kt**

1. Shape: `MaterialTheme.shapes.large` → `RoundedCornerShape(16.dp)`
2. Remove border stroke
3. Add elevation: `elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)`
4. Outer padding: `horizontal = 16.dp` → `horizontal = 20.dp`

- [ ] **Step 4: Update InsightDetailCard in InsightsUi.kt**

1. Shape: `MaterialTheme.shapes.large` → `RoundedCornerShape(16.dp)`
2. Remove border stroke
3. Add elevation: `elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)`
4. Outer padding: `horizontal = 16.dp` → `horizontal = 20.dp`

- [ ] **Step 5: Remove unused BorderStroke imports**

Check both files for `BorderStroke` — remove the import if no longer used.

- [ ] **Step 6: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/feature/insights/InsightsScreens.kt
git add app/src/main/java/com/mebudget/app/ui/common/InsightsUi.kt
git commit -m "refactor(ui): update insights screens - elevation, shapes, gradients"
```

---

### Task 7: Update Bottom Navigation Bar

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/navigation/MeBudgetNavHost.kt`

- [ ] **Step 1: Remove labels from NavigationBarItem**

Change each `NavigationBarItem` — remove the `label` parameter entirely or set it to `null`:

```kotlin
NavigationBarItem(
    selected = currentRoute == MeBudgetRoute.budgets,
    onClick = { ... },
    icon = {
        Icon(
            imageVector = if (currentRoute == MeBudgetRoute.budgets) {
                Icons.Filled.Home
            } else {
                Icons.Outlined.Home
            },
            contentDescription = "Budgets",
            modifier = Modifier.size(28.dp)
        )
    }
    // no label parameter
)
```

Similarly for the Insights item with `Icons.Default.Analytics` / use filled/outlined variants.

Note: `Icons.Outlined.Home` may need a check. If `Icons.Filled` has a corresponding `Icons.Outlined`, use it. Otherwise use `Icons.Default.Home` with different tint.

Actually, simpler approach: keep using `Icons.Default.Home` and `Icons.Default.Analytics` but increase size to 28.dp and change tint based on selected state:

```kotlin
icon = {
    Icon(
        Icons.Default.Home,
        contentDescription = "Budgets",
        modifier = Modifier.size(28.dp),
        tint = if (currentRoute == MeBudgetRoute.budgets) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
},
label = null
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/navigation/MeBudgetNavHost.kt
git commit -m "refactor(ui): simplify bottom nav - remove labels, larger icons"
```

---

### Task 8: Update Common UI Components

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/common/CommonUi.kt`

- [ ] **Step 1: Update EmptyState**

Current code — find `fun EmptyState`:

1. Remove `border = BorderStroke(...)` 
2. Shape: keep `large` or use `RoundedCornerShape(16.dp)`
3. No elevation (stays 0dp)

- [ ] **Step 2: Update PrivacyModeBanner**

Current code — find `fun PrivacyModeBanner`:

1. Remove `border = BorderStroke(...)`
2. Keep tonal fill container color

- [ ] **Step 3: Remove unused `BorderStroke` import**

Remove `import androidx.compose.foundation.BorderStroke` if no longer used in the file.

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/common/CommonUi.kt
git commit -m "refactor(ui): update common components - remove borders, align shapes"
```

---

### Task 9: Add Micro-interactions and Animations

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/common/Gradients.kt` (add motion utilities)
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgets/BudgetsScreen.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt`

- [ ] **Step 1: Add `animatePress` and `animateItem` utilities to Gradients.kt**

Append after `progressBarBrush`:

```kotlin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.DampingRatioMediumBouncy
import androidx.compose.animation.core.StiffnessMedium
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun Modifier.animatePress(): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = DampingRatioMediumBouncy, stiffness = StiffnessMedium)
    )
    graphicsLayer(scaleX = scale, scaleY = scale)
        .clickable(interactionSource = interactionSource, indication = null) { }
}
```

- [ ] **Step 2: Apply `animatePress` to card composables**

For each clickable card in BudgetsScreen (BudgetSummaryCard) and BudgetDetailScreens (WalletCard, BudgetStatusCard, etc.), add `.animatePress()` modifier.

- [ ] **Step 3: Add AnimatedVisibility to budget list and transaction list**

In BudgetsScreen.kt, wrap the `items(budgets, ...)` block with `AnimatedVisibility`:
```kotlin
items(budgets, key = { it.id }) { budget ->
    val visibility = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibility.targetState = true }
    AnimatedVisibility(
        visibleState = visibility,
        enter = fadeIn() + slideInVertically { it / 4 }
    ) {
        BudgetSummaryCard(...)
    }
}
```

Similarly for WalletCard list in BudgetDetailScreens.kt and TransactionCard list.

- [ ] **Step 4: Add `animateFloatAsState` to progress bars**

In BudgetStatusCard, WalletCard, WalletSummaryPanel, and all insight hero cards, wrap the `progress` value:

```kotlin
val animatedProgress by animateFloatAsState(
    targetValue = progress,
    animationSpec = spring(dampingRatio = DampingRatioMediumBouncy)
)
```

Then pass `animatedProgress` instead of `progress` to the `LinearProgressIndicator`.

- [ ] **Step 5: Gate behind reduced motion**

Wrap all animation calls with a check:

```kotlin
import androidx.compose.ui.platform.LocalViewConfiguration

val viewConfig = LocalViewConfiguration.current
// when passing animated values or adding animations:
if (viewConfig.hasFocusEnabled) { // reduced motion — skip animations
    // use animated values
} else {
    // use raw values
}
```

Note: `hasFocusEnabled` is a proxy flag. The actual reduced-motion check on Android is via `ContentResolver` and `Settings.Global.TRANSITION_ANIMATION_SCALE`. A simpler approach: gate behind a `val reduceMotion = false` that can be toggled later.

- [ ] **Step 6: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/common/Gradients.kt
git add app/src/main/java/com/mebudget/app/ui/feature/budgets/BudgetsScreen.kt
git add app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt
git commit -m "feat(ui): add micro-interactions - press scale, list entrance, smooth progress"
```

---

### Task 10: Final Polish and Verify Build

**Files:**
- All modified files

- [ ] **Step 1: Verify imports are clean**

For each modified file, check that unused imports have been removed:
- `BorderStroke` removed from files that no longer use it
- `Overspend`/`Success`/`Warning` import status in BudgetDetailScreens.kt — keep if used in progress bars

- [ ] **Step 2: Full build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Check for no new warnings**

Run: `./gradlew assembleDebug 2>&1 | grep "w:"` — verify no new compiler warnings from our changes.

- [ ] **Step 4: Commit all remaining changes**

```bash
git add -A
git commit -m "refactor(ui): final polish for budget-pro redesign"
```
