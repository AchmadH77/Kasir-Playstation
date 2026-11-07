package id.kasir.app.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

// Satu unit rental: satu TV + satu konsol.
@Immutable
@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val consoleType: String,
    val hourlyRate: Long
)

// Menu jajan / F&B yang bisa ditambahkan ke tagihan sesi atau dijual langsung.
@Immutable
@Entity(tableName = "snacks")
data class SnackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Long,
    val stock: Int
)

// Satu sesi main. Timer dihitung dari startEpoch dikurangi total jeda,
// jadi tetap akurat walau aplikasi ditutup. Tarif dan nama disalin (snapshot)
// saat sesi mulai, sehingga perubahan tarif tidak mengubah sesi berjalan.
@Immutable
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long,
    val unitName: String,
    val consoleType: String,
    val hourlyRate: Long,
    val mode: String,
    val paketMinutes: Int = 0,
    val paketPrice: Long = 0,
    val startEpoch: Long,
    val pausedMillis: Long = 0,
    val pauseStartEpoch: Long? = null,
    val status: String = "RUNNING",
    val endEpoch: Long? = null,
    val rentalCost: Long = 0,
    val snackCost: Long = 0,
    val total: Long = 0,
    val paymentMethod: String? = null,
    val paidAmount: Long = 0,
    val changeAmount: Long = 0
)

// Jajan yang menempel pada satu sesi (satu baris per jenis jajan).
@Immutable
@Entity(tableName = "session_snacks")
data class SessionSnackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val snackId: Long,
    val snackName: String,
    val price: Long,
    val qty: Int
)

// Penjualan jajan langsung untuk pelanggan yang tidak main.
@Immutable
@Entity(tableName = "snack_sales")
data class SnackSaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val snackId: Long,
    val snackName: String,
    val price: Long,
    val qty: Int,
    val total: Long,
    val epoch: Long,
    val paymentMethod: String
)


// Reservasi unit: pelanggan memesan satu unit untuk jam tertentu.
// Sifatnya penanda dan pengingat; memulai sesi dari reservasi mengisi
// durasi + harga paketnya ke formulir mulai sesi.
@Immutable
@Entity(tableName = "reservations")
data class ReservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long,
    val unitName: String,
    val customerName: String,
    val startEpoch: Long,
    val durationMinutes: Int,
    val price: Long = 0
)

// Pengeluaran operasional (fitur Catat Pengeluaran): biaya seperti
// restok makanan, listrik, atau sewa - dicatat agar Laporan bisa
// menampilkan laba bersih, bukan hanya pendapatan.
@Immutable
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,
    val epoch: Long
)
