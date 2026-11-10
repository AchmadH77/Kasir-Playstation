# Kasir

[![Build APK](https://github.com/Achmadh7/kasir/actions/workflows/build.yml/badge.svg)](https://github.com/Achmadh7/kasir/actions/workflows)

A **PlayStation rental** cashier (billing) app for Android, written in Kotlin + Jetpack Compose with Material 3 Expressive. Data is stored locally on the phone (Room) and the app is fully usable offline; an optional **Online Mode** can mirror it to the owner's own Firebase project as a cloud backup (see [docs/ONLINE.md](docs/ONLINE.md)).

See [CHANGELOG.md](CHANGELOG.md) for notable changes.

## Features

- **Per-unit billing** — one clean, monochrome row per TV: the name and console type, a small status line, and for a running session a status dot plus a big monospace timer or package countdown. Timers are computed from the start time stored in the database, so they stay accurate even when the app is closed. Idle rows simply read "Siap dipakai": no price is shown per unit, because the operator decides the price of every session when it starts.
- **Package sessions with a manual flat price** — starting a session picks a duration from big tappable cards (30 minutes to 5 hours) or types it into the duration field, and the price is typed by hand. The price is flat: running past the package duration adds nothing.
- **Session controls** — the session dialog is split into Sesi / Makanan / Waktu tabs: the running bill, the session's food, and pause & resume (paused time is not billed) plus add-time (duration chips with a manually typed extra price that is added to the package price). Food is added from a catalog built into the Makanan tab — no second dialog covers the session. A session can also be cancelled (snack stock is restored) or stopped for payment.
- **Reservations** — book a unit for a customer with a date & time picker, package duration chips, and an optional price. The Unit tab shows today's upcoming reservations; starting one prefills the session form and removes the reservation, and a system reminder fires 15 minutes before the start time. The booking form opens as a full-screen page.
- **Session-end alerts** — in the last 5 minutes the row's dot pulses and the timer turns red, and when the package ends an in-app popup offers to open the session. System notifications also work while the app is closed: a warning 5 minutes before the end, an end notification with a "Tunda 10 Menit" (snooze) action, and automatic rescheduling after a phone restart.
- **Snacks / F&B** — quick sell from a slim divider-separated list (monochrome initial tile and compact steppers per menu item, add-menu floating button at the bottom right); several items can be sold together in a single payment (quantities stay editable inside the payment dialog), or added to a running session's bill. Stock decreases automatically and quantities cannot exceed stock. A direct sale ends with a payment receipt (success check, hero total, item breakdown) that can be shared as text. Menus whose stock is at 3 or below raise a red count badge on the dock's Makanan tab and a warning card in the Makanan tab.
- **Payment** — Cash (amount tendered + automatic change + exact-amount and denomination shortcuts) or QRIS. Money fields format thousand separators automatically while typing.
- **Reports** — revenue totals per period (today / this week / this month / all time), a bar chart of the last 7 days of revenue, a busiest-hours chart (revenue per hour of day for the selected period, with the peak hour called out), a single hero card holding the whole calculation (total with a comparison chip, Playstation / snacks / expense rows, and the net-profit line; layout K2), two side-by-side mini chart cards (last 7 days and busiest hours), a composition strip (Playstation vs snacks vs expenses), expense logging (a compact single-card list whose section title carries the period total; the entry form follows the selected period), and session history. The two mini chart cards are equal height.
- **Backup & restore** — the whole database exports to a single JSON file through the share sheet, optionally protected with a password (AES-GCM encryption), and can be restored from a file picker after a summary confirmation; restoring replaces the current data atomically. Android's own app backup is disabled, so the database only leaves the phone through this export.
- **Online Mode (optional)** — sign in with Google and every table (units, snacks, sessions, session snacks, snack sales, reservations, expenses) mirrors to Cloud Firestore under your own account: automatic silent sync seconds after each change plus a 6-hour background pass, last-write-wins conflicts, soft deletes, and automatic consolidation when you switch accounts. Guest mode keeps everything local. Setup and data layout: [docs/ONLINE.md](docs/ONLINE.md).
- **Receipt** — session payment details that can be shared (as text) to other apps; the store name is configurable and appears on the receipt.
- **Appearance** — 4 tabs (Unit, Makanan, Laporan, Lainnya) with a floating navigation dock (NB1) where only the active tab shows its label, inside a filled pill that widens to fit it, and the other tabs are icon-only, a steel-blue (W2) light/dark colour scheme, and Material 3 Expressive motion transitions between tabs. The Lainnya tab carries a slim store header with a rename shortcut and a 2x2 grid of shortcut tiles (unit management with the live unit count, backup, restore, notification test), plus dark-mode and About rows.

On first launch the app is seeded with sample data (6 TV units and 6 snack menu items) that can all be edited or deleted.

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.4.10 |
| UI | Jetpack Compose (BOM 2026.09.00), Material3 1.5.0-beta02 (MaterialExpressiveTheme) |
| Database | Room 2.7.2 (KSP 2.3.9), offline-first (v5 adds sync fields); optional Firebase Auth + Firestore mirror |
| Build | AGP 9.1.1, Gradle 9.3.1, JDK 17 |
| Android | minSdk 26 (Android 8.0), targetSdk 36, compileSdk 37 |
| Package | `id.kasir.app` |
| Version | 1.0.0 |
| Signing | The keystore lives in a GitHub Actions secret (`KASIR_KEYSTORE_BASE64`) and is restored to `kasir.p12` at the start of every CI build — every build uses the same signature, so updates install over older versions |
| R8 | Minify + resource shrinking enabled for both debug and release builds |

## Building the APK

**Without a computer (GitHub Actions):** open the **Actions** tab of this repo → the **Build APK** workflow → **Run workflow** on branch `main` → download the **`app-release-apk`** artifact → install it on the phone (an `app-debug-apk` is also produced). The workflow runs the unit tests before building, and the release build embeds a baseline profile for faster startup. (Always start a fresh run; re-running an old run rebuilds that old commit.)

**With Android Studio:** open this repo folder, wait for Gradle Sync, then Build APK. Note: local builds need a `kasir.p12` keystore at the repo root (CI restores it from the secret automatically).

## Project structure

- `app/src/main/java/id/kasir/app/MainActivity.kt` — the main activity, the app shell + floating navigation dock, the Unit tab (unit list + timers + reservations), and the start-session, session-detail, and checkout dialogs.
- `app/src/main/java/id/kasir/app/Screens.kt` — the Makanan, Laporan, and Lainnya tabs, the unit management screen, the About dialog, and the session & snack receipt dialogs.
- `app/src/main/java/id/kasir/app/Billing.kt` — pure billing logic (package pricing, timers, rental cost) and date/currency formatting, including thousand grouping for money inputs.
- `app/src/main/java/id/kasir/app/SessionAlarm.kt` — AlarmManager scheduling and the notification receivers for session warnings, session end (with snooze), and reservation reminders.
- `app/src/main/java/id/kasir/app/Theme.kt` — the W2 steel-blue light/dark colour schemes + the Material 3 Expressive theme.
- `app/src/main/java/id/kasir/app/data/Models.kt` — Room entities (units, snacks, sessions, session snacks, snack sales, reservations).
- `app/src/main/java/id/kasir/app/data/AppDatabase.kt` — the DAO, the Room database (version 3, with migrations), and the seed data.
- `app/src/main/java/id/kasir/app/data/BackupHelper.kt` — whole-database JSON backup/restore and the CSV report builder.
- `app/src/test/java/id/kasir/app/BillingTest.kt` — unit tests for the pure logic in `Billing.kt`.
- `app/src/test/java/id/kasir/app/BackupFilesTest.kt` — backup round-trip tests.
- `app/build.gradle.kts` — app configuration, CI signing, and dependencies.
- `.github/workflows/build.yml` — the manual build workflow (workflow_dispatch): keystore restore, unit tests, then the APK builds.
- `docs/ARSITEKTUR.md` — architecture notes: data model, billing rules, payment & stock flow, notifications, signing, and CI.

## License

All Rights Reserved — see the [LICENSE](LICENSE) file.
