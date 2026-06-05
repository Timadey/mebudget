# Quick Spend UX Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign Quick Spend setup and overlay UI so the settings page is clear and reassuring, the app picker is searchable with icons, and the floating overlay looks branded and solid.

**Architecture:** Keep the existing quick-spend state and service behavior. Improve Compose presentation in `QuickSpendSettingsScreen`, add a small drawable-to-bitmap bridge for installed app icons, and restyle the Android overlay service views with a branded pill button, dim scrim, and solid rounded form panel.

**Tech Stack:** Kotlin, Jetpack Compose Material3, Android `Drawable`/`Bitmap`, Android raw Views for system overlay, existing quick-spend ViewModel and service.

---

## File Structure

- Modify `app/src/main/java/com/mebudget/app/ui/feature/quickspend/QuickSpendSettingsScreen.kt`
  - Add calm checklist layout, benefit copy, search field, selected count, and app icons.
- Create `app/src/main/java/com/mebudget/app/ui/feature/quickspend/DrawableIconImage.kt`
  - Render Android `Drawable` icons inside Compose.
- Modify `app/src/main/java/com/mebudget/app/quickspend/QuickSpendOverlayService.kt`
  - Restyle floating button and mini form with branded solid UI.

## Task 1: Searchable App Picker With Icons

**Files:**
- Create: `app/src/main/java/com/mebudget/app/ui/feature/quickspend/DrawableIconImage.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/quickspend/QuickSpendSettingsScreen.kt`

- [ ] **Step 1: Add Drawable icon renderer**

```kotlin
package com.mebudget.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun DrawableIconImage(
    drawable: Drawable?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    if (drawable == null) {
        Box(
            modifier = modifier
                .size(40.dp)
                .background(Color(0xFFE7ECF2), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        )
        return
    }

    val bitmap = remember(drawable) {
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 48
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 48
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier.size(40.dp)
    )
}
```

- [ ] **Step 2: Add search state and filtering**

In `QuickSpendSettingsScreen`, add:

```kotlin
var appSearchQuery by remember { mutableStateOf("") }
val selectedPackages = state.settings.selectedAppPackages
val filteredApps = remember(state.launchableApps, selectedPackages, appSearchQuery) {
    val query = appSearchQuery.trim().lowercase()
    state.launchableApps
        .filter { app ->
            query.isEmpty() ||
                app.label.lowercase().contains(query) ||
                app.packageName.lowercase().contains(query)
        }
        .sortedWith(
            compareByDescending<com.mebudget.app.quickspend.LaunchableApp> {
                selectedPackages.contains(it.packageName)
            }.thenBy { it.label.lowercase() }
        )
}
```

- [ ] **Step 3: Replace app list header and items**

Replace the current "Selected bank/payment apps" header and `items(state.launchableApps...)` block with:

```kotlin
item {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bank/payment apps", style = MaterialTheme.typography.titleMedium)
        Text("${selectedPackages.size} selected. The floating button only appears over apps you choose.")
        OutlinedTextField(
            value = appSearchQuery,
            onValueChange = { appSearchQuery = it },
            label = { Text("Search apps") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

items(filteredApps, key = { it.packageName }) { app ->
    val selected = selectedPackages.contains(app.packageName)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawableIconImage(
            drawable = app.icon,
            contentDescription = null
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = { onToggleApp(app.packageName) }) {
            Icon(
                imageVector = if (selected) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Default.RadioButtonUnchecked
                },
                contentDescription = if (selected) "Selected" else "Not selected"
            )
        }
    }
}
```

- [ ] **Step 4: Run build**

Run: `./gradlew compileDebugKotlin`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/feature/quickspend/DrawableIconImage.kt app/src/main/java/com/mebudget/app/ui/feature/quickspend/QuickSpendSettingsScreen.kt
git commit -m "Add searchable quick spend app picker"
```

## Task 2: Calm Checklist Settings Layout

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/quickspend/QuickSpendSettingsScreen.kt`

- [ ] **Step 1: Add setup status helpers**

Inside `QuickSpendSettingsScreen`, after `selectedBudget`, add:

```kotlin
val hasBudget = state.settings.selectedBudgetId != null
val hasApps = state.settings.selectedAppPackages.isNotEmpty()
val nextStep = when {
    !hasBudget -> "Choose the budget where quick spends should be recorded."
    !state.overlayPermissionGranted -> "Allow the floating button permission."
    !state.usageAccessGranted -> "Allow app detection permission."
    !hasApps -> "Select at least one bank or payment app."
    !state.settings.enabled -> "Turn on Quick Spend."
    else -> "Ready over selected apps."
}
```

- [ ] **Step 2: Replace top switch/status block with guided intro**

Replace the first two `item` blocks in the `LazyColumn` with:

