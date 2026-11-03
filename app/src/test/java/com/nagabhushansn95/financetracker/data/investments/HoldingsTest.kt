package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class HoldingsTest {
    private val now = LocalDateTime.of(2026, 11, 4, 9, 0)
    private var nextId = 1L
    private fun row(
        date: String,
        kind: TransactionKind,
        amount: Double,
        qty: Double? = null,
        currency: String = "INR",
        fx: Double? = null
    ) =
        TransactionEntity(
            id = nextId++,
            createdTime = now,
            modifiedTime = now,
            date = LocalDate.parse(date),
            kind = kind,
            instrumentId = 1,
            particulars = "x",
            quantity = qty,
            amount = amount,
            currency = currency,
            fxRate = fx
        )

    @Test
    fun averageCostWithPartialSaleBonusAndDividend() {
        val p = Holdings.position(
            1, listOf(
                row("2025-01-10", TransactionKind.INVEST, 10000.0, qty = 100.0),   // 100 @ 100
                row(
                    "2025-03-10",
                    TransactionKind.INVEST,
                    6000.0,
                    qty = 50.0
                ),     // 50 @ 120 → avg 106.67
                row(
                    "2025-05-01",
                    TransactionKind.INCOME,
                    0.0,
                    qty = 15.0
                ),        // bonus: 165 units, cost unchanged
                row("2025-06-01", TransactionKind.INCOME, 330.0),                  // dividend
                row(
                    "2025-09-01",
                    TransactionKind.REDEEM,
                    9000.0,
                    qty = 65.0
                ),     // sell 65 @ ~138.46; avg = 16000/165
                row("2025-09-02", TransactionKind.EXPENSE, 20.0),                  // DP charges
            )
        )
        assertEquals(100.0, p.units, 1e-9)
        val avg = 16000.0 / 165
        assertEquals(16000.0 - avg * 65, p.costHeld, 1e-6)
        assertEquals(9000.0 - avg * 65 - 20.0, p.realised, 1e-6)
        assertEquals(330.0, p.income, 1e-9)
        assertEquals(16000.0, p.totalInvested, 1e-9)
        assertEquals(9000.0, p.totalRedeemed, 1e-9)
        assertEquals(avg, p.averageCost!!, 1e-9)
        assertEquals(100.0 * 150.0, p.currentValue(unitPrice = 150.0, totalValue = null), 1e-9)
    }

    @Test
    fun amountOnlyInstrumentsReturnCostFirst() {
        val fd = Holdings.position(
            1, listOf(
                row(
                    "2024-01-01",
                    TransactionKind.INVEST,
                    100000.0,
                    qty = 1.0
                ),   // quantity 1 as the 4.x import wrote it
                row("2025-01-01", TransactionKind.REDEEM, 108000.0, qty = 1.0),
            ), unitBased = false
        )
        assertEquals(0.0, fd.costHeld, 1e-9)
        assertEquals(8000.0, fd.realised, 1e-9)
        assertNull(fd.averageCost)
        assertEquals(0.0, fd.units, 1e-9)

        val sweepIn = Holdings.position(
            1, listOf(
                row("2024-01-01", TransactionKind.INVEST, 50000.0),
                row(
                    "2024-02-01",
                    TransactionKind.REDEEM,
                    20000.0
                ),   // partial: returns cost, no profit yet
            )
        )
        assertEquals(30000.0, sweepIn.costHeld, 1e-9)
        assertEquals(0.0, sweepIn.realised, 1e-9)
        assertEquals(30000.0, sweepIn.currentValue(null, null), 1e-9)
    }

    @Test
    fun foreignCurrencyRowsUseTheRate() {
        val us = Holdings.position(
            1,
            listOf(
                row(
                    "2025-01-01",
                    TransactionKind.INVEST,
                    100.0,
                    qty = 2.0,
                    currency = "USD",
                    fx = 85.0
                )
            )
        )
        assertEquals(8500.0, us.costHeld, 1e-9)
        assertEquals(4250.0, us.averageCost!!, 1e-9)
    }

    @Test
    fun xirrMatchesAKnownCase() {
        // 10,000 in, worth 12,100 two years later → 10% a year.
        val r = Holdings.xirr(
            listOf(CashFlow(LocalDate.of(2024, 1, 1), -10000.0)),
            12100.0,
            LocalDate.of(2026, 1, 1)
        )
        assertNotNull(r)
        assertEquals(0.10, r!!, 0.002)
        // Nothing to solve without an outflow.
        assertNull(Holdings.xirr(emptyList(), 500.0, LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun positionsGroupTheLedgerByInstrument() {
        val rows = listOf(
            row("2025-01-01", TransactionKind.INVEST, 100.0, qty = 1.0),
            row("2025-01-02", TransactionKind.INVEST, 200.0, qty = 1.0).copy(instrumentId = 2),
            row("2025-01-03", TransactionKind.EXPENSE, 10.0).copy(instrumentId = null),
        )
        val positions = Holdings.positions(rows)
        assertEquals(setOf(1L, 2L), positions.keys)
        assertEquals(200.0, positions[2]!!.costHeld, 1e-9)
    }
}
