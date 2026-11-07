package id.kasir.app.data

import androidx.room.withTransaction
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject

// Seluruh data kasir dalam satu berkas JSON: dibuat dari tab Lainnya,
// dibagikan ke mana saja (WhatsApp, Drive, ...), dan dipulihkan lagi dari
// berkas yang sama. Formatnya sengaja polos:
// { app, version, exportedAtMillis, units[], snacks[], sessions[],
//   sessionSnacks[], snackSales[], reservations[], expenses[] }
data class BackupData(
    val exportedAtMillis: Long,
    val units: List<UnitEntity>,
    val snacks: List<SnackEntity>,
    val sessions: List<SessionEntity>,
    val sessionSnacks: List<SessionSnackEntity>,
    val snackSales: List<SnackSaleEntity>,
    val reservations: List<ReservationEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList()
)

suspend fun collectBackup(dao: KasirDao, exportedAtMillis: Long): BackupData =
    BackupData(
        exportedAtMillis = exportedAtMillis,
        units = dao.allUnitsOnce(),
        snacks = dao.allSnacksOnce(),
        sessions = dao.allSessionsOnce(),
        sessionSnacks = dao.allSessionSnacksOnce(),
        snackSales = dao.allSnackSalesOnce(),
        reservations = dao.allReservationsOnce(),
        expenses = dao.allExpensesOnce()
    )

fun backupFileName(exportedAtMillis: Long): String =
    "kasir-backup-" +
        SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date(exportedAtMillis)) +
        ".json"

// Mengganti seluruh isi database dengan isi backup, dalam satu transaksi:
// gagal di tengah jalan tidak meninggalkan data setengah.
suspend fun applyBackup(db: AppDatabase, data: BackupData) {
    val dao = db.dao()
    db.withTransaction {
        dao.clearSessionSnacks()
        dao.clearSnackSales()
        dao.clearReservations()
        dao.clearExpenses()
        dao.clearSessions()
        dao.clearSnacks()
        dao.clearUnits()
        data.units.forEach { dao.insertUnit(it) }
        data.snacks.forEach { dao.insertSnack(it) }
        data.sessions.forEach { dao.insertSession(it) }
        data.sessionSnacks.forEach { dao.insertSessionSnack(it) }
        data.snackSales.forEach { dao.insertSnackSale(it) }
        data.reservations.forEach { dao.insertReservation(it) }
        data.expenses.forEach { dao.insertExpense(it) }
    }
}

