package com.nagabhushansn95.financetracker.data.importer.zerodha

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.TestDb
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.CategoryScope
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.Security
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.db.insertListed
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class ZerodhaImporterTest {
    private lateinit var db: AppDatabase
    private lateinit var importer: ZerodhaImporter
    private val now = LocalDateTime.of(2026, 11, 4, 9, 0)
    private var demat = 0L
    private var bank = 0L
    private var trading = 0L
    private var goldbees = 0L
    private var parag = 0L

    @Before
    fun setUp() = runTest {
        db = TestDb.inMemory(ApplicationProvider.getApplicationContext<Context>())
        importer = ZerodhaImporter(db)
        demat = db.accountDao().insert(
            Account(
                name = "Zerodha Demat INR",
                accountType = AccountType.DEMAT,
                balance = 100000.0,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        bank = db.accountDao().insert(
            Account(
                name = "HDFC",
                accountType = AccountType.BANK,
                balance = 500000.0,
                displayOrder = 2,
                createdTime = now,
                modifiedTime = now
            )
        )
        trading = db.categoryDao()
            .insert(Category(name = "Trading", appliesTo = CategoryScope.EXPENSE, displayOrder = 1))
        db.categoryDao().insert(
            Category(
                name = "Interest & Dividends",
                appliesTo = CategoryScope.INCOME,
                displayOrder = 2
            )
        )
        goldbees = db.insertListed(
            Instrument(
                name = "GOLDBEES",
                instrumentType = InstrumentType.STOCK,
                createdTime = now,
                modifiedTime = now
            ), Security(symbol = "GOLDBEES")
        )
        parag = db.insertListed(
            Instrument(
                name = "Parag Parikh Flexi Cap Fund",
                instrumentType = InstrumentType.MUTUAL_FUND,
                createdTime = now,
                modifiedTime = now
            )
        )
        // Recorded by the 4.x importer: no broker reference.
        db.transactionDao().insert(
            TransactionEntity(
                createdTime = now,
                modifiedTime = now,
                date = LocalDate.of(2026, 10, 6),
                kind = TransactionKind.INVEST,
                fromAccountId = demat,
                instrumentId = goldbees,
                particulars = "Bought Stocks - GOLDBEES",
                quantity = 65.0,
                rate = 120.77,
                amount = 7850.05
            )
        )
    }

    @After
    fun tearDown() = db.close()

    private val tradebook = """
        symbol,isin,trade_date,exchange,segment,series,trade_type,auction,quantity,price,trade_id,order_id,order_execution_time
        GOLDBEES,INF204KB17I5,2026-10-06,NSE,EQ,EQ,buy,false,65.000000,120.769997,204482251,1100000032172761,2026-10-06T11:00:03
        NIFTYBEES,INF204KB14I2,2026-10-06,NSE,EQ,EQ,buy,false,10.000000,258.500000,403865182,1200000034646814,2026-10-06T11:00:03
        MASPTOP50,INF769K01HP3,2026-10-21,NSE,EQ,EQ,sell,false,127.000000,103.510002,407792762,1200000085347860,2026-10-21T13:45:47
        MASPTOP50,INF769K01HP3,2026-10-21,NSE,EQ,EQ,sell,false,24.000000,103.410004,407792767,1200000085347860,2026-10-21T13:45:47
    """.trimIndent()

    @Test
    fun tradebookCreatesInstrumentsMatchesOldRowsAndAddsOneDpChargePerSale() = runTest {
        val settings = importer.defaultSettings()
        assertEquals(
            demat,
            settings.dematAccountId
        )             // inferred from the existing stock purchase
        val plan = importer.plan(ZerodhaStatements.parse(tradebook), settings, now)
        assertEquals(
            1,
            plan.duplicates
        )                          // the GOLDBEES buy matches the 4.x row
        assertEquals(1, plan.legacyMatches.size)
        assertEquals(listOf("NIFTYBEES", "MASPTOP50"), plan.newInstruments.map { it.name })
        assertEquals("INF204KB17I5", plan.isinUpdates[goldbees])
        assertEquals(4, plan.toInsert.size)                       // 1 buy, 2 sells, 1 DP charge

        val result = importer.commit(plan, now)
        assertEquals(4, result.inserted)
        assertEquals(2, result.instrumentsCreated)
        assertEquals(1, result.referencesStamped)
        val rows = db.transactionDao().getAllWithInstrument()
        assertEquals(
            "zerodha:204482251",
            rows.first { it.particulars == "Bought Stocks - GOLDBEES" }.brokerRef
        )
        assertEquals("INF204KB17I5", db.instrumentDao().getSecurity(goldbees)!!.isin)
        val nifty = db.instrumentDao().getListed().first { it.security.isin == "INF204KB14I2" }
        assertEquals("NIFTYBEES", nifty.security.symbol)
        assertEquals("NSE", nifty.security.exchange)
        val dp = rows.single { it.particulars.startsWith("Depository") }
        assertEquals(TransactionKind.EXPENSE, dp.kind)
        assertEquals(trading, dp.categoryId)
        assertEquals(demat, dp.fromAccountId)
        assertEquals(ZerodhaImportSettings.DP_CHARGE, dp.amount, 1e-9)
        // Demat balance: −2585 (buy) + 13145.77 + 2481.84 (sells) − 15.34 (DP).
        assertEquals(
            100000.0 - 2585.0 + 13145.77 + 2481.84 - 15.34,
            db.accountDao().getById(demat)!!.balance,
            1e-6
        )

        // Importing the same file again changes nothing.
        val again = importer.plan(ZerodhaStatements.parse(tradebook), settings, now)
        assertEquals(0, again.toInsert.size)
        assertEquals(5, again.duplicates)
        assertTrue(again.legacyMatches.isEmpty())
    }

    @Test
    fun coinOrdersFundTheBankAndMatchFundsByNameThenIsin() = runTest {
        val coin = """
            client_id,isin,scheme_name,plan,transaction_mode,settlement_id,trade_date,ordered_at,folio_number,amount,units,nav,status,exchange_order_id,remarks,tag
            AB1234,INF879O01027,Parag Parikh Flexi Cap Fund,,BUY,2026191,10/10/2026,01:37 AM,1,25000,282.297,88.55,COMPLETE,462830020099, ,coinandroidsip
            AB1234,INF966L01689,Quant Small Cap Fund,,BUY,2026191,10/10/2026,01:37 AM,2,25000,79.467,314.58,COMPLETE,462830020098, ,coinandroidsip
        """.trimIndent()
        val plan = importer.plan(
            ZerodhaStatements.parse(coin),
            ZerodhaImportSettings(dematAccountId = demat, bankAccountId = bank),
            now
        )
        assertEquals(listOf("Quant Small Cap Fund"), plan.newInstruments.map { it.name })
        assertEquals("INF879O01027", plan.isinUpdates[parag])
        importer.commit(plan, now)
        val rows = db.transactionDao().getForInstrument(parag)
        val sip = rows.single()
        assertEquals("Mutual Funds SIP Installment - Parag Parikh Flexi Cap Fund", sip.particulars)
        assertEquals(bank, sip.fromAccountId)
        assertEquals(TransactionKind.INVEST, sip.kind)
        assertEquals(500000.0 - 50000.0, db.accountDao().getById(bank)!!.balance, 1e-9)

        // The fund tradebook for the same orders is a duplicate by reference.
        val mfTradebook = """
            symbol,isin,trade_date,exchange,segment,series,trade_type,auction,quantity,price,trade_id,order_id,order_execution_time
            PARAG PARIKH FLEXI CAP FUND - DIRECT PLAN,INF879O01027,2026-10-12,NSE,MF,,buy,false,282.297000,88.554700,462830020099,462830020099,2026-10-12T00:00:00
        """.trimIndent()
        val again = importer.plan(
            ZerodhaStatements.parse(mfTradebook),
            ZerodhaImportSettings(demat, bank),
            now
        )
        assertEquals(0, again.toInsert.size)
        assertEquals(1, again.duplicates)
    }

    private suspend fun view(id: Long) =
        com.nagabhushansn95.financetracker.data.investments.HoldingEntries.build(
            db.instrumentDao().getListed(), db.transactionDao().getAllWithInstrument()
        ).first { it.instrument.id == id }

    @Test
    fun dividendsWaitForTheBankCreditAndBecomeTheEntryWhenRecorded() = runTest {
        val parsed =
            ZerodhaStatements.parse("Symbol,Ex-date,Qty,Dividend per share,Total dividend\nGOLDBEES,2026-08-04,65,1.5,97.5\n")
        val plan = importer.plan(parsed, ZerodhaImportSettings(demat, bank), now)
        assertEquals(0, plan.toInsert.size)
        assertEquals(1, plan.toHold.size)
        val result = importer.commit(plan, now)
        assertEquals(1, result.held)
        // Nothing in the ledger yet; the dividend sits in the pending list with the statement's figures.
        assertTrue(
            db.transactionDao().getForInstrument(goldbees)
                .none { it.kind == TransactionKind.INCOME })
        val pending = db.pendingDividendDao().getAll().single()
        assertEquals(goldbees, pending.instrumentId)
        assertEquals(LocalDate.of(2026, 8, 4), pending.exDate)
        assertEquals(65.0, pending.quantity, 1e-9)
        assertEquals(1.5, pending.perShare, 1e-9)
        assertEquals(97.5, pending.gross, 1e-9)
        assertEquals("div:GOLDBEES:2026-08-04", pending.brokerRef)
        // Importing the statement again adds nothing.
        val again = importer.plan(parsed, ZerodhaImportSettings(demat, bank), now)
        assertEquals(0, again.toHold.size)
        assertEquals(1, again.duplicates)

        // The bank credits 87.75 on 10 August: the entry is recorded net, with the reference, and the pending row goes.
        com.nagabhushansn95.financetracker.data.repository.HoldingsRepository(db)
            .recordPendingDividend(
                pending,
                view(goldbees),
                LocalDate.of(2026, 8, 10),
                null,
                null,
                87.75,
                bank,
                "Dividend From Stocks - GOLDBEES",
                now
            )
        val d = db.transactionDao().getForInstrument(goldbees)
            .first { it.kind == TransactionKind.INCOME }
        assertEquals(bank, d.toAccountId)
        assertEquals(LocalDate.of(2026, 8, 10), d.date)
        assertEquals(LocalDate.of(2026, 8, 4), d.exDate)
        assertEquals(87.75, d.amount, 1e-9)
        assertEquals(65.0, d.quantity!!, 1e-9)
        assertEquals(1.5, d.rate!!, 1e-9)
        assertEquals("div:GOLDBEES:2026-08-04", d.brokerRef)
        assertEquals("Interest & Dividends", db.categoryDao().getById(d.categoryId!!)!!.name)
        assertTrue(db.pendingDividendDao().getAll().isEmpty())
        // And a third import still finds it recorded.
        val third = importer.plan(parsed, ZerodhaImportSettings(demat, bank), now)
        assertEquals(0, third.toHold.size)
        assertEquals(1, third.duplicates)
    }

    @Test
    fun dividendsRecordedOnTheCreditDateWithTheNetAmountAreMatchedByExDate() = runTest {
        db.transactionDao().insert(
            TransactionEntity(
                createdTime = now,
                modifiedTime = now,
                date = LocalDate.of(2026, 8, 10),
                kind = TransactionKind.INCOME,
                toAccountId = bank,
                instrumentId = goldbees,
                particulars = "Dividend From Stocks - GOLDBEES (Ex-Dividend Date: 04/08/2026)",
                amount = 96.9,
                exDate = LocalDate.of(2026, 8, 4)
            )
        )
        val parsed =
            ZerodhaStatements.parse("Symbol,Ex-date,Qty,Dividend per share,Total dividend\nGOLDBEES,2026-08-04,65,1.5,97.5\nGOLDBEES,2026-05-02,65,1.0,65\n")
        val plan = importer.plan(parsed, ZerodhaImportSettings(demat, bank), now)
        assertEquals(0, plan.toInsert.size)
        assertEquals(1, plan.toHold.size)
        assertEquals(1, plan.legacyMatches.size)
        importer.commit(plan, now)
        val rows = db.transactionDao().getForInstrument(goldbees)
            .filter { it.kind == TransactionKind.INCOME }
        assertEquals(1, rows.size)
        assertEquals("div:GOLDBEES:2026-08-04", rows.single().brokerRef)
        assertEquals("div:GOLDBEES:2026-05-02", db.pendingDividendDao().getAll().single().brokerRef)
    }

    @Test
    fun coinOrdersTakeTheOrderDateAndTheTradebookPrice() = runTest {
        // "ordered_at" with a date: the bank was debited on the order date, the units allotted two days later.
        val coin = """
            client_id,isin,scheme_name,plan,transaction_mode,settlement_id,trade_date,ordered_at,folio_number,amount,units,nav,status,exchange_order_id,remarks,tag
            AB1234,INF879O01027,Parag Parikh Flexi Cap Fund,,BUY,2026191,12/10/2026,10/10/2026 01:37 AM,910022,25000,282.297,88.55,COMPLETE,462830020099, ,coinandroidsip
            AB1234,INF966L01689,Quant Small Cap Fund,,BUY,2026191,12/10/2026,10/10/2026 01:37 AM,910023,25000,79.467,314.58,COMPLETE,462830020098, ,coinandroidsip
        """.trimIndent()
        val mfTradebook = """
            symbol,isin,trade_date,exchange,segment,series,trade_type,auction,quantity,price,trade_id,order_id,order_execution_time
            PARAG PARIKH FLEXI CAP FUND - DIRECT PLAN,INF879O01027,2026-10-12,NSE,MF,,buy,false,282.297000,88.554700,462830020099,462830020099,2026-10-12T00:00:00
        """.trimIndent()
        val merged = ZerodhaStatements.mergeCoinWithTradebook(
            ZerodhaStatements.parse(coin), ZerodhaStatements.parse(mfTradebook)
        )
        assertEquals(
            1,
            merged.skipped.size
        )   // the Quant order has no tradebook row: Coin's NAV stays
        val plan = importer.plan(merged, ZerodhaImportSettings(demat, bank), now)
        importer.commit(plan, now)
        val sip = db.transactionDao().getForInstrument(parag).single()
        assertEquals(LocalDate.of(2026, 10, 10), sip.date)
        assertEquals(88.5547, sip.rate!!, 1e-9)
        assertEquals(25000.0, sip.amount, 1e-9)
        val quant = db.instrumentDao().getAll().first { it.name == "Quant Small Cap Fund" }
        assertEquals("910023", db.instrumentDao().getSecurity(quant.id)!!.folio)
        assertNull(quant.notes)
        val quantRow = db.transactionDao().getForInstrument(quant.id).single()
        assertEquals(314.58, quantRow.rate!!, 1e-9)
    }

    @Test
    fun tradebookCarriesTheExchangeAndTheSeries() = runTest {
        val tradebook = """
            symbol,isin,trade_date,exchange,segment,series,trade_type,auction,quantity,price,trade_id,order_id,order_execution_time
            EMBASSY,INE041025011,2026-10-06,NSE,EQ,RR,buy,false,10,380.5,1001,2001,2026-10-06T10:00:00
            GOLDBEES,INF204KB17I5,2026-10-07,NSE,EQ,EQ,buy,false,5,121,1002,2002,2026-10-07T10:00:00
            TATAMOTORS,INE155A01022,2026-10-08,BSE,EQ,A,buy,false,1,700,1003,2003,2026-10-08T10:00:00
        """.trimIndent()
        val plan = importer.plan(
            ZerodhaStatements.parse(tradebook),
            ZerodhaImportSettings(demat, bank),
            now
        )
        importer.commit(plan, now)
        val all = db.instrumentDao().getAllSecurities()
        val embassy = all.first { it.symbol == "EMBASSY" }
        assertEquals("RR", embassy.series)
        assertEquals("NSE", embassy.exchange)
        assertNull(all.first { it.symbol == "GOLDBEES" }.series)
        val tata = all.first { it.symbol == "TATAMOTORS" }
        assertEquals("BSE", tata.exchange)
        assertEquals("A", tata.series)
    }

    @Test
    fun chargesAreTradingExpensesFromTheDematAccount() = runTest {
        val plan = importer.plan(
            ZerodhaStatements.parse("date,amount\n2026/10/06,10.87\n2026/10/13,10.89\n"),
            ZerodhaImportSettings(demat, bank),
            now
        )
        importer.commit(plan, now)
        val rows = db.transactionDao().getBetween("2026-10-01", "2026-10-31")
            .filter { it.particulars == "Zerodha Transaction Charges" }
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.kind == TransactionKind.EXPENSE && it.fromAccountId == demat && it.categoryId == trading })
        assertNull(rows.first().instrumentId)
        assertEquals(100000.0 - 21.76, db.accountDao().getById(demat)!!.balance, 1e-9)
    }

    @Test
    fun refusesToCommitWithoutTheAccounts() = runTest {
        val plan = importer.plan(
            ZerodhaStatements.parse("date,amount\n2026/10/06,10.87\n"),
            ZerodhaImportSettings(dematAccountId = null, bankAccountId = null),
            now
        )
        assertTrue(plan.needsDemat)
        assertTrue(runCatching { importer.commit(plan, now) }.isFailure)
    }
}
