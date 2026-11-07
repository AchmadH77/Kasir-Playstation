package id.kasir.app.data

import androidx.room.withTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

// Seluruh data kasir dalam satu berkas JSON: dibuat dari tab Lainnya,
// dibagikan ke mana saja (WhatsApp, Drive, ...), dan dipulihkan lagi dari
// berkas yang sama. Formatnya sengaja polos:
// { app, version, exportedAtMillis, units[], snacks[], sessions[],
//   sessionSnacks[], snackSales[], reservations[] }
data class BackupData(
    val exportedAtMillis: Long,
    val units: List<UnitEntity>,
    val snacks: List<SnackEntity>,
    val sessions: List<SessionEntity>,
    val sessionSnacks: List<SessionSnackEntity>,
    val snackSales: List<SnackSaleEntity>,
    val reservations: List<ReservationEntity> = emptyList()
)

suspend fun collectBackup(dao: KasirDao, exportedAtMillis: Long): BackupData =
    BackupData(
        exportedAtMillis = exportedAtMillis,
        units = dao.allUnitsOnce(),
        snacks = dao.allSnacksOnce(),
        sessions = dao.allSessionsOnce(),
        sessionSnacks = dao.allSessionSnacksOnce(),
        snackSales = dao.allSnackSalesOnce(),
        reservations = dao.allReservationsOnce()
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
        dao.clearSessions()
        dao.clearSnacks()
        dao.clearUnits()
        data.units.forEach { dao.insertUnit(it) }
        data.snacks.forEach { dao.insertSnack(it) }
        data.sessions.forEach { dao.insertSession(it) }
        data.sessionSnacks.forEach { dao.insertSessionSnack(it) }
        data.snackSales.forEach { dao.insertSnackSale(it) }
        data.reservations.forEach { dao.insertReservation(it) }
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

