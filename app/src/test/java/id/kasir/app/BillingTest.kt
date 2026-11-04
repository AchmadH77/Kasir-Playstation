package id.kasir.app

import id.kasir.app.data.SessionEntity
import id.kasir.app.data.UnitEntity
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

// Unit test untuk logika billing murni di Billing.kt (tanpa Android / database).
class BillingTest {

    private val min = 60000L

    private fun session(
        mode: String = MODE_OPEN,
        paketMinutes: Int = 0,
        hourlyRate: Long = 6000,
        paketPrice: Long = 0,
        startEpoch: Long = 0,
        pausedMillis: Long = 0,
        pauseStartEpoch: Long? = null,
        status: String = STATUS_RUNNING,
        endEpoch: Long? = null
    ) = SessionEntity(
        unitId = 1,
        unitName = "TV 01",
        consoleType = "PS4",
        hourlyRate = hourlyRate,
        mode = mode,
        paketMinutes = paketMinutes,
        paketPrice = paketPrice,
        startEpoch = startEpoch,
        pausedMillis = pausedMillis,
        pauseStartEpoch = pauseStartEpoch,
        status = status,
        endEpoch = endEpoch
    )

    @Test
    fun formatRupiahFormatsThousandsWithDots() {
        assertEquals("Rp 0", formatRupiah(0))
        assertEquals("Rp 500", formatRupiah(500))
        assertEquals("Rp 5.000", formatRupiah(5000))
        assertEquals("Rp 26.400", formatRupiah(26400))
        assertEquals("Rp 1.234.567", formatRupiah(1234567))
    }

    @Test
    fun formatRupiahShortCompactsForChartLabels() {
        assertEquals("0", formatRupiahShort(0))
        assertEquals("500", formatRupiahShort(500))
        assertEquals("5 rb", formatRupiahShort(5000))
        assertEquals("96 rb", formatRupiahShort(96000))
        assertEquals("1 jt", formatRupiahShort(1000000))
        assertEquals("1,5 jt", formatRupiahShort(1500000))
        assertEquals("2,7 jt", formatRupiahShort(2750000))
    }

    @Test
    fun formatMinutesHumanReadable() {
        assertEquals("0 mnt", formatMinutes(0))
        assertEquals("45 mnt", formatMinutes(45))
        assertEquals("1 jam", formatMinutes(60))
        assertEquals("1 jam 30 mnt", formatMinutes(90))
        assertEquals("2 jam 5 mnt", formatMinutes(125))
    }

    @Test
    fun openBillingRoundsUpPerTenMinuteBlock() {
        assertEquals(10, billableOpenMinutes(0))
        assertEquals(10, billableOpenMinutes(1))
        assertEquals(10, billableOpenMinutes(10))
        assertEquals(20, billableOpenMinutes(11))
        assertEquals(60, billableOpenMinutes(59))
        assertEquals(60, billableOpenMinutes(60))
        assertEquals(70, billableOpenMinutes(61))
        assertEquals(600, billableOpenMinutes(600))
    }

    @Test
    fun elapsedExcludesFinishedPause() {
        val s = session(pausedMillis = 10 * min)
        assertEquals(50, elapsedMinutes(s, 60 * min))
    }

    @Test
    fun elapsedExcludesOngoingPause() {
        val s = session(status = STATUS_PAUSED, pauseStartEpoch = 30 * min)
        assertEquals(30, elapsedMinutes(s, 90 * min))
    }

    @Test
    fun elapsedForFinishedSessionStopsAtEnd() {
        val s = session(status = STATUS_FINISHED, endEpoch = 45 * min)
        assertEquals(45, elapsedMinutes(s, 999 * min))
    }

    @Test
    fun paketBillableIsFixedWithinPaket() {
        val s = session(mode = MODE_PAKET, paketMinutes = 120)
        assertEquals(120, billableMinutes(s, 45 * min))
        assertEquals(120, billableMinutes(s, 120 * min))
    }

    @Test
    fun paketOvertimeRoundsUpPerBlock() {
        val s = session(mode = MODE_PAKET, paketMinutes = 120)
        assertEquals(130, billableMinutes(s, 121 * min))
        assertEquals(150, billableMinutes(s, 145 * min))
    }

