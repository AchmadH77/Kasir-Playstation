# Arsitektur Kasir

Dokumen ini merangkum keputusan teknis aplikasi Kasir (billing rental PlayStation): struktur kode, model data, aturan billing, alur pembayaran dan stok, tema, signing, CI, dan pengujian. Ditulis agar pengembang (atau saya di masa depan) bisa memahami aplikasi tanpa membaca semua kode dari nol.

## Gambaran umum

- Aplikasi Android satu activity (`MainActivity`), seluruh UI Jetpack Compose.
- 100% offline: semua data di database Room lokal; tidak ada jaringan, akun, atau server.
- Tanpa ViewModel: composable membaca `Flow` dari DAO secara langsung (`collectAsState`) dan menulis lewat `rememberCoroutineScope`. Logika hitung murni dipisah ke `Billing.kt` agar bisa diuji unit test.
- Navigasi 4 tab (Unit, Jajan, Laporan, Lainnya) dengan state `Int` sederhana + `AnimatedContent` (fade 300 ms), ditambah satu layar penuh Kelola Unit & Tarif yang menggantikan kerangka utama saat dibuka.

## Stack

| Komponen | Versi / catatan |
|---|---|
| Kotlin | 2.2.20 |
| Compose BOM | 2025.10.00 |
| Material 3 | 1.5.0-alpha08 (`MaterialExpressiveTheme`, `MotionScheme.expressive`) |
| AGP / Gradle | 8.11.1 / 8.13 (JDK 17) |
| Room | 2.7.2 via KSP 2.2.20-2.0.4 |
| Android | minSdk 26, targetSdk 34, compileSdk 36 |
| Package | `id.kasir.app`, versi 1.0.0 (code 1) |

## Struktur kode

| Berkas | Isi |
|---|---|
| `MainActivity.kt` | `KasirApp` (kerangka, dock navigasi melayang, state tema), tab Unit, dialog Mulai Sesi / Detail Sesi / Tambah Jajan / Pembayaran |
| `Screens.kt` | Dialog struk + `receiptText()`, tab Jajan (stepper + pembayaran massal), tab Laporan (grafik 7 hari + komposisi), tab Lainnya (header toko), layar Kelola Unit & Tarif |
| `Billing.kt` | Logika murni: waktu berlalu, pembulatan blok, biaya sewa, sisa paket, format rupiah/tanggal |
| `Theme.kt` | Skema warna hijau terang/gelap untuk `MaterialExpressiveTheme` |
| `data/Models.kt` | Lima entitas Room |
| `data/AppDatabase.kt` | `KasirDao`, `AppDatabase`, `DatabaseProvider` (singleton), `SeedData` |

## Model data (Room, versi 1)

| Entitas | Tabel | Kolom penting |
|---|---|---|
| `UnitEntity` | `units` | nama (TV 01), jenis konsol (PS3/PS4/PS5), tarif per jam |
| `SnackEntity` | `snacks` | nama, harga, stok |
| `SessionEntity` | `sessions` | unit, mode (PAKET/OPEN), waktu mulai/selesai, akumulasi jeda, durasi paket, status (RUNNING/PAUSED/FINISHED), total sewa, metode & jumlah bayar |
| `SessionSnackEntity` | `session_snacks` | sesi, jajan, jumlah, harga saat itu |
| `SnackSaleEntity` | `snack_sales` | jajan, jumlah, harga, waktu, metode bayar (penjualan langsung di tab Jajan) |

Harga selalu disimpan sebagai snapshot pada baris transaksi, jadi perubahan harga menu tidak mengubah riwayat lama.

## Aturan billing (`Billing.kt`)

