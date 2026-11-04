package id.kasir.app

import id.kasir.app.data.BackupData
import id.kasir.app.data.SessionEntity
import id.kasir.app.data.SessionSnackEntity
import id.kasir.app.data.SnackEntity
import id.kasir.app.data.SnackSaleEntity
import id.kasir.app.data.UnitEntity
import id.kasir.app.data.backupToJson
import id.kasir.app.data.parseBackup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

// Tes untuk berkas backup JSON (BackupHelper) dan ekspor laporan CSV.
class BackupFilesTest {

    private val sampleData = BackupData(
        exportedAtMillis = 1730000000000L,
        units = listOf(
            UnitEntity(id = 1, name = "TV 01", consoleType = "PS3", hourlyRate = 5000)
        ),
        snacks = listOf(
            SnackEntity(id = 1, name = "Kopi Hitam", price = 5000, stock = 20)
        ),
        sessions = listOf(
            SessionEntity(
                id = 1, unitId = 1, unitName = "TV 01", consoleType = "PS3",
                hourlyRate = 5000, mode = "PAKET", paketMinutes = 120,
                paketPrice = 12000, startEpoch = 1000, status = "FINISHED",
                endEpoch = 2000, rentalCost = 12000, snackCost = 5000,
                total = 17000, paymentMethod = "QRIS", paidAmount = 17000,
                changeAmount = 0
            ),
            SessionEntity(
                id = 2, unitId = 1, unitName = "TV 01", consoleType = "PS3",
                hourlyRate = 5000, mode = "PAKET", paketMinutes = 60,
                startEpoch = 3000, pauseStartEpoch = 3500
            )
        ),
        sessionSnacks = listOf(
            SessionSnackEntity(
                id = 1, sessionId = 1, snackId = 1, snackName = "Kopi Hitam",
                price = 5000, qty = 1
            )
        ),
        snackSales = listOf(
            SnackSaleEntity(
                id = 1, snackId = 1, snackName = "Kopi Hitam", price = 5000,
                qty = 2, total = 10000, epoch = 4000, paymentMethod = "TUNAI"
            )
        )
    )

    @Test
    fun backupRoundTripPreservesEverything() {
        assertEquals(sampleData, parseBackup(backupToJson(sampleData)))
    }

    @Test
    fun parseBackupRejectsForeignOrBrokenFiles() {
        listOf(
            "bukan json sama sekali",
            "{\"app\":\"lain\",\"exportedAtMillis\":1}",
            "{\"app\":\"kasir\",\"exportedAtMillis\":1}"
        ).forEach { text ->
            try {
                parseBackup(text)
                fail("parseBackup seharusnya menolak: $text")
            } catch (e: Exception) {
                // Ditolak sesuai harapan.
            }
        }
    }

    @Test
    fun laporanCsvContainsTotalsUnitsAndRows() {
        val csv = laporanCsv(
            storeName = "Rental PS",
            periodLabel = "Hari Ini",
            generatedAtMillis = 1730000000000L,
            grandTotal = 27000,
            sessionTotal = 17000,
            salesTotal = 10000,
            byUnit = listOf(Triple("TV 01", 1, 120) to 17000L),
            sessions = sampleData.sessions.filter { it.status == "FINISHED" },
            sales = sampleData.snackSales
        )
        assertTrue(csv.contains("Laporan Kasir - Hari Ini"))
        assertTrue(csv.contains("Total Pendapatan;27000"))
        assertTrue(csv.contains("Sewa & Sesi;17000"))
        assertTrue(csv.contains("Makanan Langsung;10000"))
        assertTrue(csv.contains("TV 01;1;120;17000"))
        assertTrue(csv.contains("Paket 2 jam;120;12000;5000;17000;QRIS"))
        assertTrue(csv.contains("Kopi Hitam;2;5000;10000;TUNAI"))
    }
}
