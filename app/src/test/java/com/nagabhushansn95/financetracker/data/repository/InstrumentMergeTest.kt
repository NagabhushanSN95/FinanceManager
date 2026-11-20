package com.nagabhushansn95.financetracker.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.TestDb
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.PriceSource
import com.nagabhushansn95.financetracker.data.db.entity.Security
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.db.insertListed
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
        db = TestDb.inMemory(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun stock(name: String, isin: String? = null) = db.insertListed(
        Instrument(
            name = name,
            instrumentType = InstrumentType.STOCK,
            createdTime = now,
            modifiedTime = now
        ), Security(symbol = name, isin = isin)
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
        db.instrumentDao()
            .setPrice(zomato, 250.0, "2025-06-01", PriceSource.MANUAL, "2025-06-01T09:00:00.000")

        HoldingsRepository(db).mergeInto(zomato, eternal, now)

        assertNull(db.instrumentDao().getById(zomato))
        val merged = db.instrumentDao().getById(eternal)!!
        assertEquals("INE758T01015", db.instrumentDao().getSecurity(eternal)!!.isin)
        assertTrue(merged.notes!!.contains("Merged from ZOMATO (ZOMATO)"))
        val rows = db.transactionDao().getForInstrument(eternal)
        assertEquals(2, rows.size)
        assertEquals(0, db.transactionDao().getForInstrument(zomato).size)
        assertTrue(db.instrumentDao().getAllSecurities().none { it.instrumentId == zomato })
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
                createdTime = now,
                modifiedTime = now
            )
        )
        assertTrue(runCatching { HoldingsRepository(db).mergeInto(s, f, now) }.isFailure)
        assertTrue(db.instrumentDao().getById(s) != null)
    }
}