fun backupToJson(data: BackupData): String {
    val root = JSONObject()
    root.put("app", "kasir")
    root.put("version", 1)
    root.put("exportedAtMillis", data.exportedAtMillis)
    root.put("units", JSONArray().apply {
        data.units.forEach { u ->
            put(JSONObject().apply {
                put("id", u.id)
                put("name", u.name)
                put("consoleType", u.consoleType)
                put("hourlyRate", u.hourlyRate)
            })
        }
    })
    root.put("snacks", JSONArray().apply {
        data.snacks.forEach { s ->
            put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("price", s.price)
                put("stock", s.stock)
            })
        }
    })
    root.put("sessions", JSONArray().apply {
        data.sessions.forEach { s ->
            put(JSONObject().apply {
                put("id", s.id)
                put("unitId", s.unitId)
                put("unitName", s.unitName)
                put("consoleType", s.consoleType)
                put("hourlyRate", s.hourlyRate)
                put("mode", s.mode)
                put("paketMinutes", s.paketMinutes)
                put("paketPrice", s.paketPrice)
                put("startEpoch", s.startEpoch)
                put("pausedMillis", s.pausedMillis)
                put("pauseStartEpoch", s.pauseStartEpoch ?: JSONObject.NULL)
                put("status", s.status)
                put("endEpoch", s.endEpoch ?: JSONObject.NULL)
                put("rentalCost", s.rentalCost)
                put("snackCost", s.snackCost)
                put("total", s.total)
                put("paymentMethod", s.paymentMethod ?: JSONObject.NULL)
                put("paidAmount", s.paidAmount)
                put("changeAmount", s.changeAmount)
            })
        }
    })
    root.put("sessionSnacks", JSONArray().apply {
        data.sessionSnacks.forEach { s ->
            put(JSONObject().apply {
                put("id", s.id)
                put("sessionId", s.sessionId)
                put("snackId", s.snackId)
                put("snackName", s.snackName)
                put("price", s.price)
                put("qty", s.qty)
            })
        }
    })
    root.put("snackSales", JSONArray().apply {
        data.snackSales.forEach { s ->
            put(JSONObject().apply {
                put("id", s.id)
                put("snackId", s.snackId)
                put("snackName", s.snackName)
                put("price", s.price)
                put("qty", s.qty)
                put("total", s.total)
                put("epoch", s.epoch)
                put("paymentMethod", s.paymentMethod)
            })
        }
    })
    root.put("expenses", JSONArray().apply {
        data.expenses.forEach { e ->
            put(JSONObject().apply {
                put("id", e.id)
                put("name", e.name)
                put("amount", e.amount)
                put("epoch", e.epoch)
            })
        }
    })
    root.put("reservations", JSONArray().apply {
        data.reservations.forEach { r ->
            put(JSONObject().apply {
                put("id", r.id)
                put("unitId", r.unitId)
                put("unitName", r.unitName)
                put("customerName", r.customerName)
                put("startEpoch", r.startEpoch)
                put("durationMinutes", r.durationMinutes)
                put("price", r.price)
            })
        }
    })
    return root.toString(2)
}

private fun <T> JSONArray.mapItems(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }

// Membaca berkas backup. Melempar exception bila berkasnya bukan backup
// Kasir atau ada bagian wajib yang hilang/rusak.
fun parseBackup(text: String): BackupData {
    val root = JSONObject(text)
    require(root.optString("app") == "kasir") { "Bukan berkas backup Kasir" }
    return BackupData(
        exportedAtMillis = root.getLong("exportedAtMillis"),
        units = root.getJSONArray("units").mapItems { o ->
            UnitEntity(
                id = o.getLong("id"),
                name = o.getString("name"),
                consoleType = o.getString("consoleType"),
                hourlyRate = o.getLong("hourlyRate")
            )
        },
        snacks = root.getJSONArray("snacks").mapItems { o ->
            SnackEntity(
                id = o.getLong("id"),
                name = o.getString("name"),
                price = o.getLong("price"),
                stock = o.getInt("stock")
            )
        },
        sessions = root.getJSONArray("sessions").mapItems { o ->
            SessionEntity(
                id = o.getLong("id"),
                unitId = o.getLong("unitId"),
                unitName = o.getString("unitName"),
                consoleType = o.getString("consoleType"),
                hourlyRate = o.getLong("hourlyRate"),
                mode = o.getString("mode"),
                paketMinutes = o.getInt("paketMinutes"),
                paketPrice = o.optLong("paketPrice", 0),
                startEpoch = o.getLong("startEpoch"),
                pausedMillis = o.getLong("pausedMillis"),
                pauseStartEpoch = if (o.isNull("pauseStartEpoch")) null else o.getLong("pauseStartEpoch"),
                status = o.getString("status"),
                endEpoch = if (o.isNull("endEpoch")) null else o.getLong("endEpoch"),
                rentalCost = o.getLong("rentalCost"),
                snackCost = o.getLong("snackCost"),
                total = o.getLong("total"),
                paymentMethod = if (o.isNull("paymentMethod")) null else o.getString("paymentMethod"),
                paidAmount = o.getLong("paidAmount"),
                changeAmount = o.getLong("changeAmount")
            )
        },
        sessionSnacks = root.getJSONArray("sessionSnacks").mapItems { o ->
            SessionSnackEntity(
                id = o.getLong("id"),
                sessionId = o.getLong("sessionId"),
                snackId = o.getLong("snackId"),
                snackName = o.getString("snackName"),
                price = o.getLong("price"),
                qty = o.getInt("qty")
            )
        },
        snackSales = root.getJSONArray("snackSales").mapItems { o ->
            SnackSaleEntity(
                id = o.getLong("id"),
                snackId = o.getLong("snackId"),
                snackName = o.getString("snackName"),
                price = o.getLong("price"),
                qty = o.getInt("qty"),
                total = o.getLong("total"),
                epoch = o.getLong("epoch"),
                paymentMethod = o.getString("paymentMethod")
            )
        },
        // Backup lama belum punya pengeluaran: anggap kosong juga.
        expenses = root.optJSONArray("expenses")?.mapItems { o ->
            ExpenseEntity(
                id = o.getLong("id"),
                name = o.getString("name"),
                amount = o.getLong("amount"),
                epoch = o.getLong("epoch")
            )
        } ?: emptyList(),
        // Backup lama belum punya reservasi: anggap kosong, jangan gagal.
        reservations = root.optJSONArray("reservations")?.mapItems { o ->
            ReservationEntity(
                id = o.getLong("id"),
                unitId = o.getLong("unitId"),
                unitName = o.getString("unitName"),
                customerName = o.getString("customerName"),
                startEpoch = o.getLong("startEpoch"),
                durationMinutes = o.getInt("durationMinutes"),
                price = o.optLong("price", 0)
            )
        } ?: emptyList()
    )
}

