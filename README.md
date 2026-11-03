# Kasir

Aplikasi kasir (billing) khusus **rental PlayStation** untuk Android, ditulis dengan Kotlin + Jetpack Compose dan Material 3 Expressive. Semua data tersimpan lokal di HP (Room), tanpa server.

## Fitur

- **Billing per unit** — daftar semua unit/TV dengan status kosong/bermain/jeda dan timer besar yang berjalan. Timer dihitung dari waktu mulai yang tersimpan di database, jadi tetap akurat walau aplikasi ditutup.
- **Dua mode main** — Paket (durasi tetap 1–5 jam, hitung mundur, kelebihan waktu ditagih per blok) dan Open Billing (jalan terus, dibulatkan ke atas per blok 10 menit).
- **Kelola sesi** — tambah waktu, jeda & lanjut (waktu jeda tidak dihitung), tambah jajan ke tagihan, batalkan sesi (stok jajan kembali), stop & bayar.
- **Jajan / F&B** — menu dengan stok; masuk ke tagihan sesi atau dijual langsung ke pelanggan yang tidak main. Stok berkurang otomatis.
- **Pembayaran** — Tunai (uang diterima + kembalian otomatis + tombol uang pas/pecahan) atau QRIS.
- **Laporan** — total pendapatan per periode (hari ini / minggu ini / bulan ini / semua) terpisah sewa dan jajan langsung; unit terlaris; riwayat sesi.
- **Struk** — rincian pembayaran yang bisa dibagikan (teks) ke aplikasi lain; nama toko bisa diatur.
- **Pengaturan** — kelola unit & tarif per jam per jenis konsol (PS3/PS4/PS5), mode gelap/terang tersimpan.

Saat pertama dibuka, aplikasi berisi data contoh (6 unit TV dan 6 menu jajan) yang bisa diubah atau dihapus.

## Tech stack

| | |
|---|---|
| Bahasa | Kotlin 2.2.20 |
| UI | Jetpack Compose (BOM 2025.10.00), Material3 1.5.0-alpha08 (MaterialExpressiveTheme) |
| Database | Room 2.7.2 (KSP 2.2.20-2.0.4), sepenuhnya offline |
| Build | AGP 8.11.1, Gradle 8.13, JDK 17 |
| Android | minSdk 26 (Android 8.0), targetSdk 34, compileSdk 36 |
| Package | `id.kasir.app` |
| Versi | 1.0.0 |
| Signing | Keystore tetap di repo (`kasir.p12`, PKCS12) — semua build CI memakai tanda tangan yang sama, jadi update bisa dipasang menimpa versi lama |
| R8 | Minify + shrink resources aktif di build debug dan release |

## Cara build APK

**Tanpa komputer (GitHub Actions):** buka tab **Actions** di repo ini → workflow **Build APK** → **Run workflow** di branch `main` → unduh artifact **`app-debug-apk`** → pasang di HP. (Selalu mulai run baru; menjalankan ulang run lama akan membuild commit lama.)

**Dengan Android Studio:** buka folder repo ini, tunggu Gradle Sync, lalu Build APK.

## Struktur proyek

- `app/src/main/java/id/kasir/app/MainActivity.kt` — activity utama, kerangka 4 tab, tab Unit (daftar unit + timer), dialog mulai sesi, detail sesi, dan pembayaran.
- `app/src/main/java/id/kasir/app/Screens.kt` — tab Jajan, Laporan, Lainnya, layar kelola unit & tarif, dan dialog struk.
- `app/src/main/java/id/kasir/app/Billing.kt` — logika billing murni (timer, pembulatan blok 10 menit, biaya sewa) dan format tanggal/rupiah.
- `app/src/main/java/id/kasir/app/Theme.kt` — skema warna hijau terang/gelap + tema Material 3 Expressive.
- `app/src/main/java/id/kasir/app/data/Models.kt` — entitas Room (unit, jajan, sesi, jajan sesi, penjualan jajan).
- `app/src/main/java/id/kasir/app/data/AppDatabase.kt` — DAO, database Room, dan data contoh awal.
- `app/build.gradle.kts` — konfigurasi aplikasi, signing CI, dan dependensi.
- `.github/workflows/build.yml` — workflow build manual (workflow_dispatch).
