package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DepositInterestTest {
    private val now = LocalDateTime.of(2026, 11, 5, 9, 0)
    private fun instrument(id: Long, type: InstrumentType = InstrumentType.FIXED_DEPOSIT) =
        Instrument(
            id = id,
            name = "I$id",
            instrumentType = type,
            createdTime = now,
            modifiedTime = now
        )

    private fun t(
        id: Long,
        date: String,
        kind: TransactionKind,
        amount: Double,
        instrument: Long,
        to: Long? = null,
        inStats: Boolean = true
    ) =
        TransactionEntity(
            id = id,
            createdTime = now,
            modifiedTime = now,
            date = LocalDate.parse(date),
            kind = kind,
            toAccountId = to,
            instrumentId = instrument,
            particulars = "",
            amount = amount,
            includeInStatistics = inStats,
        )

    @Test
    fun interestInsideThePayoutIsDatedOnTheLastPayout() {
        val rows = listOf(
            t(1, "2025-02-03", TransactionKind.INVEST, 100000.0, 1),
            t(2, "2026-02-03", TransactionKind.REDEEM, 106398.0, 1, to = 7),
            // renewed: the payout went nowhere, the interest is still realised that day
            t(3, "2025-02-03", TransactionKind.INVEST, 50000.0, 2),
            t(4, "2026-02-03", TransactionKind.REDEEM, 53000.0, 2),
            // sweep-in style: its interest is already its own rows
            t(5, "2025-02-03", TransactionKind.INVEST, 100000.0, 3),
            t(6, "2025-03-01", TransactionKind.REDEEM, 100000.0, 3, to = 7),
            t(7, "2025-03-01", TransactionKind.INCOME, 500.0, 3, to = 7),
            // still open
            t(8, "2026-02-03", TransactionKind.INVEST, 100000.0, 4),
            // a stock, not a deposit
            t(9, "2025-02-03", TransactionKind.INVEST, 1000.0, 5),
            t(10, "2025-06-03", TransactionKind.REDEEM, 1500.0, 5, to = 7),
        )
        val out = FixedDeposits.depositInterest(
            rows,
            listOf(
                instrument(1),
                instrument(2),
                instrument(3),
                instrument(4),
                instrument(5, InstrumentType.STOCK)
            )
        )
            .sortedBy { it.instrumentId }
        assertEquals(listOf(1L, 2L), out.map { it.instrumentId })
        assertEquals(6398.0, out[0].amount, 0.001)
        assertEquals(LocalDate.of(2026, 2, 3), out[0].date)
        assertEquals(7L, out[0].toAccountId)
        assertEquals(3000.0, out[1].amount, 0.001)
        assertTrue(out[1].toAccountId == null)
    }

    @Test
    fun rowsKeptOutOfStatisticsAreIgnored() {
        val rows = listOf(
            t(1, "2025-02-03", TransactionKind.INVEST, 100000.0, 1, inStats = false),
            t(2, "2026-02-03", TransactionKind.REDEEM, 106398.0, 1, to = 7, inStats = false),
        )
        assertTrue(FixedDeposits.depositInterest(rows, listOf(instrument(1))).isEmpty())
    }
}
