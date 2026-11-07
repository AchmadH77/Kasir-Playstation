# Changelog

All notable changes to Kasir are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); the app itself
is version 1.0.0. A GitHub Release will be cut from the next verified
build, and this file's Unreleased section will roll into it.

## [Unreleased]

### Added

- Expense logging (database v4): record operating expenses from Reports —
  the hero card gains a net-profit line, an expense card and list follow
  the selected period, and the CSV export includes expenses and net
  profit. Expenses are part of backups too.
- Busiest Hours chart in Reports: revenue per hour of day (00-23) for the
  selected period, with the peak hour highlighted and called out.
- Reservations: book a unit ahead with date/time, package duration and an
  optional price; starting a session from a reservation prefills the form,
  and a system reminder fires 15 minutes before the start time.
- Session notifications: a warning 5 minutes before a package ends and an
  alert when it ends, with a 10-minute snooze action.
- Low-stock warnings for snack menus whose stock is at 3 or below: a count
  badge on the dock's Makanan icon, a warning card, and a detail list.
- Reports compare the selected period with the previous one (LP10): a
  comparison chip on the total and a previous-value line on each card.

### Changed

- Reports: the "Makanan langsung" card now uses exactly the Playstation
  card's colours in both themes, and the unit rows' "Mulai" button is a
  filled navy button matching the tab header band.
- Makanan tab redesigned as a slim divider-separated list (MK2) with
  monochrome initial tiles and compact steppers; the add-menu button is
  now a floating button at the bottom right.
- Floating dock slimmed to a pill (NB1): only the active tab shows its
  label, and the pill widens with an animation when the tab changes.
- Lainnya tab reorganized as a shortcut grid (LN3) under a slim store
  header.
- Checkout dialog shows a hero total with large Tunai/QRIS cards (PB1);
  snacks inside the session dialog are filled cards with big add buttons
  (SM5); the reservation form is a full-screen page (RV5).
- Theme switched to a hand-built steel-blue scheme (W2) in both light and
  dark modes.
- Reports: the "Unit Terlaris" section and the per-unit CSV block were
  removed, and "Sewa & sesi" was renamed to "Playstation".

### Security

- The signing keystore no longer lives in the repository tree; CI restores
  it from the `KASIR_KEYSTORE_BASE64` repository secret at build time. The
  key remains in older git history (see docs/ARSITEKTUR.md).
- Android backup of the app's data is disabled (`allowBackup=false`), so
  the database cannot be copied out through device backup; the in-app JSON
  backup remains the way to move data.
- Backups can optionally be protected with a password: the JSON is wrapped
  in an AES-GCM envelope whose key is derived from the password (PBKDF2).
  Plain backups, including older files, remain supported.
