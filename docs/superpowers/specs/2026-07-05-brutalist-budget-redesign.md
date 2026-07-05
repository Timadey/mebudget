# Brutalist Budget Redesign

A full visual redesign of the MeBudget budgets list and budget detail screens
in a brutalist aesthetic.

## Design Direction

Brutalist (raw, confrontational, intentionally uncomfortable, maximum contrast).
One weight (Black 900), two colors (white / black), one accent (red for negative
amounts). Hard 4dp borders replace all shadows and elevation. Zero rounded
corners on cards; 24dp pill radius on buttons.

## Palette

| Token               | Value      |
|---------------------|------------|
| `background`        | `#FFFFFF`  |
| `surface`           | `#FFFFFF`  |
| `onSurface`         | `#000000`  |
| `primary`           | `#000000`  |
| `error`             | `#FF0000`  |
| `surfaceVariant`    | `#F5F5F5`  |

No secondary, tertiary, or other accent colors. Positive amounts are black,
negative amounts are `#FF0000`. Status dots: green `#00AA00` / amber
`#FF8800` / red `#FF0000`.

## Typography

Single weight: **Black 900** using the existing Outfit font (loaded in
`Type.kt`). All sizes are based on the font weight for emphasis, so
readability comes from size contrast rather than weight variation:

| Role             | Size   | Style              |
|------------------|--------|--------------------|
| Hero balance     | 56sp   | `headlineLarge`    |
| Section headers  | 24sp   | `titleLarge`       |
| Card content     | 16sp   | `bodyLarge` / `bodyMedium` |
| Labels / status  | 12sp   | `labelSmall`, `letterSpacing 2sp` |
| Date dividers    | 11sp   | with overline-ish treatment |

## Shapes

- Cards: `RoundedCornerShape(0.dp)` — hard edges
- Buttons: `RoundedCornerShape(24.dp)` — exaggerated pill
- Chips / tags: `RoundedCornerShape(0.dp)`
- Dialogs: hard 4dp black border, no rounded corners

## Spacing

- Screen padding: `20.dp` horizontal, `8.dp` top/bottom
- Card padding: `20.dp` inner
- Inter-card gap: `12.dp`
- Title-to-content gap: `8.dp`

## Card Definition

Every card uses:
```
border = BorderStroke(4.dp, Color.Black)
elevation = 0.dp
shadow = none
colors = CardDefaults.cardColors(containerColor = Color.White)
```

No tonal fills, no elevation shadows, no gradient backgrounds.

## Screens

### 1. Budgets List (refactor of `BudgetsScreen`)

**Header.** No app bar. The header is raw text:

```
■ BUDGETS ■

1     $12,450.00
4 active wallets
──────────────────────────────────────
```

- "1" is the count, blown up to 56sp Black, left-aligned
- `$12,450.00` right-aligned on same line
- Thin hairline (`1.dp` black) full-width below, no margin

**Budget row.** Each BudgetSummaryCard becomes a hard rectangle card:

```
┌──────────────────────────────────────┐
│ MARCH BUDGET                         │
│ 03/01 – 03/31              ● ON TRACK│
│ $5,200.00                        [>] │
└──────────────────────────────────────┘
```

- `●` is filled black/green/red
- `[>]` is a button (24dp pill, black border, no fill) with a thick `>` character
- No delete/duplicate/other icons visible — `[>]` opens sub-actions (Edit / Duplicate / Delete as text rows)

**Empty state.** Full-width black-bordered card, centered text:

```
No budgets yet
Create your first budget.
┌──────────────────────────────────────┐
│           + CREATE BUDGET            │
└──────────────────────────────────────┘
```

**"New" button.** A full-width black filled pill at the bottom. White text: `+ CREATE BUDGET`.

**Dialogs (`AlertDialog` variants).** Hard 4dp black border. `containerColor = White`. Title all-caps 24sp. Buttons are plain text, no fill.

### 2. Budget Detail (refactor of `BudgetOverviewScreen`)

**Top bar.** No app bar. Back is a chunky `[<]` text button. Title is the budget name. Privacy toggle is a single letter: `[P]` (privacy on) / `[p]` (privacy off). Settings is `[S]`.

**Section switcher.** Three chunky buttons in a row, each with 4dp black border:

```
┌──────────┐ ┌──────────┐ ┌────────────┐
│  WALLETS  │ │ ACTIVITY │ │  INSIGHTS  │
└──────────┘ └──────────┘ └────────────┘
```

