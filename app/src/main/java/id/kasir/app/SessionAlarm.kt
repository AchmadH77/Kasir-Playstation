package id.kasir.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import id.kasir.app.data.DatabaseProvider
import id.kasir.app.data.ReservationEntity
import id.kasir.app.data.SessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Notifikasi sistem seputar sesi dan reservasi, dijadwalkan dengan
// AlarmManager supaya tetap muncul walaupun aplikasi tertutup:
// - peringatan 5 menit sebelum paket habis,
// - notifikasi saat paket habis (dengan aksi Tunda 10 Menit),
// - pengingat 15 menit sebelum reservasi mulai.
// Penanda di SharedPreferences mencegah bunyi ganda: memperpanjang
// sesi mengubah jam akhirnya, jadi peringatan boleh berbunyi lagi,
// tetapi rekonsiliasi alarm tidak memicu notifikasi yang sama dua kali.

const val SESSION_END_CHANNEL = "sesi_habis"
const val EXTRA_OPEN_SESSION = "id.kasir.app.OPEN_SESSION"

private const val ALARM_PREFS = "kasir_alarms"
private const val WARN_OFFSET = 100_000
private const val RESERVATION_OFFSET = 200_000
private const val WARN_BEFORE_MS = 5 * 60_000L
private const val REMIND_BEFORE_MS = 15 * 60_000L
private const val SNOOZE_MS = 10 * 60_000L

fun ensureSessionChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(SESSION_END_CHANNEL) == null) {
            val channel = NotificationChannel(
                SESSION_END_CHANNEL,
                "Waktu sesi habis",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifikasi saat waktu paket sebuah unit habis"
            manager.createNotificationChannel(channel)
        }
    }
}

private fun alarmPrefs(context: Context) =
    context.getSharedPreferences(ALARM_PREFS, Context.MODE_PRIVATE)

object SessionAlarm {

    private fun sessionIntent(
        context: Context,
        kind: String,
        session: SessionEntity
    ): Intent =
        Intent(context, SessionEndReceiver::class.java).apply {
            putExtra("kind", kind)
            putExtra("sessionId", session.id)
            putExtra("unitName", session.unitName)
            putExtra("paketMinutes", session.paketMinutes)
        }

