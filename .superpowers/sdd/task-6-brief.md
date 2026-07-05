# Task 6: Cleanup unused imports

## Context

Final cleanup task for the brutalist budget redesign. After Tasks 1-5, some files may have unused imports that need to be removed.

## Files to check

- `app/src/main/java/com/mebudget/app/ui/feature/budgets/BudgetsScreen.kt`
- `app/src/main/java/com/mebudget/app/ui/feature/budgetdetail/BudgetDetailScreens.kt`
- `app/src/main/java/com/mebudget/app/ui/common/CommonUi.kt`

## Changes

1. Run `./gradlew compileDebugKotlin` first to confirm the build is already green
2. For each file, identify any imports that are unused:
   - Check if the import is referenced elsewhere in the file
   - Remove unused imports only if they are clearly not used
3. After removing imports, rebuild and fix any mistakes

## Tip

Some imports may generate warnings but are still needed by the Kotlin compiler (e.g., utility extensions). Only remove an import if:
- You've confirmed the symbol it imports is not used in the file
- The file compiles without it

## Compilation

Run `./gradlew compileDebugKotlin` (timeout 300s) and ensure BUILD SUCCESSFUL.

## Commit

Use message: `chore: remove unused imports after brutalist refactor`
