package com.nagabhushansn95.financetracker.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.FdStatus
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.investments.FixedDeposits
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummaryBuilder
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class FixedDepositRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: FixedDepositRepository
    private val now = LocalDateTime.of(2026, 11, 4, 9, 0)
    private var bank = 0L

    @Before
    fun setUp() = runTest {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
        repo = FixedDepositRepository(db)
        bank = db.accountDao().insert(
            Account(
                name = "SBI",
                accountType = AccountType.BANK,
                balance = 500000.0,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
    }

    @After
    fun tearDown() = db.close()

    private fun terms(months: Int = 12, rate: Double = 6.25) =
        FixedDeposit(
            instrumentId = 0,
            bankName = "SBI",
            periodMonths = months,
            rate = rate,
            tdsRate = 0.1
        )

    @Test
    fun createMatureAndEstimate() = runTest {
        val id = repo.create(
            NewFixedDeposit("FD #1", 100000.0, LocalDate.of(2026, 2, 3), bank, terms()),
            now
        )
        assertEquals(400000.0, db.accountDao().getById(bank)!!.balance, 0.001)
        val stored = db.instrumentDao().getFixedDeposit(id)!!
        assertEquals(LocalDate.of(2027, 2, 3), stored.maturityDate)
        assertEquals(3, stored.compoundingMonths)
        assertEquals(FdStatus.ACTIVE, stored.status)

        val views = FixedDeposits.build(
            db.instrumentDao().getAll(),
            db.instrumentDao().getAllFixedDeposits(),
            db.transactionDao().getAllWithInstrument(),
            emptyList(),
            LocalDate.of(2026, 7, 6)
        )
        val v = views.single()
        assertEquals(100000.0, v.principal, 0.001)
        assertEquals(106398.0, v.maturityAmount!!, 0.5)
        assertEquals(639.8, v.tdsAtMaturity, 0.1)
        assertTrue(v.estimated > 102000 && v.estimated < 103000)
        assertEquals(212L, v.daysLeft)
        assertTrue(v.isOpen)
        assertEquals("Fixed deposit opened · SBI 1 year @ 6.25%", v.rows.single().particulars)

        // The Investment Summary values the deposit by the same formula.
        val summary = InvestmentSummaryBuilder.build(
            db.instrumentDao().getAll(),
            db.instrumentDao().getAllCategories(),
            db.transactionDao().getAllWithInstrument(),
            emptyList(),
            db.accountDao().getAll(),
            db.transactionDao().getAllWithInstrument(),
            asOf = LocalDate.of(2026, 7, 6),
            fixedDeposits = db.instrumentDao().getAllFixedDeposits()
        )
        assertEquals(v.estimated, summary.rows.single().currentValue, 0.01)

        repo.close(
            id,
            LocalDate.of(2027, 2, 3),
            105758.0,
            bank,
            FdStatus.CLOSED_AT_MATURITY,
            tdsReported = 640.0,
            now = now
        )
        assertEquals(505758.0, db.accountDao().getById(bank)!!.balance, 0.001)
        assertFalse(db.instrumentDao().getById(id)!!.active)
        val closed = FixedDeposits.build(
            db.instrumentDao().getAll(),
            db.instrumentDao().getAllFixedDeposits(),
            db.transactionDao().getAllWithInstrument(),
            emptyList(),
            LocalDate.of(2027, 3, 1)
        ).single()
        assertEquals(FdStatus.CLOSED_AT_MATURITY, closed.status)
        assertEquals(0.0, closed.estimated, 0.001)
        assertEquals(5758.0, closed.position.realised, 0.001)
        assertEquals(640.0, closed.terms!!.tdsReported!!, 0.001)
    }

    @Test
    fun renewLinksTheChainAndPaysOutTheRemainder() = runTest {
        val old = repo.create(
            NewFixedDeposit("FD #1", 100000.0, LocalDate.of(2025, 2, 3), bank, terms()),
            now
        )
        val new = repo.renew(
            oldId = old,
            date = LocalDate.of(2026, 2, 3),
            maturedAmount = 106398.0,
            renewedAmount = 100000.0,
            remainderToAccountId = bank,
            newName = FixedDeposits.nextName(db.instrumentDao().getAll()),
            newTerms = FixedDeposit(instrumentId = 0, periodMonths = 18, rate = 6.6),
            now = now,
        )
        val oldTerms = db.instrumentDao().getFixedDeposit(old)!!
        val newTerms = db.instrumentDao().getFixedDeposit(new)!!
        assertEquals(FdStatus.RENEWED, oldTerms.status)
        assertEquals(new, oldTerms.nextFdId)
        assertEquals(old, newTerms.previousFdId)
        assertEquals("SBI", newTerms.bankName)
        assertEquals(0.1, newTerms.tdsRate!!, 0.001)
        assertEquals(LocalDate.of(2027, 8, 3), newTerms.maturityDate)
        assertEquals("FD #2", db.instrumentDao().getById(new)!!.name)
        assertFalse(db.instrumentDao().getById(old)!!.active)
        // Bank: -100000 at creation, +6398 remainder on renewal; the renewed principal never touched it.
        assertEquals(406398.0, db.accountDao().getById(bank)!!.balance, 0.001)
        val rows = db.transactionDao().getAllWithInstrument().sortedBy { it.id }
        assertEquals(
            listOf(
                TransactionKind.INVEST,
                TransactionKind.REDEEM,
                TransactionKind.REDEEM,
                TransactionKind.INVEST
            ), rows.map { it.kind })
        assertNull(rows[1].toAccountId)
        assertEquals(bank, rows[2].toAccountId)
        assertNull(rows[3].fromAccountId)
        val views = FixedDeposits.build(
            db.instrumentDao().getAll(),
            db.instrumentDao().getAllFixedDeposits(),
            rows,
            emptyList(),
            LocalDate.of(2026, 3, 1)
        )
        val oldView = views.first { it.instrument.id == old }
        assertEquals(6398.0, oldView.position.realised, 0.001)
        assertEquals(0.0, oldView.estimated, 0.001)
        val newView = views.first { it.instrument.id == new }
        assertEquals(100000.0, newView.principal, 0.001)
        assertEquals(LocalDate.of(2026, 2, 3), newView.depositDate)
    }

    @Test
    fun cannotRenewMoreThanMatured() = runTest {
        val old = repo.create(
            NewFixedDeposit("FD #1", 100000.0, LocalDate.of(2025, 2, 3), bank, terms()),
            now
        )
        val failed = runCatching {
            repo.renew(
                old,
                LocalDate.of(2026, 2, 3),
                106398.0,
                200000.0,
                bank,
                "FD #2",
                FixedDeposit(instrumentId = 0, periodMonths = 12, rate = 6.0),
                now
            )
        }
        assertTrue(failed.isFailure)
        assertEquals(1, db.transactionDao().count())
    }
}
