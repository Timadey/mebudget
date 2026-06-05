# Overlay Quick Spend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a manual amount-first quick-spend overlay that appears over user-selected bank/payment apps and saves normal expense transactions into MeBudget.

**Architecture:** Keep the business rules pure and tested, store setup state in SharedPreferences, add a Compose settings screen for configuration, and run a foreground-aware Android overlay service for the floating button and mini form. The overlay service must only accept manual input and must save through the existing `BudgetRepository.addExpense(...)` path.

**Tech Stack:** Kotlin, Android SDK overlay APIs, UsageStatsManager, SharedPreferences, Jetpack Compose, Room-backed `BudgetRepository`, JUnit 4.

---

## File Structure

- Create `app/src/main/java/com/mebudget/app/quickspend/QuickSpendSettings.kt`
  - Pure settings model and setup-completion logic.
- Create `app/src/main/java/com/mebudget/app/quickspend/QuickSpendSettingsStore.kt`
  - SharedPreferences-backed persistence for selected budget, selected app package names, and enabled state.
- Create `app/src/test/java/com/mebudget/app/quickspend/QuickSpendSettingsTest.kt`
  - Unit tests for setup-completion logic and selected app matching.
- Create `app/src/main/java/com/mebudget/app/quickspend/QuickSpendPermissions.kt`
  - Android permission checks and settings intents for overlay and usage access.
- Create `app/src/main/java/com/mebudget/app/quickspend/ForegroundAppDetector.kt`
  - Foreground package lookup through `UsageStatsManager`.
- Create `app/src/main/java/com/mebudget/app/quickspend/InstalledAppSource.kt`
  - Launchable app query for the settings picker.
- Create `app/src/main/java/com/mebudget/app/ui/QuickSpendViewModel.kt`
  - Compose state holder for setup, app selection, wallet selection, and service start/stop requests.
- Modify `app/src/main/java/com/mebudget/app/ui/BudgetViewModelFactories.kt`
  - Add `QuickSpendViewModelFactory`.
- Create `app/src/main/java/com/mebudget/app/ui/feature/quickspend/QuickSpendSettingsScreen.kt`
  - Compose settings screen.
- Modify `app/src/main/java/com/mebudget/app/ui/navigation/MeBudgetNavigation.kt`
  - Add `quickSpendSettings` route.
- Modify `app/src/main/java/com/mebudget/app/ui/navigation/MeBudgetNavHost.kt`
  - Add settings navigation item and route.
- Modify `app/src/main/java/com/mebudget/app/ui/app/MeBudgetApp.kt`
  - Wire `QuickSpendViewModel` into navigation.
- Modify `app/src/main/java/com/mebudget/app/MainActivity.kt`
  - Instantiate `QuickSpendViewModel`.
- Create `app/src/main/java/com/mebudget/app/quickspend/QuickSpendOverlayService.kt`
  - Floating button, mini form, foreground app polling, and expense save.
- Modify `app/src/main/AndroidManifest.xml`
  - Add overlay, package query, and service declarations.

## Task 1: Settings Model And Tests

**Files:**
- Create: `app/src/main/java/com/mebudget/app/quickspend/QuickSpendSettings.kt`
- Test: `app/src/test/java/com/mebudget/app/quickspend/QuickSpendSettingsTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.mebudget.app.quickspend

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickSpendSettingsTest {

    @Test
    fun `setup is complete only when enabled budget apps and permissions are present`() {
        val incomplete = QuickSpendSettings(
            enabled = true,
            selectedBudgetId = 1,
            selectedAppPackages = setOf("com.bank.app")
        )

        assertFalse(
            incomplete.isSetupComplete(
                overlayPermissionGranted = true,
                usageAccessGranted = false
            )
        )

        assertTrue(
            incomplete.isSetupComplete(
                overlayPermissionGranted = true,
                usageAccessGranted = true
            )
        )
    }

    @Test
    fun `setup is incomplete when disabled or missing selected data`() {
        assertFalse(
            QuickSpendSettings(
                enabled = false,
                selectedBudgetId = 1,
                selectedAppPackages = setOf("com.bank.app")
            ).isSetupComplete(true, true)
        )

        assertFalse(
            QuickSpendSettings(
                enabled = true,
                selectedBudgetId = null,
                selectedAppPackages = setOf("com.bank.app")
            ).isSetupComplete(true, true)
        )

        assertFalse(
            QuickSpendSettings(
                enabled = true,
                selectedBudgetId = 1,
                selectedAppPackages = emptySet()
            ).isSetupComplete(true, true)
        )
    }

    @Test
    fun `selected foreground app match is exact package match`() {
        val settings = QuickSpendSettings(
            enabled = true,
            selectedBudgetId = 1,
            selectedAppPackages = setOf("com.bank.app")
        )

        assertTrue(settings.matchesForegroundPackage("com.bank.app"))
        assertFalse(settings.matchesForegroundPackage("com.other.app"))
        assertFalse(settings.matchesForegroundPackage(null))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.mebudget.app.quickspend.QuickSpendSettingsTest`

