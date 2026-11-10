package id.kasir.app

import id.kasir.app.data.ExpenseEntity
import id.kasir.app.data.SessionEntity
import id.kasir.app.data.SessionSnackEntity
import id.kasir.app.data.SyncLogic
import id.kasir.app.data.UnitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncLogicTest {

    @Test
    fun remoteMenang_capLebihBaruMenang() {
        assertTrue(SyncLogic.remoteMenang(200, false, 100, false))
        assertFalse(SyncLogic.remoteMenang(100, false, 200, false))
    }

    @Test
    fun remoteMenang_capSamaKuburanMenang() {
        assertTrue(SyncLogic.remoteMenang(100, true, 100, false))
        assertFalse(SyncLogic.remoteMenang(100, false, 100, true))
        assertFalse(SyncLogic.remoteMenang(100, true, 100, true))
    }

    @Test
    fun unit_putaranPenuh() {
        val asal = UnitEntity(
            id = 7, name = "TV 01", consoleType = "PS5", hourlyRate = 10000,
            remoteId = "unit-7", updatedAt = 1234, terhapus = 0, kotor = 1
        )
        val hasil = SyncLogic.unitDariPeta(SyncLogic.unitKePeta(asal))!!
        assertEquals("TV 01", hasil.name)
        assertEquals("PS5", hasil.consoleType)
        assertEquals(10000L, hasil.hourlyRate)
        assertEquals("unit-7", hasil.remoteId)
        assertEquals(1234L, hasil.updatedAt)
        assertEquals(0, hasil.terhapus)
        assertEquals(0, hasil.kotor)
        assertEquals(0L, hasil.id)
    }

    @Test
    fun sesi_putaranPenuhDenganNilaiNull() {
        val asal = SessionEntity(
            id = 3, unitId = 9, unitName = "TV 03", consoleType = "PS4",
            hourlyRate = 8000, mode = "PAKET", paketMinutes = 120,
            paketPrice = 15000, startEpoch = 5000, pausedMillis = 0,
            pauseStartEpoch = null, status = "RUNNING", endEpoch = null,
            remoteId = "sesi-3", updatedAt = 6000, terhapus = 0, kotor = 1
        )
        val peta = SyncLogic.sesiKePeta(asal, "unit-9")
        val hasil = SyncLogic.sesiDariPeta(peta, 42)!!
        assertEquals(42L, hasil.unitId)
        assertEquals("unit-9", SyncLogic.sesiUnitRemoteId(peta))
        assertEquals(120, hasil.paketMinutes)
        assertEquals(15000L, hasil.paketPrice)
        assertNull(hasil.pauseStartEpoch)
        assertNull(hasil.endEpoch)
        assertNull(hasil.paymentMethod)
        assertEquals("RUNNING", hasil.status)
    }

    @Test
    fun sesiSnack_menunjukIndukLewatRemoteId() {
        val asal = SessionSnackEntity(
            id = 5, sessionId = 3, snackId = 2, snackName = "Kopi Hitam",
            price = 5000, qty = 2,
            remoteId = "sesisnack-5", updatedAt = 7000, terhapus = 1, kotor = 1
        )
        val peta = SyncLogic.sesiSnackKePeta(asal, "sesi-3", "snack-2")
        assertEquals("sesi-3", SyncLogic.sesiSnackSesiRemoteId(peta))
        assertEquals("snack-2", SyncLogic.sesiSnackSnackRemoteId(peta))
        val hasil = SyncLogic.sesiSnackDariPeta(peta, 30, 20)!!
        assertEquals(30L, hasil.sessionId)
        assertEquals(20L, hasil.snackId)
        assertEquals(1, hasil.terhapus)
        assertEquals(2, hasil.qty)
    }

    @Test
    fun pengeluaran_putaranPenuh() {
        val asal = ExpenseEntity(
            id = 4, name = "Listrik", amount = 150000, epoch = 9000,
            remoteId = "pengeluaran-4", updatedAt = 9000, terhapus = 0, kotor = 1
        )
        val hasil = SyncLogic.pengeluaranDariPeta(SyncLogic.pengeluaranKePeta(asal))!!
        assertEquals("Listrik", hasil.name)
        assertEquals(150000L, hasil.amount)
        assertEquals(9000L, hasil.epoch)
    }

    @Test
    fun dariPeta_remoteIdKosongDitolak() {
        assertNull(SyncLogic.unitDariPeta(mapOf("name" to "X")))
        assertNull(SyncLogic.unitDariPeta(mapOf("remoteId" to "  ")))
    }
}
