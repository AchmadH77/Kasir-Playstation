package id.kasir.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KasirDao {

    // Unit
    @Query("SELECT * FROM units WHERE terhapus = 0 ORDER BY name ASC")
    fun unitsFlow(): Flow<List<UnitEntity>>

    @Query("SELECT COUNT(*) FROM units WHERE terhapus = 0")
    suspend fun unitCount(): Int

    @Insert
    suspend fun insertUnit(unit: UnitEntity): Long

    @Update
    suspend fun updateUnit(unit: UnitEntity)

    @Query("UPDATE units SET terhapus = 1, kotor = 1, updatedAt = :now WHERE id = :id")
    suspend fun deleteUnit(id: Long, now: Long)

    // Jajan
    @Query("SELECT * FROM snacks WHERE terhapus = 0 ORDER BY name ASC")
    fun snacksFlow(): Flow<List<SnackEntity>>

    @Query("SELECT COUNT(*) FROM snacks WHERE terhapus = 0")
    suspend fun snackCount(): Int

    @Insert
    suspend fun insertSnack(snack: SnackEntity): Long

    @Update
    suspend fun updateSnack(snack: SnackEntity)

    @Query("UPDATE snacks SET terhapus = 1, kotor = 1, updatedAt = :now WHERE id = :id")
    suspend fun deleteSnack(id: Long, now: Long)

    @Query("UPDATE snacks SET stock = stock - :qty, kotor = 1, updatedAt = :now WHERE id = :id")
    suspend fun decreaseStock(id: Long, qty: Int, now: Long)

    @Query("UPDATE snacks SET stock = stock + :qty, kotor = 1, updatedAt = :now WHERE id = :id")
    suspend fun increaseStock(id: Long, qty: Int, now: Long)

    // Sesi
    @Query("SELECT * FROM sessions WHERE status != 'FINISHED' AND terhapus = 0 ORDER BY startEpoch ASC")
    fun activeSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE status = 'FINISHED' AND terhapus = 0 ORDER BY endEpoch DESC")
    fun finishedSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id AND terhapus = 0 LIMIT 1")
    suspend fun sessionOnce(id: Long): SessionEntity?

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("UPDATE sessions SET terhapus = 1, kotor = 1, updatedAt = :now WHERE id = :id")
    suspend fun deleteSession(id: Long, now: Long)

    // Jajan di dalam sesi
    @Query("SELECT * FROM session_snacks WHERE sessionId = :sessionId AND terhapus = 0 ORDER BY snackName ASC")
    fun sessionSnacksFlow(sessionId: Long): Flow<List<SessionSnackEntity>>

    @Query("SELECT * FROM session_snacks WHERE sessionId = :sessionId AND terhapus = 0")
    suspend fun sessionSnacksOnce(sessionId: Long): List<SessionSnackEntity>

    @Query("SELECT * FROM session_snacks WHERE sessionId = :sessionId AND snackId = :snackId AND terhapus = 0 LIMIT 1")
    suspend fun sessionSnack(sessionId: Long, snackId: Long): SessionSnackEntity?

    @Insert
    suspend fun insertSessionSnack(item: SessionSnackEntity): Long

    @Update
    suspend fun updateSessionSnack(item: SessionSnackEntity)

    @Query("UPDATE session_snacks SET terhapus = 1, kotor = 1, updatedAt = :now WHERE id = :id")
    suspend fun deleteSessionSnack(id: Long, now: Long)

    @Query("UPDATE session_snacks SET terhapus = 1, kotor = 1, updatedAt = :now WHERE sessionId = :sessionId")
    suspend fun deleteSessionSnacks(sessionId: Long, now: Long)

    // Penjualan jajan langsung
    @Query("SELECT * FROM snack_sales WHERE terhapus = 0 ORDER BY epoch DESC")
    fun snackSalesFlow(): Flow<List<SnackSaleEntity>>

    @Insert
    suspend fun insertSnackSale(sale: SnackSaleEntity): Long

    // Reservasi unit
    @Query("SELECT * FROM reservations WHERE terhapus = 0 ORDER BY startEpoch ASC")
    fun reservationsFlow(): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE id = :id AND terhapus = 0 LIMIT 1")
    suspend fun reservationOnce(id: Long): ReservationEntity?

    @Insert
    suspend fun insertReservation(reservation: ReservationEntity): Long

    @Query("UPDATE reservations SET terhapus = 1, kotor = 1, updatedAt = :now WHERE id = :id")
    suspend fun deleteReservation(id: Long, now: Long)

    // Pengeluaran operasional
    @Query("SELECT * FROM expenses WHERE terhapus = 0 ORDER BY epoch DESC")
    fun expensesFlow(): Flow<List<ExpenseEntity>>

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("UPDATE expenses SET terhapus = 1, kotor = 1, updatedAt = :now WHERE id = :id")
    suspend fun deleteExpense(id: Long, now: Long)


    // ---- Permukaan sinkron (Mode Online) ----
    // Baris kotor menunggu naik; getByRemoteId mencari pasangan lokal
    // dari dokumen awan; purge membuang kuburan yang sudah bersih.

    @Query("SELECT * FROM units WHERE kotor = 1")
    suspend fun unitsKotor(): List<UnitEntity>

    @Query("SELECT * FROM units WHERE remoteId = :remoteId LIMIT 1")
    suspend fun unitByRemoteId(remoteId: String): UnitEntity?

    @Query("UPDATE units SET kotor = 0 WHERE remoteId = :remoteId AND updatedAt = :updatedAt")
    suspend fun unitTandaiBersih(remoteId: String, updatedAt: Long): Int

    @Query("DELETE FROM units WHERE terhapus = 1 AND kotor = 0")
    suspend fun purgeUnits(): Int

    @Query("UPDATE units SET kotor = 1")
    suspend fun unitsTandaiSemuaKotor(): Int

    @Query("SELECT * FROM snacks WHERE kotor = 1")
    suspend fun snacksKotor(): List<SnackEntity>

    @Query("SELECT * FROM snacks WHERE remoteId = :remoteId LIMIT 1")
    suspend fun snackByRemoteId(remoteId: String): SnackEntity?

    @Query("UPDATE snacks SET kotor = 0 WHERE remoteId = :remoteId AND updatedAt = :updatedAt")
    suspend fun snackTandaiBersih(remoteId: String, updatedAt: Long): Int

    @Query("DELETE FROM snacks WHERE terhapus = 1 AND kotor = 0")
    suspend fun purgeSnacks(): Int

    @Query("UPDATE snacks SET kotor = 1")
    suspend fun snacksTandaiSemuaKotor(): Int

    @Query("SELECT * FROM sessions WHERE kotor = 1")
    suspend fun sessionsKotor(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE remoteId = :remoteId LIMIT 1")
    suspend fun sessionByRemoteId(remoteId: String): SessionEntity?

    @Query("UPDATE sessions SET kotor = 0 WHERE remoteId = :remoteId AND updatedAt = :updatedAt")
    suspend fun sessionTandaiBersih(remoteId: String, updatedAt: Long): Int

    @Query("DELETE FROM sessions WHERE terhapus = 1 AND kotor = 0")
    suspend fun purgeSessions(): Int

    @Query("UPDATE sessions SET kotor = 1")
    suspend fun sessionsTandaiSemuaKotor(): Int

    @Query("SELECT * FROM session_snacks WHERE kotor = 1")
    suspend fun sessionSnacksKotor(): List<SessionSnackEntity>

    @Query("SELECT * FROM session_snacks WHERE remoteId = :remoteId LIMIT 1")
    suspend fun sessionSnackByRemoteId(remoteId: String): SessionSnackEntity?

    @Query("UPDATE session_snacks SET kotor = 0 WHERE remoteId = :remoteId AND updatedAt = :updatedAt")
    suspend fun sessionSnackTandaiBersih(remoteId: String, updatedAt: Long): Int

    @Query("DELETE FROM session_snacks WHERE terhapus = 1 AND kotor = 0")
    suspend fun purgeSessionSnacks(): Int

    @Query("UPDATE session_snacks SET kotor = 1")
    suspend fun sessionSnacksTandaiSemuaKotor(): Int

    @Query("SELECT * FROM snack_sales WHERE kotor = 1")
    suspend fun snackSalesKotor(): List<SnackSaleEntity>

    @Query("SELECT * FROM snack_sales WHERE remoteId = :remoteId LIMIT 1")
    suspend fun snackSaleByRemoteId(remoteId: String): SnackSaleEntity?

    @Query("UPDATE snack_sales SET kotor = 0 WHERE remoteId = :remoteId AND updatedAt = :updatedAt")
    suspend fun snackSaleTandaiBersih(remoteId: String, updatedAt: Long): Int

    @Query("DELETE FROM snack_sales WHERE terhapus = 1 AND kotor = 0")
    suspend fun purgeSnackSales(): Int

    @Query("UPDATE snack_sales SET kotor = 1")
    suspend fun snackSalesTandaiSemuaKotor(): Int

    @Query("SELECT * FROM reservations WHERE kotor = 1")
    suspend fun reservationsKotor(): List<ReservationEntity>

    @Query("SELECT * FROM reservations WHERE remoteId = :remoteId LIMIT 1")
    suspend fun reservationByRemoteId(remoteId: String): ReservationEntity?

    @Query("UPDATE reservations SET kotor = 0 WHERE remoteId = :remoteId AND updatedAt = :updatedAt")
    suspend fun reservationTandaiBersih(remoteId: String, updatedAt: Long): Int

    @Query("DELETE FROM reservations WHERE terhapus = 1 AND kotor = 0")
    suspend fun purgeReservations(): Int

    @Query("UPDATE reservations SET kotor = 1")
    suspend fun reservationsTandaiSemuaKotor(): Int

    @Query("SELECT * FROM expenses WHERE kotor = 1")
    suspend fun expensesKotor(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE remoteId = :remoteId LIMIT 1")
    suspend fun expenseByRemoteId(remoteId: String): ExpenseEntity?

    @Query("UPDATE expenses SET kotor = 0 WHERE remoteId = :remoteId AND updatedAt = :updatedAt")
    suspend fun expenseTandaiBersih(remoteId: String, updatedAt: Long): Int

    @Query("DELETE FROM expenses WHERE terhapus = 1 AND kotor = 0")
    suspend fun purgeExpenses(): Int

    @Query("UPDATE expenses SET kotor = 1")
    suspend fun expensesTandaiSemuaKotor(): Int

    // Backup & restore: baca semua baris sekaligus dan kosongkan tabel.
    @Query("SELECT * FROM units")
    suspend fun allUnitsOnce(): List<UnitEntity>

    @Query("SELECT * FROM snacks")
    suspend fun allSnacksOnce(): List<SnackEntity>

    @Query("SELECT * FROM sessions")
    suspend fun allSessionsOnce(): List<SessionEntity>

    @Query("SELECT * FROM session_snacks")
    suspend fun allSessionSnacksOnce(): List<SessionSnackEntity>

    @Query("SELECT * FROM snack_sales")
    suspend fun allSnackSalesOnce(): List<SnackSaleEntity>

    @Query("SELECT * FROM reservations")
    suspend fun allReservationsOnce(): List<ReservationEntity>

    @Query("SELECT * FROM expenses")
    suspend fun allExpensesOnce(): List<ExpenseEntity>

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM reservations")
    suspend fun clearReservations()

    @Query("DELETE FROM session_snacks")
    suspend fun clearSessionSnacks()

    @Query("DELETE FROM snack_sales")
    suspend fun clearSnackSales()

    @Query("DELETE FROM sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM snacks")
    suspend fun clearSnacks()

    @Query("DELETE FROM units")
    suspend fun clearUnits()
}

@Database(
    entities = [
        UnitEntity::class,
        SnackEntity::class,
        SessionEntity::class,
        SessionSnackEntity::class,
        SnackSaleEntity::class,
        ReservationEntity::class,
        ExpenseEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): KasirDao
}

// v2: kolom paketPrice di sessions - harga paket manual yang diketik saat
// mulai sesi (0 = sesi lama, harga dari tarif per jam).
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sessions ADD COLUMN paketPrice INTEGER NOT NULL DEFAULT 0")
    }
}

