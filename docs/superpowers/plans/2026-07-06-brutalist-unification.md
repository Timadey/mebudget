# Brutalist Unification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert all 4 insight screens, 5 shared insight components, and 7 common UI components to the brutalist style for complete visual coherence.

**Architecture:** Three-layer conversion — extract shared `BlockProgressBar` to `Gradients.kt`, convert insight screens + their shared components to use BrutalistBudgetTheme with sharp corners and 4dp borders, convert common UI components and wrap remaining dialog callers in BrutalistBudgetTheme. Remove dead code (`QuickAmountChips`, `WalletDropdown`, `GradientProgressBar`).

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Outfit font (Black 900 weight)

## Global Constraints

- No domain/data/ViewModel changes
- All `RoundedCornerShape(x.dp)` → `RoundedCornerShape(0.dp)`
- All `elevation = x.dp` → `elevation = 0.dp`
- Cards with 0 elevation get `border = BorderStroke(4.dp, MaterialTheme.colorScheme.outline)`
- Color signaling preserved: red (`0xFFFF0000`) for overspent/danger, orange (`0xFFFF8800`) for warning, green (`0xFF00AA00`) for good/healthy
- All text stays Outfit Black 900 weight

---

### Task 1: Extract BlockProgressBar to Gradients.kt and remove from BudgetDetailScreens.kt

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/common/Gradients.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt`

**Interfaces:**
- Consumes: `BlockProgressBar` implementation at `BudgetDetailScreens.kt:1047`
- Produces: Public `BlockProgressBar` in `Gradients.kt` with same signature + `color: Color` parameter

- [ ] **Step 1: Add BlockProgressBar to Gradients.kt**

Append to `Gradients.kt`, preserving all existing code:

```kotlin
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.Offset

@Composable
fun BlockProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified,
    segments: Int = 12
) {
    val resolvedColor = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
    val resolvedTrackColor = if (trackColor != Color.Unspecified) trackColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val filledSegments = (progress * segments).toInt().coerceIn(0, segments)

    Box(
        modifier = modifier
            .clipToBounds()
            .drawBehind {
                val segmentWidth = size.width / segments
                for (i in 0 until segments) {
                    val left = segmentWidth * i
                    val c = if (i < filledSegments) resolvedColor else resolvedTrackColor
                    drawRect(
                        color = c,
                        topLeft = Offset(left, 0f),
                        size = Size(segmentWidth - 2.dp.toPx(), size.height)
                    )
                }
            }
    )
}
```

Add these imports: `import androidx.compose.ui.geometry.Offset`, `import androidx.compose.foundation.layout.height`, `import androidx.compose.ui.unit.Dp`, `import androidx.compose.runtime.getValue`, `import androidx.compose.runtime.remember`, `import androidx.compose.foundation.clipToBounds`.

- [ ] **Step 2: Remove private BlockProgressBar from BudgetDetailScreens.kt**

Remove lines 1047-1075 (`private fun BlockProgressBar(...) { ... }`). Also remove unused imports if `Offset` and `Size` become unused elsewhere in the file (check grep for `Offset` and `Size` usage outside the removed block).

- [ ] **Step 3: Remove GradientProgressBar and progressBarBrush from Gradients.kt**

Delete `progressBarBrush()` function (lines 25-43) and `GradientProgressBar` composable (lines 45-75). Remove unused imports after deletion:
- `import androidx.compose.foundation.shape.RoundedCornerShape`
- `import androidx.compose.animation.core.animateFloatAsState`
- `import androidx.compose.animation.core.spring`
- `import androidx.compose.runtime.getValue`
- `import androidx.compose.ui.draw.clip`
- `import androidx.compose.ui.geometry.CornerRadius`
- `import androidx.compose.ui.graphics.Brush`
- `import com.mebudget.app.ui.theme.Moss`
- `import com.mebudget.app.ui.theme.Overspend`
- `import com.mebudget.app.ui.theme.Pine`
- `import com.mebudget.app.ui.theme.Rust`
- `import com.mebudget.app.ui.theme.Warning`

- [ ] **Step 4: Compile and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/common/Gradients.kt app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt
git commit -m "refactor: extract BlockProgressBar to Gradients.kt, remove GradientProgressBar"
```

---