Active = filled black (white text), inactive = white (black text). Instant swap, no animation.

**Budget status card.** A hard rectangle showing remaining balance:

```
┌──────────────────────────────────────┐
│  REMAINING                           │
│  $5,450.00                           │
│  Planned $12,000             45% ███░│
│  ██████████████░░░░░░░░░░░░░░░░░░░░░  │
└──────────────────────────────────────┘
```

The progress line is 4dp tall `█` block characters in black, `░` for unfilled.

**BudgetActionStrip.** Removed. Action buttons are inline with wallet content.

**Wallet rows.** Each wallet in the list is a hard card:

```
┌──────────────────────────────────────┐
│ GROCERIES                            │
│ Planned $1,200               66%     │
│ Balance $800                  ███░░  │
└──────────────────────────────────────┘
```

Progress is 3dp block characters. `●` status dot placed on the right edge of the header line.

**Activity tab.** Plain list of transactions separated by `── 07/03 ──` date divider texts. Each transaction is a single `Row` — no card, no border, no icon box. Just columns:

```
07/03  Safeway              -$45.20  [EXP]
07/02  Transfer → Gas       +$200.00 [TRF]
06/30  Adjustment           -$10.00  [ADJ]
```

Type tags `[EXP]`, `[TRF]`, `[ADJ]` are black-border pill chips.

**Insights tab.** Refactor of `BudgetInsightSection` — same hard-card style with 4dp borders.

### 3. Wallet Detail (refactor of `WalletDetailScreen`)

No app bar. Back is `[<]` text. Wallet name is plain 24sp. `[⋮]` opens a text dropdown (Edit / Archive / Delete).

```
[<]  GROCERIES                      [⋮]

$ 8 0 0 . 0 0

Planned $1,200           66%
████████░░░░░░░░░░░░

[ ADD EXPENSE ]    [ MOVE MONEY ]

── 07/03 ──
Safeway               -$45.20
── 07/02 ──
Transfer to Gas       +$200.00
── 06/30 ──
Adjustment            -$10.00
```

- The hero balance has character spacing (`$ 8 0 0 . 0 0`) like a gas station price sign, 56sp Black
- Progress bar is 6dp tall block characters
- Action buttons are full-height pills with 4dp black border, black text on white
- History is same raw row format as Activity tab

### 4. Forms (Expense / Transfer / Adjustment Bottom Sheets)

The standard `ModalBottomSheet` gets brutalist treatment:

- No drag handle — replaced by a 4dp tall full-width black bar at the top
- Title is the sheet title (`ADD EXPENSE`, `MOVE MONEY`, `ADJUSTMENT`) at 24sp Black
- Inputs use a bottom-border style: 1dp black line beneath the label, no outlined box
- Labels are all-caps `10sp` with `letterSpacing 2sp`
- Amount input is `28sp Black`, centered or prominent
- Note is an optional expandable section
- Save button: full-width black filled pill with white `SAVE` text
- Cancel: plain text link below the button

Wallet dropdown uses Material's `ExposedDropdownMenuBox` styled with hard borders.

---

## Changes Not Covered

The following are out of scope for this redesign:
- Data layer, ViewModel, or domain logic changes
- Database schema or migrations
- Navigation structure (nav host, bottom nav bar)
- Budget creation / deletion flow logic (visual only)
- Navigation bar / privacy mode toggle visual treatment (handled in separate nav redesign)
- `Gradients.kt` (no longer needed after removing LinearProgressIndicator from budget screens)
- `CommonUi.kt` (EmptyState, PrivacyModeBanner, etc.)

## Implementation Order

1. **Theme & Tokens** — update `Type.kt`, `Color.kt` (or Theme.kt) to set brutalist
   defaults. Remove any color values that add unwanted accent colors.
2. **Budgets list** — `BudgetSummaryCard` + `TotalSummarySection` → hard borders,
   no shadow, new layout, remove icons, replace with text actions
3. **Budget detail** — `BudgetStatusCard`, `WalletCard`, section switcher,
   activity tab transaction rows (remove card wrapper)
4. **Wallet detail** — `WalletSummaryPanel` (remove hero card, giant spaced
   number), `WalletActionStrip`, history rows
5. **Forms** — bottom sheets for expense/transfer/adjustment with brutalist
   styling
6. **Cleanup** — remove unused `BorderStroke` / `GradientProgressBar` references
   from budget screens, clean imports