// v3: tabel reservations untuk fitur reservasi unit.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS reservations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "unitId INTEGER NOT NULL, " +
                "unitName TEXT NOT NULL, " +
                "customerName TEXT NOT NULL, " +
                "startEpoch INTEGER NOT NULL, " +
                "durationMinutes INTEGER NOT NULL, " +
                "price INTEGER NOT NULL)"
        )
    }
}

// v4: tabel expenses untuk fitur Catat Pengeluaran (laba bersih).
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "amount INTEGER NOT NULL, " +
                "epoch INTEGER NOT NULL)"
        )
    }
}

// v5: kolom sinkron Mode Online di ketujuh tabel - remoteId (identitas
// awan), updatedAt (cap sunting terakhir untuk adu mutakhir),
// terhapus (hapus lunak/kuburan), kotor (menunggu naik). Baris lama
// diberi remoteId dari id lokalnya dan updatedAt dari cap waktunya
// sendiri bila ada (sesi/penjualan/reservasi/pengeluaran), 0 bila tidak.
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val tabel = listOf(
            Triple("units", "unit-", "0"),
            Triple("snacks", "snack-", "0"),
            Triple("sessions", "sesi-", "startEpoch"),
            Triple("session_snacks", "sesisnack-", "0"),
            Triple("snack_sales", "jualsnack-", "epoch"),
            Triple("reservations", "reservasi-", "startEpoch"),
            Triple("expenses", "pengeluaran-", "epoch")
        )
        for ((nama, awalan, capWaktu) in tabel) {
            db.execSQL("ALTER TABLE $nama ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE $nama ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $nama ADD COLUMN terhapus INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $nama ADD COLUMN kotor INTEGER NOT NULL DEFAULT 1")
            db.execSQL("UPDATE $nama SET remoteId = '$awalan' || id, updatedAt = $capWaktu")
        }
    }
}

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "kasir.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
        }
    }
}

