package com.nagabhushansn95.financetracker.data.investments

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentValuation
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.db.entity.ValuationSource
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class InvestmentSummaryTest {
    private lateinit var db: AppDatabase
    private val now = LocalDateTime.of(2026, 11, 4, 9, 0)
    private val today = LocalDate.of(2026, 11, 4)

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun seedsTheTreeOnceAndRollsHoldingsUp() = runTest {
        assertTrue(InvestmentCategories.seedIfEmpty(db))
        assertTrue(!InvestmentCategories.seedIfEmpty(db))
        val categories = db.instrumentDao().getAllCategories()
        val reit = categories.first { it.name == "REIT" }
        val stocks = categories.first { it.name == InvestmentCategories.STOCKS }
        assertEquals(stocks.id, reit.parentId)

        val bank = db.accountDao().insert(
            Account(
                name = "Bank",
                accountType = AccountType.BANK,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        val epf = db.accountDao().insert(
            Account(
                name = "EPF",
                accountType = AccountType.PROVIDENT_FUND,
                balance = 3300.0,
                displayOrder = 2,
                createdTime = now,
                modifiedTime = now
            )
        )
        val embassy = db.instrumentDao().insert(
            Instrument(
                name = "EMBASSY",
                instrumentType = InstrumentType.STOCK,
                categoryId = reit.id,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        val goldNoCat = db.instrumentDao().insert(
            Instrument(
                name = "GOLDBEES",
                instrumentType = InstrumentType.STOCK,
                displayOrder = 2,
                createdTime = now,
                modifiedTime = now
            )
        )
        val fd = db.instrumentDao().insert(
            Instrument(
                name = "FD 1",
                instrumentType = InstrumentType.FIXED_DEPOSIT,
                displayOrder = 3,
                createdTime = now,
                modifiedTime = now
            )
        )
        val family = db.instrumentDao().insert(
            Instrument(
                name = "Amma FD",
                instrumentType = InstrumentType.FIXED_DEPOSIT,
                owner = "Amma",
                displayOrder = 4,
                createdTime = now,
                modifiedTime = now
            )
        )
        val loan = db.instrumentDao().insert(
            Instrument(
                name = "Loan to a cousin",
                instrumentType = InstrumentType.OTHER,
                displayOrder = 5,
                createdTime = now,
                modifiedTime = now
            )
        )

        suspend fun tx(
            date: String,
            kind: TransactionKind,
            amount: Double,
            instrument: Long? = null,
            qty: Double? = null,
            from: Long? = null,
            to: Long? = null,
            contributor: String? = null
        ) =
            db.transactionDao().insert(
                TransactionEntity(
                    createdTime = now,
                    modifiedTime = now,
                    date = LocalDate.parse(date),
                    kind = kind,
                    fromAccountId = from,
                    toAccountId = to,
                    instrumentId = instrument,
                    particulars = "x",
                    quantity = qty,
                    amount = amount,
                    contributor = contributor
                )
            )
        tx("2025-01-01", TransactionKind.INVEST, 10000.0, embassy, 100.0, from = bank)
        tx(
            "2025-06-01",
            TransactionKind.INCOME,
            300.0,
            embassy,
            to = bank
        )                 // dividend
        tx("2025-01-01", TransactionKind.INVEST, 5000.0, goldNoCat, 50.0, from = bank)
        tx(
            "2025-01-01",
            TransactionKind.INVEST,
            100000.0,
            fd,
            qty = 1.0,
            from = bank
        )   // 4.x style: quantity 1 on a deposit
        tx(
            "2025-01-01",
            TransactionKind.INVEST,
            50000.0,
            family
        )                           // owner Amma: excluded
        tx(
            "2025-01-01",
            TransactionKind.INVEST,
            20000.0,
            loan,
            from = bank
        )   // type OTHER: savings, not counted
        tx("2025-02-01", TransactionKind.INCOME, 1000.0, to = epf, contributor = "Employee")
        tx("2025-02-01", TransactionKind.INCOME, 1000.0, to = epf, contributor = "Employer")
        tx(
            "2025-03-31",
            TransactionKind.INCOME,
            1300.0,
            to = epf,
            contributor = "Interest"
        )  // interest: not a contribution
        db.instrumentDao().insertValuation(
            InstrumentValuation(
                instrumentId = embassy,
                date = today,
                unitPrice = 120.0,
                source = ValuationSource.MANUAL,
                fetchedAt = now
            )
        )

        // Targets: FD takes the whole goal corpus; REIT half of the wealth corpus. Goal 50,000 of a 120,300 total.
        val fdCat = categories.first { it.name == InvestmentCategories.FIXED_DEPOSITS }
        db.instrumentDao().updateCategory(
            fdCat.copy(
                targetGoalPct = 0.8,
                targetWealthPct = 0.0,
                tolerancePct = 0.02
            )
        )
        db.instrumentDao().updateCategory(reit.copy(targetWealthPct = 0.5))

        val summary = InvestmentSummaryBuilder.build(
            db.instrumentDao().getAll(),
            db.instrumentDao().getAllCategories(),
            db.transactionDao().getAllWithInstrument(),
            db.instrumentDao().latestValuations(),
            db.accountDao().getAll(),
            db.transactionDao().getBetween("2000-01-01", "2099-12-31"),
            db.instrumentDao().getAllRiskLevels(),
            50_000.0,
            today,
        )

        val stocksLine = summary.categories.first { it.name == InvestmentCategories.STOCKS }
        assertEquals(15000.0, stocksLine.invested, 1e-9)
        assertEquals(
            12000.0 + 5000.0,
            stocksLine.current,
            1e-9
        )   // EMBASSY at price, GOLDBEES at cost
        assertEquals(300.0, stocksLine.income, 1e-9)
        assertEquals(listOf("REIT", "Unassigned"), stocksLine.children.map { it.name })
        val fdLine = summary.categories.first { it.name == InvestmentCategories.FIXED_DEPOSITS }
        assertEquals(100000.0, fdLine.invested, 1e-9)               // the family FD is not there
        val pf = summary.categories.first { it.name == InvestmentCategories.PROVIDENT_FUNDS }
        assertEquals(2000.0, pf.invested, 1e-9)
        assertEquals(3300.0, pf.current, 1e-9)
        assertEquals(1300.0, pf.income, 1e-9)                     // interest
        assertEquals(categories.first { it.name == InvestmentCategories.PROVIDENT_FUNDS }.id, pf.id)
        assertEquals(epf, pf.children.single().accountId)
        // Order follows the category tree: deposits, funds (empty, dropped), stocks, PF.
        assertEquals(
            listOf(
                InvestmentCategories.FIXED_DEPOSITS,
                InvestmentCategories.STOCKS,
                InvestmentCategories.PROVIDENT_FUNDS
            ), summary.categories.map { it.name })
        assertEquals(115000.0 + 2000.0, summary.total.invested, 1e-9)
        assertEquals(17000.0 + 100000.0 + 3300.0, summary.total.current, 1e-9)
        // The loan (type OTHER) has its screen and its Savings column, but no place in the totals or the tables.
        assertEquals(20000.0, summary.byType.getValue(InstrumentType.OTHER).invested, 1e-9)
        assertTrue(summary.categories.none { it.name == InvestmentCategories.OTHERS })
        assertEquals(1, summary.rows.count { it.instrument.id == loan })
        assertEquals(15000.0, summary.zerodha.invested, 1e-9)
        assertEquals(115000.0, summary.liquid.invested, 1e-9)
        assertEquals(2, summary.holdings.size)   // stocks and funds only
        assertEquals(today, summary.latestPriceDate)
        assertEquals(InvestmentCategories.DEFAULT_RISK_LEVELS, summary.riskLevels.map { it.name })
        val noRisk = summary.riskLevels.first { it.name == InvestmentCategories.NO_RISK }
        assertEquals(100000.0 + 2000.0, noRisk.invested, 1e-9)       // FD + PF
        val medium = summary.riskLevels.first { it.name == InvestmentCategories.MEDIUM_RISK }
        assertEquals(
            15000.0,
            medium.invested,
            1e-9
        )                  // REIT child + Stocks top-level default

        // Derived targets: (goal × goal share + (total − goal) × wealth share) / total.
        val total = 120_300.0
        assertEquals(total, summary.total.current, 1e-9)
        assertEquals(50_000.0 * 0.8 / total, fdLine.targetPct!!, 1e-9)
        assertEquals(
            70_300.0 * 0.5 / total,
            stocksLine.children.first { it.name == "REIT" }.targetPct!!,
            1e-9
        )
        assertEquals(0.5, stocksLine.wealthPct!!, 1e-9)                // rolled up from REIT
        assertEquals(70_300.0 * 0.5 / total, stocksLine.targetPct!!, 1e-9)
        assertEquals(null, pf.targetPct)
        assertTrue(fdLine.offTarget(total))                            // 83% held vs 33% target, tolerance 2%
        assertEquals(1.0, summary.total.targetPct!!, 1e-9)
        assertEquals(InvestmentCategories.NO_RISK, fdLine.riskLevel)
    }

    @Test
    fun withoutAGoalTheTargetsFollowTheWealthShares() = runTest {
        InvestmentCategories.seedIfEmpty(db)
        val categories = db.instrumentDao().getAllCategories()
        val fdCat = categories.first { it.name == InvestmentCategories.FIXED_DEPOSITS }
        db.instrumentDao().updateCategory(fdCat.copy(targetGoalPct = 0.8, targetWealthPct = 0.25))
        val fd = db.instrumentDao().insert(
            Instrument(
                name = "FD 1",
                instrumentType = InstrumentType.FIXED_DEPOSIT,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        db.transactionDao().insert(
            TransactionEntity(
                createdTime = now,
                modifiedTime = now,
                date = LocalDate.parse("2025-01-01"),
                kind = TransactionKind.INVEST,
                instrumentId = fd,
                particulars = "x",
                amount = 1000.0
            )
        )
        val summary = InvestmentSummaryBuilder.build(
            db.instrumentDao().getAll(),
            db.instrumentDao().getAllCategories(),
            db.transactionDao().getAllWithInstrument(),
            emptyList(),
            emptyList(),
            emptyList(),
            db.instrumentDao().getAllRiskLevels(),
            null,
            today,
        )
        assertEquals(0.25, summary.categories.single().targetPct!!, 1e-9)
    }
}
