package id.kasir.app

import id.kasir.app.data.ReservationEntity
import id.kasir.app.data.SessionEntity
import id.kasir.app.data.SnackSaleEntity
import id.kasir.app.data.SnackEntity
import id.kasir.app.data.UnitEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

const val MODE_PAKET = "PAKET"
const val MODE_OPEN = "OPEN"
const val STATUS_RUNNING = "RUNNING"
const val STATUS_PAUSED = "PAUSED"
const val STATUS_FINISHED = "FINISHED"
const val PAY_TUNAI = "TUNAI"
const val PAY_QRIS = "QRIS"

// Open billing dibulatkan ke atas per blok 10 menit, seperti billing rental pada umumnya.
const val BLOCK_MINUTES = 10

val CONSOLE_TYPES = listOf("PS3", "PS4", "PS5")
val PAKET_OPTIONS = listOf(30, 60, 120, 180, 240, 300)

// Tarif per jam mengikuti jenis konsol; formulir unit tidak lagi meminta
// tarif sendiri (varian U2, dipilih 2026-11-05).
fun hourlyRateFor(consoleType: String): Long =
    when (consoleType.trim().uppercase()) {
        "PS3" -> 5_000L
        "PS4" -> 8_000L
        "PS5" -> 10_000L
        else -> 0L
    }

// Tarif yang berlaku untuk satu unit: dari jenis konsolnya. Nilai tersimpan
// di unit hanya dipakai bila jenis konsolnya tidak dikenal.
fun effectiveRate(unit: UnitEntity): Long =
    hourlyRateFor(unit.consoleType).takeIf { it > 0L } ?: unit.hourlyRate

fun formatRupiah(value: Long): String {
    val nf = NumberFormat.getNumberInstance(Locale("id", "ID"))
    return "Rp " + nf.format(value)
}

// Tampilan nominal saat diketik di kolom uang: digit diberi pemisah
// ribuan (5000 -> "5.000") supaya terbaca. State tetap digit mentah;
// "," atau "." yang ikut terketik tersaring keluar oleh filter digit.
fun groupThousands(digits: String): String {
    val clean = digits.filter { it.isDigit() }.trimStart('0')
    if (clean.isEmpty()) return if (digits.any { it.isDigit() }) "0" else ""
    val sb = StringBuilder()
    val first = clean.length % 3
    if (first > 0) sb.append(clean.substring(0, first))
    var i = first
    while (i < clean.length) {
        if (sb.isNotEmpty()) sb.append('.')
        sb.append(clean.substring(i, i + 3))
        i += 3
    }
    return sb.toString()
}

// Format ringkas untuk label grafik: 96000 -> "96 rb", 1500000 -> "1,5 jt".
fun formatRupiahShort(value: Long): String {
    if (value < 1000L) return value.toString()
    if (value < 1000000L) return (value / 1000L).toString() + " rb"
    val whole = value / 1000000L
    val tenth = (value % 1000000L) / 100000L
    return if (tenth == 0L) {
        whole.toString() + " jt"
    } else {
        whole.toString() + "," + tenth + " jt"
    }
}

fun formatMinutes(min: Int): String {
    val h = min / 60
    val m = min % 60
    return when {
        h > 0 && m > 0 -> "$h jam $m mnt"
        h > 0 -> "$h jam"
        else -> "$m mnt"
    }
}

// Lama main berjalan dalam milidetik; waktu jeda tidak dihitung.
fun elapsedMillis(session: SessionEntity, now: Long): Long {
    var paused = session.pausedMillis
    val ps = session.pauseStartEpoch
    if (session.status == STATUS_PAUSED && ps != null) {
        paused += (now - ps)
    }
    val end = session.endEpoch ?: now
    return (end - session.startEpoch - paused).coerceAtLeast(0)
}

fun elapsedMinutes(session: SessionEntity, now: Long): Int =
    (elapsedMillis(session, now) / 60000L).toInt()