```kotlin
item {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Quick Spend", style = MaterialTheme.typography.headlineSmall)
        Text("Record expenses while using your bank or payment app, so your budget balance stays accurate.")
        Text("Manual entry only. MeBudget does not read your bank screen.", style = MaterialTheme.typography.bodySmall)
    }
}

item {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (state.setupComplete) "Ready over selected apps" else nextStep,
            style = MaterialTheme.typography.titleMedium
        )
        ChecklistRow("Choose quick-spend budget", hasBudget)
        ChecklistRow("Allow floating button", state.overlayPermissionGranted)
        ChecklistRow("Allow app detection", state.usageAccessGranted)
        ChecklistRow("Select bank/payment apps", hasApps)
        ChecklistRow("Enable Quick Spend", state.settings.enabled)
    }
}

item {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("What you gain", style = MaterialTheme.typography.titleMedium)
        Text("Record before or after payment.")
        Text("Avoid balance mismatch between your bank and budget.")
        Text("Works only on apps you choose.")
    }
}

item {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Enable Quick Spend", style = MaterialTheme.typography.titleMedium)
            Text("Show a small MeBudget button over selected apps.")
        }
        Switch(
            checked = state.settings.enabled,
            onCheckedChange = onToggleEnabled
        )
    }
}
```

- [ ] **Step 3: Add checklist row composable**

Add near the bottom of the file:

```kotlin
@Composable
private fun ChecklistRow(
    label: String,
    complete: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (complete) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null
        )
        Text(label)
    }
}
```

- [ ] **Step 4: Run build**

Run: `./gradlew compileDebugKotlin`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/feature/quickspend/QuickSpendSettingsScreen.kt
git commit -m "Redesign quick spend setup checklist"
```

## Task 3: Branded Floating Button And Solid Form

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/quickspend/QuickSpendOverlayService.kt`

- [ ] **Step 1: Restyle floating button**

In `showOverlay()`, replace the `Button` with a `TextView` inside a `GradientDrawable` background:

```kotlin
val button = TextView(this).apply {
    text = "₦ Budget"
    textSize = 14f
    setTextColor(android.graphics.Color.WHITE)
    gravity = Gravity.CENTER
    setPadding(24, 14, 24, 14)
    background = android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        cornerRadius = 48f
        setColor(android.graphics.Color.rgb(32, 111, 96))
    }
    elevation = 8f
    setOnClickListener { showMiniForm() }
}
```

Keep the existing `attachDragHandler(button, params)` call.

- [ ] **Step 2: Add dim scrim and solid panel**

In `showMiniForm()`, replace the root `LinearLayout` usage with a `FrameLayout` overlay:

```kotlin
val root = FrameLayout(this).apply {
    setBackgroundColor(0x33000000)
}
val panel = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(28, 24, 28, 24)
    background = android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        cornerRadius = 24f
        setColor(android.graphics.Color.WHITE)
    }
    elevation = 12f
}
val titleText = TextView(this).apply {
    text = "Record Spend"
    textSize = 18f
    setTextColor(android.graphics.Color.rgb(22, 28, 36))
}
panel.addView(titleText)
panel.addView(amountInput)
panel.addView(noteInput)
panel.addView(walletSpinner)
panel.addView(statusText)
panel.addView(saveButton)
panel.addView(cancelButton)
root.addView(
    panel,
    FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.CENTER
    ).apply {
        leftMargin = 24
        rightMargin = 24
    }
)
```

Then call `windowManager.addView(root, params)` and assign `overlayView = root`.

- [ ] **Step 3: Adjust form layout params**

In `showMiniForm()`, set:

```kotlin
width = WindowManager.LayoutParams.MATCH_PARENT
height = WindowManager.LayoutParams.MATCH_PARENT
gravity = Gravity.TOP or Gravity.START
x = 0
y = 0
```

This allows the dim scrim to fill the overlay while the panel stays centered.

- [ ] **Step 4: Run build**

Run: `./gradlew compileDebugKotlin`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mebudget/app/quickspend/QuickSpendOverlayService.kt
git commit -m "Polish quick spend overlay UI"
```

## Task 4: Final Verification

**Files:**
- Modify only if verification exposes issues.

- [ ] **Step 1: Run final verification**

Run: `./gradlew testDebugUnitTest compileDebugKotlin`

Expected: PASS.

- [ ] **Step 2: Optional device verification**

If a device or emulator is connected, run: `./gradlew installDebug`

Manual checks:

- Quick Spend setup explains the feature and privacy boundary.
- Setup checklist shows next missing action.
- App search filters by label and package name.
- App rows show icons.
- Selected apps are shown before unselected apps.
- Floating button reads `₦ Budget`, is draggable, and stays compact.
- Tapping the floating button opens a solid centered panel over a dimmed scrim.
- Expense save still records a normal expense.

- [ ] **Step 3: Commit verification fixes if needed**

If verification required changes:

```bash
git add app/src/main/java/com/mebudget/app
git commit -m "Fix quick spend UX verification issues"
```

If verification required no code changes, do not create an empty commit.

## Self-Review

Spec coverage:

- Checklist setup page is covered by Task 2.
- Explanation of benefits and privacy boundary is covered by Task 2.
- App icons and search are covered by Task 1.
- Branded floating button is covered by Task 3.
- Solid form and dimmed background are covered by Task 3.

Placeholder scan:

- No placeholder sections. Each task has exact files, code blocks, commands, and expected results.

Type consistency:

- `DrawableIconImage`, `QuickSpendSettingsScreen`, and `QuickSpendOverlayService` names match existing files and planned changes.

