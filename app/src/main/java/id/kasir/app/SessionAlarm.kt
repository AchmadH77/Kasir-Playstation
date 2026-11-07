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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Notifikasi sistem saat waktu paket habis: dijadwalkan dengan
// AlarmManager setiap sesi berjalan, jadi tetap muncul walaupun
// aplikasi sedang tertutup. Mengetuknya membuka rincian sesi itu.

const val SESSION_END_CHANNEL = "sesi_habis"
const val EXTRA_OPEN_SESSION = "id.kasir.app.OPEN_SESSION"

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

object SessionAlarm {

    private fun broadcastIntent(
        context: Context,
        sessionId: Long,
        unitName: String,
        paketMinutes: Int
    ): Intent =
        Intent(context, SessionEndReceiver::class.java).apply {
            putExtra("sessionId", sessionId)
            putExtra("unitName", unitName)
            putExtra("paketMinutes", paketMinutes)
        }

    fun schedule(
        context: Context,
        sessionId: Long,
        unitName: String,
        paketMinutes: Int,
        endEpoch: Long
    ) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            sessionId.toInt(),
            broadcastIntent(context, sessionId, unitName, paketMinutes),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val trigger = if (endEpoch <= System.currentTimeMillis()) {
            System.currentTimeMillis() + 1000L
        } else {
            endEpoch
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                alarm.canScheduleExactAlarms()
            ) {
                alarm.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, trigger, pi
                )
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        } catch (e: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    fun cancel(context: Context, sessionId: Long) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context,
            sessionId.toInt(),
            Intent(context, SessionEndReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarm.cancel(pi)
        pi.cancel()
    }
}

class SessionEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getLongExtra("sessionId", -1L)
        if (sessionId <= 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val session = DatabaseProvider.get(context).dao()
                    .sessionOnce(sessionId)
                // Alarm bisa basi (sesi diperpanjang/dijeda/selesai):
                // tampilkan hanya bila sesi memang sudah habis sekarang.
                if (session != null &&
                    session.status != STATUS_FINISHED &&
                    session.mode == MODE_PAKET &&
                    session.status != STATUS_PAUSED &&
                    remainingMillis(session, System.currentTimeMillis()) <= 0
                ) {
                    showSessionEndNotification(
                        context,
                        session.id,
                        session.unitName,
                        session.paketMinutes
                    )
                }
            } catch (e: Exception) {
                // Satu alarm gagal tidak boleh menjatuhkan proses.
            } finally {
                pending.finish()
            }
        }
    }
}

private fun showSessionEndNotification(
    context: Context,
    sessionId: Long,
    unitName: String,
    paketMinutes: Int
) {
    ensureSessionChannel(context)
    val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_OPEN_SESSION, sessionId)
    }
    val contentPi = PendingIntent.getActivity(
        context,
        sessionId.toInt(),
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val text = "Waktu sesi di " + unitName + " sudah habis (Paket " +
        formatMinutes(paketMinutes) + "). Ketuk untuk membuka sesinya."
    val notification = NotificationCompat.Builder(context, SESSION_END_CHANNEL)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("Waktu Habis")
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .setContentIntent(contentPi)
        .build()
    try {
        NotificationManagerCompat.from(context)
            .notify(sessionId.toInt(), notification)
    } catch (e: SecurityException) {
        // Izin notifikasi belum diberikan: lewati diam-diam.
    }
}

// Setelah HP menyala ulang semua alarm hilang: jadwalkan ulang untuk
// sesi paket yang masih berjalan.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessions = DatabaseProvider.get(context).dao()
                    .activeSessionsFlow()
                    .first()
                sessions
                    .filter {
                        it.mode == MODE_PAKET && it.status != STATUS_PAUSED
                    }
                    .forEach {
                        SessionAlarm.schedule(
                            context,
                            it.id,
                            it.unitName,
                            it.paketMinutes,
                            paketEndEpoch(it)
                        )
                    }
            } catch (e: Exception) {
                // Abaikan: rekonsiliasi di aplikasi akan menjadwalkan ulang.
            } finally {
                pending.finish()
            }
        }
    }
}