- Waktu berlalu = (waktu selesai atau sekarang) − waktu mulai − total jeda − jeda yang sedang berjalan. Karena dihitung dari epoch tersimpan, timer akurat walau aplikasi ditutup atau HP restart.
- **Open Billing**: menit tertagih = waktu berlalu dibulatkan **ke atas** per blok 10 menit, minimum 1 blok.
- **Paket**: durasi paket menjadi dasar; kelebihan waktu di atas paket ditagih dengan pembulatan blok 10 menit yang sama. Harga paket linear dari tarif per jam (tidak ada tabel harga paket terpisah).
- Biaya sewa = tarif per jam × menit tertagih / 60, dibulatkan ke rupiah terdekat.
- Konstanta: `BLOCK_MINUTES = 10`, pilihan paket 60–300 menit, jenis konsol PS3/PS4/PS5.
- Helper tampilan: `formatRupiah` (Rp1.500.000), `formatRupiahShort` untuk label grafik (96 rb, 1,5 jt), `formatMinutes`, `formatHms`, serta batas periode `startOfDay` / `startOfWeek` (Senin) / `startOfMonth` untuk Laporan.

## Alur pembayaran & stok

- Checkout sesi: total = total sewa + total jajan sesi. Tunai mewajibkan uang diterima ≥ total dan menampilkan kembalian; QRIS selalu pas. Setelah bayar, sesi berstatus FINISHED dan struk ditawarkan untuk dibagikan.
- Membatalkan sesi mengembalikan stok semua jajan sesi lalu menghapus sesi beserta baris jajannya.
- Penjualan langsung di tab Jajan: pengguna menaikkan stepper beberapa item (dibatasi stok), lalu membayar semuanya dalam satu dialog (Tunai/QRIS). Setiap item menjadi satu `SnackSaleEntity` dan stoknya dikurangi.
- Jajan yang ditambahkan ke sesi mengurangi stok saat itu juga; mengurangi jumlah atau membatalkan sesi mengembalikannya.

## Preferensi (SharedPreferences `kasir_prefs`)

| Key | Isi |
|---|---|
| `dark_theme` | Boolean mode gelap; tidak ada key = ikut sistem |
| `store_name` | Nama toko di struk dan header tab Lainnya (bawaan "Rental PS") |

## Tema & desain

- Skema hijau buatan tangan (terang: primary `#146C43`, container `#B7E4C7`; gelap: primary `#7BD88F`, container `#14532D`) di bawah `MaterialExpressiveTheme`.
- Navigasi memakai **dock melayang** (`FloatingDock`): kapsul `surfaceContainerHigh` berbayangan, tab aktif berupa pil `primary` berisi ikon + label, tab lain ikon saja. Konten tab diberi bantalan bawah 96 dp agar tidak tertutup dock.
- Bahasa UI: Bahasa Indonesia.

## Signing & R8

- Keystore tetap `kasir.p12` (PKCS12) di root repo; `signingConfigs.ci` di `app/build.gradle.kts` memakainya untuk build debug **dan** release, jadi setiap APK CI bertanda tangan sama dan bisa saling menimpa saat dipasang.
- R8 (`minifyEnabled` + `shrinkResources`) aktif di kedua build type. `app/proguard-rules.pro` menjaga paket `id.kasir.app.data.**` (Room memuat `AppDatabase_Impl`/`KasirDao_Impl` secara reflektif berdasarkan nama) dan atribut `SourceFile`/`LineNumberTable`/`Signature`/anotasi.

## CI (GitHub Actions)

Workflow `.github/workflows/build.yml` bersifat `workflow_dispatch` (dijalankan manual), 7 langkah:

1. Checkout
2. JDK 17 (Temurin)
3. Install SDK Android 36 + build-tools 36.0.0 lewat `sdkmanager`
4. Setup Gradle 8.13 (`gradle/actions/setup-gradle@v4`, cache aktif via `org.gradle.caching=true`)
5. **Unit tests** — `gradle :app:testDebugUnitTest`
6. Build debug APK — `gradle assembleDebug`
7. Upload artifact `app-debug-apk`

Catatan: selalu mulai **Run workflow** baru; *Re-run jobs* membuild ulang commit lama.

## Pengujian

`BillingTest` (JUnit 4, 16 test) menguji logika murni `Billing.kt`: format rupiah & rupiah pendek, pembulatan blok, waktu berlalu termasuk jeda, menit tertagih paket + overtime, biaya sewa kedua mode, sisa waktu paket, dan batas hari/minggu/bulan. Test berjalan di CI pada setiap build.
