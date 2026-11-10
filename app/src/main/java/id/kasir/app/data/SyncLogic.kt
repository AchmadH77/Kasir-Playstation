package id.kasir.app.data

/**
 * Logika sinkronisasi Mode Online yang murni (tanpa Firebase, tanpa
 * Android) supaya bisa diuji di JVM: pemetaan Entity <-> peta dokumen
 * Firestore dan aturan konflik last-write-wins. Pola yang sama seperti
 * KasBulanan dan Apotek Lestari.
 *
 * Baris anak (sesi, jajan sesi, penjualan jajan, reservasi) menunjuk
 * induknya lewat remoteId induk (unitRemoteId / sessionRemoteId /
 * snackRemoteId), karena ID lokal Long berbeda di setiap perangkat.
 * Semua angka disimpan sebagai Long di dokumen.
 */
object SyncLogic {

    /**
     * Aturan konflik: salinan remote menang bila cap waktunya lebih
     * baru; bila sama persis, tombstone (penghapusan) yang menang
     * supaya penghapusan tidak hidup lagi.
     */
    fun remoteMenang(
        remoteUpdatedAt: Long,
        remoteTerhapus: Boolean,
        lokalUpdatedAt: Long,
        lokalTerhapus: Boolean
    ): Boolean =
        if (remoteUpdatedAt != lokalUpdatedAt) remoteUpdatedAt > lokalUpdatedAt
        else remoteTerhapus && !lokalTerhapus

    private fun Map<String, Any?>.teks(kunci: String): String? = this[kunci] as? String
    private fun Map<String, Any?>.teksWajib(kunci: String): String = this[kunci] as? String ?: ""
    private fun Map<String, Any?>.angka(kunci: String): Long =
        (this[kunci] as? Number)?.toLong() ?: 0L

    private fun Map<String, Any?>.angkaNull(kunci: String): Long? =
        (this[kunci] as? Number)?.toLong()

    private fun Map<String, Any?>.int(kunci: String): Int = angka(kunci).toInt()
    private fun Map<String, Any?>.terhapus(): Int =
        if (this["terhapus"] as? Boolean == true) 1 else 0

    private fun dasar(remoteId: String, updatedAt: Long, terhapus: Int): MutableMap<String, Any?> =
        mutableMapOf(
            "remoteId" to remoteId,
            "updatedAt" to updatedAt,
            "terhapus" to (terhapus == 1)
        )

    // ---- Unit ----

    fun unitKePeta(e: UnitEntity): Map<String, Any?> =
        dasar(e.remoteId, e.updatedAt, e.terhapus).apply {
            put("name", e.name)
            put("consoleType", e.consoleType)
            put("hourlyRate", e.hourlyRate)
        }

    fun unitDariPeta(m: Map<String, Any?>): UnitEntity? {
        val remoteId = m.teks("remoteId")?.takeIf { it.isNotBlank() } ?: return null
        return UnitEntity(
            name = m.teksWajib("name"),
            consoleType = m.teksWajib("consoleType"),
            hourlyRate = m.angka("hourlyRate"),
            remoteId = remoteId,
            updatedAt = m.angka("updatedAt"),
            terhapus = m.terhapus(),
            kotor = 0
        )
    }

    // ---- Snack ----

    fun snackKePeta(e: SnackEntity): Map<String, Any?> =
        dasar(e.remoteId, e.updatedAt, e.terhapus).apply {
            put("name", e.name)
            put("price", e.price)
            put("stock", e.stock.toLong())
        }

    fun snackDariPeta(m: Map<String, Any?>): SnackEntity? {
        val remoteId = m.teks("remoteId")?.takeIf { it.isNotBlank() } ?: return null
        return SnackEntity(
            name = m.teksWajib("name"),
            price = m.angka("price"),
            stock = m.int("stock"),
            remoteId = remoteId,
            updatedAt = m.angka("updatedAt"),
            terhapus = m.terhapus(),
            kotor = 0
        )
    }

    // ---- Sesi ----

    fun sesiKePeta(e: SessionEntity, unitRemoteId: String): Map<String, Any?> =
        dasar(e.remoteId, e.updatedAt, e.terhapus).apply {
            put("unitRemoteId", unitRemoteId)
            put("unitName", e.unitName)
            put("consoleType", e.consoleType)
            put("hourlyRate", e.hourlyRate)
            put("mode", e.mode)
            put("paketMinutes", e.paketMinutes.toLong())
            put("paketPrice", e.paketPrice)
            put("startEpoch", e.startEpoch)
            put("pausedMillis", e.pausedMillis)
            put("pauseStartEpoch", e.pauseStartEpoch)
            put("status", e.status)
            put("endEpoch", e.endEpoch)
            put("rentalCost", e.rentalCost)
            put("snackCost", e.snackCost)
            put("total", e.total)
            put("paymentMethod", e.paymentMethod)
            put("paidAmount", e.paidAmount)
            put("changeAmount", e.changeAmount)
        }

    fun sesiUnitRemoteId(m: Map<String, Any?>): String? = m.teks("unitRemoteId")

