package id.kasir.app.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await

/**
 * Lapisan online opsional (pola yang sama dengan KasBulanan dan Apotek
 * Lestari, docs/ONLINE.md). Semua akses Firebase dijaga: tanpa
 * google-services.json Firebase tidak pernah terinisialisasi dan
 * lapisan ini diam - aplikasi berjalan offline persis seperti
 * sebelumnya.
 */
object FirebaseKit {
    fun siap(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    fun auth(): FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }

    fun firestore(): FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        null
    }

    fun emailMasuk(): String? = auth()?.currentUser?.email

    fun keluar() {
        auth()?.signOut()
    }

    /**
     * Login Google hanya mungkin bila google-services.json memuat web
     * client id (muncul setelah provider Google diaktifkan dan sidik
     * jari SHA-1 didaftarkan - lihat docs/ONLINE.md). Dicek lewat
     * resource yang dibangkitkan plugin, bukan referensi langsung,
     * supaya kode tetap terkompilasi saat file-nya belum memuatnya.
     */
    fun googleSiap(context: Context): Boolean =
        context.resources.getIdentifier(
            "default_web_client_id", "string", context.packageName
        ) != 0

    suspend fun masukDenganGoogle(context: Context): String {
        val auth = auth() ?: throw IllegalStateException("Firebase belum dikonfigurasi")
        val idRes = context.resources.getIdentifier(
            "default_web_client_id", "string", context.packageName
        )
        if (idRes == 0) throw IllegalStateException("Login Google belum dikonfigurasi")
        val opsi = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(idRes))
            .build()
        val permintaan = GetCredentialRequest.Builder()
            .addCredentialOption(opsi)
            .build()
        val hasil = CredentialManager.create(context).getCredential(context, permintaan)
        val kredensial = hasil.credential
        if (kredensial is CustomCredential &&
            kredensial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val akun = GoogleIdTokenCredential.createFrom(kredensial.data)
            val hasilMasuk = auth.signInWithCredential(
                GoogleAuthProvider.getCredential(akun.idToken, null)
            ).await()
            return hasilMasuk.user?.email ?: akun.id
        }
        throw IllegalStateException("Kredensial Google tidak dikenal")
    }
}

sealed interface HasilSync {
    data object BelumDikonfigurasi : HasilSync
    data object BelumMasuk : HasilSync
    data class Berhasil(val didorong: Int, val ditarik: Int) : HasilSync
    data class Gagal(val pesan: String) : HasilSync
}

/**
 * Konsolidasi ganti akun (pelajaran dari Apotek Lestari, langsung
 * termasuk sejak awal): tandai SEMUA baris kotor supaya sinkron
 * berikutnya menaikkan seluruh isi lokal ke pohon awan akun yang baru
 * masuk. updatedAt sengaja tidak diubah - adu mutakhir harus tetap
 * jujur terhadap suntingan lebih baru dari perangkat lain.
 */
suspend fun tandaiSemuaKotor(dao: KasirDao) {
    dao.unitsTandaiSemuaKotor()
    dao.snacksTandaiSemuaKotor()
    dao.sessionsTandaiSemuaKotor()
    dao.sessionSnacksTandaiSemuaKotor()
    dao.snackSalesTandaiSemuaKotor()
    dao.reservationsTandaiSemuaKotor()
    dao.expensesTandaiSemuaKotor()
}

/**
 * Sinkronisasi dua arah Room <-> Firestore: dorong baris kotor (induk
 * dulu, baru anaknya), lalu tarik semua dokumen dan terapkan yang
 * menang menurut last-write-wins. Struktur server di bawah
 * users/{uid}/: unit, snack, sesi, sesi_snack, jual_snack, reservasi,
 * pengeluaran - satu dokumen per baris, dikunci remoteId-nya.
 */
class SyncManager(private val dao: KasirDao) {

