package com.nagabhushansn95.financetracker.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class StatisticsDaoTest {
    private lateinit var db: AppDatabase
    private val now = LocalDateTime.of(2026, 11, 4, 9, 0)
    private var bank = 0L;
    private var usdWallet = 0L;
    private var epf = 0L;
    private var ppf = 0L
    private var food = 0L;
    private var travel = 0L;
    private var salary = 0L;
    private var stock = 0L

    @Before
    fun setUp() = runTest {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
        bank = db.accountDao().insert(
            Account(
                name = "Bank",
                accountType = AccountType.BANK,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        usdWallet = db.accountDao().insert(
            Account(
                name = "USD",
                accountType = AccountType.WALLET,
                currency = "USD",
                displayOrder = 2,
                createdTime = now,
                modifiedTime = now
            )
        )
        epf = db.accountDao().insert(
            Account(
                name = "EPF",
                accountType = AccountType.PROVIDENT_FUND,
                displayOrder = 3,
                createdTime = now,
                modifiedTime = now
            )
        )
        ppf = db.accountDao().insert(
            Account(
                name = "PPF",
                accountType = AccountType.PROVIDENT_FUND,
                displayOrder = 4,
                createdTime = now,
                modifiedTime = now
            )
        )
        food = db.categoryDao().insert(Category(name = "Food", displayOrder = 1))
        travel = db.categoryDao().insert(Category(name = "Travel", displayOrder = 2))
        salary = db.categoryDao().insert(Category(name = "Salary", displayOrder = 3))
        stock = db.instrumentDao().insert(
            Instrument(
                name = "X",
                instrumentType = InstrumentType.STOCK,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )

        suspend fun add(
            date: String,
            kind: TransactionKind,
            amount: Double,
            from: Long? = null,
            to: Long? = null,
            category: Long? = null,
            instrument: Long? = null,
            currency: String = "INR",
            fx: Double? = null,
            inStats: Boolean = true
        ) =
            db.transactionDao().insert(
                TransactionEntity(
                    createdTime = now,
                    modifiedTime = now,
                    date = LocalDate.parse(date),
                    kind = kind,
                    fromAccountId = from,
                    toAccountId = to,
                    categoryId = category,
                    instrumentId = instrument,
                    particulars = "x",
                    amount = amount,
                    currency = currency,
                    fxRate = fx,
                    includeInStatistics = inStats
                )
            )

        // October
        add("2026-10-05", TransactionKind.INCOME, 50000.0, to = bank, category = salary)
        add(
            "2026-10-06",
            TransactionKind.INCOME,
            3000.0,
            to = epf
        )                               // employer PF: savings, not income
        add("2026-10-07", TransactionKind.EXPENSE, 1200.0, from = bank, category = food)
        add("2026-10-08", TransactionKind.EXPENSE, 800.0, from = bank, category = travel)
        add(
            "2026-10-09",
            TransactionKind.TRANSFER,
            5000.0,
            from = bank,
            to = ppf
        )                 // PPF deposit: savings
        add(
            "2026-10-10",
            TransactionKind.INVEST,
            10000.0,
            from = bank,
            instrument = stock
        )        // savings
        add(
            "2026-10-11",
            TransactionKind.INVEST,
            999.0,
            instrument = stock
        )                       // renewal-style, no cash: ignored
        add(
            "2026-10-12",
            TransactionKind.EXPENSE,
            100.0,
            from = usdWallet,
            category = food,
            currency = "USD"
        )           // no fx: counts 0
        add(
            "2026-10-13",
            TransactionKind.EXPENSE,
            10.0,
            from = usdWallet,
            category = food,
            currency = "USD",
            fx = 84.0
        ) // 840 INR
        add(
            "2026-10-14",
            TransactionKind.EXPENSE,
            500.0,
            from = bank,
            category = food,
            inStats = false
        )                // excluded
        // November
        add(
            "2026-11-02",
            TransactionKind.REDEEM,
            4000.0,
            to = bank,
            instrument = stock
        )          // withdrawal
        add(
            "2026-11-03",
            TransactionKind.TRANSFER,
            2500.0,
            from = ppf,
            to = bank
        )                 // PPF withdrawal
        add("2026-11-03", TransactionKind.EXPENSE, 300.0, from = bank, category = food)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun monthlyCountersFollowThePlanRules() = runTest {
        val rows = db.statisticsDao()
            .periodTotals("2026-01-01", "2026-12-31", 7, false, emptyList(), false, emptyList())
        assertEquals(listOf("2026-10", "2026-11"), rows.map { it.period })
        val oct = rows[0]
        assertEquals(
            2840.0,
            oct.expense,
            1e-9
        )       // 1200 + 800 + 840 (USD w/o rate = 0, excluded row ignored)
        assertEquals(50000.0, oct.income, 1e-9)       // employer PF not counted
        assertEquals(18000.0, oct.savings, 1e-9)      // 3000 PF + 5000 PPF + 10000 invest
        assertEquals(0.0, oct.withdrawals, 1e-9)
        val nov = rows[1]
        assertEquals(300.0, nov.expense, 1e-9)
        assertEquals(6500.0, nov.withdrawals, 1e-9)   // 4000 redeem + 2500 PPF out
    }

    @Test
    fun yearlyGroupingAndFilters() = runTest {
        val year = db.statisticsDao()
            .periodTotals("2000-01-01", "2099-12-31", 4, false, emptyList(), false, emptyList())
        assertEquals(listOf("2026"), year.map { it.period })
        assertEquals(3140.0, year.single().expense, 1e-9)

        val onlyUsd = db.statisticsDao().periodTotals(
            "2026-01-01",
            "2026-12-31",
            7,
            true,
            listOf(usdWallet),
            false,
            emptyList()
        )
        assertEquals(840.0, onlyUsd.single().expense, 1e-9)

        val onlyTravel = db.statisticsDao()
            .periodTotals("2026-01-01", "2026-12-31", 7, false, emptyList(), true, listOf(travel))
        assertEquals(800.0, onlyTravel.single().expense, 1e-9)
    }

    @Test
    fun categoryBreakdown() = runTest {
        val expense = db.statisticsDao()
            .categoryTotals("EXPENSE", "2026-10-01", "2026-10-31", false, emptyList())
        assertEquals(listOf(food, travel), expense.map { it.categoryId })
        assertEquals(2040.0, expense[0].total, 1e-9)
        assertEquals(3, expense[0].count)

        val income = db.statisticsDao()
            .categoryTotals("INCOME", "2026-10-01", "2026-10-31", false, emptyList())
        assertEquals(listOf(salary), income.map { it.categoryId })  // PF credit excluded
        assertEquals("2026-10-05", db.statisticsDao().earliestDate())
    }
}
