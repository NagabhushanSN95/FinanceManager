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
    fun refusesToCommitWithoutTheAccounts() = runTest {
        val plan = importer.plan(
            ZerodhaStatements.parse(
                "symbol,isin,trade_date,exchange,segment,series,trade_type,auction,quantity,price,trade_id,order_id,order_execution_time\n" +
                        "GOLDBEES,INF204KB17I5,2026-10-06,NSE,EQ,EQ,buy,false,10,120.0,1,1,2026-10-06T10:00:00\n"
            ),
            ZerodhaImportSettings(dematAccountId = null, bankAccountId = null),
            now
        )
        assertTrue(plan.needsDemat)
        assertTrue(runCatching { importer.commit(plan, now) }.isFailure)
    }

    private val ledgerHeader =
        "particulars,posting_date,cost_center,voucher_type,debit,credit,net_balance"
    private val tradebookHeader =
        "symbol,isin,trade_date,exchange,segment,series,trade_type,auction,quantity,price,trade_id,order_id,order_execution_time"

    private suspend fun planLedger(tradebook: String, ledger: String): ImportPlan {
        val merged = ZerodhaStatements.mergeTradebookWithLedger(
            ZerodhaStatements.parse(tradebookHeader + "\n" + tradebook.trimIndent()),
            ZerodhaStatements.parse(ledgerHeader + "\n" + ledger.trimIndent()),
        )
        return importer.plan(
            merged,
            ZerodhaImportSettings(dematAccountId = demat, bankAccountId = bank),
            now
        )
    }

    @Test
    fun ledgerChargesAreTheSettlementLessTheTradesBookedToThePaisaWithTheRemainderCarried() =
        runTest {
            // Day 1: a buy of 12,000 settles at 12,007.2175, so the charges are 7.2175 → 7.22 booked, −0.0025 carried.
            // Day 2: a sale of 5,000 settles as a credit of 4,991.8869, so the charges are 8.1131; the carried total
            // 15.3306 rounds to 15.33, so 8.11 is booked (8.1131 alone would have rounded to 8.11 too; the carry is what
            // keeps the booked total on the ledger's running figure).
            val plan = planLedger(
                """
            GOLDBEES,INF204KB17I5,2026-11-02,NSE,EQ,EQ,buy,false,100,120.0,100001,200001,2026-11-02T10:00:00
            GOLDBEES,INF204KB17I5,2026-11-03,NSE,EQ,EQ,sell,false,40,125.0,100002,200002,2026-11-03T10:00:00
            """,
                """
            Funds added using UPI autopay from RYU781 with UPI reference number 668699710073,2026-11-02,NSE-EQ - Z,Bank Receipts,0.000000,25000.000000,25000.000000
            Net settlement for Equity with settlement number: 2026210,2026-11-02,NSE-EQ - Z,Book Voucher,12007.217500,0.000000,12992.782500
            DP Charges for Sale of GOLDBEES on 03/11/2026,2026-11-03,NSE-EQ - Z,Journal Entry,15.340000,0.000000,12977.442500
            Net settlement for Equity with settlement number: 2026211,2026-11-03,NSE-EQ - Z,Book Voucher,0.000000,4991.886900,17969.329400
            Payout of 10000.0/- to HDFC BANK LTD 5010 A/C number as per withdrawal request made on 2026-11-03 with reference number abc123,2026-11-04,NSE-EQ - Z,Bank Payments,10000.000000,0.000000,7969.329400
            """,
            )
            assertEquals(StatementType.TRADEBOOK_AND_LEDGER, plan.type)
            val rows = plan.toInsert.map { it.transaction }
            val byRef = rows.associateBy { it.brokerRef }
            assertEquals(7, rows.size)
            val funds = byRef.getValue("ledger:in:668699710073")
            assertEquals(TransactionKind.TRANSFER, funds.kind)
            assertEquals(bank, funds.fromAccountId); assertEquals(demat, funds.toAccountId)
            assertEquals(25000.0, funds.amount, 1e-9)
            assertEquals("Zerodha Kite Weekly SIP Auto Debit", funds.particulars)
            val payout = byRef.getValue("ledger:out:abc123")
            assertEquals(demat, payout.fromAccountId); assertEquals(bank, payout.toAccountId)
            assertEquals(LocalDate.of(2026, 11, 4), payout.date)
            assertEquals(7.22, byRef.getValue("ledger:charges:2026-11-02").amount, 1e-9)
            assertEquals(8.11, byRef.getValue("ledger:charges:2026-11-03").amount, 1e-9)
            assertEquals(
                "Zerodha Transaction Charges",
                byRef.getValue("ledger:charges:2026-11-03").particulars
            )
            // The DP charge comes from the ledger under the tradebook's own key, linked to the stock; none is synthesised.
            val dp = byRef.getValue("kite:dp:2026-11-03:GOLDBEES")
            assertEquals(15.34, dp.amount, 1e-9)
            assertEquals(goldbees, dp.instrumentId)
            assertEquals(trading, dp.categoryId)
            assertEquals(1, rows.count { it.particulars.startsWith("Depository") })

            val result = importer.commit(plan, now)
            assertEquals(7, result.inserted)
            // 100000 + 25000 − 12000 − 7.22 − 15.34 + 5000 − 8.11 − 10000
            assertEquals(107969.33, db.accountDao().getById(demat)!!.balance, 1e-6)
            // Imported again: everything is already there.
            assertEquals(
                0, planLedger(
                    "GOLDBEES,INF204KB17I5,2026-11-02,NSE,EQ,EQ,buy,false,100,120.0,100001,200001,2026-11-02T10:00:00",
                    "Net settlement for Equity with settlement number: 2026210,2026-11-02,NSE-EQ - Z,Book Voucher,12007.217500,0.000000,12992.782500"
                ).toInsert.size
            )
        }

    @Test
    fun aSettlementDayWithoutTradesIsLeftOutAndHandEnteredTransfersAreMatched() = runTest {
        // A transfer recorded by hand on the ledger's date is the same movement; it gets the reference instead of a twin.
        db.transactionDao().insert(
            TransactionEntity(
                createdTime = now,
                modifiedTime = now,
                date = LocalDate.of(2026, 11, 2),
                kind = TransactionKind.TRANSFER,
                fromAccountId = bank,
                toAccountId = demat,
                particulars = "",
                amount = 25000.0,
            )
        )
        val plan = planLedger(
            "GOLDBEES,INF204KB17I5,2026-11-02,NSE,EQ,EQ,buy,false,100,120.0,100001,200001,2026-11-02T10:00:00",
            """
            Funds added using UPI from RYU781 with reference number 93534936262,2026-11-02,NSE-EQ - Z,Bank Receipts,0.000000,25000.000000,25000.000000
            Net settlement for Equity with settlement number: 2026210,2026-11-02,NSE-EQ - Z,Book Voucher,12007.217500,0.000000,12992.782500
            Net settlement for Equity with settlement number: 2026211,2026-11-03,NSE-EQ - Z,Book Voucher,3000.500000,0.000000,9992.282500
            AMC for Demat Account for 2026-07-16 to 2026-10-15,2026-11-03,NSE-EQ - Z,Journal Entry,88.500000,0.000000,9903.782500
            """,
        )
        val refs = plan.toInsert.map { it.transaction.brokerRef }
        assertTrue("ledger:charges:2026-11-02" in refs)
        assertTrue("ledger:charges:2026-11-03" !in refs)       // no trades on the 3rd in the app or the tradebook
        assertTrue(plan.skipped.any { it.startsWith("2026-11-03") && it.contains("no trades") })
        assertEquals(1, plan.legacyMatches.size)
        assertEquals("ledger:in:93534936262", plan.legacyMatches.values.first())
        val amc =
            plan.toInsert.first { it.transaction.brokerRef == "ledger:amc:2026-11-03" }.transaction
        assertEquals("Zerodha Charges - Quarterly Account Maintenance Charges", amc.particulars)
        assertEquals(88.5, amc.amount, 1e-9)
    }
}