// ---------- Backup terenkripsi (opsional) ----------
// JSON polos dibungkus amplop { app, version, encrypted: true, kdf,
// iterations, salt, iv, data }: kunci diturunkan dari kata sandi lewat
// PBKDF2-HMAC-SHA256, isi dienkripsi AES-GCM. Kata sandi tidak pernah
// disimpan; GCM memeriksa keaslian isi sehingga sandi yang salah selalu
// ketahuan. Berkas lama tanpa amplop tetap backup polos biasa.
private const val BACKUP_KDF_ITERATIONS = 100_000

fun isEncryptedBackup(text: String): Boolean {
    val root = try {
        JSONObject(text)
    } catch (e: Exception) {
        return false
    }
    return root.optString("app") == "kasir" &&
        root.optBoolean("encrypted", false)
}

fun encryptBackup(plainText: String, password: String): String {
    val random = SecureRandom()
    val salt = ByteArray(16).also { random.nextBytes(it) }
    val iv = ByteArray(12).also { random.nextBytes(it) }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(
        Cipher.ENCRYPT_MODE,
        backupKey(password, salt, BACKUP_KDF_ITERATIONS),
        GCMParameterSpec(128, iv)
    )
    val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    val b64 = Base64.getEncoder()
    return JSONObject().apply {
        put("app", "kasir")
        put("version", 1)
        put("encrypted", true)
        put("kdf", "PBKDF2WithHmacSHA256")
        put("iterations", BACKUP_KDF_ITERATIONS)
        put("salt", b64.encodeToString(salt))
        put("iv", b64.encodeToString(iv))
        put("data", b64.encodeToString(cipherBytes))
    }.toString(2)
}

// Teks backup polos, atau null bila kata sandinya salah atau berkasnya
// rusak - data yang sedang berjalan tidak disentuh sama sekali.
fun decryptBackup(text: String, password: String): String? = try {
    val root = JSONObject(text)
    require(
        root.optString("app") == "kasir" &&
            root.optBoolean("encrypted", false)
    )
    val b64 = Base64.getDecoder()
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(
        Cipher.DECRYPT_MODE,
        backupKey(
            password,
            b64.decode(root.getString("salt")),
            root.optInt("iterations", BACKUP_KDF_ITERATIONS)
        ),
        GCMParameterSpec(128, b64.decode(root.getString("iv")))
    )
    String(cipher.doFinal(b64.decode(root.getString("data"))), Charsets.UTF_8)
} catch (e: Exception) {
    null
}

private fun backupKey(
    password: String,
    salt: ByteArray,
    iterations: Int
): SecretKey {
    val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
}
