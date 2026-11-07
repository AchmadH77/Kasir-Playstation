# Kasir Architecture

This document summarizes the technical decisions behind Kasir (a PlayStation rental billing app): code structure, data model, billing rules, payment and stock flow, notifications, theming, signing, CI, and testing — so a developer (or future me) can understand the app without reading all of the code from scratch.

## Overview

- A single-activity Android app (`MainActivity`); the entire UI is Jetpack Compose.
- 100% offline: all data lives in a local Room database; no network, accounts, or server.
- No ViewModel: composables read DAO `Flow`s directly (`collectAsState`) and write through `rememberCoroutineScope`. Pure calculation logic is separated into `Billing.kt` so it can be unit tested.
- Navigation is a simple `Int` tab state (Unit, Snacks, Reports, More) + `AnimatedContent` with Material 3 Expressive spring transitions, plus one full-screen Unit Management screen that replaces the main shell while open.

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
| `MainActivity.kt` | `KasirApp` (shell, floating navigation dock, theme state, time-up popup), the Unit tab (unit rows, reservations card), and the Start Session / Session Detail / Checkout dialogs |
| `Screens.kt` | The session & snack receipt dialogs + share texts, the Snacks tab (steppers + batch payment with editable quantities), the Reports tab (7-day chart + composition), the More tab (store header, About dialog), and the Unit Management screen |
| `Billing.kt` | Pure logic: elapsed time, package remaining/end, rental cost, reservation & low-stock helpers, currency/date formatting |
| `SessionAlarm.kt` | AlarmManager scheduling (session warning/end, reservation reminders), the notification receivers incl. snooze, and the boot rescheduler |
| `Theme.kt` | The W2 steel-blue light/dark colour schemes for `MaterialExpressiveTheme` |
| `data/Models.kt` | The six Room entities |
| `data/AppDatabase.kt` | `KasirDao`, `AppDatabase`, `DatabaseProvider` (singleton), `SeedData` |

## Data model (Room, version 3)

| Entity | Table | Key columns |
|---|---|---|
| `UnitEntity` | `units` | name (TV 01), console type (PS3/PS4/PS5) |
| `SnackEntity` | `snacks` | name, price, stock |
| `SessionEntity` | `sessions` | unit, mode (PAKET/OPEN legacy), start/end epochs, accumulated pause, package duration, package price, status (RUNNING/PAUSED/FINISHED), rental total, payment method & amount |
| `SessionSnackEntity` | `session_snacks` | session, snack, quantity, price at the time |
| `SnackSaleEntity` | `snack_sales` | snack, quantity, price, timestamp, payment method (direct sales from the Snacks tab) |
| `ReservationEntity` | `reservations` | unit + name snapshot, customer name, start epoch, duration minutes, agreed price |

Migrations: `MIGRATION_1_2` adds the package price (v2); `MIGRATION_2_3` adds the reservations table (v3).

Prices are always stored as snapshots on the transaction rows, so changing a menu price never alters past records. Units no longer carry a typed hourly rate: `Billing.hourlyRateFor` maps the console type to a reference rate (PS3 5000, PS4 8000, PS5 10000) that is used only as a snapshot/fallback for legacy sessions — it is never displayed, because the operator types each session's price by hand.

## Billing rules (`Billing.kt`)

- Elapsed time = (end time or now) − start time − accumulated pause − the currently ongoing pause. Because it is computed from stored epochs, timers stay accurate even if the app is closed or the phone restarts.
- **Package (the only way to start a session)**: the operator picks a duration (cards for 30–300 minutes, or types it) and types a **flat price by hand**. Running past the package duration adds nothing. Adding time extends the duration and adds a manually typed extra price to the package price.
- **Open Billing (legacy)**: sessions created before the package-only change keep rate-based billing — billable minutes = elapsed time rounded **up** to 10-minute blocks, minimum 1 block; cost = hourly rate × billable minutes / 60, rounded to the nearest rupiah.
- Constants: `BLOCK_MINUTES = 10` (legacy), `PAKET_OPTIONS = 30..300` minutes, console types PS3/PS4/PS5, `LOW_STOCK_THRESHOLD = 3`.
- Display helpers: `formatRupiah` (Rp1.500.000), `formatRupiahShort` for chart labels (96 rb, 1,5 jt), `formatMinutes`, `formatHms`, `paketEndEpoch` (start + pauses + package duration), and the period bounds `startOfDay` / `startOfWeek` (Monday) / `startOfMonth` used by Reports.

## Payment & stock flow

- Session checkout: total = rental total + session snacks total. Cash requires the tendered amount to be ≥ the total and shows the change; QRIS is always exact. The payment dialog leads with a navy band showing the total and two large Tunai/QRIS cards. After payment the session becomes FINISHED and a receipt is offered for sharing.
- Cancelling a session restores the stock of all of its snacks, then deletes the session and its snack rows.
- Direct sales in the Snacks tab: the user raises steppers on several items (capped by stock), then pays for all of them in one dialog (Cash/QRIS) where **quantities remain editable** — lowering a quantity to zero removes the row. Each item becomes one `SnackSaleEntity`, its stock is decreased, and a snack receipt dialog closes the sale.
- Snacks added to a session decrease stock immediately; lowering the quantity or cancelling the session returns it. Food is added from a catalog built into the session dialog's Makanan tab (no separate add dialog).
- Low stock: menus with stock ≤ 3 are listed by `Billing.lowStockSnacks`; the dock shows a count badge on the Makanan tab and the tab itself shows a warning card that opens the full list.

