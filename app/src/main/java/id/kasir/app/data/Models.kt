package id.kasir.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Satu unit rental: satu TV + satu konsol.
@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val consoleType: String,
    val hourlyRate: Long
)

// Menu jajan / F&B yang bisa ditambahkan ke tagihan sesi atau dijual langsung.
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
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long,
    val unitName: String,
    val consoleType: String,
    val hourlyRate: Long,
    val mode: String,
    val paketMinutes: Int = 0,
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

