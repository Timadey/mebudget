# Task 5: Update CommonUi.kt — EmptyState

## Context

Part of a brutalist budget redesign. The `EmptyState` composable is used across both budgets list and budget detail screens. It needs to match the brutalist card style (hard border, white fill, zero elevation, Black 900 text).

## Files

- Modify: `app/src/main/java/com/mebudget/app/ui/common/CommonUi.kt`

## Changes

Replace the `EmptyState` composable with:

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
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(4.dp, Color.Black)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = Color.Black.copy(alpha = 0.6f)
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(actionLabel.uppercase(), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}
```

Add these imports at the top of the file (if not already present):
- `import androidx.compose.foundation.BorderStroke`
- `import androidx.compose.ui.graphics.Color` (likely already imported)

Keep all other composables in the file unchanged.

## Compilation

Run: `./gradlew compileDebugKotlin` (timeout 300s).