fun billableOpenMinutes(elapsedMin: Int): Int {
    if (elapsedMin <= 0) return BLOCK_MINUTES
    return ceil(elapsedMin.toDouble() / BLOCK_MINUTES).toInt() * BLOCK_MINUTES
}

// Total menit yang ditagih untuk satu sesi pada waktu tertentu.
fun billableMinutes(session: SessionEntity, now: Long): Int {
    val elapsed = elapsedMinutes(session, now)
    return if (session.mode == MODE_PAKET) {
        val over = elapsed - session.paketMinutes
        if (over <= 0) session.paketMinutes else session.paketMinutes + billableOpenMinutes(over)
    } else {
        billableOpenMinutes(elapsed)
    }
}

// Biaya sewa kotor pada waktu tertentu. Sesi paket berharga manual
// (paketPrice > 0) selalu ditagih sebesar harga yang diketik saat mulai:
// flat, tidak bertambah walau lewat dari durasi paketnya.
fun rentalCost(session: SessionEntity, now: Long): Long {
    if (session.paketPrice > 0L) return session.paketPrice
    return session.hourlyRate * billableMinutes(session, now) / 60
}

// Menit yang ditampilkan sebagai durasi tertagih: untuk sesi berharga
// manual selalu durasi paketnya, bukan hasil hitungan blok.
fun billedMinutes(session: SessionEntity, now: Long): Int =
    if (session.paketPrice > 0L) session.paketMinutes else billableMinutes(session, now)

fun remainingMillis(session: SessionEntity, now: Long): Long =
    session.paketMinutes * 60000L - elapsedMillis(session, now)

// Jam dinding saat paket sesi berakhir, dengan asumsi tidak ada jeda
// tambahan mulai sekarang. Dipakai untuk menjadwalkan alarm notifikasi.
fun paketEndEpoch(session: SessionEntity): Long =
    session.startEpoch + session.pausedMillis + session.paketMinutes * 60000L

// Reservasi terdekat untuk satu unit mulai dari [fromMs] (inklusif):
// yang jam mulainya paling awal di antara yang belum lewat.
fun nextReservationFor(
    reservations: List<ReservationEntity>,
    unitId: Long,
    fromMs: Long
): ReservationEntity? =
    reservations
        .filter { it.unitId == unitId && it.startEpoch >= fromMs }
        .minByOrNull { it.startEpoch }

// Menu makanan dianggap stoknya menipis pada jumlah segini atau kurang
// (0 berarti habis). Dipakai lencana di tab Makanan dan kartu peringatan.
const val LOW_STOCK_THRESHOLD = 3

fun lowStockSnacks(snacks: List<SnackEntity>): List<SnackEntity> =
    snacks
        .filter { it.stock <= LOW_STOCK_THRESHOLD }
        .sortedBy { it.stock }

fun formatHms(millis: Long): String {
    val totalSec = (millis / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

fun formatDateTime(epoch: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(epoch))

fun formatTime(epoch: Long): String =
    SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(epoch))

fun startOfDay(epoch: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = epoch
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun startOfWeek(epoch: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = startOfDay(epoch)
    c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    return c.timeInMillis
}

fun startOfMonth(epoch: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = startOfDay(epoch)
    c.set(Calendar.DAY_OF_MONTH, 1)
    return c.timeInMillis
}

// Pendapatan per jam (0-23) untuk grafik Jam Tersibuk di Laporan:
// sesi dihitung pada jam selesainya (endEpoch), penjualan langsung
// pada jam transaksinya; yang dijumlahkan adalah total masing-masing.
fun hourlyTotals(
    sessions: List<SessionEntity>,
    sales: List<SnackSaleEntity>
): LongArray {
    val buckets = LongArray(24)
    val cal = Calendar.getInstance()
    fun add(epoch: Long, total: Long) {
        cal.timeInMillis = epoch
        buckets[cal.get(Calendar.HOUR_OF_DAY)] += total
    }
    sessions.forEach { s -> s.endEpoch?.let { add(it, s.total) } }
    sales.forEach { add(it.epoch, it.total) }
    return buckets
}