## Reservations

- The Unit tab shows a Reservations card with today's upcoming bookings (max 3 + "+N more"). A reservation stores a snapshot of the unit name, the customer, the start time (date + time pickers), a package duration, and an optional price. The booking form is a full-screen page with a navy hero header and a bottom-pinned save button, not a dialog.
- Starting a reservation opens the Start Session form prefilled with its duration and price; starting deletes the reservation. A reminder notification fires 15 minutes before the start (see Notifications).

## Notifications & alarms (`SessionAlarm.kt`)

- While a package session runs, two alarms are scheduled with `AlarmManager`: a **warning 5 minutes before** the package end and the **end alarm** itself. Pausing cancels both; resuming or adding time reschedules them. When exact alarms are not permitted, the code falls back to `setAndAllowWhileIdle`.
- The end notification carries a **"Tunda 10 Menit"** (snooze) action. Posted-alert markers and snooze deadlines live in the `kasir_alarms` SharedPreferences keyed by session end time, so reconciliation or a restart never reposts the same alert; extending a session changes its end time and may legitimately alert again.
- Receivers re-validate against the database before posting (a finished session stays silent), notification taps open the session via an intent extra, and a `BootReceiver` reschedules everything after a restart. Reservation reminders work the same way, 15 minutes before the start.
- Permissions: `POST_NOTIFICATIONS` (requested at runtime on Android 13+), `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`. The More tab has a "Tes Notifikasi" entry that posts a sample end notification.

## Preferences (SharedPreferences)

| File / key | Meaning |
|---|---|
| `kasir_prefs` / `dark_theme` | Dark-mode boolean; an absent key follows the system setting |
| `kasir_prefs` / `store_name` | Store name on receipts and in the More tab header (default "Rental PS") |
| `kasir_alarms` / `end_<id>`, `warn_<id>`, `snooze_<id>` | Notification dedupe markers and snooze deadlines (see Notifications) |

## Theme & design

- The **W2 "Biru Baja"** steel-blue schemes, hand-written in `Theme.kt` (light: primary `#2E5A88`, container `#D3E3F3`; dark: primary `#6FA8DC`, container `#1B3A5C`) with every surface-container role specified explicitly, and a warm amber tertiary for paused/low-stock accents. Brand bands (header, start-session hero, receipt totals, About) use navy `#16324F` in both modes. The Reports food card keeps a warm dark orange as the complementary accent.
- Navigation uses a **floating dock** (`FloatingDock`): a shadowed `surfaceContainerHigh` capsule whose width hugs its content (NB1 layout): only the active tab shows its label, inside a filled `primary` pill that widens to fit the text (animated with `animateContentSize`); the other tabs are icon-only. Tab content gets 120 dp of bottom padding so nothing hides behind the dock.
- Unit rows are monochrome (one surface tone for every state); a running session shows a small status dot (pulsing in the last 5 minutes) and a monospace timer.
- UI language: Indonesian.

## Signing & R8

- The signing keystore is **not in the repo**: CI restores `kasir.p12` from the `KASIR_KEYSTORE_BASE64` Actions secret as the first build step. `signingConfigs.ci` in `app/build.gradle.kts` then uses it for both debug **and** release builds, so every CI APK carries the same signature and updates install over older builds. (The keystore file still exists in old git history; rotating it would change the app signature and force a reinstall, so it is kept and only its distribution is restricted.)
- R8 (`minifyEnabled` + `shrinkResources`) is enabled for both build types. `app/proguard-rules.pro` keeps the `id.kasir.app.data.**` package (Room loads `AppDatabase_Impl`/`KasirDao_Impl` reflectively by name) and the `SourceFile`/`LineNumberTable`/`Signature`/annotation attributes.

## CI (GitHub Actions)

The `.github/workflows/build.yml` workflow is `workflow_dispatch` (run manually), 10 steps:

1. Checkout
2. **Restore signing keystore** — decode the `KASIR_KEYSTORE_BASE64` secret to `kasir.p12`
3. JDK 17 (Temurin)
4. Install Android SDK 37 (`platforms;android-37.0`) + build-tools 36.0.0 via `sdkmanager`
5. Set up Gradle 9.3.1 (`gradle/actions/setup-gradle@v4`, caching on via `org.gradle.caching=true`)
6. **Unit tests** — `gradle :app:testDebugUnitTest`
7. Build the debug APK — `gradle assembleDebug`
8. Build the release APK — `gradle assembleRelease` (embeds the baseline profile from `app/src/release/generated/baselineProfiles/baseline-prof.txt`, so hot code paths are compiled at install time)
9. Upload the `app-debug-apk` artifact
10. Upload the `app-release-apk` artifact — the build to install for daily use

Note: always start a fresh **Run workflow**; *Re-run jobs* rebuilds the old commit.

## Testing

29 unit tests across two files run in CI on every build. `BillingTest` (26 tests) covers the pure logic in `Billing.kt`: currency and short-currency formatting, thousand grouping, block rounding, elapsed time including pauses, package billable minutes + legacy overtime, rental cost for both modes, remaining package time and `paketEndEpoch`, the next-reservation helper, the low-stock helper, day/week/month bounds, and hourly revenue buckets. `BackupFilesTest` (3 tests) covers the backup/restore JSON round-trip.