    private fun setAlarm(
        context: Context,
        requestCode: Int,
        intent: Intent,
        trigger: Long
    ) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val at = if (trigger <= System.currentTimeMillis()) {
            System.currentTimeMillis() + 1000L
        } else {
            trigger
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                alarm.canScheduleExactAlarms()
            ) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun cancelCode(context: Context, requestCode: Int, clazz: Class<*>) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, clazz),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarm.cancel(pi)
        pi.cancel()
    }

    // Jadwalkan alarm peringatan + alarm habis untuk satu sesi berjalan.
    fun schedule(context: Context, session: SessionEntity) {
        val endEpoch = paketEndEpoch(session)
        val prefs = alarmPrefs(context)
        // Alarm habis: lewati bila jam akhir ini sudah pernah dinotifikasi
        // dan tidak ada tunda yang sedang berjalan.
        val notifiedEnd = prefs.getLong("end_" + session.id, -1L)
        val snoozeUntil = prefs.getLong("snooze_" + session.id, 0L)
        val endTrigger = maxOf(endEpoch, snoozeUntil)
        if (endTrigger > System.currentTimeMillis() || notifiedEnd != endEpoch) {
            setAlarm(
                context,
                session.id.toInt(),
                sessionIntent(context, "end", session),
                endTrigger
            )
        }
        // Alarm peringatan 5 menit sebelumnya, sekali per jam akhir.
        val notifiedWarn = prefs.getLong("warn_" + session.id, -1L)
        if (notifiedWarn != endEpoch) {
            setAlarm(
                context,
                session.id.toInt() + WARN_OFFSET,
                sessionIntent(context, "warn", session),
                endEpoch - WARN_BEFORE_MS
            )
        }
    }

    fun cancel(context: Context, sessionId: Long) {
        cancelCode(context, sessionId.toInt(), SessionEndReceiver::class.java)
        cancelCode(
            context,
            sessionId.toInt() + WARN_OFFSET,
            SessionEndReceiver::class.java
        )
    }

    // Tunda notifikasi habis 10 menit: rekonsiliasi alarm menghormati
    // batas tunda ini, dan bunyi berikutnya diizinkan lagi.
    fun snooze(context: Context, session: SessionEntity) {
        val until = System.currentTimeMillis() + SNOOZE_MS
        alarmPrefs(context).edit()
            .putLong("snooze_" + session.id, until)
            .remove("end_" + session.id)
            .apply()
        setAlarm(
            context,
            session.id.toInt(),
            sessionIntent(context, "end", session),
            until
        )
    }

    fun scheduleReservation(context: Context, reservation: ReservationEntity) {
        val trigger = reservation.startEpoch - REMIND_BEFORE_MS
        if (trigger <= System.currentTimeMillis() &&
            System.currentTimeMillis() >= reservation.startEpoch
        ) {
            return // Sudah lewat jam mulai: tidak perlu pengingat.
        }
        val intent = Intent(context, ReservationReminderReceiver::class.java)
            .apply {
                putExtra("reservationId", reservation.id)
                putExtra("unitName", reservation.unitName)
                putExtra("customerName", reservation.customerName)
                putExtra("startEpoch", reservation.startEpoch)
                putExtra("durationMinutes", reservation.durationMinutes)
            }
        setAlarm(
            context,
            reservation.id.toInt() + RESERVATION_OFFSET,
            intent,
            trigger
        )
    }

    fun cancelReservation(context: Context, reservationId: Long) {
        cancelCode(
            context,
            reservationId.toInt() + RESERVATION_OFFSET,
            ReservationReminderReceiver::class.java
        )
    }
}

class SessionEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getLongExtra("sessionId", -1L)
        if (sessionId <= 0) return
        val kind = intent.getStringExtra("kind") ?: "end"
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val session = DatabaseProvider.get(context).dao()
                    .sessionOnce(sessionId)
                if (session != null &&
                    session.status != STATUS_FINISHED &&
                    session.mode == MODE_PAKET &&
                    session.status != STATUS_PAUSED
                ) {
                    val now = System.currentTimeMillis()
                    val remaining = remainingMillis(session, now)
                    val endEpoch = paketEndEpoch(session)
                    val prefs = alarmPrefs(context)
                    if (kind == "warn") {
                        // Beri toleransi 30 detik untuk alarm tidak persis.
                        if (remaining in 1..(WARN_BEFORE_MS + 30_000L) &&
                            prefs.getLong("warn_" + sessionId, -1L) != endEpoch
                        ) {
                            prefs.edit()
                                .putLong("warn_" + sessionId, endEpoch)
                                .apply()
                            showSessionNotification(
                                context,
                                session.id.toInt() + WARN_OFFSET,
                                "Sesi Mau Habis",
                                "Sisa waktu di " + session.unitName +
                                    " tinggal 5 menit (Paket " +
                                    formatMinutes(session.paketMinutes) +
                                    "). Ketuk untuk membuka sesinya.",
                                session.id,
                                withSnooze = false
                            )
                        }
                    } else {
                        val snoozeUntil =
                            prefs.getLong("snooze_" + sessionId, 0L)
                        if (remaining <= 0 && snoozeUntil <= now &&
                            prefs.getLong("end_" + sessionId, -1L) != endEpoch
                        ) {
                            prefs.edit()
                                .putLong("end_" + sessionId, endEpoch)
                                .remove("snooze_" + sessionId)
                                .apply()
                            showSessionNotification(
                                context,
                                session.id.toInt(),
                                "Waktu Habis",
                                "Waktu sesi di " + session.unitName +
                                    " sudah habis (Paket " +
                                    formatMinutes(session.paketMinutes) +
                                    "). Ketuk untuk membuka sesinya.",
                                session.id,
                                withSnooze = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Satu alarm gagal tidak boleh menjatuhkan proses.
            } finally {
                pending.finish()
            }
        }
    }
}

