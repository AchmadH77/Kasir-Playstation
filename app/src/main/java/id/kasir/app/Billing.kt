package id.kasir.app

import id.kasir.app.data.SessionEntity
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
val PAKET_OPTIONS = listOf(60, 120, 180, 240, 300)

fun formatRupiah(value: Long): String {
    val nf = NumberFormat.getNumberInstance(Locale("id", "ID"))
    return "Rp " + nf.format(value)
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

// Biaya sewa kotor pada waktu tertentu.
fun rentalCost(session: SessionEntity, now: Long): Long =
    session.hourlyRate * billableMinutes(session, now) / 60

fun remainingMillis(session: SessionEntity, now: Long): Long =
    session.paketMinutes * 60000L - elapsedMillis(session, now)

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