// Data contoh saat aplikasi pertama kali dibuka, supaya langsung bisa dicoba.
// Semua bisa diubah atau dihapus dari menu pengelolaan.
object SeedData {
    suspend fun ensure(dao: KasirDao) {
        if (dao.unitCount() == 0) {
            dao.insertUnit(UnitEntity(name = "TV 01", consoleType = "PS3", hourlyRate = 5000))
            dao.insertUnit(UnitEntity(name = "TV 02", consoleType = "PS3", hourlyRate = 5000))
            dao.insertUnit(UnitEntity(name = "TV 03", consoleType = "PS4", hourlyRate = 8000))
            dao.insertUnit(UnitEntity(name = "TV 04", consoleType = "PS4", hourlyRate = 8000))
            dao.insertUnit(UnitEntity(name = "TV 05", consoleType = "PS5", hourlyRate = 10000))
            dao.insertUnit(UnitEntity(name = "TV 06", consoleType = "PS5", hourlyRate = 10000))
        }
        if (dao.snackCount() == 0) {
            dao.insertSnack(SnackEntity(name = "Indomie Goreng", price = 7000, stock = 20))
            dao.insertSnack(SnackEntity(name = "Kopi Hitam", price = 5000, stock = 20))
            dao.insertSnack(SnackEntity(name = "Es Teh Manis", price = 5000, stock = 20))
            dao.insertSnack(SnackEntity(name = "Air Mineral", price = 4000, stock = 30))
            dao.insertSnack(SnackEntity(name = "Snack Kentang", price = 6000, stock = 20))
            dao.insertSnack(SnackEntity(name = "Gorengan", price = 5000, stock = 20))
        }
    }
}

