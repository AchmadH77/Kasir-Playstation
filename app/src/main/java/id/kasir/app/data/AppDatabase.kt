package id.kasir.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KasirDao {

    // Unit
    @Query("SELECT * FROM units ORDER BY name ASC")
    fun unitsFlow(): Flow<List<UnitEntity>>

    @Query("SELECT COUNT(*) FROM units")
    suspend fun unitCount(): Int

    @Insert
    suspend fun insertUnit(unit: UnitEntity): Long

    @Update
    suspend fun updateUnit(unit: UnitEntity)

    @Delete
    suspend fun deleteUnit(unit: UnitEntity)

    // Jajan
    @Query("SELECT * FROM snacks ORDER BY name ASC")
    fun snacksFlow(): Flow<List<SnackEntity>>

    @Query("SELECT COUNT(*) FROM snacks")
    suspend fun snackCount(): Int

    @Insert
    suspend fun insertSnack(snack: SnackEntity): Long

    @Update
    suspend fun updateSnack(snack: SnackEntity)

    @Delete
    suspend fun deleteSnack(snack: SnackEntity)

    @Query("UPDATE snacks SET stock = stock - :qty WHERE id = :id")
    suspend fun decreaseStock(id: Long, qty: Int)

    @Query("UPDATE snacks SET stock = stock + :qty WHERE id = :id")
    suspend fun increaseStock(id: Long, qty: Int)

    // Sesi
    @Query("SELECT * FROM sessions WHERE status != 'FINISHED' ORDER BY startEpoch ASC")
    fun activeSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE status = 'FINISHED' ORDER BY endEpoch DESC")
    fun finishedSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun sessionOnce(id: Long): SessionEntity?

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    // Jajan di dalam sesi
    @Query("SELECT * FROM session_snacks WHERE sessionId = :sessionId ORDER BY snackName ASC")
    fun sessionSnacksFlow(sessionId: Long): Flow<List<SessionSnackEntity>>

    @Query("SELECT * FROM session_snacks WHERE sessionId = :sessionId")
    suspend fun sessionSnacksOnce(sessionId: Long): List<SessionSnackEntity>

    @Query("SELECT * FROM session_snacks WHERE sessionId = :sessionId AND snackId = :snackId LIMIT 1")
    suspend fun sessionSnack(sessionId: Long, snackId: Long): SessionSnackEntity?

    @Insert
    suspend fun insertSessionSnack(item: SessionSnackEntity): Long

    @Update
    suspend fun updateSessionSnack(item: SessionSnackEntity)

    @Delete
    suspend fun deleteSessionSnack(item: SessionSnackEntity)

    @Query("DELETE FROM session_snacks WHERE sessionId = :sessionId")
    suspend fun deleteSessionSnacks(sessionId: Long)

    // Penjualan jajan langsung
    @Query("SELECT * FROM snack_sales ORDER BY epoch DESC")
    fun snackSalesFlow(): Flow<List<SnackSaleEntity>>

    @Insert
    suspend fun insertSnackSale(sale: SnackSaleEntity): Long

    // Reservasi unit
    @Query("SELECT * FROM reservations ORDER BY startEpoch ASC")
    fun reservationsFlow(): Flow<List<ReservationEntity>>

    @Insert
    suspend fun insertReservation(reservation: ReservationEntity): Long

    @Delete
    suspend fun deleteReservation(reservation: ReservationEntity)

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
        ReservationEntity::class
    ],
    version = 3,
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

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "kasir.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
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

