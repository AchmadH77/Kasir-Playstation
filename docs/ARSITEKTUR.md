# Kasir Architecture

This document summarizes the technical decisions behind Kasir (a PlayStation rental billing app): code structure, data model, billing rules, payment and stock flow, theming, signing, CI, and testing — so a developer (or future me) can understand the app without reading all of the code from scratch.

## Overview

- A single-activity Android app (`MainActivity`); the entire UI is Jetpack Compose.
- 100% offline: all data lives in a local Room database; no network, accounts, or server.
- No ViewModel: composables read DAO `Flow`s directly (`collectAsState`) and write through `rememberCoroutineScope`. Pure calculation logic is separated into `Billing.kt` so it can be unit tested.
- Navigation is a simple `Int` tab state (Unit, Snacks, Reports, More) + `AnimatedContent` (300 ms fade), plus one full-screen Unit & Rate Management screen that replaces the main shell while open.

## Stack

| Component | Version / notes |
|---|---|
| Kotlin | 2.4.10 |
| Compose BOM | 2026.09.00 |
| Material 3 | 1.5.0-beta02 (`MaterialExpressiveTheme`, `MotionScheme.expressive`) |
| AGP / Gradle | 9.1.1 / 9.3.1 (JDK 17) |
| Room | 2.7.2 via KSP 2.3.9 |
| Android | minSdk 26, targetSdk 36, compileSdk 37 |
| Package | `id.kasir.app`, version 1.0.0 (code 1) |

## Code structure

| File | Contents |
|---|---|
| `MainActivity.kt` | `KasirApp` (shell, floating navigation dock, theme state), the Unit tab, and the Start Session / Session Detail / Add Snack / Checkout dialogs |
| `Screens.kt` | The receipt dialog + `receiptText()`, the Snacks tab (steppers + batch payment), the Reports tab (7-day chart + composition), the More tab (store header), and the Unit & Rate Management screen |
| `Billing.kt` | Pure logic: elapsed time, block rounding, rental cost, remaining package time, currency/date formatting |
| `Theme.kt` | The green light/dark colour schemes for `MaterialExpressiveTheme` |
| `data/Models.kt` | The five Room entities |
| `data/AppDatabase.kt` | `KasirDao`, `AppDatabase`, `DatabaseProvider` (singleton), `SeedData` |

## Data model (Room, version 1)

| Entity | Table | Key columns |
|---|---|---|
| `UnitEntity` | `units` | name (TV 01), console type (PS3/PS4/PS5), hourly rate |
| `SnackEntity` | `snacks` | name, price, stock |
| `SessionEntity` | `sessions` | unit, mode (PAKET/OPEN), start/end epochs, accumulated pause, package duration, status (RUNNING/PAUSED/FINISHED), rental total, payment method & amount |
| `SessionSnackEntity` | `session_snacks` | session, snack, quantity, price at the time |
| `SnackSaleEntity` | `snack_sales` | snack, quantity, price, timestamp, payment method (direct sales from the Snacks tab) |

Prices are always stored as snapshots on the transaction rows, so changing a menu price never alters past records.

## Billing rules (`Billing.kt`)

- Elapsed time = (end time or now) − start time − accumulated pause − the currently ongoing pause. Because it is computed from stored epochs, timers stay accurate even if the app is closed or the phone restarts.
- **Open Billing**: billable minutes = elapsed time rounded **up** to 10-minute blocks, minimum 1 block.
- **Package**: the package duration is the base; time beyond the package is billed with the same 10-minute block rounding. Package prices are linear from the hourly rate (there is no separate package price table).
- Rental cost = hourly rate × billable minutes / 60, rounded to the nearest rupiah.
- Constants: `BLOCK_MINUTES = 10`, package options 60–300 minutes, console types PS3/PS4/PS5.
- Display helpers: `formatRupiah` (Rp1.500.000), `formatRupiahShort` for chart labels (96 rb, 1,5 jt), `formatMinutes`, `formatHms`, and the period bounds `startOfDay` / `startOfWeek` (Monday) / `startOfMonth` used by Reports.

## Payment & stock flow

- Session checkout: total = rental total + session snacks total. Cash requires the tendered amount to be ≥ the total and shows the change; QRIS is always exact. After payment the session becomes FINISHED and a receipt is offered for sharing.
- Cancelling a session restores the stock of all of its snacks, then deletes the session and its snack rows.
- Direct sales in the Snacks tab: the user raises steppers on several items (capped by stock), then pays for all of them in one dialog (Cash/QRIS). Each item becomes one `SnackSaleEntity` and its stock is decreased.
- Snacks added to a session decrease stock immediately; lowering the quantity or cancelling the session returns it.

## Preferences (SharedPreferences `kasir_prefs`)

| Key | Meaning |
|---|---|
| `dark_theme` | Dark-mode boolean; an absent key follows the system setting |
| `store_name` | Store name on receipts and in the More tab header (default "Rental PS") |

## Theme & design

- Hand-written green schemes (light: primary `#146C43`, container `#B7E4C7`; dark: primary `#7BD88F`, container `#14532D`) under `MaterialExpressiveTheme`.
- Navigation uses a **floating dock** (`FloatingDock`): a shadowed `surfaceContainerHigh` capsule where the active tab is a solid `primary` pill with icon + label and the other tabs are icon-only. Tab content gets 96 dp of bottom padding so nothing hides behind the dock.
- UI language: Indonesian.

## Signing & R8

- A fixed keystore `kasir.p12` (PKCS12) sits at the repo root; `signingConfigs.ci` in `app/build.gradle.kts` uses it for both debug **and** release builds, so every CI APK carries the same signature and updates install over older builds.
- R8 (`minifyEnabled` + `shrinkResources`) is enabled for both build types. `app/proguard-rules.pro` keeps the `id.kasir.app.data.**` package (Room loads `AppDatabase_Impl`/`KasirDao_Impl` reflectively by name) and the `SourceFile`/`LineNumberTable`/`Signature`/annotation attributes.

## CI (GitHub Actions)

The `.github/workflows/build.yml` workflow is `workflow_dispatch` (run manually), 9 steps:

1. Checkout
2. JDK 17 (Temurin)
3. Install Android SDK 37 (`platforms;android-37.0`) + build-tools 36.0.0 via `sdkmanager`
4. Set up Gradle 9.3.1 (`gradle/actions/setup-gradle@v4`, caching on via `org.gradle.caching=true`)
5. **Unit tests** — `gradle :app:testDebugUnitTest`
6. Build the debug APK — `gradle assembleDebug`
7. Build the release APK — `gradle assembleRelease` (embeds the baseline profile from `app/src/release/generated/baselineProfiles/baseline-prof.txt`, so hot code paths are compiled at install time)
8. Upload the `app-debug-apk` artifact
9. Upload the `app-release-apk` artifact — the build to install for daily use

Note: always start a fresh **Run workflow**; *Re-run jobs* rebuilds the old commit.

## Testing

`BillingTest` (JUnit 4, 16 tests) covers the pure logic in `Billing.kt`: currency and short-currency formatting, block rounding, elapsed time including pauses, package billable minutes + overtime, rental cost for both modes, remaining package time, and day/week/month bounds. The tests run in CI on every build.