### Task 2: Convert shared insight components (InsightsUi.kt)

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/common/InsightsUi.kt`

**Interfaces:**
- Consumes: `BlockProgressBar` from `Gradients.kt`
- Produces: Updated components used by `InsightsScreens.kt` and `BudgetDetailScreens.kt`

- [ ] **Step 1: Convert BudgetInsightSection**

Replace the `Card` at line 30-36 with brutalist style:

```kotlin
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.outline)
    ) {
```

Add import: `import androidx.compose.foundation.BorderStroke`

- [ ] **Step 2: Convert InsightMetricCard**

Replace the `Card` at line 128-131:

```kotlin
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
```

- [ ] **Step 3: Convert ObservationList**

Replace the `Card` inside the `forEach` loop (lines 162-167):

```kotlin
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
```

- [ ] **Step 4: Convert InsightDetailCard**

Replace the `Card` at line 194-200:

```kotlin
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.outline)
    ) {
```

- [ ] **Step 5: Convert TransferPathRow**

Replace `HorizontalDivider` color (line 238):

```kotlin
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
```

- [ ] **Step 6: Compile and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/common/InsightsUi.kt
git commit -m "refactor: convert InsightsUi components to brutalist (0.dp corners, 4dp borders)"
```

---

### Task 3: Convert BudgetInsightsScreen and GlobalInsightsScreen (InsightsScreens.kt)

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/insights/InsightsScreens.kt`

- [ ] **Step 1: Update imports at top of file**

Replace `import com.mebudget.app.ui.common.GradientProgressBar` with:
```kotlin
import com.mebudget.app.ui.common.BlockProgressBar
import com.mebudget.app.ui.theme.BrutalistBudgetTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
```
Remove unused imports: `import androidx.compose.material3.AssistChip`, `import androidx.compose.material3.AssistChipDefaults`, `import androidx.compose.material3.MediumTopAppBar`, `import androidx.compose.material3.Scaffold`, `import androidx.compose.material3.TopAppBarDefaults`, `import androidx.compose.material.icons.automirrored.filled.ArrowBack`, `import androidx.compose.material3.Icon`, `import androidx.compose.material3.IconButton`, `import androidx.compose.material.icons.Icons` (keep if used elsewhere, check).

- [ ] **Step 2: Wrap BudgetInsightsScreen in BrutalistBudgetTheme**

At line 55, insert `BrutalistBudgetTheme {` after the function opening `{` (line 60: `val insights = detail.insights`), and wrap everything inside.

Replace the Scaffold + MediumTopAppBar pattern (lines 63-91) with the brutalist header:

```kotlin
    val insights = detail.insights

    BrutalistBudgetTheme {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("[<]", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Budget Insights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = detail.budget.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                PrivacyToggleButton(
                    privacyModeEnabled = privacyModeEnabled,
                    onTogglePrivacyMode = onTogglePrivacyMode
                )
            }

            HorizontalDivider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.onSurface
            )
```

Then the LazyColumn from line 93+ continues without the `Scaffold` + `padding` wrapper — replace `LazyColumn` modifier:

```kotlin
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 0.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
```

Close the `Column` and `BrutalistBudgetTheme` before the function closing `}`:
```
        }
    }
