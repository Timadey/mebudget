# Overlay Quick Spend Design

## Problem

MeBudget currently depends on the user remembering to record every expense. When the user sends money from a bank app and forgets to record it, the budget balance diverges from the bank balance. Once that happens, the budget app becomes harder to trust.

The feature should reduce forgetting by making expense capture available directly during the banking/payment flow. The app should remain a manual budget tool. It must not read bank app screens, capture credentials, automate payments, or create transactions without explicit user input.

## Goals

- Let the user record spending quickly before or after sending money from a bank/payment app.
- Make the fastest path amount-first: amount, optional note, wallet, save.
- Show a floating MeBudget entry point only over user-selected bank/payment apps.
- Reuse the existing expense transaction path so wallet balances and validation rules remain consistent.
- Provide clear setup for Android overlay and usage access permissions.

## Non-Goals

- Automatic bank transaction import.
- Reading bank app balances or transaction details.
- Accessibility scraping.
- Password, PIN, OTP, or card data capture.
- Payment automation.
- Bank-specific integrations.

## Core Flow

1. User configures an active budget for quick spend.
2. User selects bank/payment apps from installed launchable apps.
3. User grants Display over other apps permission.
4. User grants Usage Access permission.
5. When a selected app is in the foreground, MeBudget shows a small floating button.
6. User taps the floating button to open a compact quick-spend form.
7. User enters amount, optional note, chooses wallet, and saves.
8. MeBudget creates a normal expense for today in the selected budget.
9. The form closes and wallet balances update through existing budget detail observation.

## Settings And Permissions

Add a Quick Spend settings area that controls the feature.

Settings store:

- selected quick-spend budget id
- selected bank/payment app package names
- overlay quick spend enabled state

The settings screen shows setup status:

- active budget selected
- at least one app selected
- overlay permission granted
- usage access granted

The feature remains disabled until required setup is complete.

Display over other apps permission is required to draw the floating button and mini form above other apps. The app should open Android's system overlay permission page for MeBudget when the user taps the setup action.

Usage Access is required to know which app is currently in front. MeBudget uses it only to compare the foreground package name with the user-selected package names. If Usage Access is missing, bank-app-only display should be disabled. A later fallback can support an always-visible floating button, but that is outside the first implementation.

## App Selection

MeBudget should not hardcode bank apps as the source of truth. Users may use different Nigerian banks, fintech apps, payment apps, or package variants.

The app picker should list installed launchable apps with:

- app name
- package name
- app icon if practical
- selected/unselected state

The user manually selects the apps where the floating button should appear. Selected package names are saved locally.

## Overlay Behavior

The overlay service owns a small floating MeBudget button.

Button behavior:

- appears only when overlay quick spend is enabled
- appears only when the foreground app package is selected
- is draggable so it can be moved away from important bank app controls
- stays compact and unobtrusive
- opens the mini form when tapped

Mini form fields:

- amount
- note
- wallet
- save
- cancel

The amount field should be focused first. Date defaults to today. The form should not include extra fields in the first version because speed is the main product requirement.

## Save Behavior

Saving quick spend calls the existing repository expense path:

`BudgetRepository.addExpense(budgetId, walletId, amount, todayEpochDay, note)`

This keeps quick-spend transactions consistent with normal expenses:

- same Room transaction model
- same validation rules
- same negative balance behavior
- same budget detail and wallet balance calculations
- same transaction history display

## Error Handling

The mini form should surface short errors:

- invalid amount: "Enter a valid amount."
- no wallet selected: "Choose a wallet."
- no active budget configured: open Quick Spend settings
- no wallets in active budget: open the active budget setup path
- overlay permission revoked: stop the overlay and show disabled setup state
- usage access revoked: stop foreground-app detection and show disabled setup state
- negative-balance validation failure: reuse the existing validation message

The overlay should fail closed. If permissions or setup become invalid, it should disappear instead of showing in the wrong context.

## Safety And Privacy Boundaries

The overlay must be manual-entry only.

MeBudget must not:

- inspect bank app UI content
- read balances from another app
- read transaction references from another app
- capture screenshots
- log password, PIN, OTP, or card entry fields
- automate bank app taps
- create expenses from detected bank activity

The settings screen should explain that the floating button is only for manual expense recording and does not connect to or read bank apps.

## Testing

Unit tests:

- settings completion logic
- selected app matching against foreground package
- quick-spend draft validation where logic is extracted from UI
- repository save path remains normal expense behavior

Manual Android verification:

- setup shows missing permission states correctly
- overlay permission flow returns to the app cleanly
- usage access flow returns to the app cleanly
- floating button appears only over selected apps
- floating button does not appear over unselected apps
- draggable button position persists during service lifetime
- quick-spend save creates an expense for today
- errors display for invalid amount and missing wallet
- revoking overlay permission hides the overlay
- revoking usage access disables bank-app detection

