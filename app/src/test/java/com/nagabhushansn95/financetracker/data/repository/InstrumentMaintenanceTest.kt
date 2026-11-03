package com.nagabhushansn95.financetracker.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class InstrumentMaintenanceTest {
    private lateinit var db: AppDatabase
    private val now = LocalDateTime.of(2026, 11, 4, 9, 0)
    private val today = LocalDate.of(2026, 11, 4)
    private var bank = 0L

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
    }

    @After
    fun tearDown() = db.close()

    private suspend fun instrument(name: String, type: InstrumentType = InstrumentType.STOCK) =
        db.instrumentDao().insert(
            Instrument(
                name = name,
                instrumentType = type,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )

    private suspend fun tx(
        kind: TransactionKind,
        instrument: Long,
        qty: Double?,
        daysAgo: Long,
        amount: Double = 100.0
    ) =
        db.transactionDao().insert(
            TransactionEntity(
                createdTime = now,
                modifiedTime = now,
                date = today.minusDays(daysAgo),
                kind = kind,
                fromAccountId = bank.takeIf { kind == TransactionKind.INVEST },
                toAccountId = bank.takeIf { kind == TransactionKind.REDEEM || kind == TransactionKind.INCOME },
                instrumentId = instrument,
                particulars = "x",
                quantity = qty, amount = amount,
            )
        )

    @Test
    fun retiresSoldOutStocksIdleForNinetyDays() = runTest {
        val soldOutOld = instrument("SOLD-OLD")
        tx(TransactionKind.INVEST, soldOutOld, 10.0, 400); tx(
        TransactionKind.REDEEM,
        soldOutOld,
        10.0,
        120
    )
        val soldOutRecent = instrument("SOLD-RECENT")
        tx(TransactionKind.INVEST, soldOutRecent, 10.0, 400); tx(
        TransactionKind.REDEEM,
        soldOutRecent,
        10.0,
        30
    )
        val stillHeld = instrument("HELD")
        tx(TransactionKind.INVEST, stillHeld, 10.0, 400); tx(
        TransactionKind.REDEEM,
        stillHeld,
        4.0,
        200
    )
        val bonusThenSold = instrument("BONUS")
        tx(TransactionKind.INVEST, bonusThenSold, 10.0, 400); tx(
        TransactionKind.INCOME,
        bonusThenSold,
        5.0,
        300,
        amount = 0.0
    ); tx(TransactionKind.REDEEM, bonusThenSold, 15.0, 200)
        val noUnits = instrument("SIP-NO-UNITS", InstrumentType.MUTUAL_FUND)
        tx(TransactionKind.INVEST, noUnits, null, 400); tx(
        TransactionKind.REDEEM,
        noUnits,
        null,
        200
    )
        val fd = instrument("FD", InstrumentType.FIXED_DEPOSIT)
        tx(TransactionKind.INVEST, fd, 1.0, 400); tx(TransactionKind.REDEEM, fd, 1.0, 200)
        val dividendOnly = instrument("DIV-ONLY")
        tx(TransactionKind.INCOME, dividendOnly, null, 200)
        // Sold out, but a dividend row still carries the 4.x quantity of 1 with cash: that is not a bonus unit.
        val soldWithDividend = instrument("SOLD-DIV")
        tx(TransactionKind.INVEST, soldWithDividend, 10.0, 400); tx(
        TransactionKind.INCOME,
        soldWithDividend,
        1.0,
        300,
        amount = 50.0
    ); tx(TransactionKind.REDEEM, soldWithDividend, 10.0, 150)

        val retired = InstrumentMaintenance(db).retireIdleHoldings(today, now = now)

        assertEquals(setOf("SOLD-OLD", "BONUS", "SOLD-DIV"), retired.map { it.name }.toSet())
        val active = db.instrumentDao().getAll().filter { it.active }.map { it.name }.toSet()
        assertEquals(setOf("SOLD-RECENT", "HELD", "SIP-NO-UNITS", "FD", "DIV-ONLY"), active)

        // Running again changes nothing.
        assertTrue(InstrumentMaintenance(db).retireIdleHoldings(today, now = now).isEmpty())
    }

    @Test
    fun buyingAgainReactivatesARetiredInstrument() = runTest {
        val stock = instrument("AGAIN")
        tx(TransactionKind.INVEST, stock, 10.0, 400); tx(TransactionKind.REDEEM, stock, 10.0, 120)
        InstrumentMaintenance(db).retireIdleHoldings(today, now = now)
        assertFalse(db.instrumentDao().getById(stock)!!.active)

        TransactionRepository(db).add(
            TransactionEntity(
                createdTime = now,
                modifiedTime = now,
                date = today,
                kind = TransactionKind.INVEST,
                fromAccountId = bank,
                instrumentId = stock,
                particulars = "buy again",
                quantity = 5.0,
                amount = 500.0
            )
        )
        assertTrue(db.instrumentDao().getById(stock)!!.active)
    }
}
