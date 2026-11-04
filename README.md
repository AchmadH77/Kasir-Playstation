# Kasir

[![Build APK](https://github.com/Achmadh7/kasir/actions/workflows/build.yml/badge.svg)](https://github.com/Achmadh7/kasir/actions/workflows)

A **PlayStation rental** cashier (billing) app for Android, written in Kotlin + Jetpack Compose with Material 3 Expressive. All data is stored locally on the phone (Room) — no server.

## Features

- **Per-unit billing** — a wide row per TV with a coloured status accent (vacant / playing / paused / overtime) and a running timer or package countdown. Timers are computed from the start time stored in the database, so they stay accurate even when the app is closed.
- **Two play modes** — Package (fixed duration of 1–5 hours, countdown, overtime billed per block) and Open Billing (runs until stopped, rounded up to 10-minute blocks).
- **Session controls** — add time, pause & resume (paused time is not billed), add snacks to the bill, cancel a session (snack stock is restored), stop & pay.
- **Snacks / F&B** — quick sell with steppers and a coloured tile per menu item; several items can be sold together in a single payment, or added to a running session's bill. Stock decreases automatically and quantities cannot exceed stock.
- **Payment** — Cash (amount tendered + automatic change + exact-amount and denomination shortcuts) or QRIS.
- **Reports** — revenue totals per period (today / this week / this month / all time), a bar chart of the last 7 days of revenue, a revenue composition (rental vs snacks) with percentages, best-selling units, and session history.
- **Receipt** — payment details that can be shared (as text) to other apps; the store name is configurable and appears on the receipt.
- **Appearance** — 4 tabs (Unit, Snacks, Reports, More) with a floating navigation dock; the More tab carries a store header, a shortcut to unit & rate management with the live unit count, and a persisted dark/light mode.

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
- `app/src/main/java/id/kasir/app/Screens.kt` — the Snacks, Reports, and More tabs, the unit & rate management screen, and the receipt dialog.
- `app/src/main/java/id/kasir/app/Billing.kt` — pure billing logic (timers, 10-minute block rounding, rental cost) and date/currency formatting.
- `app/src/main/java/id/kasir/app/Theme.kt` — the green light/dark colour schemes + the Material 3 Expressive theme.
- `app/src/main/java/id/kasir/app/data/Models.kt` — Room entities (units, snacks, sessions, session snacks, snack sales).
- `app/src/main/java/id/kasir/app/data/AppDatabase.kt` — the DAO, the Room database, and the seed data.
- `app/src/test/java/id/kasir/app/BillingTest.kt` — 16 unit tests for the pure logic in `Billing.kt`.
- `app/build.gradle.kts` — app configuration, CI signing, and dependencies.
- `.github/workflows/build.yml` — the manual build workflow (workflow_dispatch): unit tests, then the APK build.
- `docs/ARSITEKTUR.md` — architecture notes: data model, billing rules, payment & stock flow, signing, and CI.

## License

MIT — see the [LICENSE](LICENSE) file.
