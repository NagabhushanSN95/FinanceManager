package com.nagabhushansn95.financetracker.ui.investments

import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.Security
import com.nagabhushansn95.financetracker.data.investments.HoldingRow
import com.nagabhushansn95.financetracker.data.investments.Position
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class HoldingSortTest {
    private val now = LocalDateTime.of(2026, 11, 17, 10, 0)

    private fun row(
        name: String, invested: Double, value: Double, first: String?, last: String?, xirr: Double?,
        shortName: String? = null, units: Double = if (invested > 0) 10.0 else 0.0,
    ) = HoldingRow(
        instrument = Instrument(
            name = name,
            instrumentType = InstrumentType.STOCK,
            createdTime = now,
            modifiedTime = now
        ),
        position = Position(
            instrumentId = 0,
            units = units,
            costHeld = invested,
            totalInvested = invested,
            totalRedeemed = 0.0,
            realised = 0.0,
            income = 0.0,
            firstDate = first?.let(LocalDate::parse),
            lastDate = last?.let(LocalDate::parse),
            hasUnits = true,
            cashFlows = emptyList(),
        ),
        categoryName = "", unitPrice = null, priceDate = null, currentValue = value, xirr = xirr,
        security = Security(shortName = shortName),
    )

    private val a =
        row("Apple Inc", 1000.0, 1500.0, "2020-01-01", "2026-10-01", 0.12)        // +50%
    private val b =
        row("Beta Ltd", 4000.0, 4400.0, "2022-06-01", "2022-06-01", 0.05, "B")    // +10%
    private val c = row("Gamma FD #10", 2000.0, 1800.0, "2021-03-01", "2026-11-10", -0.04)  // -10%
    private val sold =
        row("Zeta", 0.0, 0.0, "2019-01-01", "2024-01-01", null)              // nothing held
    private val all = listOf(c, sold, a, b)

    private fun names(sort: HoldingSort, descending: Boolean) =
        all.sortedWith(sort.comparator(descending)).map { it.shownName }

    @Test
    fun nameUsesTheShownNameInNaturalOrderEitherWay() {
        assertEquals(
            listOf("Apple Inc", "B", "Gamma FD #10", "Zeta"),
            names(HoldingSort.NAME, false)
        )
        assertEquals(
            listOf("Zeta", "Gamma FD #10", "B", "Apple Inc"),
            names(HoldingSort.NAME, true)
        )
    }

    @Test
    fun dateGoesByTheFirstEntryOldestFirstAndByTheLastEntryNewestFirst() {
        assertEquals(
            listOf("Zeta", "Apple Inc", "Gamma FD #10", "B"),
            names(HoldingSort.DATE, false)
        )
        assertEquals(
            listOf("Gamma FD #10", "Apple Inc", "Zeta", "B"),
            names(HoldingSort.DATE, true)
        )
        assertEquals(LocalDate.parse("2020-01-01"), HoldingSort.dateOf(a, false))
        assertEquals(LocalDate.parse("2026-10-01"), HoldingSort.dateOf(a, true))
    }

    @Test
    fun amountsAndProfitsPutRowsWithoutAFigureLastInBothDirections() {
        assertEquals(
            listOf("B", "Gamma FD #10", "Apple Inc", "Zeta"),
            names(HoldingSort.INVESTED, true)
        )
        assertEquals(
            listOf("Apple Inc", "Gamma FD #10", "B", "Zeta"),
            names(HoldingSort.INVESTED, false)
        )
        assertEquals(
            listOf("B", "Gamma FD #10", "Apple Inc", "Zeta"),
            names(HoldingSort.VALUE, true)
        )
        assertEquals(
            listOf("Apple Inc", "B", "Gamma FD #10", "Zeta"),
            names(HoldingSort.PROFIT, true)
        )
        assertEquals(
            listOf("Apple Inc", "B", "Gamma FD #10", "Zeta"),
            names(HoldingSort.PROFIT_PCT, true)
        )
        assertEquals(
            listOf("Gamma FD #10", "B", "Apple Inc", "Zeta"),
            names(HoldingSort.PROFIT_PCT, false)
        )
        assertEquals(
            listOf("Apple Inc", "B", "Gamma FD #10", "Zeta"),
            names(HoldingSort.XIRR, true)
        )
        assertEquals(
            listOf("Gamma FD #10", "B", "Apple Inc", "Zeta"),
            names(HoldingSort.XIRR, false)
        )
    }

    @Test
    fun heldProfitPercentIsNullWhenNothingIsHeld() {
        assertEquals(0.5, a.heldProfitPct!!, 1e-9)
        assertEquals(null, sold.heldProfitPct)
    }
}