    fun sesiDariPeta(m: Map<String, Any?>, unitId: Long): SessionEntity? {
        val remoteId = m.teks("remoteId")?.takeIf { it.isNotBlank() } ?: return null
        return SessionEntity(
            unitId = unitId,
            unitName = m.teksWajib("unitName"),
            consoleType = m.teksWajib("consoleType"),
            hourlyRate = m.angka("hourlyRate"),
            mode = m.teksWajib("mode"),
            paketMinutes = m.int("paketMinutes"),
            paketPrice = m.angka("paketPrice"),
            startEpoch = m.angka("startEpoch"),
            pausedMillis = m.angka("pausedMillis"),
            pauseStartEpoch = m.angkaNull("pauseStartEpoch"),
            status = m.teks("status") ?: "RUNNING",
            endEpoch = m.angkaNull("endEpoch"),
            rentalCost = m.angka("rentalCost"),
            snackCost = m.angka("snackCost"),
            total = m.angka("total"),
            paymentMethod = m.teks("paymentMethod"),
            paidAmount = m.angka("paidAmount"),
            changeAmount = m.angka("changeAmount"),
            remoteId = remoteId,
            updatedAt = m.angka("updatedAt"),
            terhapus = m.terhapus(),
            kotor = 0
        )
    }

    // ---- Jajan sesi ----

    fun sesiSnackKePeta(
        e: SessionSnackEntity,
        sessionRemoteId: String,
        snackRemoteId: String
    ): Map<String, Any?> =
        dasar(e.remoteId, e.updatedAt, e.terhapus).apply {
            put("sessionRemoteId", sessionRemoteId)
            put("snackRemoteId", snackRemoteId)
            put("snackName", e.snackName)
            put("price", e.price)
            put("qty", e.qty.toLong())
        }

    fun sesiSnackSesiRemoteId(m: Map<String, Any?>): String? = m.teks("sessionRemoteId")
    fun sesiSnackSnackRemoteId(m: Map<String, Any?>): String? = m.teks("snackRemoteId")

    fun sesiSnackDariPeta(m: Map<String, Any?>, sessionId: Long, snackId: Long): SessionSnackEntity? {
        val remoteId = m.teks("remoteId")?.takeIf { it.isNotBlank() } ?: return null
        return SessionSnackEntity(
            sessionId = sessionId,
            snackId = snackId,
            snackName = m.teksWajib("snackName"),
            price = m.angka("price"),
            qty = m.int("qty"),
            remoteId = remoteId,
            updatedAt = m.angka("updatedAt"),
            terhapus = m.terhapus(),
            kotor = 0
        )
    }

    // ---- Penjualan jajan langsung ----

    fun jualSnackKePeta(e: SnackSaleEntity, snackRemoteId: String): Map<String, Any?> =
        dasar(e.remoteId, e.updatedAt, e.terhapus).apply {
            put("snackRemoteId", snackRemoteId)
            put("snackName", e.snackName)
            put("price", e.price)
            put("qty", e.qty.toLong())
            put("total", e.total)
            put("epoch", e.epoch)
            put("paymentMethod", e.paymentMethod)
        }

    fun jualSnackSnackRemoteId(m: Map<String, Any?>): String? = m.teks("snackRemoteId")

    fun jualSnackDariPeta(m: Map<String, Any?>, snackId: Long): SnackSaleEntity? {
        val remoteId = m.teks("remoteId")?.takeIf { it.isNotBlank() } ?: return null
        return SnackSaleEntity(
            snackId = snackId,
            snackName = m.teksWajib("snackName"),
            price = m.angka("price"),
            qty = m.int("qty"),
            total = m.angka("total"),
            epoch = m.angka("epoch"),
            paymentMethod = m.teksWajib("paymentMethod"),
            remoteId = remoteId,
            updatedAt = m.angka("updatedAt"),
            terhapus = m.terhapus(),
            kotor = 0
        )
    }

    // ---- Reservasi ----

    fun reservasiKePeta(e: ReservationEntity, unitRemoteId: String): Map<String, Any?> =
        dasar(e.remoteId, e.updatedAt, e.terhapus).apply {
            put("unitRemoteId", unitRemoteId)
            put("unitName", e.unitName)
            put("customerName", e.customerName)
            put("startEpoch", e.startEpoch)
            put("durationMinutes", e.durationMinutes.toLong())
            put("price", e.price)
        }

    fun reservasiUnitRemoteId(m: Map<String, Any?>): String? = m.teks("unitRemoteId")

    fun reservasiDariPeta(m: Map<String, Any?>, unitId: Long): ReservationEntity? {
        val remoteId = m.teks("remoteId")?.takeIf { it.isNotBlank() } ?: return null
        return ReservationEntity(
            unitId = unitId,
            unitName = m.teksWajib("unitName"),
            customerName = m.teksWajib("customerName"),
            startEpoch = m.angka("startEpoch"),
            durationMinutes = m.int("durationMinutes"),
            price = m.angka("price"),
            remoteId = remoteId,
            updatedAt = m.angka("updatedAt"),
            terhapus = m.terhapus(),
            kotor = 0
        )
    }

    // ---- Pengeluaran ----

    fun pengeluaranKePeta(e: ExpenseEntity): Map<String, Any?> =
        dasar(e.remoteId, e.updatedAt, e.terhapus).apply {
            put("name", e.name)
            put("amount", e.amount)
            put("epoch", e.epoch)
        }

    fun pengeluaranDariPeta(m: Map<String, Any?>): ExpenseEntity? {
        val remoteId = m.teks("remoteId")?.takeIf { it.isNotBlank() } ?: return null
        return ExpenseEntity(
            name = m.teksWajib("name"),
            amount = m.angka("amount"),
            epoch = m.angka("epoch"),
            remoteId = remoteId,
            updatedAt = m.angka("updatedAt"),
            terhapus = m.terhapus(),
            kotor = 0
        )
    }
}