Expected: FAIL because `QuickSpendSettings` does not exist.

- [ ] **Step 3: Implement settings model**

```kotlin
package com.mebudget.app.quickspend

data class QuickSpendSettings(
    val enabled: Boolean = false,
    val selectedBudgetId: Long? = null,
    val selectedAppPackages: Set<String> = emptySet()
) {
    fun isSetupComplete(
        overlayPermissionGranted: Boolean,
        usageAccessGranted: Boolean
    ): Boolean {
        return enabled &&
            selectedBudgetId != null &&
            selectedAppPackages.isNotEmpty() &&
            overlayPermissionGranted &&
            usageAccessGranted
    }

    fun matchesForegroundPackage(packageName: String?): Boolean {
        return packageName != null && selectedAppPackages.contains(packageName)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.mebudget.app.quickspend.QuickSpendSettingsTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mebudget/app/quickspend/QuickSpendSettings.kt app/src/test/java/com/mebudget/app/quickspend/QuickSpendSettingsTest.kt
git commit -m "Add quick spend settings model"
```

## Task 2: Settings Persistence

**Files:**
- Create: `app/src/main/java/com/mebudget/app/quickspend/QuickSpendSettingsStore.kt`
- Test: `app/src/test/java/com/mebudget/app/quickspend/QuickSpendSettingsStoreTest.kt`

- [ ] **Step 1: Write store contract test with a fake implementation**

```kotlin
package com.mebudget.app.quickspend

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickSpendSettingsStoreTest {

    @Test
    fun `memory store round trips settings`() {
        val store = InMemoryQuickSpendSettingsStore()
        val expected = QuickSpendSettings(
            enabled = true,
            selectedBudgetId = 42,
            selectedAppPackages = setOf("com.bank.one", "com.bank.two")
        )

        store.save(expected)

        assertEquals(expected, store.load())
    }
}

private class InMemoryQuickSpendSettingsStore : QuickSpendSettingsStore {
    private var current = QuickSpendSettings()

    override fun load(): QuickSpendSettings = current

    override fun save(settings: QuickSpendSettings) {
        current = settings
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.mebudget.app.quickspend.QuickSpendSettingsStoreTest`

Expected: FAIL because `QuickSpendSettingsStore` does not exist.

- [ ] **Step 3: Add SharedPreferences-backed store**

