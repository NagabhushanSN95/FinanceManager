package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.HoldingEvent
import com.nagabhushansn95.financetracker.data.db.entity.HoldingEventKind
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.Listed
import com.nagabhushansn95.financetracker.data.db.entity.Security
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class HoldingEntriesTest {
    private val now = LocalDateTime.of(2026, 11, 10, 9, 0)
    private val stock = Listed(
        Instrument(
            id = 4,
            name = "Nippon India ETF Nifty 50 BeES",
            instrumentType = InstrumentType.STOCK,
            createdTime = now,
            modifiedTime = now
        ),
        Security(instrumentId = 4, symbol = "NIFTYBEES", exchange = "NSE"),
    )
    private val fund = Listed(
        Instrument(
            id = 29,
            name = "Parag Parikh Flexi Cap Fund",
            instrumentType = InstrumentType.MUTUAL_FUND,
            createdTime = now,
            modifiedTime = now
        ),
        Security(instrumentId = 29, symbol = "122639", exchange = "AMFI"),
    )

    private fun row(
        id: Long,
        instrument: Long,
        kind: TransactionKind,
        date: String,
        amount: Double,
        particulars: String,
        qty: Double? = null,
        rate: Double? = null,
        from: Long? = null,
        to: Long? = null
    ) = TransactionEntity(
        id = id,
        createdTime = now,
        modifiedTime = now,
        date = LocalDate.parse(date),
        kind = kind,
        fromAccountId = from,
        toAccountId = to,
        instrumentId = instrument,
        particulars = particulars,
        quantity = qty,
        rate = rate,
        amount = amount,
    )

    @Test
    fun rowsAreClassifiedByKindUnitsAndAmount() {
        assertEquals(
            HoldingEntryKind.BUY,
            HoldingEntries.kindOf(
                row(
                    1,
                    4,
                    TransactionKind.INVEST,
                    "2026-01-01",
                    2850.0,
                    "Bought Stocks - NIFTYBEES",
                    10.0,
                    285.0,
                    from = 49
                )
            )
        )
        assertEquals(
            HoldingEntryKind.SELL,
            HoldingEntries.kindOf(
                row(
                    2,
                    4,
                    TransactionKind.REDEEM,
                    "2026-01-02",
                    2900.0,
                    "Sold Stocks - NIFTYBEES",
                    10.0,
                    290.0,
                    to = 49
                )
            )
        )
        assertEquals(
            HoldingEntryKind.DIVIDEND,
            HoldingEntries.kindOf(
                row(
                    3,
                    4,
                    TransactionKind.INCOME,
                    "2026-01-03",
                    50.0,
                    "Dividend From Stocks - NIFTYBEES",
                    10.0,
                    5.0,
                    to = 36
                )
            )
        )
        // Bonus and split units are events beside the ledger, never rows in it.
        assertEquals(
            HoldingEntryKind.BONUS,
            HoldingEntries.kindOf(
                HoldingEvent(
                    instrumentId = 4,
                    date = LocalDate.parse("2026-01-04"),
                    kind = HoldingEventKind.BONUS,
                    units = 10.0,
                    ratio = "1:1"
                )
            )
        )
        assertEquals(
            HoldingEntryKind.SPLIT,
            HoldingEntries.kindOf(
                HoldingEvent(
                    instrumentId = 4,
                    date = LocalDate.parse("2026-01-05"),
                    kind = HoldingEventKind.SPLIT,
                    units = 40.0,
                    ratio = "1:5"
                )
            )
        )
        assertNull(
            HoldingEntries.kindOf(
                HoldingEvent(
                    instrumentId = 9,
                    date = LocalDate.parse("2026-01-05"),
                    kind = HoldingEventKind.TDS_DEDUCTED,
                    amount = 640.0
                )
            )
        )
        assertEquals(
            HoldingEntryKind.CHARGES,
            HoldingEntries.kindOf(
                row(
                    6,
                    4,
                    TransactionKind.EXPENSE,
                    "2026-01-06",
                    15.34,
                    "Depository Participant Charges For Selling Stocks - NIFTYBEES",
                    from = 49
                )
            )
        )
        assertNull(
            HoldingEntries.kindOf(
                row(
                    7,
                    4,
                    TransactionKind.TRANSFER,
                    "2026-01-07",
                    1.0,
                    "x",
                    from = 49,
                    to = 36
                )
            )
        )
    }

    @Test
    fun particularsFollowTheHouseStyleAndContinueTheLastOfTheKind() {
        val view = HoldingEntries.build(
            listOf(stock, fund),
            listOf(
                row(
                    1,
                    4,
                    TransactionKind.INVEST,
                    "2026-01-01",
                    2850.0,
                    "Bought Stocks - NIFTYBEES",
                    10.0,
                    285.0,
                    from = 49
                ),
                row(
                    2,
                    4,
                    TransactionKind.INCOME,
                    "2026-02-01",
                    50.0,
                    "Dividend From Stocks - NIFTYBEES (Ex-Dividend Date: 20/01/2026)",
                    to = 36
                ),
                row(
                    3,
                    29,
                    TransactionKind.INVEST,
                    "2026-01-05",
                    5000.0,
                    "Mutual Funds SIP Installment - Parag Parikh Flexi Cap Fund",
                    55.123,
                    90.7,
                    from = 36
                ),
            )
        )
        val n = view.first { it.instrument.id == 4L }
        val p = view.first { it.instrument.id == 29L }
        assertEquals(
            "Bought Stocks - NIFTYBEES",
            HoldingEntries.suggestedParticulars(HoldingEntryKind.BUY, n)
        )
        assertEquals(
            "Sold Stocks - NIFTYBEES",
            HoldingEntries.suggestedParticulars(HoldingEntryKind.SELL, n)
        )
        // The old ex-date suffix is dropped: the ex-date has its own field now.
        assertEquals(
            "Dividend From Stocks - NIFTYBEES",
            HoldingEntries.suggestedParticulars(HoldingEntryKind.DIVIDEND, n)
        )
        assertEquals(
            "Bonus Shares 1:1 - NIFTYBEES",
            HoldingEntries.suggestedParticulars(HoldingEntryKind.BONUS, n, "1:1")
        )
        assertEquals(
            "Stock Split 1:5 - NIFTYBEES",
            HoldingEntries.suggestedParticulars(HoldingEntryKind.SPLIT, n, "1:5")
        )
        assertEquals(
            "Depository Participant Charges For Selling Stocks - NIFTYBEES",
            HoldingEntries.suggestedParticulars(HoldingEntryKind.CHARGES, n)
        )
        assertEquals(
            "Mutual Funds SIP Installment - Parag Parikh Flexi Cap Fund",
            HoldingEntries.suggestedParticulars(HoldingEntryKind.BUY, p)
        )
        assertEquals(
            "Redeemed Mutual Funds - Parag Parikh Flexi Cap Fund",
            HoldingEntries.suggestedParticulars(HoldingEntryKind.SELL, p)
        )
        assertEquals(
            listOf(
                HoldingEntryKind.BUY,
                HoldingEntryKind.SELL,
                HoldingEntryKind.DIVIDEND,
                HoldingEntryKind.CHARGES
            ), HoldingEntries.kindsFor(InstrumentType.MUTUAL_FUND)
        )
        assertEquals(
            "Purchase" to "Redeem",
            HoldingEntryKind.BUY.title(InstrumentType.MUTUAL_FUND) to HoldingEntryKind.SELL.title(
                InstrumentType.MUTUAL_FUND
            )
        )
        assertEquals("1" to "5", HoldingEntries.ratioOf("Stock Split 1:5 - NIFTYBEES"))
        assertEquals(49L, n.lastTradeAccount)
        assertEquals(36L, n.lastDividendAccount)
    }

    @Test
    fun unitsFromRatiosAndTheRowsTheSheetWrites() {
        assertEquals(100.0, HoldingEntries.bonusUnits(100.0, 1.0, 1.0), 1e-9)
        assertEquals(50.0, HoldingEntries.bonusUnits(100.0, 1.0, 2.0), 1e-9)
        assertEquals(400.0, HoldingEntries.splitUnits(100.0, 1.0, 5.0), 1e-9)
        val view = HoldingView(
            stock.instrument,
            stock.security,
            emptyList(),
            Holdings.position(4, emptyList())
        )
        val buy = HoldingEntries.toTransaction(
            view,
            HoldingEntryKind.BUY,
            LocalDate.of(2026, 1, 1),
            10.0,
            285.0,
            2851.5,
            49,
            " ",
            null,
            null,
            now = now
        )
        assertEquals(TransactionKind.INVEST, buy.kind)
        assertEquals(49L, buy.fromAccountId)
        assertNull(buy.toAccountId)
        assertEquals(10.0, buy.quantity!!, 1e-9)
        assertEquals(285.0, buy.rate!!, 1e-9)
        assertEquals(2851.5, buy.amount, 1e-9)
        assertEquals("Bought Stocks - NIFTYBEES", buy.particulars)
        val dividend = HoldingEntries.toTransaction(
            view,
            HoldingEntryKind.DIVIDEND,
            LocalDate.of(2026, 2, 1),
            10.0,
            5.0,
            50.0,
            36,
            "Dividend From Stocks - NIFTYBEES",
            LocalDate.of(2026, 1, 20),
            7,
            now = now
        )
        assertEquals(TransactionKind.INCOME, dividend.kind)
        assertEquals(36L, dividend.toAccountId)
        assertEquals(7L, dividend.categoryId)
        assertEquals(LocalDate.of(2026, 1, 20), dividend.exDate)
        val bonus = HoldingEntries.toEvent(
            view,
            HoldingEntryKind.BONUS,
            LocalDate.of(2026, 3, 1),
            10.0,
            "1:1"
        )
        assertEquals(HoldingEventKind.BONUS, bonus.kind)
        assertEquals(4L, bonus.instrumentId)
        assertEquals(10.0, bonus.units!!, 1e-9)
        assertEquals("1:1", bonus.ratio)
        assertEquals(LocalDate.of(2026, 3, 1), bonus.date)
        assertTrue(runCatching {
            HoldingEntries.toTransaction(
                view,
                HoldingEntryKind.BONUS,
                LocalDate.of(2026, 3, 1),
                10.0,
                null,
                0.0,
                null,
                "",
                null,
                null,
                now = now
            )
        }.isFailure)
        val charges = HoldingEntries.toTransaction(
            view,
            HoldingEntryKind.CHARGES,
            LocalDate.of(2026, 3, 2),
            null,
            null,
            15.34,
            49,
            "",
            null,
            9,
            now = now
        )
        assertEquals(TransactionKind.EXPENSE, charges.kind)
        assertEquals(49L, charges.fromAccountId)
        assertEquals(9L, charges.categoryId)
        assertEquals(
            "Depository Participant Charges For Selling Stocks - NIFTYBEES",
            charges.particulars
        )
    }

    @Test
    fun problemsAreNamedBeforeSaving() {
        val held = HoldingView(
            stock.instrument,
            stock.security,
            listOf(
                row(
                    1,
                    4,
                    TransactionKind.INVEST,
                    "2026-01-01",
                    2850.0,
                    "b",
                    10.0,
                    285.0,
                    from = 49
                )
            ),
            Holdings.position(
                4,
                listOf(
                    row(
                        1,
                        4,
                        TransactionKind.INVEST,
                        "2026-01-01",
                        2850.0,
                        "b",
                        10.0,
                        285.0,
                        from = 49
                    )
                )
            )
        )
        assertNull(HoldingEntries.problem(held, HoldingEntryKind.SELL, 10.0, 2900.0, 49, null))
        assertEquals(
            "Only 10 units are held",
            HoldingEntries.problem(held, HoldingEntryKind.SELL, 11.0, 2900.0, 49, null)
        )
        assertEquals(
            "Choose the account",
            HoldingEntries.problem(held, HoldingEntryKind.BUY, 1.0, 100.0, null, null)
        )
        assertEquals(
            "Units must be more than zero",
            HoldingEntries.problem(held, HoldingEntryKind.BUY, 0.0, 100.0, 49, null)
        )
        assertEquals(
            "The amount must be more than zero",
            HoldingEntries.problem(held, HoldingEntryKind.DIVIDEND, null, 0.0, 36, null)
        )
        assertNull(HoldingEntries.problem(held, HoldingEntryKind.BONUS, 10.0, 0.0, null, null))
        val empty = HoldingView(
            stock.instrument,
            stock.security,
            emptyList(),
            Holdings.position(4, emptyList())
        )
        assertEquals(
            "Nothing is held to split",
            HoldingEntries.problem(empty, HoldingEntryKind.SPLIT, 10.0, 0.0, null, null)
        )
    }
}
