package com.nagabhushansn95.financetracker.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentValuation
import com.nagabhushansn95.financetracker.data.db.entity.Template
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.db.entity.ValuationSource
import com.nagabhushansn95.financetracker.data.investments.Holdings
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
class InstrumentMergeTest {
    private lateinit var db: AppDatabase
    private val now = LocalDateTime.of(2026, 11, 4, 9, 0)

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun stock(name: String, isin: String? = null) = db.instrumentDao().insert(
        Instrument(
            name = name,
            symbol = name,
            isin = isin,
            instrumentType = InstrumentType.STOCK,
            displayOrder = 1,
            createdTime = now,
            modifiedTime = now
        )
    )

    private fun t(
        date: String,
        kind: TransactionKind,
        instrument: Long,
        qty: Double,
        amount: Double,
        from: Long? = null,
        to: Long? = null
    ) =
        TransactionEntity(
            createdTime = now,
            modifiedTime = now,
            date = LocalDate.parse(date),
            kind = kind,
            fromAccountId = from,
            toAccountId = to,
            instrumentId = instrument,
            particulars = "$kind",
            quantity = qty,
            rate = amount / qty,
            amount = amount
        )

    @Test
    fun renamedStockMergesIntoTheNewSymbol() = runTest {
        val demat = db.accountDao().insert(
            Account(
                name = "Demat",
                accountType = AccountType.DEMAT,
                balance = 0.0,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        val zomato = stock("ZOMATO", isin = "INE758T01015")
        val eternal = stock("ETERNAL")
        db.transactionDao().insertAll(
            listOf(
                t("2025-01-10", TransactionKind.INVEST, zomato, 100.0, 20000.0, from = demat),
                t("2026-03-01", TransactionKind.REDEEM, eternal, 40.0, 10000.0, to = demat),
            )
        )
        db.transactionDao().insertTemplate(
            Template(
                particulars = "Buy",
                kind = TransactionKind.INVEST,
                instrumentId = zomato,
                fromAccountId = demat
            )
        )
        db.instrumentDao().insertValuation(
            InstrumentValuation(
                instrumentId = zomato,
                date = LocalDate.of(2025, 6, 1),
                unitPrice = 250.0,
                source = ValuationSource.MANUAL,
                fetchedAt = now
            )
        )

        EditDataRepository(db).mergeInstruments(zomato, eternal, now)

        assertNull(db.instrumentDao().getById(zomato))
        val merged = db.instrumentDao().getById(eternal)!!
        assertEquals("INE758T01015", merged.isin)
        assertTrue(merged.notes!!.contains("Merged from ZOMATO (ZOMATO)"))
        val rows = db.transactionDao().getForInstrument(eternal)
        assertEquals(2, rows.size)
        assertEquals(0, db.transactionDao().getForInstrument(zomato).size)
        assertEquals(eternal, db.transactionDao().getAllTemplates().single().instrumentId)
        assertTrue(db.instrumentDao().valuationHistory(zomato).isEmpty())
        // 100 bought at 200, 40 sold at 250: 60 left at cost 12,000 and 2,000 realised.
        val p = Holdings.position(eternal, rows, unitBased = true)
        assertEquals(60.0, p.units, 1e-9)
        assertEquals(12000.0, p.costHeld, 1e-6)
        assertEquals(2000.0, p.realised, 1e-6)
    }

    @Test
    fun mergeRefusesDifferentTypes() = runTest {
        val s = stock("A")
        val f = db.instrumentDao().insert(
            Instrument(
                name = "Fund",
                instrumentType = InstrumentType.MUTUAL_FUND,
                displayOrder = 2,
                createdTime = now,
                modifiedTime = now
            )
        )
        assertTrue(runCatching { EditDataRepository(db).mergeInstruments(s, f, now) }.isFailure)
        assertTrue(db.instrumentDao().getById(s) != null)
    }

    @Test
    fun bonusAndSplitAddUnitsAtNoCost() = runTest {
        val demat = db.accountDao().insert(
            Account(
                name = "Demat",
                accountType = AccountType.DEMAT,
                balance = 0.0,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        val s = stock("IRCTC")
        db.transactionDao()
            .insert(t("2025-01-10", TransactionKind.INVEST, s, 100.0, 50000.0, from = demat))
        val repo = EditDataRepository(db)
        repo.addBonusUnits(
            s,
            100.0,
            LocalDate.of(2025, 6, 1),
            "Bonus Shares 1:1 - IRCTC",
            now
        )     // 1:1 bonus
        repo.addBonusUnits(
            s,
            800.0,
            LocalDate.of(2026, 1, 1),
            "Stock split 1:5 - IRCTC",
            now
        )      // 200 -> 1000
        val rows = db.transactionDao().getForInstrument(s)
        val p = Holdings.position(s, rows, unitBased = true)
        assertEquals(1000.0, p.units, 1e-9)
        assertEquals(50000.0, p.costHeld, 1e-6)
        assertEquals(50.0, p.averageCost!!, 1e-9)
        assertEquals(0.0, p.income, 1e-9)
        assertEquals(
            0.0,
            db.accountDao().getById(demat)!!.balance,
            1e-6
        )   // bonus rows touch no account
        assertTrue(runCatching {
            repo.addBonusUnits(
                s,
                0.0,
                LocalDate.of(2026, 1, 1),
                "x",
                now
            )
        }.isFailure)
    }
}