// Aksi "Tunda 10 Menit" pada notifikasi waktu habis.
class SessionSnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getLongExtra("sessionId", -1L)
        if (sessionId <= 0) return
        NotificationManagerCompat.from(context).cancel(sessionId.toInt())
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val session = DatabaseProvider.get(context).dao()
                    .sessionOnce(sessionId)
                if (session != null && session.status != STATUS_FINISHED) {
                    SessionAlarm.snooze(context, session)
                }
            } catch (e: Exception) {
                // Abaikan.
            } finally {
                pending.finish()
            }
        }
    }
}

class ReservationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reservationId = intent.getLongExtra("reservationId", -1L)
        if (reservationId <= 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reservation = DatabaseProvider.get(context).dao()
                    .reservationOnce(reservationId)
                // Pengingat hanya bila reservasinya masih ada dan belum
                // lewat jauh dari jam mulai.
                if (reservation != null &&
                    System.currentTimeMillis() <=
                    reservation.startEpoch + WARN_BEFORE_MS
                ) {
                    showSessionNotification(
                        context,
                        reservation.id.toInt() + RESERVATION_OFFSET,
                        "Reservasi Sebentar Lagi",
                        reservation.unitName + " untuk " +
                            reservation.customerName + " mulai " +
                            formatTime(reservation.startEpoch) + " (Paket " +
                            formatMinutes(reservation.durationMinutes) + ").",
                        openSessionId = null,
                        withSnooze = false
                    )
                }
            } catch (e: Exception) {
                // Abaikan.
            } finally {
                pending.finish()
            }
        }
    }
}

private fun showSessionNotification(
    context: Context,
    notificationId: Int,
    title: String,
    text: String,
    openSessionId: Long?,
    withSnooze: Boolean
) {
    ensureSessionChannel(context)
    val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (openSessionId != null) {
            putExtra(EXTRA_OPEN_SESSION, openSessionId)
        }
    }
    val contentPi = PendingIntent.getActivity(
        context,
        notificationId,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val builder = NotificationCompat.Builder(context, SESSION_END_CHANNEL)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .setContentIntent(contentPi)
    if (withSnooze && openSessionId != null) {
        val snoozeIntent = Intent(context, SessionSnoozeReceiver::class.java)
            .apply { putExtra("sessionId", openSessionId) }
        val snoozePi = PendingIntent.getBroadcast(
            context,
            openSessionId.toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "Tunda 10 Menit", snoozePi)
    }
    try {
        NotificationManagerCompat.from(context)
            .notify(notificationId, builder.build())
    } catch (e: SecurityException) {
        // Izin notifikasi belum diberikan: lewati diam-diam.
    }
}

// Untuk tombol Tes Notifikasi di tab Lainnya.
fun postTestSessionNotification(context: Context) {
    showSessionNotification(
        context,
        999_001,
        "Waktu Habis",
        "Ini contoh notifikasi: waktu sesi di TV 01 sudah habis " +
            "(Paket 2 jam). Ketuk untuk membuka aplikasinya.",
        openSessionId = null,
        withSnooze = false
    )
}

// Setelah HP menyala ulang semua alarm hilang: jadwalkan ulang untuk
// sesi paket yang masih berjalan dan reservasi yang akan datang.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = DatabaseProvider.get(context).dao()
                dao.activeSessionsFlow()
                    .first()
                    .filter {
                        it.mode == MODE_PAKET && it.status != STATUS_PAUSED
                    }
                    .forEach { SessionAlarm.schedule(context, it) }
                val dayStart = startOfDay(System.currentTimeMillis())
                dao.reservationsFlow()
                    .first()
                    .filter { it.startEpoch >= dayStart }
                    .forEach { SessionAlarm.scheduleReservation(context, it) }
            } catch (e: Exception) {
                // Abaikan: rekonsiliasi di aplikasi akan menjadwalkan ulang.
            } finally {
                pending.finish()
            }
        }
    }
}
