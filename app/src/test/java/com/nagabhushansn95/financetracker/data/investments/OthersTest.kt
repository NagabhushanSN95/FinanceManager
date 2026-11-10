package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class OthersTest {
    private val now = LocalDateTime.of(2026, 11, 10, 9, 0)

    private fun instrument(id: Long, name: String, active: Boolean = true) = Instrument(
        id = id,
        name = name,
        instrumentType = InstrumentType.OTHER,
        displayOrder = id.toInt(),
        active = active,
        createdTime = now,
        modifiedTime = now
    )

    private fun row(
        id: Long,
        instrument: Long,
        kind: TransactionKind,
        date: String,
        amount: Double,
        particulars: String
    ) =
        TransactionEntity(
            id = id,
            createdTime = now,
            modifiedTime = now,
            date = LocalDate.parse(date),
            kind = kind,
            fromAccountId = 36L.takeIf { kind == TransactionKind.INVEST },
            toAccountId = 36L.takeIf { kind != TransactionKind.INVEST },
            instrumentId = instrument,
            particulars = particulars,
            amount = amount,
            quantity = 1.0,
            rate = amount,
        )

    @Test
    fun aLoanSumsUpAndSettlesWhenEverythingIsBack() {
        val views = Others.build(
            listOf(
                instrument(1, "Loan To Mahadeva Anna"),
                instrument(2, "Loan To Lakshmi Tejas"),
                instrument(3, "Amma's Account")
            ),
            listOf(
                row(
                    1,
                    1,
                    TransactionKind.INVEST,
                    "2025-06-10",
                    200000.0,
                    "Loaned To Mahadeva Anna For Baby Expenses"
                ),
                row(2, 1, TransactionKind.REDEEM, "2026-03-23", 10000.0, "Mahadev Anna Paid Back"),
                row(3, 2, TransactionKind.INVEST, "2022-03-22", 10000.0, "Lent To Lakshmi Tejas"),
                row(4, 2, TransactionKind.REDEEM, "2022-05-02", 4000.0, "Lakshmi Tejas Paid Back"),
                row(5, 2, TransactionKind.REDEEM, "2023-01-30", 6000.0, "Lakshmi Tejas Paid Back"),
            )
        )
        assertEquals(listOf(1L, 2L, 3L), views.map { it.instrument.id })
        val mahadeva = views[0]
        assertEquals(190000.0, mahadeva.outstanding, 1e-9)
        assertFalse(mahadeva.settled)
        assertTrue(mahadeva.isLoan)
        assertEquals("Loaned" to "Paid back", mahadeva.outLabel to mahadeva.inLabel)
        assertEquals(OtherEntryKind.IN, Others.likelyNext(mahadeva))
        // Continues the last entry of the kind.
        assertEquals(
            "Loaned To Mahadeva Anna For Baby Expenses",
            Others.suggestedParticulars(OtherEntryKind.OUT, mahadeva)
        )
        assertEquals(
            "Mahadev Anna Paid Back",
            Others.suggestedParticulars(OtherEntryKind.IN, mahadeva)
        )

        val lakshmi = views[1]
        assertTrue(lakshmi.settled)
        assertEquals(0.0, lakshmi.outstanding, 1e-9)
        assertEquals(LocalDate.of(2023, 1, 30), lakshmi.lastDate)
        assertEquals(OtherEntryKind.OUT, Others.likelyNext(lakshmi))

        val amma = views[2]
        assertFalse(amma.isLoan)
        assertFalse(amma.settled) // nothing recorded yet is not settled
        assertEquals("Put in" to "Taken back", amma.outLabel to amma.inLabel)
        assertEquals(
            "Transferred To Amma's Account",
            Others.defaultParticulars(OtherEntryKind.OUT, amma)
        )
        assertEquals(
            "Withdrawn From Amma's Account",
            Others.defaultParticulars(OtherEntryKind.IN, amma)
        )
        assertEquals(
            "Loaned To Mahadeva Anna",
            Others.defaultParticulars(
                OtherEntryKind.OUT,
                OtherView(mahadeva.instrument, emptyList())
            )
        )
        assertEquals(
            "Mahadeva Anna Paid Back",
            Others.defaultParticulars(
                OtherEntryKind.IN,
                OtherView(mahadeva.instrument, emptyList())
            )
        )
    }

    @Test
    fun aRetiredInstrumentCountsAsSettledAndOddRowsAreNotEntries() {
        val v =
            Others.build(listOf(instrument(5, "Old Account", active = false)), emptyList()).single()
        assertTrue(v.settled)
        assertEquals(
            OtherEntryKind.OUT,
            Others.entryKindOf(row(1, 5, TransactionKind.INVEST, "2026-01-01", 1.0, "x"))
        )
        assertEquals(
            OtherEntryKind.IN,
            Others.entryKindOf(row(1, 5, TransactionKind.REDEEM, "2026-01-01", 1.0, "x"))
        )
        assertNull(
            Others.entryKindOf(
                row(
                    1,
                    5,
                    TransactionKind.INCOME,
                    "2026-01-01",
                    1.0,
                    "interest"
                )
            )
        )
    }

    @Test
    fun theLedgerRowCarriesTheBankOnTheRightSideAndNoUnits() {
        val v = OtherView(instrument(7, "Loan To Shreedhar Kulkarni"), emptyList())
        val out = Others.toTransaction(
            v,
            OtherEntryKind.OUT,
            LocalDate.of(2020, 5, 11),
            15000.0,
            "  ",
            36,
            now = now
        )
        assertEquals(TransactionKind.INVEST, out.kind)
        assertEquals(36L, out.fromAccountId)
        assertNull(out.toAccountId)
        assertNull(out.categoryId)
        assertNull(out.quantity)
        assertNull(out.rate)
        assertEquals(7L, out.instrumentId)
        assertEquals(
            "Loaned To Shreedhar Kulkarni",
            out.particulars
        ) // blank particulars fall back to the default
        val existing =
            row(9, 7, TransactionKind.INVEST, "2020-05-11", 15000.0, "Lent To Shreedhar Kulkarni")
        val back = Others.toTransaction(
            v,
            OtherEntryKind.IN,
            LocalDate.of(2026, 12, 1),
            5000.0,
            "Shreedhar Paid Back",
            1,
            existing,
            now
        )
        assertEquals(9L, back.id)
        assertEquals(TransactionKind.REDEEM, back.kind)
        assertEquals(1L, back.toAccountId)
        assertNull(back.fromAccountId)
        assertNull(back.quantity)
        assertEquals(5000.0, back.amount, 1e-9)
    }
}
