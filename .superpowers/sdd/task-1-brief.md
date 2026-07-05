# Task 1: Theme — Add Brutalist color scheme and wrapper

## Context

This is the first task of a 6-task plan to redesign the MeBudget app with a brutalist aesthetic (white background, black text, red error, 4dp black borders, zero elevation). This task adds the foundational theme tokens that all subsequent tasks will use.

## Files

- Modify: `app/src/main/java/com/mebudget/app/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/mebudget/app/ui/theme/Theme.kt`

## Changes

### 1. Color.kt — Add BrutalistLightColors

After the existing `Overspend = Color(0xFFB24733)` line at the end of the file, add:

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

Add the import `import androidx.compose.material3.lightColorScheme` if not already present (likely it's already imported since `Color.kt` uses it transitively via `Theme.kt` — check first; if `lightColorScheme` isn't imported, add it).

Looking at Color.kt, it only has `import androidx.compose.ui.graphics.Color`. So **add**:
```kotlin
import androidx.compose.material3.lightColorScheme
```

### 2. Theme.kt — Add BrutalistBudgetTheme composable

Add imports at the top (after existing imports):
```kotlin
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalColorScheme
```

After the existing `MeBudgetTheme` function, add:

```kotlin
@Composable
fun BrutalistBudgetTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColorScheme provides BrutalistLightColors,
        content = content
    )
}
```

## Compilation

Run: `./gradlew compileDebugKotlin` — must pass.

Note: `LocalColorScheme` may be deprecated in newer Compose BOM versions. If it's unavailable, use this alternative approach instead:

```kotlin
// Alternative if LocalColorScheme is not available:
@Composable
fun BrutalistBudgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrutalistLightColors,
        typography = androidx.compose.material3.MaterialTheme.typography,
        content = content
    )
}
```

Use whichever compiles.
