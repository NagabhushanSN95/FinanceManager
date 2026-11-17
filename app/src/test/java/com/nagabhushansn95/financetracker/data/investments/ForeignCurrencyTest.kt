package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.FxRate
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ForeignCurrencyTest {
    private val now = LocalDateTime.of(2026, 11, 13, 9, 0)
    private val bank = Account(
        id = 1,
        name = "HDFC",
        accountType = AccountType.BANK,
        displayOrder = 1,
        createdTime = now,
        modifiedTime = now
    )
    private val card = Account(
        id = 2,
        name = "Forex USD",
        accountType = AccountType.WALLET,
        currency = "USD",
        displayOrder = 2,
        createdTime = now,
        modifiedTime = now
    )
    private val cash = Account(
        id = 3,
        name = "Wallet USD",
        accountType = AccountType.WALLET,
        currency = "USD",
        displayOrder = 3,
        createdTime = now,
        modifiedTime = now
    )
    private var nextId = 1L

    private fun row(
        date: String, kind: TransactionKind, amount: Double, from: Long? = null, to: Long? = null,
        toAmount: Double? = null, fx: Double? = null,
    ) = TransactionEntity(
        id = nextId++,
        createdTime = now,
        modifiedTime = now,
        date = LocalDate.parse(date),
        kind = kind,
        fromAccountId = from,
        toAccountId = to,
        particulars = "x",
        amount = amount,
        toAmount = toAmount,
        fxRate = fx,
    )

    @Test
    fun spendsTakeTheAverageCostOfTheMoneyLoaded() {
        // Load 100 @ 90, spend 40, load 100 @ 100, spend 40, spend 40: the example from the design discussion.
        val rows = listOf(
            row("2026-01-01", TransactionKind.TRANSFER, 9000.0, from = 1, to = 2, toAmount = 100.0),
            row("2026-01-02", TransactionKind.EXPENSE, 40.0, from = 2),
            row(
                "2026-01-03",
                TransactionKind.TRANSFER,
                10000.0,
                from = 1,
                to = 2,
                toAmount = 100.0
            ),
            row("2026-01-04", TransactionKind.EXPENSE, 40.0, from = 2),
            row("2026-01-05", TransactionKind.EXPENSE, 40.0, from = 2),
        )
        val values = ForeignCurrency.rupeeValues(listOf(bank, card), rows)
        assertEquals(9000.0, values[1]!!, 1e-9)
        assertEquals(3600.0, values[2]!!, 1e-9)
        assertEquals(10000.0, values[3]!!, 1e-9)
        assertEquals(3850.0, values[4]!!, 1e-9)
        assertEquals(3850.0, values[5]!!, 1e-9)
        // What was spent plus what is left is what was loaded.
        assertEquals(19000.0, 3600.0 + 3850.0 + 3850.0 + 80 * 96.25, 1e-9)
    }

    @Test
    fun creditsWithoutALoadUseTheirOwnRateTheDaysRateOrNothing() {
        val rows = listOf(
            row(
                "2026-02-01",
                TransactionKind.INCOME,
                50.0,
                to = 3,
                fx = 84.0
            ),          // typed rate
            row(
                "2026-02-02",
                TransactionKind.INCOME,
                10.0,
                to = 3
            ),                     // the day's rate
            row(
                "2026-02-03",
                TransactionKind.EXPENSE,
                30.0,
                from = 3
            ),                  // average of the pool
            row(
                "2026-02-04",
                TransactionKind.EXPENSE,
                100.0,
                from = 2
            ),                 // empty pool: the day's rate (2 Feb's)
            row("2026-02-05", TransactionKind.EXPENSE, 10.0, from = 2, fx = 90.0),       // pinned
        )
        val rates = listOf(
            FxRate(
                date = LocalDate.of(2026, 2, 2),
                base = "USD",
                quote = "INR",
                rate = 86.0,
                source = "test"
            )
        )
        val values = ForeignCurrency.rupeeValues(listOf(bank, card, cash), rows, rates)
        assertEquals(4200.0, values[1]!!, 1e-9)
        assertEquals(860.0, values[2]!!, 1e-9)
        assertEquals(30 * (4200.0 + 860.0) / 60, values[3]!!, 1e-9)
        assertEquals(8600.0, values[4]!!, 1e-9)
        assertEquals(900.0, values[5]!!, 1e-9)
        // No rate anywhere: a spend from an empty pool counts nothing rather than a dollar as a rupee.
        assertEquals(
            0.0,
            ForeignCurrency.rupeeValues(listOf(bank, card), listOf(rows[3]))[4]!!,
            1e-9
        )
        // A rupee-only row is not valued at all.
        assertNull(
            ForeignCurrency.rupeeValues(
                listOf(bank),
                listOf(row("2026-02-06", TransactionKind.EXPENSE, 5.0, from = 1))
            )[6]
        )
    }

    @Test
    fun moneyComingBackAndMovingBetweenForeignAccounts() {
        val rows = listOf(
            row("2026-03-01", TransactionKind.TRANSFER, 8500.0, from = 1, to = 2, toAmount = 100.0),
            row(
                "2026-03-02",
                TransactionKind.TRANSFER,
                40.0,
                from = 2,
                to = 3
            ),                      // card → cash, same currency
            row("2026-03-03", TransactionKind.EXPENSE, 20.0, from = 3),
            row(
                "2026-03-04",
                TransactionKind.TRANSFER,
                60.0,
                from = 2,
                to = 1,
                toAmount = 5400.0
            ),   // back to rupees
        )
        val values = ForeignCurrency.rupeeValues(listOf(bank, card, cash), rows)
        assertEquals(3400.0, values[2]!!, 1e-9)
        assertEquals(1700.0, values[3]!!, 1e-9)
        assertEquals(5400.0, values[4]!!, 1e-9)
    }
}