    suspend fun sinkronSekarang(context: Context): HasilSync {
        if (!FirebaseKit.siap(context)) return HasilSync.BelumDikonfigurasi
        val uid = FirebaseKit.auth()?.currentUser?.uid ?: return HasilSync.BelumMasuk
        val fs = FirebaseKit.firestore() ?: return HasilSync.BelumDikonfigurasi
        return try {
            var didorong = 0
            var ditarik = 0
            val akar = fs.collection("users").document(uid)

            // 1) Dorong baris kotor: unit & snack (induk) lebih dulu,
            // sesi sesudahnya, anak-anak terakhir karena mereka
            // menunjuk remoteId induknya.
            for (e in dao.unitsKotor()) {
                if (e.remoteId.isBlank()) continue
                akar.collection("unit").document(e.remoteId)
                    .set(SyncLogic.unitKePeta(e)).await()
                dao.unitTandaiBersih(e.remoteId, e.updatedAt)
                didorong++
            }
            for (e in dao.snacksKotor()) {
                if (e.remoteId.isBlank()) continue
                akar.collection("snack").document(e.remoteId)
                    .set(SyncLogic.snackKePeta(e)).await()
                dao.snackTandaiBersih(e.remoteId, e.updatedAt)
                didorong++
            }
            for (e in dao.sessionsKotor()) {
                if (e.remoteId.isBlank()) continue
                val unit = dao.unitByIdAny(e.unitId)
                akar.collection("sesi").document(e.remoteId)
                    .set(SyncLogic.sesiKePeta(e, unit?.remoteId ?: "")).await()
                dao.sessionTandaiBersih(e.remoteId, e.updatedAt)
                didorong++
            }
            for (e in dao.sessionSnacksKotor()) {
                if (e.remoteId.isBlank()) continue
                val sesi = dao.sessionByIdAny(e.sessionId) ?: continue
                val snack = dao.snackByIdAny(e.snackId)
                akar.collection("sesi_snack").document(e.remoteId)
                    .set(SyncLogic.sesiSnackKePeta(e, sesi.remoteId, snack?.remoteId ?: "")).await()
                dao.sessionSnackTandaiBersih(e.remoteId, e.updatedAt)
                didorong++
            }
            for (e in dao.snackSalesKotor()) {
                if (e.remoteId.isBlank()) continue
                val snack = dao.snackByIdAny(e.snackId)
                akar.collection("jual_snack").document(e.remoteId)
                    .set(SyncLogic.jualSnackKePeta(e, snack?.remoteId ?: "")).await()
                dao.snackSaleTandaiBersih(e.remoteId, e.updatedAt)
                didorong++
            }
            for (e in dao.reservationsKotor()) {
                if (e.remoteId.isBlank()) continue
                val unit = dao.unitByIdAny(e.unitId)
                akar.collection("reservasi").document(e.remoteId)
                    .set(SyncLogic.reservasiKePeta(e, unit?.remoteId ?: "")).await()
                dao.reservationTandaiBersih(e.remoteId, e.updatedAt)
                didorong++
            }
            for (e in dao.expensesKotor()) {
                if (e.remoteId.isBlank()) continue
                akar.collection("pengeluaran").document(e.remoteId)
                    .set(SyncLogic.pengeluaranKePeta(e)).await()
                dao.expenseTandaiBersih(e.remoteId, e.updatedAt)
                didorong++
            }

            // 2) Tarik semua dokumen dengan urutan yang sama; induk
            // diterapkan lebih dulu supaya anak menemukan induknya.
            for (doc in akar.collection("unit").get().await().documents) {
                val e = SyncLogic.unitDariPeta(doc.data ?: continue) ?: continue
                terapkanUnit(e)
                ditarik++
            }
            for (doc in akar.collection("snack").get().await().documents) {
                val e = SyncLogic.snackDariPeta(doc.data ?: continue) ?: continue
                terapkanSnack(e)
                ditarik++
            }
            for (doc in akar.collection("sesi").get().await().documents) {
                val data = doc.data ?: continue
                val unitRemote = SyncLogic.sesiUnitRemoteId(data)
                val unit = if (unitRemote != null) dao.unitByRemoteId(unitRemote) else null
                val e = SyncLogic.sesiDariPeta(data, unit?.id ?: 0L) ?: continue
                terapkanSesi(e)
                ditarik++
            }
            for (doc in akar.collection("sesi_snack").get().await().documents) {
                val data = doc.data ?: continue
                val sesiRemote = SyncLogic.sesiSnackSesiRemoteId(data) ?: continue
                val sesi = dao.sessionByRemoteId(sesiRemote) ?: continue
                val snackRemote = SyncLogic.sesiSnackSnackRemoteId(data)
                val snack = if (snackRemote != null) dao.snackByRemoteId(snackRemote) else null
                val e = SyncLogic.sesiSnackDariPeta(data, sesi.id, snack?.id ?: 0L) ?: continue
                terapkanSesiSnack(e)
                ditarik++
            }
            for (doc in akar.collection("jual_snack").get().await().documents) {
                val data = doc.data ?: continue
                val snackRemote = SyncLogic.jualSnackSnackRemoteId(data)
                val snack = if (snackRemote != null) dao.snackByRemoteId(snackRemote) else null
                val e = SyncLogic.jualSnackDariPeta(data, snack?.id ?: 0L) ?: continue
                terapkanJualSnack(e)
                ditarik++
            }
            for (doc in akar.collection("reservasi").get().await().documents) {
                val data = doc.data ?: continue
                val unitRemote = SyncLogic.reservasiUnitRemoteId(data)
                val unit = if (unitRemote != null) dao.unitByRemoteId(unitRemote) else null
                val e = SyncLogic.reservasiDariPeta(data, unit?.id ?: 0L) ?: continue
                terapkanReservasi(e)
                ditarik++
            }
            for (doc in akar.collection("pengeluaran").get().await().documents) {
                val e = SyncLogic.pengeluaranDariPeta(doc.data ?: continue) ?: continue
                terapkanPengeluaran(e)
                ditarik++
            }

            dao.purgeUnits()
            dao.purgeSnacks()
            dao.purgeSessions()
            dao.purgeSessionSnacks()
            dao.purgeSnackSales()
            dao.purgeReservations()
            dao.purgeExpenses()
            HasilSync.Berhasil(didorong, ditarik)
        } catch (e: Exception) {
            HasilSync.Gagal(e.message ?: "Sinkronisasi gagal")
        }
    }

