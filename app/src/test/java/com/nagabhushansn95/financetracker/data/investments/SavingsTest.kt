package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InvestmentCategory
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SavingsTest {
    private val now = LocalDateTime.of(2026, 11, 4, 9, 0)
    private val stocks =
        InvestmentCategory(id = 1, name = InvestmentCategories.STOCKS, displayOrder = 1)
    private val reit = InvestmentCategory(id = 2, name = "REIT", parentId = 1, displayOrder = 2)
    private val fds =
        InvestmentCategory(id = 3, name = InvestmentCategories.FIXED_DEPOSITS, displayOrder = 0)
    private val bank = Account(
        id = 10,
        name = "Bank",
        accountType = AccountType.BANK,
        displayOrder = 1,
        createdTime = now,
        modifiedTime = now
    )
    private val epf = Account(
        id = 11,
        name = "EPF",
        accountType = AccountType.PROVIDENT_FUND,
        displayOrder = 2,
        createdTime = now,
        modifiedTime = now
    )
    private val embassy = Instrument(
        id = 20,
        name = "EMBASSY",
        instrumentType = InstrumentType.STOCK,
        categoryId = 2,
        displayOrder = 1,
        createdTime = now,
        modifiedTime = now
    )
    private val fd = Instrument(
        id = 21,
        name = "FD #1",
        instrumentType = InstrumentType.FIXED_DEPOSIT,
        displayOrder = 2,
        createdTime = now,
        modifiedTime = now
    )
    private val familyFd = Instrument(
        id = 22,
        name = "FD #2",
        instrumentType = InstrumentType.FIXED_DEPOSIT,
        owner = "Appa Amma",
        displayOrder = 3,
        createdTime = now,
        modifiedTime = now
    )

    private fun t(
        date: String,
        kind: TransactionKind,
        amount: Double,
        instrument: Long? = null,
        from: Long? = null,
        to: Long? = null,
        currency: String = "INR",
        fx: Double? = null
    ) =
        TransactionEntity(
            createdTime = now,
            modifiedTime = now,
            date = LocalDate.parse(date),
            kind = kind,
            fromAccountId = from,
            toAccountId = to,
            instrumentId = instrument,
            particulars = "",
            amount = amount,
            currency = currency,
            fxRate = fx
        )

    @Test
    fun monthlyAndYearlyTables() {
        val rows = listOf(
            t("2026-01-10", TransactionKind.INVEST, 50000.0, instrument = 20, from = 10),
            t("2026-01-20", TransactionKind.REDEEM, 10000.0, instrument = 20, to = 10),
            t("2026-02-01", TransactionKind.INVEST, 100000.0, instrument = 21, from = 10),
            t(
                "2026-02-01",
                TransactionKind.INVEST,
                100000.0,
                instrument = 22
            ),          // family: ignored
            t(
                "2026-02-05",
                TransactionKind.INCOME,
                4500.0,
                to = 11
            ),                     // PF contribution
            t(
                "2026-02-05",
                TransactionKind.TRANSFER,
                12500.0,
                from = 10,
                to = 11
            ),       // PPF-style deposit
            t(
                "2025-12-30",
                TransactionKind.TRANSFER,
                2000.0,
                from = 11,
                to = 10
            ),        // PF withdrawal
            t(
                "2026-02-09",
                TransactionKind.INCOME,
                300.0,
                instrument = 20,
                to = 10
            ),     // dividend: not savings
            t(
                "2026-02-10",
                TransactionKind.INVEST,
                100.0,
                instrument = 20,
                from = 10,
                currency = "USD",
                fx = 85.0
            ),
        )
        val monthly = Savings.build(
            rows,
            listOf(embassy, fd, familyFd),
            listOf(fds, stocks, reit),
            listOf(bank, epf),
            monthly = true
        )
        assertEquals(listOf("2026-02", "2026-01", "2025-12"), monthly.map { it.key })
        val feb = monthly[0]
        assertEquals(
            listOf(
                InvestmentCategories.FIXED_DEPOSITS,
                InvestmentCategories.STOCKS,
                InvestmentCategories.PROVIDENT_FUNDS
            ), feb.lines.map { it.name })
        assertEquals(100000.0, feb.lines[0].credits, 0.001)
        assertEquals(8500.0, feb.lines[1].credits, 0.001)
        assertEquals(17000.0, feb.lines[2].credits, 0.001)
        assertEquals(125500.0, feb.total, 0.001)
        val jan = monthly[1]
        assertEquals(40000.0, jan.total, 0.001)
        assertEquals(10000.0, jan.lines.single().debits, 0.001)
        assertEquals(-2000.0, monthly[2].total, 0.001)

        val yearly = Savings.build(
            rows,
            listOf(embassy, fd, familyFd),
            listOf(fds, stocks, reit),
            listOf(bank, epf),
            monthly = false
        )
        assertEquals(listOf("2026", "2025"), yearly.map { it.key })
        assertEquals(165500.0, yearly[0].total, 0.001)
        assertEquals(175500.0, yearly[0].credits, 0.001)
    }
}