```kotlin
package com.mebudget.app.quickspend

import android.content.Context
import android.content.SharedPreferences

interface QuickSpendSettingsStore {
    fun load(): QuickSpendSettings
    fun save(settings: QuickSpendSettings)
}

class SharedPreferencesQuickSpendSettingsStore(
    context: Context
) : QuickSpendSettingsStore {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "quick_spend_settings",
        Context.MODE_PRIVATE
    )

    override fun load(): QuickSpendSettings {
        val budgetId = preferences.getLong(KEY_BUDGET_ID, NO_BUDGET_ID)
            .takeIf { it != NO_BUDGET_ID }
        return QuickSpendSettings(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            selectedBudgetId = budgetId,
            selectedAppPackages = preferences.getStringSet(KEY_PACKAGES, emptySet()).orEmpty()
        )
    }

    override fun save(settings: QuickSpendSettings) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putLong(KEY_BUDGET_ID, settings.selectedBudgetId ?: NO_BUDGET_ID)
            .putStringSet(KEY_PACKAGES, settings.selectedAppPackages)
            .apply()
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_BUDGET_ID = "selected_budget_id"
        const val KEY_PACKAGES = "selected_app_packages"
        const val NO_BUDGET_ID = -1L
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew testDebugUnitTest --tests com.mebudget.app.quickspend.QuickSpendSettingsStoreTest --tests com.mebudget.app.quickspend.QuickSpendSettingsTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mebudget/app/quickspend/QuickSpendSettingsStore.kt app/src/test/java/com/mebudget/app/quickspend/QuickSpendSettingsStoreTest.kt
git commit -m "Persist quick spend settings"
```

## Task 3: Permission And Foreground App Utilities

**Files:**
- Create: `app/src/main/java/com/mebudget/app/quickspend/QuickSpendPermissions.kt`
- Create: `app/src/main/java/com/mebudget/app/quickspend/ForegroundAppDetector.kt`

- [ ] **Step 1: Add permission helpers**

```kotlin
package com.mebudget.app.quickspend

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object QuickSpendPermissions {
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun overlaySettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun usageAccessSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }
}
```

- [ ] **Step 2: Add foreground detector**

```kotlin
package com.mebudget.app.quickspend

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.concurrent.TimeUnit

class ForegroundAppDetector(
    private val context: Context
) {
    fun currentForegroundPackage(): String? {
        if (!QuickSpendPermissions.hasUsageAccess(context)) return null
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - TimeUnit.MINUTES.toMillis(2),
            now
        )
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }
}
```

- [ ] **Step 3: Run build**

Run: `./gradlew compileDebugKotlin`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mebudget/app/quickspend/QuickSpendPermissions.kt app/src/main/java/com/mebudget/app/quickspend/ForegroundAppDetector.kt
git commit -m "Add quick spend permission utilities"
```

## Task 4: Installed App Picker Source

**Files:**
- Create: `app/src/main/java/com/mebudget/app/quickspend/InstalledAppSource.kt`

- [ ] **Step 1: Add launchable app model and source**

```kotlin
package com.mebudget.app.quickspend

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val icon: Drawable?
)