    @Test
    fun openRentalCostUsesRoundedBlocks() {
        val s = session(mode = MODE_OPEN, hourlyRate = 6000)
        assertEquals(7000L, rentalCost(s, 61 * min))
        assertEquals(6000L, rentalCost(s, 60 * min))
    }

    @Test
    fun paketRentalCostAndOvertime() {
        val s = session(mode = MODE_PAKET, paketMinutes = 120, hourlyRate = 8000)
        assertEquals(16000L, rentalCost(s, 120 * min))
        assertEquals(20000L, rentalCost(s, 145 * min))
    }

    @Test
    fun remainingCountsDownAndGoesNegative() {
        val s = session(mode = MODE_PAKET, paketMinutes = 120)
        assertEquals(90 * min, remainingMillis(s, 30 * min))
        assertEquals(-30 * min, remainingMillis(s, 150 * min))
    }

    @Test
    fun formatHmsPadsAndClamps() {
        assertEquals("00:00:00", formatHms(0))
        assertEquals("00:00:59", formatHms(59000))
        assertEquals("01:01:01", formatHms(3661000))
        assertEquals("00:00:00", formatHms(-5000))
    }

    @Test
    fun startOfDayIsMidnightSameDay() {
        val c = Calendar.getInstance()
        c.set(2026, Calendar.NOVEMBER, 4, 15, 30, 45)
        val start = startOfDay(c.timeInMillis)
        val sc = Calendar.getInstance()
        sc.timeInMillis = start
        assertEquals(2026, sc.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, sc.get(Calendar.MONTH))
        assertEquals(4, sc.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, sc.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, sc.get(Calendar.MINUTE))
        assertEquals(0, sc.get(Calendar.SECOND))
    }

    @Test
    fun startOfMonthIsFirstDayMidnight() {
        val c = Calendar.getInstance()
        c.set(2026, Calendar.NOVEMBER, 17, 9, 15, 0)
        val start = startOfMonth(c.timeInMillis)
        val sc = Calendar.getInstance()
        sc.timeInMillis = start
        assertEquals(1, sc.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, sc.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun startOfWeekIsNotAfterStartOfDay() {
        val c = Calendar.getInstance()
        c.set(2026, Calendar.NOVEMBER, 6, 12, 0, 0)
        val epoch = c.timeInMillis
        assertEquals(true, startOfWeek(epoch) <= startOfDay(epoch))
        assertEquals(true, startOfWeek(epoch) <= epoch)
    }

    @Test
    fun hourlyRateFollowsConsoleType() {
        assertEquals(5_000L, hourlyRateFor("PS3"))
        assertEquals(8_000L, hourlyRateFor("PS4"))
        assertEquals(10_000L, hourlyRateFor("PS5"))
        assertEquals(0L, hourlyRateFor("PS2"))
    }

    @Test
    fun effectiveRatePrefersConsoleRateOverStoredValue() {
        val ps5 = UnitEntity(name = "TV 09", consoleType = "PS5", hourlyRate = 7_000L)
        assertEquals(10_000L, effectiveRate(ps5))
        val unknown = UnitEntity(name = "TV 10", consoleType = "PS2", hourlyRate = 7_000L)
        assertEquals(7_000L, effectiveRate(unknown))
    }

    @Test
    fun manualPaketPriceIsFlatEvenIntoOvertime() {
        val s = session(mode = MODE_PAKET, paketMinutes = 120, paketPrice = 12_000L)
        assertEquals(12_000L, rentalCost(s, 60 * min))
        assertEquals(12_000L, rentalCost(s, 300 * min))
        assertEquals(120, billedMinutes(s, 300 * min))
    }

    @Test
    fun zeroPaketPriceKeepsRateBasedBilling() {
        val s = session(mode = MODE_PAKET, paketMinutes = 120, hourlyRate = 6_000L)
        assertEquals(12_000L, rentalCost(s, 120 * min))
        assertEquals(120, billedMinutes(s, 120 * min))
    }
}
