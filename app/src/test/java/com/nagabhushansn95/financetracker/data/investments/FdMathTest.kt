package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FdMathTest {
    @Test
    fun maturityMatchesTheSheet() {
        // 1,00,000 at 5.1% for 12 months, quarterly: the sheet prints 1,05,198.
        val fd =
            FixedDeposit(instrumentId = 1, periodMonths = 12, rate = 5.1, compoundingMonths = 3)
        assertEquals(
            105198.0,
            FdMath.maturityAmount(fd, 100000.0, LocalDate.of(2020, 8, 30))!!,
            0.5
        )
        // 7% for 24 months: 1,14,888.
        assertEquals(
            114888.0,
            FdMath.maturityAmount(
                fd.copy(periodMonths = 24, rate = 7.0),
                100000.0,
                LocalDate.of(2024, 11, 6)
            )!!,
            0.5
        )
        // 366 days at 6.25%: four full quarters compounded, one day simple = 1,06,416 (the bank's printed amount).
        assertEquals(
            106416.0,
            FdMath.maturityAmount(
                fd.copy(periodMonths = null, periodDays = 366, rate = 6.25),
                100000.0,
                LocalDate.of(2026, 5, 10)
            )!!,
            0.5
        )
        assertEquals(
            LocalDate.of(2027, 5, 11),
            FdMath.maturityDate(LocalDate.of(2026, 5, 10), null, 366)
        )
        assertNull(FdMath.maturityAmount(fd.copy(rate = null), 100000.0, LocalDate.of(2026, 5, 10)))
    }

    @Test
    fun estimateAccruesAndCapsAtMaturity() {
        val fd = FixedDeposit(
            instrumentId = 1,
            periodMonths = 12,
            rate = 6.25,
            compoundingMonths = 3,
            maturityDate = LocalDate.of(2027, 2, 3)
        )
        val deposit = LocalDate.of(2026, 2, 3)
        assertEquals(100000.0, FdMath.estimate(fd, 100000.0, deposit, deposit)!!, 0.001)
        // One full quarter (to 3 May) compounded, then 64 days simple.
        val expected = 100000.0 * 1.015625 * (1 + 0.0625 * 64 / 365.0)
        assertEquals(
            expected,
            FdMath.estimate(fd, 100000.0, deposit, LocalDate.of(2026, 7, 6))!!,
            0.01
        )
        assertEquals(
            106398.0,
            FdMath.estimate(fd, 100000.0, deposit, LocalDate.of(2030, 1, 1))!!,
            0.5
        )
    }

    @Test
    fun tdsAndLabels() {
        assertEquals(148.8, FdMath.tds(1488.0, 0.1), 0.001)
        assertEquals(148.8, FdMath.tds(1488.0, 10.0), 0.001)
        assertEquals(0.0, FdMath.tds(1488.0, null), 0.001)
        assertEquals("1 year", FdMath.periodLabel(12, null))
        assertEquals("18 months", FdMath.periodLabel(18, 0))
        assertEquals("366 days", FdMath.periodLabel(null, 366))
        assertEquals("1 month 5 days", FdMath.periodLabel(1, 5))
        assertNull(FdMath.periodLabel(0, 0))
        assertEquals("6.25", FdMath.plain(6.25))
        assertEquals("7", FdMath.plain(7.0))
    }
}