    // Penerapan hasil tarikan: baris baru disisipkan bersih; baris
    // lama hanya ditimpa bila salinan remote menang adu mutakhir.
    private suspend fun terapkanUnit(r: UnitEntity) {
        val lokal = dao.unitByRemoteId(r.remoteId)
        if (lokal == null) {
            dao.insertUnit(r)
        } else if (SyncLogic.remoteMenang(r.updatedAt, r.terhapus == 1, lokal.updatedAt, lokal.terhapus == 1)) {
            dao.updateUnit(r.copy(id = lokal.id))
        }
    }

    private suspend fun terapkanSnack(r: SnackEntity) {
        val lokal = dao.snackByRemoteId(r.remoteId)
        if (lokal == null) {
            dao.insertSnack(r)
        } else if (SyncLogic.remoteMenang(r.updatedAt, r.terhapus == 1, lokal.updatedAt, lokal.terhapus == 1)) {
            dao.updateSnack(r.copy(id = lokal.id))
        }
    }

    private suspend fun terapkanSesi(r: SessionEntity) {
        val lokal = dao.sessionByRemoteId(r.remoteId)
        if (lokal == null) {
            dao.insertSession(r)
        } else if (SyncLogic.remoteMenang(r.updatedAt, r.terhapus == 1, lokal.updatedAt, lokal.terhapus == 1)) {
            dao.updateSession(r.copy(id = lokal.id))
        }
    }

    private suspend fun terapkanSesiSnack(r: SessionSnackEntity) {
        val lokal = dao.sessionSnackByRemoteId(r.remoteId)
        if (lokal == null) {
            dao.insertSessionSnack(r)
        } else if (SyncLogic.remoteMenang(r.updatedAt, r.terhapus == 1, lokal.updatedAt, lokal.terhapus == 1)) {
            dao.updateSessionSnack(r.copy(id = lokal.id))
        }
    }

    private suspend fun terapkanJualSnack(r: SnackSaleEntity) {
        val lokal = dao.snackSaleByRemoteId(r.remoteId)
        if (lokal == null) {
            dao.insertSnackSale(r)
        } else if (SyncLogic.remoteMenang(r.updatedAt, r.terhapus == 1, lokal.updatedAt, lokal.terhapus == 1)) {
            dao.updateSnackSale(r.copy(id = lokal.id))
        }
    }

    private suspend fun terapkanReservasi(r: ReservationEntity) {
        val lokal = dao.reservationByRemoteId(r.remoteId)
        if (lokal == null) {
            dao.insertReservation(r)
        } else if (SyncLogic.remoteMenang(r.updatedAt, r.terhapus == 1, lokal.updatedAt, lokal.terhapus == 1)) {
            dao.updateReservation(r.copy(id = lokal.id))
        }
    }

    private suspend fun terapkanPengeluaran(r: ExpenseEntity) {
        val lokal = dao.expenseByRemoteId(r.remoteId)
        if (lokal == null) {
            dao.insertExpense(r)
        } else if (SyncLogic.remoteMenang(r.updatedAt, r.terhapus == 1, lokal.updatedAt, lokal.terhapus == 1)) {
            dao.updateExpense(r.copy(id = lokal.id))
        }
    }
}

/** Sync berkala di latar belakang; diam bila belum dikonfigurasi/masuk. */
class SyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!FirebaseKit.siap(applicationContext)) return Result.success()
        if (FirebaseKit.auth()?.currentUser == null) return Result.success()
        val dao = DatabaseProvider.get(applicationContext).dao()
        return when (val hasil = SyncManager(dao).sinkronSekarang(applicationContext)) {
            is HasilSync.Berhasil -> {
                applicationContext
                    .getSharedPreferences("kasir_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putLong("online_sync_terakhir", System.currentTimeMillis())
                    .apply()
                Result.success()
            }
            is HasilSync.Gagal -> Result.retry()
            else -> Result.success()
        }
    }
}

/** Jadwalkan sync berkala 6 jam sekali (hanya berjalan saat ada jaringan). */
fun jadwalkanSyncBerkala(context: Context) {
    val syarat = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val permintaan = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
        .setConstraints(syarat)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "kasir-sync",
        ExistingPeriodicWorkPolicy.KEEP,
        permintaan
    )
}
