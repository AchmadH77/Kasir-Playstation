# Kasir

Aplikasi kasir (point of sale) untuk Android, ditulis dengan Kotlin + Jetpack Compose dan Material 3 Expressive. Proyek ini dimulai dari nol sebagai aplikasi kasir umum (bukan khusus apotek) — fitur akan ditambahkan bertahap.

Status saat ini: **kerangka awal** — aplikasi bisa dibuild dan dipasang, menampilkan layar Beranda sederhana. Belum ada fitur transaksi/produk.

## Tech stack

| | |
|---|---|
| Bahasa | Kotlin 2.2.20 |
| UI | Jetpack Compose (BOM 2025.10.00), Material3 1.5.0-alpha08 (MaterialExpressiveTheme) |
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

- `app/src/main/java/id/kasir/app/MainActivity.kt` — activity utama + layar Beranda awal.
- `app/src/main/java/id/kasir/app/Theme.kt` — skema warna hijau terang/gelap + tema Material 3 Expressive.
- `app/build.gradle.kts` — konfigurasi aplikasi, signing CI, dan dependensi.
- `.github/workflows/build.yml` — workflow build manual (workflow_dispatch).
