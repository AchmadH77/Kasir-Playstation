# Aturan ProGuard/R8 aplikasi Kasir.
# Minify (R8) + shrink resources aktif di build debug dan release
# (lihat build.gradle.kts). R8 mode penuh adalah bawaan AGP 8.x.

# Room: KSP menghasilkan AppDatabase_Impl dan KasirDao_Impl di paket data,
# dan Room memuatnya lewat refleksi berdasarkan nama kelas (kelas + "_Impl").
# Tanpa aturan ini, R8 bisa mengganti nama / membuang kelas tersebut dan
# database gagal dibuka saat aplikasi berjalan.
# Paket data dipertahankan utuh: entitas, DAO, database, dan helper-nya.
-keep class id.kasir.app.data.** { *; }

# Atribut yang dibutuhkan refleksi dan debugging stack trace.
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*

# Compose dan Room membawa consumer rules-nya sendiri dari library,
# jadi tidak perlu aturan tambahan di sini.
