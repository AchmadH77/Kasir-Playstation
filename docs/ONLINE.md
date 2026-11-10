# Online Mode (optional Firebase sync)

Kasir is offline-first: the Room database on the phone is always the
source of truth and the app works fully without internet. Online Mode
adds an optional per-account cloud mirror in Cloud Firestore, using the
same design as KasBulanan and Apotek Lestari:

- Sign in with Google (or skip and stay a guest;
  guest data lives only on the phone).
- Every row of the seven tables carries sync fields (`remoteId`,
  `updatedAt`, soft-delete tombstone, dirty flag; database v5).
- Writes sync automatically about 5 seconds after the last change
  (silently), on sign-in, from the header sync chip or the Lainnya tab,
  and periodically every 6 hours in the background.
- Conflicts resolve last-write-wins on `updatedAt`; an exact tie goes
  to the deletion, so deletes never resurrect.
- Switching accounts re-uploads the full local state into the newly
  signed-in account, so data never splits across accounts.
- Restoring a JSON backup marks everything dirty, so the restored
  state syncs up into the signed-in account.

The whole layer is dormant until a Firebase config file exists: the
google-services Gradle plugin is only applied when
`app/google-services.json` is present, and every Firebase call in the
app is guarded, so builds without the file behave exactly like the
offline app.

## One-time setup (Firebase Console)

1. Open the [Firebase Console](https://console.firebase.google.com/)
   and pick a project. Kasir may share one project with KasBulanan /
   Apotek Lestari - the data stays separate because each app uses its
   own collection names under `users/{uid}/`.
2. Add an Android app with package name **`id.kasir.app`**.
3. In that app's settings, add the release certificate SHA-1
   fingerprint (needed for Google sign-in):
   `83:67:81:6C:2D:E7:64:6E:BC:B1:DF:74:AB:13:7F:AB:BB:08:1D:57`
4. Under Authentication -> Sign-in method, enable **Google** (the
   gate is Google-only, like Apotek Lestari).
5. Create a Cloud Firestore database (production mode is fine) and
   publish the rules from `firestore.rules` at the repo root
   (Firestore Database -> Rules). If the project is shared with the
   other apps, the rules are already this generic - nothing to change.
6. Download the generated `google-services.json` and place it at
   **`app/google-services.json`** in this repo (it is not committed by
   default in this setup - hand it to whoever maintains the build, or
   commit it to this private working repo if that is your practice).
7. Build and install. On first launch the login gate appears; sign in
   with the account you want to keep as your primary.

## Data layout

Under `users/{uid}/`, one document per row, keyed by `remoteId`:

| Collection    | Rows                | Notes                                  |
|---------------|---------------------|----------------------------------------|
| `unit`        | rental units        |                                        |
| `snack`       | snack menu          |                                        |
| `sesi`        | rental sessions     | points at its unit via `unitRemoteId`  |
| `sesi_snack`  | snacks in a session | via `sessionRemoteId` + `snackRemoteId` |
| `jual_snack`  | direct snack sales  | via `snackRemoteId`                    |
| `reservasi`   | reservations        | via `unitRemoteId`                     |
| `pengeluaran` | expenses            |                                        |

## Notes and limits

- Always use ONE primary account. The app consolidates automatically
  when you sign in with a different account, but the other account's
  tree keeps its older partial copy - simplest is to never use it.
- Sign-in is Google-only: there is no password to manage, and the
  split-account trap of mixing sign-in methods cannot happen.
- Sessions are records: a running session synced to a second phone
  shows the same timers (they derive from the stored start time), but
  system alarms and notifications stay per-device, and if the same
  session is edited on two devices, the later edit wins.
- The encrypted JSON backup remains the offline safety net; export one
  after important days. Excel/CSV reports are not backups.
- Sync is a mirror, not a sharing feature: there is no multi-user or
  multi-cashier mode.
