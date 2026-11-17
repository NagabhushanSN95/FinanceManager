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
import com.nagabhushansn95.financetracker.data.investments.Statistics
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
        db = TestDb.inMemory(ApplicationProvider.getApplicationContext<Context>())
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
        val rows = Statistics(db).periodTotals(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            7,
            emptyList(),
            emptyList()
        )
        assertEquals(listOf("2026-10", "2026-11"), rows.map { it.period })
        val oct = rows[0]
        assertEquals(
            2840.0,
            oct.expense,
            1e-9
        )       // 1200 + 800 + 840 (the USD spend nothing can price counts 0; the excluded row is ignored)
        assertEquals(50000.0, oct.income, 1e-9)       // employer PF not counted
        assertEquals(18000.0, oct.savings, 1e-9)      // 3000 PF + 5000 PPF + 10000 invest
        assertEquals(0.0, oct.withdrawals, 1e-9)
        val nov = rows[1]
        assertEquals(300.0, nov.expense, 1e-9)
        assertEquals(6500.0, nov.withdrawals, 1e-9)   // 4000 redeem + 2500 PPF out
    }

    @Test
    fun yearlyGroupingAndFilters() = runTest {
        val statistics = Statistics(db)
        val year = statistics.periodTotals(
            LocalDate.of(2000, 1, 1),
            LocalDate.of(2099, 12, 31),
            4,
            emptyList(),
            emptyList()
        )
        assertEquals(listOf("2026"), year.map { it.period })
        assertEquals(3140.0, year.single().expense, 1e-9)

        val onlyUsd = statistics.periodTotals(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            7,
            listOf(usdWallet),
            emptyList()
        )
        assertEquals(840.0, onlyUsd.single().expense, 1e-9)

        val onlyTravel = statistics.periodTotals(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            7,
            emptyList(),
            listOf(travel)
        )
        assertEquals(800.0, onlyTravel.single().expense, 1e-9)
    }

    @Test
    fun categoryBreakdown() = runTest {
        val expense = Statistics(db).categoryTotals(
            TransactionKind.EXPENSE,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 31),
            emptyList()
        )
        assertEquals(listOf(food, travel), expense.map { it.categoryId })
        assertEquals(2040.0, expense[0].total, 1e-9)
        assertEquals(3, expense[0].count)

        val income = Statistics(db).categoryTotals(
            TransactionKind.INCOME,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 31),
            emptyList()
        )
        assertEquals(listOf(salary), income.map { it.categoryId })  // PF credit excluded
        assertEquals("2026-10-05", db.statisticsDao().earliestDate())
    }

    @Test
    fun depositInterestCreditedAsItsOwnRowIsIncomeAndAWithdrawal() = runTest {
        val deposit = db.instrumentDao().insert(
            Instrument(
                name = "FD #1",
                instrumentType = InstrumentType.FIXED_DEPOSIT,
                createdTime = now,
                modifiedTime = now
            )
        )

        fun row(
            date: String,
            kind: TransactionKind,
            amount: Double,
            from: Long? = null,
            to: Long? = null
        ) = TransactionEntity(
            createdTime = now,
            modifiedTime = now,
            date = LocalDate.parse(date),
            kind = kind,
            fromAccountId = from,
            toAccountId = to,
            instrumentId = deposit,
            particulars = "x",
            amount = amount,
        )
        db.transactionDao().insert(row("2027-01-05", TransactionKind.INCOME, 9.0, to = bank))
        db.transactionDao().insert(row("2027-01-05", TransactionKind.REDEEM, 19128.0, to = bank))
        val january = db.statisticsDao()
            .periodTotals("2027-01-01", "2027-01-31", 7, false, emptyList(), false, emptyList())
            .single()
        assertEquals(9.0, january.income, 0.001)
        assertEquals(19137.0, january.withdrawals, 0.001)
    }
}