```

- [ ] **Step 3: Convert BudgetInsightHeroCard (line 262-385)**

Replace `Card` at lines 276-283:
```kotlin
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.outline)
    ) {
```

Replace `GradientProgressBar` call at lines 340-346:
```kotlin
                BlockProgressBar(
                    progress = spentRatio.coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )
```

- [ ] **Step 4: Convert PriorityInsightCard (line 746-784)**

Replace `Card` at lines 751-757:
```kotlin
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.outline)
    ) {
```

- [ ] **Step 5: Convert SignalBadge (line 787-807)**

Replace entire composable:
```kotlin
@Composable
private fun SignalBadge(label: String, tone: SignalTone) {
    val bgColor = when (tone) {
        SignalTone.Good -> Color(0xFF00AA00)
        SignalTone.Warning -> Color(0xFFFF8800)
        SignalTone.Danger -> Color(0xFFFF0000)
    }
    Surface(
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = bgColor
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}
```

- [ ] **Step 6: Convert WalletHealthRow (line 388-479)**

Replace `Card` at lines 400-403:
```kotlin
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
```

Replace `GradientProgressBar` call at lines 451-457:
```kotlin
                BlockProgressBar(
                    progress = spentRatio.coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = when (tone) {
                        SignalTone.Danger -> Color(0xFFFF0000)
                        SignalTone.Warning -> Color(0xFFFF8800)
                        SignalTone.Good -> Color(0xFF00AA00)
                    },
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )
```

- [ ] **Step 7: Convert PatternSummaryRow (line 810-833)**

Replace `HorizontalDivider` color (line 831):
```kotlin
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
```

- [ ] **Step 8: Convert TransferRouteRow (line 836-880)**

Replace `Card` at lines 842-845:
```kotlin
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
```

Read the remaining lines of TransferRouteRow (846-880) to see the full structure. The `HorizontalDivider` inside should also be updated:
```kotlin
HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
```

- [ ] **Step 9: Convert GlobalInsightsScreen (line 483+ same pattern)**

Same pattern as BudgetInsightsScreen:
1. Wrap in `BrutalistBudgetTheme { ... }`
2. Replace `Scaffold` + `MediumTopAppBar` with brutalist header Row
3. Account for `showBack` parameter — conditionally show `[<]` button:

```
TextButton(onClick = onBack) {
    if (showBack) Text("[<]", ...) else null
}
```

4. All inner Card calls get 0.dp corners + 0 elevation + 4dp border
5. Replace any `GradientProgressBar` with `BlockProgressBar`
6. Replace `SignalBadge` uses (already updated in step 5)

**Important**: GlobalInsightsScreen uses `PrivacyModeBanner` at line 541 and `LoadingState` at line 527 — these still work within BrutalistBudgetTheme.

- [ ] **Step 10: Compile and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/feature/insights/InsightsScreens.kt
git commit -m "refactor: convert BudgetInsightsScreen and GlobalInsightsScreen to brutalist"
```

---

### Task 4: Convert WalletHistoryDetailScreen and TransferPatternDetailScreen (InsightsScreens.kt)

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/insights/InsightsScreens.kt`

These two screens have the same Scaffold + MediumTopAppBar pattern as the first two, but simpler content (no GradientProgressBar, no SignalBadge in the body).

- [ ] **Step 1: Convert WalletHistoryDetailScreen (line 985-1105)**

Replace `Scaffold` + `MediumTopAppBar` (lines 991-1019) with brutalist header inside `BrutalistBudgetTheme { ... }`. Same pattern as Task 3 Step 2. Header shows `insight.displayName` as title and "Wallet history" as subtitle.

- [ ] **Step 2: Convert TransferPatternDetailScreen (line 1109-1188)**

Same conversion. Header shows `"${insight.sourceDisplayName} -> ${insight.destinationDisplayName}"` as title and "Transfer path history" as subtitle.

- [ ] **Step 3: Compile and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/feature/insights/InsightsScreens.kt
git commit -m "refactor: convert WalletHistoryDetailScreen and TransferPatternDetailScreen to brutalist"
```

---

### Task 5: Convert CommonUi.kt components

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/common/CommonUi.kt`

- [ ] **Step 1: Remove QuickAmountChips (dead code, lines 37-53)**

Delete the entire `QuickAmountChips` composable (lines 37-53).

- [ ] **Step 2: Convert EmptyState (lines 55-102)**

Replace hardcoded `Color.White` / `Color.Black` with theme tokens, and fix button shape:

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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(actionLabel.uppercase(), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}
```

- [ ] **Step 3: Convert PrivacyModeBanner (lines 117-155)**

```kotlin
@Composable
fun PrivacyModeBanner(
    modifier: Modifier = Modifier,
    onTogglePrivacyMode: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.padding(horizontal = 20.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Privacy mode is on",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Amounts are hidden across the app.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (onTogglePrivacyMode != null) {
                TextButton(onClick = onTogglePrivacyMode) {
                    Text("Show", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
```

- [ ] **Step 4: Convert DateInputField (lines 157-228)**

Convert `OutlinedTextField` (lines 169-179) to brutalist style — use transparent background, black (onSurface) label, border underline:

```kotlin
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                label = { Text(label, fontWeight = FontWeight.Black) },
                placeholder = { Text("Select date", fontWeight = FontWeight.Black) },
                trailingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    cursorColor = MaterialTheme.colorScheme.onSurface
                )
            )
```

Replace `AssistChip` with brutalist chips:
```kotlin
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { onValueChange(LocalDate.now().toString()) },
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = "Today",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (allowClear && value.isNotBlank()) {
                Surface(
                    onClick = { onValueChange("") },
                    shape = RoundedCornerShape(0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Clear",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
```

The `DatePickerDialog` (lines 203-227) can stay as-is — system dialog, acceptable to keep default style. Add `import com.mebudget.app.ui.common.GradientsKt` if needed (no, it's in the same package `com.mebudget.app.ui`).

Also need to add: `import androidx.compose.foundation.clickable`, `import androidx.compose.material3.Surface` (for the chip replacement). Need to add `import com.mebudget.app.data.toPickerMillis` and `import com.mebudget.app.data.toStoredDate` if not already present.

- [ ] **Step 5: Convert BudgetDialog (lines 230-297)**

Replace `AlertDialog` title to use Black 900 font weight and theme colors:
```kotlin
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = { Text(title, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    label = { Text("Budget name", fontWeight = FontWeight.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        cursorColor = MaterialTheme.colorScheme.onSurface
                    )
                )
```

Keep `DateInputField` calls (lines 251-261) — they're now brutalist from Step 4.

Keep `EnumDropdown` call (lines 262-270) — will be converted in Step 6.

Update button text (lines 287-295) for Black 900 weight:
```kotlin
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text(saveLabel, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Black)
            }
        }
```

- [ ] **Step 6: Remove dead WalletDropdown (lines 299-341)**

Delete the entire `WalletDropdown` composable (lines 299-341). This is dead code — all callers use `BrutalistWalletDropdown` from BudgetDetailScreens.

- [ ] **Step 7: Convert WalletTemplateDropdown (lines 343-385)**

Convert `OutlinedTextField` to brutalist style. Same pattern as DateInputField:
```kotlin
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedBudget?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontWeight = FontWeight.Black) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = budgets.isNotEmpty()
                ),
            shape = RoundedCornerShape(0.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
```

Also convert `DropdownMenuItem` text to Black 900 font weight.

- [ ] **Step 8: Convert EnumDropdown (lines 387-428)**

Same brutalist OutlinedTextField conversion as WalletTemplateDropdown. Convert `DropdownMenuItem` text to Black 900 weight.

- [ ] **Step 9: Convert LoadingState (lines 430-441)**

Update to use Black 900 font weight:
```kotlin
    Text(
        text = "Loading...",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
```

- [ ] **Step 10: Compile and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If errors occur, check for:
- Missing imports (`Surface`, `OutlinedTextFieldDefaults`, `Color`, `clickable`, `Icons.Default.CalendarMonth`)
- Unused imports after deleting QuickAmountChips and WalletDropdown
- Any leftover references to deleted functions

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/common/CommonUi.kt
git commit -m "refactor: convert CommonUi to brutalist, remove dead code"
```

---

### Task 6: Wrap dialog calls in BudgetsScreen.kt with BrutalistBudgetTheme

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/budgets/BudgetsScreen.kt`

- [ ] **Step 1: Add BrutalistBudgetTheme import**

Add `import com.mebudget.app.ui.theme.BrutalistBudgetTheme` if not already present.

- [ ] **Step 2: Wrap BudgetDialog call**

Lines 165-174. Wrap in BrutalistBudgetTheme:
```kotlin
    if (showBlankBudgetDialog) {
        BrutalistBudgetTheme {
            BudgetDialog(
                title = "New Budget",
                onDismiss = { showBlankBudgetDialog = false },
                onSave = {
                    onCreateBudget(it)
                    showBlankBudgetDialog = false
                }
            )
        }
    }
```

- [ ] **Step 3: Wrap TemplateBudgetDialog call**

Lines 176-186. Wrap in BrutalistBudgetTheme:
```kotlin
    templateBudgetId?.let { selectedTemplateId ->
        BrutalistBudgetTheme {
            TemplateBudgetDialog(
                budgets = budgets,
                initialTemplateBudgetId = selectedTemplateId,
                onDismiss = { templateBudgetId = null },
                onCreateFromTemplate = { sourceBudgetId, name ->
                    onDuplicateBudget(sourceBudgetId, name)
                    templateBudgetId = null
                }
            )
        }
    }
```

- [ ] **Step 4: Compile and verify**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/feature/budgets/BudgetsScreen.kt
git commit -m "refactor: wrap BudgetsScreen dialogs in BrutalistBudgetTheme"
```

---

### Task 7: Final build and test verification

**Files:** None

- [ ] **Step 1: Run full compilation**

```bash
./gradlew compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run all tests**

```bash
./gradlew test
```
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 3: Verify no remaining rounded corners in insight components**

Check files for any remaining `RoundedCornerShape` with non-zero values:
```bash
rg "RoundedCornerShape" app/src/main/java/com/mebudget/app/ui/feature/insights/
rg "RoundedCornerShape" app/src/main/java/com/mebudget/app/ui/common/
```
Expected: Only `RoundedCornerShape(0.dp)` for insight and common components (DatePickerDialog may still have system-default shape — acceptable)

- [ ] **Step 4: Verify no GradientProgressBar usage remains**

```bash
rg "GradientProgressBar"
```
Expected: No matches outside Gradients.kt (which will have been removed in Task 1)

- [ ] **Step 5: Commit any final fixes**

```bash
git add -A
git commit -m "chore: final build verification and cleanup"
```
