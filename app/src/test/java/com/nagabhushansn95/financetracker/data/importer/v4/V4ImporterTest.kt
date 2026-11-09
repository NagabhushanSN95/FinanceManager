package com.nagabhushansn95.financetracker.data.importer.v4

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class V4ImporterTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun mapsEveryBranch() = runTest {
        val src = FakeV4Source()
        val expense =
            src.tx("2026/11/01", "Debit Wallet09 Exp02", "Dinner On Zomato", 385.19, rate = 385.19)
        val income = src.tx("2026/10/30", "Credit Bank03", "Salary", 100000.0)
        val interest = src.tx("2026/10/30", "Credit Bank02", "SBI SB Interest Credited", 120.0)
        val dividend = src.tx(
            "2026/08/20",
            "Credit Bank03",
            "Dividend From Stocks - BAGMANE (Ex-Dividend Date: 14/08/2026)",
            2864.74
        )
        val transfer = src.tx("2026/10/01", "Transfer Bank03 Bank17", "", 50000.0)
        val buy = src.tx(
            "2026/10/27",
            "Transfer Bank17 Bank16",
            "Bought Stocks - SILVERBEES",
            9025.0,
            rate = 180.5,
            quantity = 50.0
        )
        val sell = src.tx(
            "2026/10/21",
            "Transfer Bank16 Bank17",
            "Sold Stocks - MON100",
            3701.64,
            rate = 308.47,
            quantity = 12.0
        )
        val profit =
            src.tx("2026/10/21", "Credit Bank16", "Profit In Selling Stocks - MON100", 15218.79)
        val loss =
            src.tx("2024/10/29", "Debit Bank16 Exp11", "Loss In Selling Stocks - OLAELEC", 13.11)
        val dp = src.tx(
            "2026/10/21",
            "Debit Bank17 Exp20",
            "Depository Participant Charges For Selling Stocks - MON100",
            15.34
        )
        val sip = src.tx(
            "2026/10/10",
            "Transfer Bank03 Bank16",
            "Mutual Funds SIP Installment - Parag Parikh Flexi Cap Fund",
            25000.0,
            rate = 88.55,
            quantity = 282.297
        )
        val fd = src.tx(
            "2026/06/01",
            "Transfer Bank03 Bank16",
            "Created A Fixed Deposit For 12 Months",
            100000.0
        )
        val fdInterest =
            src.tx("2026/09/30", "Credit Bank16", "Fixed Deposit Interest Credited", 1800.0)
        val fdMatured = src.tx(
            "2026/10/15",
            "Transfer Bank16 Bank03",
            "Fixed Deposit Matured And Closed",
            101800.0
        )
        val epfEmployer = src.tx(
            "2016/08/24",
            "Credit Bank16",
            "Invested In Employees Provident Fund - Employer Contribution",
            4500.0
        )
        val epfInterest = src.tx(
            "2017/03/31",
            "Credit Bank16",
            "Employees Provident Fund Interest Credited",
            300.0
        )
        val ppfIn = src.tx(
            "2025/04/05",
            "Transfer Bank02 Bank16",
            "Invested In Public Provident Fund - Self",
            12500.0
        )
        val premium = src.tx(
            "2025/03/15",
            "Transfer Bank03 Bank16",
            "Life Insurance Premium - HDFC Life - Guaranteed Wealth Plus",
            125400.0
        )
        val amma = src.tx(
            "2016/10/03",
            "Transfer Bank03 Bank16",
            "Transferred To Amma's KVGB Account",
            75000.0
        )

        val report = V4Importer(db).import(src)

        // accounts: 3 wallets + 4 banks (minus Investments) + Demat USD + EPF + PPF
        val accounts = db.accountDao().getAll()
        assertEquals(10, accounts.size)
        assertEquals(
            AccountType.CREDIT_CARD,
            accounts.single { it.name == "HDFC Millenia Credit Card" }.accountType
        )
        assertEquals(
            AccountType.DEMAT,
            accounts.single { it.name == "Zerodha Demat INR" }.accountType
        )
        assertEquals("USD", accounts.single { it.name == "Zerodha Demat USD" }.currency)
        assertEquals("USD", accounts.single { it.name == "Wallet USD" }.currency)
        assertTrue(accounts.single { it.name == "Wallet USD" }.deleted)
        assertNull(accounts.firstOrNull { it.name == "Investments" })
        assertEquals(4800.0, accounts.single { it.name == "EPF" }.balance, 1e-9)
        assertEquals(12500.0, accounts.single { it.name == "PPF" }.balance, 1e-9)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), accounts.map { it.displayOrder })

        // categories keep v4 ids, plus one income category for interest and dividends
        val categories = db.categoryDao().getAll()
        assertEquals(4, categories.size)
        assertEquals("Food", db.categoryDao().getById(2)!!.name)

        // instruments
        val instruments = db.instrumentDao().getAll()
        assertEquals(
            setOf(
                "BAGMANE",
                "SILVERBEES",
                "MON100",
                "Parag Parikh Flexi Cap Fund",
                "Fixed Deposits (unmapped)",
                "HDFC Life - Guaranteed Wealth Plus",
                "Amma's KVGB Account"
            ), instruments.map { it.name }.toSet()
        )
        assertEquals("NSE", instruments.single { it.name == "MON100" }.exchange)
        val policy = db.instrumentDao()
            .getInsurancePolicy(instruments.single { it.instrumentType == InstrumentType.INSURANCE }.id)!!
        assertEquals("HDFC Life", policy.provider)
        assertEquals("Guaranteed Wealth Plus", policy.policyName)
        assertEquals(
            com.nagabhushansn95.financetracker.data.db.entity.InsuranceType.LIFE,
            policy.insuranceType
        )
        assertNotNull(
            db.instrumentDao()
                .getFixedDeposit(instruments.single { it.instrumentType == InstrumentType.FIXED_DEPOSIT }.id)
        )

        // transactions
        val dao = db.transactionDao()
        assertEquals(17, dao.count())
        assertEquals(2, report.dropped.size)
        assertEquals(15218.79, report.realisedPnlByInstrument["MON100"]!!, 1e-9)
        assertEquals(-13.11, report.realisedPnlByInstrument["OLAELEC"]!!, 1e-9)
        assertNull(dao.getById(profit.id.toLong()))
        assertNull(dao.getById(loss.id.toLong()))

        suspend fun tx(v4: V4Transaction) = dao.getById(v4.id.toLong())!!
        fun acc(name: String) = accounts.single { it.name == name }.id
        fun inst(name: String) = instruments.single { it.name == name }.id

        tx(expense).let {
            assertEquals(
                TransactionKind.EXPENSE,
                it.kind
            ); assertEquals(acc("Amazon Pay"), it.fromAccountId); assertEquals(2L, it.categoryId)
        }
        tx(income).let {
            assertEquals(
                TransactionKind.INCOME,
                it.kind
            ); assertEquals(acc("HDFC Bank"), it.toAccountId); assertNull(it.categoryId)
        }
        tx(interest).let {
            assertEquals(
                "Interest & Dividends",
                categories.single { c -> c.id == it.categoryId }.name
            )
        }
        tx(dividend).let {
            assertEquals(TransactionKind.INCOME, it.kind); assertEquals(
            inst("BAGMANE"),
            it.instrumentId
        )
            assertEquals(LocalDate.of(2026, 8, 14), it.exDate); assertEquals(
            "Interest & Dividends",
            categories.single { c -> c.id == it.categoryId }.name
        )
        }
        tx(transfer).let {
            assertEquals(
                TransactionKind.TRANSFER,
                it.kind
            ); assertEquals(acc("HDFC Bank"), it.fromAccountId); assertEquals(
            acc("Zerodha Demat INR"),
            it.toAccountId
        )
        }
        tx(buy).let {
            assertEquals(TransactionKind.INVEST, it.kind); assertEquals(
            acc("Zerodha Demat INR"),
            it.fromAccountId
        ); assertNull(it.toAccountId)
            assertEquals(inst("SILVERBEES"), it.instrumentId); assertEquals(
            50.0,
            it.quantity!!,
            1e-9
        ); assertEquals(180.5, it.rate!!, 1e-9)
        }
        tx(sell).let {
            assertEquals(
                TransactionKind.REDEEM,
                it.kind
            ); assertEquals(acc("Zerodha Demat INR"), it.toAccountId); assertEquals(
            inst("MON100"),
            it.instrumentId
        ); assertEquals(12.0, it.quantity!!, 1e-9)
        }
        tx(dp).let {
            assertEquals(TransactionKind.EXPENSE, it.kind); assertEquals(
            inst("MON100"),
            it.instrumentId
        ); assertEquals(20L, it.categoryId)
        }
        tx(sip).let {
            assertEquals(
                TransactionKind.INVEST,
                it.kind
            ); assertEquals(inst("Parag Parikh Flexi Cap Fund"), it.instrumentId); assertEquals(
            282.297,
            it.quantity!!,
            1e-9
        )
        }
        tx(fd).let {
            assertEquals(
                TransactionKind.INVEST,
                it.kind
            ); assertEquals(inst("Fixed Deposits (unmapped)"), it.instrumentId)
        }
        tx(fdInterest).let {
            assertEquals(
                TransactionKind.INCOME,
                it.kind
            ); assertNull(it.toAccountId); assertEquals(
            inst("Fixed Deposits (unmapped)"),
            it.instrumentId
        )
        }
        tx(fdMatured).let {
            assertEquals(
                TransactionKind.REDEEM,
                it.kind
            ); assertEquals(acc("HDFC Bank"), it.toAccountId)
        }
        tx(epfEmployer).let {
            assertEquals(
                TransactionKind.INCOME,
                it.kind
            ); assertEquals(acc("EPF"), it.toAccountId); assertEquals("Employer", it.contributor)
        }
        tx(epfInterest).let {
            assertEquals("Interest", it.contributor); assertEquals(
            acc("EPF"),
            it.toAccountId
        )
        }
        tx(ppfIn).let {
            assertEquals(
                TransactionKind.TRANSFER,
                it.kind
            ); assertEquals(acc("State Bank Of India"), it.fromAccountId); assertEquals(
            acc("PPF"),
            it.toAccountId
        )
        }
        tx(premium).let {
            assertEquals(
                TransactionKind.INVEST,
                it.kind
            ); assertEquals(inst("HDFC Life - Guaranteed Wealth Plus"), it.instrumentId)
        }
        tx(amma).let {
            assertEquals(
                TransactionKind.INVEST,
                it.kind
            ); assertEquals(inst("Amma's KVGB Account"), it.instrumentId)
        }

        // templates: the profit template is dropped
        val templates = dao.getAllTemplates()
        assertEquals(3, templates.size)
        assertEquals(
            TransactionKind.INVEST,
            templates.single { it.particulars == "Bought Stocks -" }.kind
        )
        assertEquals(
            acc("PPF"),
            templates.single { it.particulars.startsWith("Invested In Public") }.toAccountId
        )
        assertEquals(3, report.templates)
    }

    @Test(expected = V4ImportException::class)
    fun abortsOnUnknownInvestmentRow() = runTest {
        val src = FakeV4Source()
        src.tx("2026/01/01", "Transfer Bank03 Bank16", "Something New", 10.0)
        V4Importer(db).import(src)
    }

    @Test
    fun abortLeavesDatabaseEmpty() = runTest {
        val src = FakeV4Source()
        src.tx("2026/01/01", "Transfer Bank03 Bank16", "Something New", 10.0)
        runCatching { V4Importer(db).import(src) }
        assertEquals(0, db.accountDao().getAll().size)
        assertEquals(0, db.transactionDao().count())
    }
}
