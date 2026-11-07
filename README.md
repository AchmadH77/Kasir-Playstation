# Kasir

[![Build APK](https://github.com/Achmadh7/kasir/actions/workflows/build.yml/badge.svg)](https://github.com/Achmadh7/kasir/actions/workflows)

A **PlayStation rental** cashier (billing) app for Android, written in Kotlin + Jetpack Compose with Material 3 Expressive. All data is stored locally on the phone (Room) — no server.

## Features

- **Per-unit billing** — a wide row per TV with a coloured status accent (vacant / playing / paused / overtime) and a running timer or package countdown. Timers are computed from the start time stored in the database, so they stay accurate even when the app is closed. Idle rows simply read "Siap dipakai": no price is shown per unit, because the operator decides the price of every session when it starts.
- **Package sessions with a manual flat price** — starting a session picks a duration from big tappable cards (1–5 hours) or types any whole number of hours directly into the duration field, and the price is typed by hand. The price is flat: running past the package duration adds nothing.
- **Session controls** — the session dialog is split into Sesi / Makanan / Waktu tabs: the running bill, the session's food, and pause & resume (paused time is not billed) plus add-time (duration chips with a manually typed extra price). Food is added from a catalog built into the Makanan tab — no second dialog covers the session. A session can also be cancelled (snack stock is restored) or stopped for payment.
- **Snacks / F&B** — quick sell with steppers and a coloured tile per menu item; several items can be sold together in a single payment, or added to a running session's bill. Stock decreases automatically and quantities cannot exceed stock. A direct sale ends with a payment receipt (success check, hero total, item breakdown) that can be shared as text.
- **Payment** — Cash (amount tendered + automatic change + exact-amount and denomination shortcuts) or QRIS. Money fields format thousand separators automatically while typing.
- **Reports** — revenue totals per period (today / this week / this month / all time), a bar chart of the last 7 days of revenue, a revenue composition (rental vs snacks) with percentages, best-selling units, and session history. Reports can be exported as CSV.
- **Backup & restore** — the whole database exports to a single JSON file through the share sheet and can be restored from a file picker after a summary confirmation; restoring replaces the current data atomically.
- **Receipt** — session payment details that can be shared (as text) to other apps; the store name is configurable and appears on the receipt.
- **Appearance** — 4 tabs (Unit, Makanan, Laporan, Lainnya) with a floating navigation dock and Material 3 Expressive motion transitions between tabs. The Lainnya tab carries a store header, a shortcut to unit management with the live unit count, backup & restore, and a persisted dark/light mode.

On first launch the app is seeded with sample data (6 TV units and 6 snack menu items) that can all be edited or deleted.

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.4.10 |
| UI | Jetpack Compose (BOM 2026.09.00), Material3 1.5.0-beta02 (MaterialExpressiveTheme) |
| Database | Room 2.7.2 (KSP 2.3.9), fully offline |
| Build | AGP 9.1.1, Gradle 9.3.1, JDK 17 |
| Android | minSdk 26 (Android 8.0), targetSdk 36, compileSdk 37 |
| Package | `id.kasir.app` |
| Version | 1.0.0 |
| Signing | A fixed keystore in the repo (`kasir.p12`, PKCS12) — every CI build uses the same signature, so updates install over older versions |
| R8 | Minify + resource shrinking enabled for both debug and release builds |

## Building the APK

**Without a computer (GitHub Actions):** open the **Actions** tab of this repo → the **Build APK** workflow → **Run workflow** on branch `main` → download the **`app-release-apk`** artifact → install it on the phone (an `app-debug-apk` is also produced). The workflow runs the unit tests before building, and the release build embeds a baseline profile for faster startup. (Always start a fresh run; re-running an old run rebuilds that old commit.)

**With Android Studio:** open this repo folder, wait for Gradle Sync, then Build APK.

## Project structure

- `app/src/main/java/id/kasir/app/MainActivity.kt` — the main activity, the app shell + floating navigation dock, the Unit tab (unit list + timers), and the start-session, session-detail, and checkout dialogs.
- `app/src/main/java/id/kasir/app/Screens.kt` — the Makanan, Laporan, and Lainnya tabs, the unit management screen, and the session & snack receipt dialogs.
- `app/src/main/java/id/kasir/app/Billing.kt` — pure billing logic (package pricing, timers, rental cost) and date/currency formatting, including thousand grouping for money inputs.
- `app/src/main/java/id/kasir/app/Theme.kt` — the green light/dark colour schemes + the Material 3 Expressive theme.
- `app/src/main/java/id/kasir/app/data/Models.kt` — Room entities (units, snacks, sessions, session snacks, snack sales).
- `app/src/main/java/id/kasir/app/data/AppDatabase.kt` — the DAO, the Room database, and the seed data.
- `app/src/main/java/id/kasir/app/data/BackupHelper.kt` — whole-database JSON backup/restore and the CSV report export.
- `app/src/test/java/id/kasir/app/BillingTest.kt` — unit tests for the pure logic in `Billing.kt`.
- `app/src/test/java/id/kasir/app/BackupFilesTest.kt` — backup round-trip tests.
- `app/build.gradle.kts` — app configuration, CI signing, and dependencies.
- `.github/workflows/build.yml` — the manual build workflow (workflow_dispatch): unit tests, then the APK build.
- `docs/ARSITEKTUR.md` — architecture notes: data model, billing rules, payment & stock flow, signing, and CI.

## License

All Rights Reserved — see the [LICENSE](LICENSE) file.
