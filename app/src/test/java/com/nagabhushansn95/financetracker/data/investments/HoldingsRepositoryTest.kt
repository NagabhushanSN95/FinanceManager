package com.nagabhushansn95.financetracker.data.investments

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.TestDb
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.repository.HoldingsRepository
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
class HoldingsRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: HoldingsRepository
    private val now = LocalDateTime.of(2026, 11, 10, 9, 0)
    private var demat = 0L
    private var ammaDemat = 0L
    private var bank = 0L

    @Before
    fun setUp() = runTest {
        db = TestDb.inMemory(ApplicationProvider.getApplicationContext<Context>())
        repo = HoldingsRepository(db)
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
        ammaDemat = db.accountDao().insert(
            Account(
                name = "Amma Zerodha Demat INR",
                accountType = AccountType.DEMAT,
                balance = 50000.0,
                displayOrder = 2,
                createdTime = now,
                modifiedTime = now
            )
        )
        bank = db.accountDao().insert(
            Account(
                name = "HDFC Bank",
                accountType = AccountType.BANK,
                balance = 10000.0,
                displayOrder = 3,
                createdTime = now,
                modifiedTime = now
            )
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun view(id: Long): HoldingView =
        HoldingEntries.build(
            db.instrumentDao().getListed(),
            db.transactionDao().getAllWithInstrument(),
            db.holdingEventDao().getAll(),
        ).single { it.instrument.id == id }

    private suspend fun balance(id: Long) = db.accountDao().getById(id)!!.balance

    @Test
    fun aStockIsBoughtThroughTwoAccountsSoldPerAccountAndPaysADividend() = runTest {
        val id = repo.add(
            "Nippon India ETF Nifty 50 BeES",
            InstrumentType.STOCK,
            "NIFTYBEES",
            null,
            null,
            "INR",
            null,
            "Self",
            null,
            now
        )
        val i = db.instrumentDao().getSecurity(id)!!
        assertEquals("NSE", i.exchange)
        assertEquals("NIFTYBEES", i.symbol)

        repo.saveEntry(
            view(id),
            HoldingEntryKind.BUY,
            LocalDate.of(2026, 1, 5),
            100.0,
            250.0,
            25000.0,
            demat,
            "",
            now = now
        )
        repo.saveEntry(
            view(id),
            HoldingEntryKind.BUY,
            LocalDate.of(2026, 2, 5),
            50.0,
            300.0,
            15000.0,
            ammaDemat,
            "",
            now = now
        )
        assertEquals(75000.0, balance(demat), 1e-9)
        assertEquals(35000.0, balance(ammaDemat), 1e-9)
        var v = view(id)
        assertEquals(150.0, v.position.units, 1e-9)
        assertEquals(mapOf(demat to 100.0, ammaDemat to 50.0), v.position.unitsByAccount)
        assertEquals("Bought Stocks - NIFTYBEES", v.rows.first().particulars)

        // Selling from Amma's account takes her lot (bought at 300), not the older one at 250.
        assertEquals(
            -500.0,
            Holdings.realisedBySelling(
                id,
                v.rows,
                50.0,
                14500.0,
                LocalDate.of(2026, 3, 1),
                ammaDemat
            ),
            1e-9
        )
        assertEquals(
            2000.0,
            Holdings.realisedBySelling(id, v.rows, 50.0, 14500.0, LocalDate.of(2026, 3, 1), demat),
            1e-9
        )
        val sellId = repo.saveEntry(
            view(id),
            HoldingEntryKind.SELL,
            LocalDate.of(2026, 3, 1),
            50.0,
            290.0,
            14500.0,
            ammaDemat,
            "",
            now = now
        )
        assertEquals(49500.0, balance(ammaDemat), 1e-9)
        v = view(id)
        assertEquals(-500.0, v.position.realised, 1e-9)
        assertEquals(mapOf(demat to 100.0), v.position.unitsByAccount)
        assertEquals("Sold Stocks - NIFTYBEES", db.transactionDao().getById(sellId)!!.particulars)

        val divId = repo.saveEntry(
            view(id),
            HoldingEntryKind.DIVIDEND,
            LocalDate.of(2026, 4, 1),
            null,
            null,
            120.0,
            bank,
            "",
            exDate = LocalDate.of(2026, 3, 20),
            now = now
        )
        val div = db.transactionDao().getById(divId)!!
        assertEquals(TransactionKind.INCOME, div.kind)
        assertEquals("Dividend From Stocks - NIFTYBEES", div.particulars)
        assertEquals(LocalDate.of(2026, 3, 20), div.exDate)
        assertEquals("Interest & Dividends", db.categoryDao().getById(div.categoryId!!)!!.name)
        assertEquals(10120.0, balance(bank), 1e-9)
        assertEquals(120.0, view(id).position.income, 1e-9)

        val chargeId = repo.saveEntry(
            view(id),
            HoldingEntryKind.CHARGES,
            LocalDate.of(2026, 3, 1),
            null,
            null,
            15.34,
            ammaDemat,
            "",
            now = now
        )
        assertEquals(
            "Trading",
            db.categoryDao().getById(db.transactionDao().getById(chargeId)!!.categoryId!!)!!.name
        )
        assertEquals(49484.66, balance(ammaDemat), 1e-6)

        val bonusId =
            repo.saveEvent(view(id), HoldingEntryKind.BONUS, LocalDate.of(2026, 5, 1), 100.0, "1:1")
        v = view(id)
        assertEquals(200.0, v.position.units, 1e-9)
        assertEquals(125.0, v.position.averageCost!!, 1e-9)
        val bonus = db.holdingEventDao().getById(bonusId)!!
        assertEquals(100.0, bonus.units!!, 1e-9)
        assertEquals("1:1", bonus.ratio)
        assertEquals(listOf(HoldingEntryKind.BONUS), v.events.map { HoldingEntries.kindOf(it) })

        // Editing the sale moves it to the main demat: the older lot at 250 is consumed instead.
        repo.saveEntry(
            view(id),
            HoldingEntryKind.SELL,
            LocalDate.of(2026, 3, 1),
            50.0,
            290.0,
            14500.0,
            demat,
            "Sold Stocks - NIFTYBEES",
            existing = db.transactionDao().getById(sellId),
            now = now
        )
        assertEquals(89500.0, balance(demat), 1e-9)
        assertEquals(34984.66, balance(ammaDemat), 1e-6)
        assertEquals(2000.0, view(id).position.realised - (-15.34), 1e-6)

        repo.deleteEntry(divId, now)
        assertEquals(10000.0, balance(bank), 1e-9)
        repo.delete(id, now)
        assertNull(db.instrumentDao().getById(id))
        assertEquals(100000.0, balance(demat), 1e-9)
        assertEquals(50000.0, balance(ammaDemat), 1e-9)
    }

    @Test
    fun aFundIsBoughtFromTheBankAndCannotBeOversold() = runTest {
        val id = repo.add(
            "Parag Parikh Flexi Cap Fund",
            InstrumentType.MUTUAL_FUND,
            "122639",
            null,
            null,
            "INR",
            null,
            "Self",
            null,
            now
        )
        assertEquals("AMFI", db.instrumentDao().getSecurity(id)!!.exchange)
        repo.saveEntry(
            view(id),
            HoldingEntryKind.BUY,
            LocalDate.of(2026, 1, 5),
            55.123,
            90.7,
            5000.0,
            bank,
            "Mutual Funds SIP Installment - Parag Parikh Flexi Cap Fund",
            now = now
        )
        assertEquals(5000.0, balance(bank), 1e-9)
        val failed = runCatching {
            repo.saveEntry(
                view(id),
                HoldingEntryKind.SELL,
                LocalDate.of(2026, 2, 5),
                60.0,
                95.0,
                5700.0,
                bank,
                "",
                now = now
            )
        }
        assertEquals("Only 55.123 units are held", failed.exceptionOrNull()?.message)
        assertTrue(runCatching {
            repo.add(
                "  ",
                InstrumentType.MUTUAL_FUND,
                null,
                null,
                null,
                "INR",
                null,
                "Self",
                null,
                now
            )
        }.isFailure)
        assertTrue(runCatching {
            repo.saveEntry(
                view(id),
                HoldingEntryKind.BUY,
                LocalDate.of(2026, 2, 5),
                1.0,
                90.0,
                90.0,
                null,
                "",
                now = now
            )
        }.isFailure)
        assertEquals(5000.0, balance(bank), 1e-9)
    }
}