class InstalledAppSource(
    private val context: Context
) {
    fun loadLaunchableApps(): List<LaunchableApp> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .map { resolveInfo ->
                LaunchableApp(
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(packageManager)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
```

- [ ] **Step 2: Run build**

Run: `./gradlew compileDebugKotlin`

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mebudget/app/quickspend/InstalledAppSource.kt
git commit -m "Add installed app picker source"
```

## Task 5: Quick Spend ViewModel

**Files:**
- Create: `app/src/main/java/com/mebudget/app/ui/QuickSpendViewModel.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/BudgetViewModelFactories.kt`

- [ ] **Step 1: Add ViewModel state and actions**

```kotlin
package com.mebudget.app.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mebudget.app.data.BudgetSummary
import com.mebudget.app.quickspend.InstalledAppSource
import com.mebudget.app.quickspend.LaunchableApp
import com.mebudget.app.quickspend.QuickSpendPermissions
import com.mebudget.app.quickspend.QuickSpendSettings
import com.mebudget.app.quickspend.SharedPreferencesQuickSpendSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuickSpendUiState(
    val settings: QuickSpendSettings = QuickSpendSettings(),
    val budgets: List<BudgetSummary> = emptyList(),
    val launchableApps: List<LaunchableApp> = emptyList(),
    val overlayPermissionGranted: Boolean = false,
    val usageAccessGranted: Boolean = false
) {
    val setupComplete: Boolean
        get() = settings.isSetupComplete(
            overlayPermissionGranted = overlayPermissionGranted,
            usageAccessGranted = usageAccessGranted
        )
}

class QuickSpendViewModel(
    private val application: Application
) : ViewModel() {
    private val repository = application.budgetRepository()
    private val settingsStore = SharedPreferencesQuickSpendSettingsStore(application)
    private val installedAppSource = InstalledAppSource(application)
    private val settings = MutableStateFlow(settingsStore.load())
    private val launchableApps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    private val permissionTick = MutableStateFlow(0)

    val uiState: StateFlow<QuickSpendUiState> = combine(
        settings,
        repository.observeBudgetSummaries(),
        launchableApps,
        permissionTick
    ) { currentSettings, budgets, apps, _ ->
        QuickSpendUiState(
            settings = currentSettings,
            budgets = budgets,
            launchableApps = apps,
            overlayPermissionGranted = QuickSpendPermissions.canDrawOverlays(application),
            usageAccessGranted = QuickSpendPermissions.hasUsageAccess(application)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = QuickSpendUiState(settings = settingsStore.load())
    )

    fun refresh() {
        permissionTick.value += 1
        viewModelScope.launch {
            launchableApps.value = installedAppSource.loadLaunchableApps()
        }
    }

    fun setEnabled(enabled: Boolean) {
        save(settings.value.copy(enabled = enabled))
    }

    fun selectBudget(budgetId: Long?) {
        save(settings.value.copy(selectedBudgetId = budgetId))
    }

    fun toggleApp(packageName: String) {
        val packages = settings.value.selectedAppPackages
        val updated = if (packages.contains(packageName)) {
            packages - packageName
        } else {
            packages + packageName
        }
        save(settings.value.copy(selectedAppPackages = updated))
    }

    fun overlaySettingsIntent(): Intent {
        return QuickSpendPermissions.overlaySettingsIntent(application)
    }

    fun usageAccessSettingsIntent(): Intent {
        return QuickSpendPermissions.usageAccessSettingsIntent()
    }

    private fun save(updated: QuickSpendSettings) {
        settingsStore.save(updated)
        settings.value = updated
    }
}

class QuickSpendViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return QuickSpendViewModel(application) as T
    }
}
```

- [ ] **Step 2: Remove duplicate factory if added elsewhere**

If `BudgetViewModelFactories.kt` already contains factories only, place `QuickSpendViewModelFactory` there instead of keeping it in `QuickSpendViewModel.kt`. The final codebase must define `QuickSpendViewModelFactory` exactly once.

- [ ] **Step 3: Run build**

Run: `./gradlew compileDebugKotlin`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/QuickSpendViewModel.kt app/src/main/java/com/mebudget/app/ui/BudgetViewModelFactories.kt
git commit -m "Add quick spend setup view model"
```

## Task 6: Settings Screen And Navigation

**Files:**
- Create: `app/src/main/java/com/mebudget/app/ui/feature/quickspend/QuickSpendSettingsScreen.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/navigation/MeBudgetNavigation.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/navigation/MeBudgetNavHost.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/app/MeBudgetApp.kt`
- Modify: `app/src/main/java/com/mebudget/app/MainActivity.kt`

- [ ] **Step 1: Add route constant**

In `MeBudgetNavigation.kt`, add:

```kotlin
const val quickSpendSettings = "quick-spend-settings"
```

inside `MeBudgetRoute`.

- [ ] **Step 2: Add settings screen**

```kotlin
package com.mebudget.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun QuickSpendSettingsScreen(
    state: QuickSpendUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectBudget: (Long?) -> Unit,
    onToggleApp: (String) -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenUsageSettings: () -> Unit
) {
    LaunchedEffect(Unit) { onRefresh() }
    var budgetMenuExpanded by remember { mutableStateOf(false) }
    val selectedBudget = state.budgets.firstOrNull { it.id == state.settings.selectedBudgetId }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quick Spend") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Floating quick spend", style = MaterialTheme.typography.titleMedium)
                        Text("Manual expense entry over selected payment apps.")
                    }
                    Switch(
                        checked = state.settings.enabled,
                        onCheckedChange = onToggleEnabled
                    )
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = budgetMenuExpanded,
                    onExpandedChange = { budgetMenuExpanded = !budgetMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBudget?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Quick-spend budget") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(budgetMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = budgetMenuExpanded,
                        onDismissRequest = { budgetMenuExpanded = false }
                    ) {
                        state.budgets.forEach { budget ->
                            DropdownMenuItem(
                                text = { Text(budget.name) },
                                onClick = {
                                    onSelectBudget(budget.id)
                                    budgetMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                PermissionRow(
                    title = "Display over other apps",
                    granted = state.overlayPermissionGranted,
                    buttonLabel = "Allow Floating Button",
                    onClick = onOpenOverlaySettings
                )
            }

            item {
                PermissionRow(
                    title = "Usage Access",
                    granted = state.usageAccessGranted,
                    buttonLabel = "Allow App Detection",
                    onClick = onOpenUsageSettings
                )
            }

            item {
                Text("Selected bank/payment apps", style = MaterialTheme.typography.titleMedium)
            }

            items(state.launchableApps, key = { it.packageName }) { app ->
                val selected = state.settings.selectedAppPackages.contains(app.packageName)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.label)
                        Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onToggleApp(app.packageName) }) {
                        Icon(
                            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (selected) "Selected" else "Not selected"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(if (granted) "Allowed" else "Required")
        }
        if (!granted) {
            Button(onClick = onClick) {
                Text(buttonLabel)
            }
        }
    }
}
```

- [ ] **Step 3: Wire ViewModel through app entry**

In `MainActivity.kt`, create the view model:

```kotlin
val quickSpendViewModel: QuickSpendViewModel = viewModel(
    factory = QuickSpendViewModelFactory(application)
)
```

Pass it into `MeBudgetApp`.

In `MeBudgetApp.kt`, add a `quickSpendViewModel: QuickSpendViewModel` parameter, collect its `uiState`, and pass state/actions into `MeBudgetNavHost`.

- [ ] **Step 4: Add nav item and composable**

In `MeBudgetNavHost.kt`, add `MeBudgetRoute.quickSpendSettings` to `topLevelRoutes`, add a bottom navigation item with `Icons.Default.Settings`, and add:

```kotlin
composable(MeBudgetRoute.quickSpendSettings) {
    QuickSpendSettingsScreen(
        state = quickSpendUiState,
        onBack = { navController.popBackStack() },
        onRefresh = onQuickSpendRefresh,
        onToggleEnabled = onQuickSpendToggleEnabled,
        onSelectBudget = onQuickSpendSelectBudget,
        onToggleApp = onQuickSpendToggleApp,
        onOpenOverlaySettings = onOpenQuickSpendOverlaySettings,
        onOpenUsageSettings = onOpenQuickSpendUsageSettings
    )
}
```

Use `LocalContext.current.startActivity(...)` in `MeBudgetNavHost` for the two permission intents supplied by the ViewModel.

- [ ] **Step 5: Run build**

Run: `./gradlew compileDebugKotlin`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/feature/quickspend/QuickSpendSettingsScreen.kt app/src/main/java/com/mebudget/app/ui/navigation/MeBudgetNavigation.kt app/src/main/java/com/mebudget/app/ui/navigation/MeBudgetNavHost.kt app/src/main/java/com/mebudget/app/ui/app/MeBudgetApp.kt app/src/main/java/com/mebudget/app/MainActivity.kt app/src/main/java/com/mebudget/app/ui/BudgetViewModelFactories.kt
git commit -m "Add quick spend settings screen"
```

## Task 7: Overlay Service Manifest And Lifecycle

**Files:**
- Create: `app/src/main/java/com/mebudget/app/quickspend/QuickSpendOverlayService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add manifest permissions and service**

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />

<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

Inside `<application>` add:

```xml
<service
    android:name=".quickspend.QuickSpendOverlayService"
    android:exported="false" />
```

- [ ] **Step 2: Add service skeleton with polling**

```kotlin
package com.mebudget.app.quickspend

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager
import com.mebudget.app.data.AppDatabase
import com.mebudget.app.data.BudgetRepository

class QuickSpendOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var settingsStore: QuickSpendSettingsStore
    private lateinit var foregroundAppDetector: ForegroundAppDetector
    private lateinit var repository: BudgetRepository
    private val handler = Handler(Looper.getMainLooper())
    private var overlayVisible = false

    private val pollRunnable = object : Runnable {
        override fun run() {
            updateOverlayVisibility()
            handler.postDelayed(this, POLL_INTERVAL_MILLIS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settingsStore = SharedPreferencesQuickSpendSettingsStore(this)
        foregroundAppDetector = ForegroundAppDetector(this)
        repository = AppDatabase.getInstance(this).run {
            BudgetRepository(budgetDao(), walletDao(), transactionDao())
        }
        handler.post(pollRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        hideOverlay()
        super.onDestroy()
    }

    private fun updateOverlayVisibility() {
        val settings = settingsStore.load()
        val setupComplete = settings.isSetupComplete(
            overlayPermissionGranted = QuickSpendPermissions.canDrawOverlays(this),
            usageAccessGranted = QuickSpendPermissions.hasUsageAccess(this)
        )
        val shouldShow = setupComplete &&
            settings.matchesForegroundPackage(foregroundAppDetector.currentForegroundPackage())
        if (shouldShow && !overlayVisible) showOverlay()
        if (!shouldShow && overlayVisible) hideOverlay()
    }

    private fun showOverlay() {
        overlayVisible = true
    }

    private fun hideOverlay() {
        overlayVisible = false
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 1_000L
    }
}
```

- [ ] **Step 3: Run build**

Run: `./gradlew compileDebugKotlin`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mebudget/app/quickspend/QuickSpendOverlayService.kt app/src/main/AndroidManifest.xml
git commit -m "Add quick spend overlay service skeleton"
```

## Task 8: Floating Button And Mini Form

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/quickspend/QuickSpendOverlayService.kt`

- [ ] **Step 1: Implement floating button view**

Replace `showOverlay()` and `hideOverlay()` with an implementation that creates a `FrameLayout` containing a compact `Button` labeled `+`. Use `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` on Android O and newer. Use `FLAG_NOT_FOCUSABLE` while only the floating button is visible.

Required behavior:

- add the button through `windowManager.addView(...)`
- support drag by updating `params.x` and `params.y`
- call `showMiniForm()` when clicked
- remove the overlay view in `hideOverlay()`

- [ ] **Step 2: Implement mini form view**

Inside the service, add `showMiniForm()` that swaps the button view for a vertical `LinearLayout` with:

```kotlin
val amountInput = EditText(this).apply {
    hint = "Amount"
    inputType = InputType.TYPE_CLASS_NUMBER
}
val noteInput = EditText(this).apply {
    hint = "Note"
    inputType = InputType.TYPE_CLASS_TEXT
}
val walletSpinner = Spinner(this)
val saveButton = Button(this).apply { text = "Save" }
val cancelButton = Button(this).apply { text = "Cancel" }
```

Load active wallets for the selected budget with `walletDao.getWalletsForBudget(selectedBudgetId).filterNot { it.archived }` inside a coroutine on `Dispatchers.IO`, then bind names into the spinner on the main thread.

- [ ] **Step 3: Implement save**

On save:

```kotlin
val amount = amountInput.text.toString().trim().toLongOrNull()
if (amount == null || amount <= 0L) {
    amountInput.error = "Enter a valid amount."
    return@setOnClickListener
}
val wallet = activeWallets.getOrNull(walletSpinner.selectedItemPosition)
if (wallet == null) {
    Toast.makeText(this, "Choose a wallet.", Toast.LENGTH_SHORT).show()
    return@setOnClickListener
}
serviceScope.launch {
    val result = repository.addExpense(
        budgetId = selectedBudgetId,
        walletId = wallet.id,
        amount = amount,
        dateEpochDay = LocalDate.now().toEpochDay(),
        note = noteInput.text.toString()
    )
    withContext(Dispatchers.Main) {
        result.onSuccess {
            Toast.makeText(this@QuickSpendOverlayService, "Expense recorded.", Toast.LENGTH_SHORT).show()
            showOverlay()
        }.onFailure {
            Toast.makeText(this@QuickSpendOverlayService, it.message ?: "Something went wrong.", Toast.LENGTH_SHORT).show()
        }
    }
}
```

Add a `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` field and cancel it in `onDestroy()`.

- [ ] **Step 4: Run build**

Run: `./gradlew compileDebugKotlin`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mebudget/app/quickspend/QuickSpendOverlayService.kt
git commit -m "Add quick spend overlay form"
```

## Task 9: Start And Stop Overlay Service From Settings

**Files:**
- Modify: `app/src/main/java/com/mebudget/app/ui/QuickSpendViewModel.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/feature/quickspend/QuickSpendSettingsScreen.kt`

- [ ] **Step 1: Add service control to ViewModel**

Add:

```kotlin
fun syncOverlayService() {
    val currentState = uiState.value
    val intent = Intent(application, QuickSpendOverlayService::class.java)
    if (currentState.setupComplete) {
        application.startService(intent)
    } else {
        application.stopService(intent)
    }
}
```

Import `com.mebudget.app.quickspend.QuickSpendOverlayService`.

Call `syncOverlayService()` after `save(updated)` and inside `refresh()`.

- [ ] **Step 2: Show setup status**

Add a status row near the top of `QuickSpendSettingsScreen`:

```kotlin
Text(
    text = if (state.setupComplete) {
        "Ready over selected apps"
    } else {
        "Complete setup to enable the floating button"
    },
    style = MaterialTheme.typography.bodyMedium
)
```

- [ ] **Step 3: Run build and tests**

Run: `./gradlew testDebugUnitTest compileDebugKotlin`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mebudget/app/ui/QuickSpendViewModel.kt app/src/main/java/com/mebudget/app/ui/feature/quickspend/QuickSpendSettingsScreen.kt
git commit -m "Control quick spend overlay service"
```

## Task 10: Manual Device Verification

**Files:**
- Modify only if verification exposes issues.

- [ ] **Step 1: Install debug app**

Run: `./gradlew installDebug`

Expected: PASS and app installed on the connected Android device or emulator.

- [ ] **Step 2: Verify settings setup**

Manual checks:

- Quick Spend appears as a top-level navigation item.
- Budget picker lists existing budgets.
- App picker lists launchable apps.
- Overlay permission button opens Android overlay settings.
- Usage Access button opens Android usage access settings.
- Setup status changes to ready after budget, app, overlay, usage access, and enabled switch are all configured.

- [ ] **Step 3: Verify overlay behavior**

Manual checks:

- Floating button appears over a selected app.
- Floating button does not appear over an unselected app.
- Button can be dragged.
- Tapping button opens amount, note, wallet, save, and cancel.
- Saving with a valid amount creates an expense for today in the selected budget.
- Saving with blank or zero amount shows "Enter a valid amount."
- Revoking overlay permission hides the overlay.
- Revoking usage access disables bank-app detection.

- [ ] **Step 4: Final verification command**

Run: `./gradlew testDebugUnitTest compileDebugKotlin`

Expected: PASS.

- [ ] **Step 5: Commit fixes from verification**

If verification required code changes:

```bash
git add app/src/main/java/com/mebudget/app app/src/main/AndroidManifest.xml app/src/test/java/com/mebudget/app
git commit -m "Fix quick spend overlay verification issues"
```

If verification required no code changes, do not create an empty commit.

## Self-Review

Spec coverage:

- Manual amount-first quick spend is covered by Tasks 8 and 9.
- Selected bank/payment apps are covered by Tasks 4 and 6.
- Overlay permission and usage access setup are covered by Tasks 3, 6, 7, and 9.
- Existing expense save path is covered by Task 8.
- Safety boundary is preserved by using only package-name detection and manual entry.
- Error handling is covered by Tasks 8 and 10.

Placeholder scan:

- The plan contains no placeholder sections. Each task names exact files, commands, and expected verification results.

Type consistency:

- `QuickSpendSettings`, `QuickSpendSettingsStore`, `QuickSpendPermissions`, `ForegroundAppDetector`, `InstalledAppSource`, `QuickSpendViewModel`, and `QuickSpendOverlayService` names are consistent across tasks.

