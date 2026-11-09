package com.nagabhushansn95.financetracker.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.FdEvent
import com.nagabhushansn95.financetracker.data.db.entity.FdEventKind
import com.nagabhushansn95.financetracker.data.db.entity.FdStatus
import com.nagabhushansn95.financetracker.data.db.entity.FdTemplate
import com.nagabhushansn95.financetracker.data.db.entity.FdType
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.investments.FdLedgerEntry
import com.nagabhushansn95.financetracker.data.investments.FdTax
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
        assertEquals(
            0.0,
            v.tdsAtMaturity,
            0.001
        ) // 6,398 of interest in the year: under the TDS threshold
        assertTrue(v.estimated > 102000 && v.estimated < 103000)
        assertEquals(212L, v.daysLeft)
        assertTrue(v.isOpen)
        assertEquals("Created A Fixed Deposit For 12 Months", v.rows.single().particulars)

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
        // The chain reads the same from either end, oldest first.
        val byId = views.associateBy { it.instrument.id }
        assertEquals(listOf(old, new), FixedDeposits.chain(old, byId).map { it.instrument.id })
        assertEquals(listOf(old, new), FixedDeposits.chain(new, byId).map { it.instrument.id })
    }

    @Test
    fun chainFollowsEveryRenewalAndSurvivesADanglingLink() = runTest {
        val first = repo.create(
            NewFixedDeposit("FD #1", 100000.0, LocalDate.of(2024, 2, 3), bank, terms()),
            now
        )
        val second = repo.renew(
            first,
            LocalDate.of(2025, 2, 3),
            106398.0,
            106398.0,
            null,
            "FD #2",
            terms(),
            now = now
        )
        val third = repo.renew(
            second,
            LocalDate.of(2026, 2, 3),
            113000.0,
            113000.0,
            null,
            "FD #3",
            terms(),
            now = now
        )
        val views = FixedDeposits.build(
            db.instrumentDao().getAll(), db.instrumentDao().getAllFixedDeposits(),
            db.transactionDao().getAllWithInstrument(), emptyList(), LocalDate.of(2026, 3, 1)
        )
        val byId = views.associateBy { it.instrument.id }
        assertEquals(
            listOf(first, second, third),
            FixedDeposits.chain(second, byId).map { it.instrument.id })
        // Without the first deposit the chain starts at the second.
        val without = byId - first
        assertEquals(
            listOf(second, third),
            FixedDeposits.chain(third, without).map { it.instrument.id })
        assertTrue(FixedDeposits.chain(first, without).isEmpty())
    }

    @Test
    fun chainLedgerMergesRenewalsAndDerivesInterestAndTax() = runTest {
        val first = repo.create(
            NewFixedDeposit("FD #1", 100000.0, LocalDate.of(2024, 2, 3), bank, terms()),
            now
        )
        // 106398 matured, 640 TDS reported, 100000 renewed and the rest paid out.
        val second = repo.renew(
            first,
            LocalDate.of(2025, 2, 3),
            106398.0,
            100000.0,
            bank,
            "FD #2",
            terms(),
            tdsReported = 640.0,
            now = now
        )
        val views = FixedDeposits.build(
            db.instrumentDao().getAll(), db.instrumentDao().getAllFixedDeposits(),
            db.transactionDao().getAllWithInstrument(), emptyList(), LocalDate.of(2025, 3, 1)
        )
        val chain = FixedDeposits.chain(first, views.associateBy { it.instrument.id })
        val ledger = FixedDeposits.ledger(chain)
        assertEquals(
            listOf(
                "open FD #1",
                "interest FD #1",
                "tax FD #1",
                "renewal FD #1→FD #2",
                "payout FD #1"
            ),
            ledger.map {
                when (it) {
                    is FdLedgerEntry.Row -> (if (it.row.kind == TransactionKind.INVEST) "open " else "payout ") + it.deposit.instrument.name
                    is FdLedgerEntry.Renewal -> "renewal ${it.deposit.instrument.name}→${it.into.instrument.name}"
                    is FdLedgerEntry.Derived -> (if (it.tax) "tax " else "interest ") + it.deposit.instrument.name
                    is FdLedgerEntry.Event -> (if (it.tax) "tds event " else "interest event ") + it.deposit.instrument.name
                    is FdLedgerEntry.SweepIn -> "sweep-in " + it.deposit.instrument.name
                }
            }
        )
        val interest = ledger.filterIsInstance<FdLedgerEntry.Derived>().first { !it.tax }
        assertEquals(6398.0 + 640.0, interest.amount, 0.001)
        assertEquals(
            640.0,
            ledger.filterIsInstance<FdLedgerEntry.Derived>().first { it.tax }.amount,
            0.001
        )
        assertEquals(
            100000.0,
            ledger.filterIsInstance<FdLedgerEntry.Renewal>().single().amount,
            0.001
        )
        assertEquals(640.0, db.instrumentDao().getFixedDeposit(first)!!.tdsReported!!, 0.001)
        // The open deposit alone: just its opening row.
        assertEquals(
            1,
            FixedDeposits.ledger(listOf(views.first { it.instrument.id == second })).size
        )
    }

    @Test
    fun recordedEventsAppearInTheLedgerAndReduceTheDerivedInterest() = runTest {
        val id = repo.create(
            NewFixedDeposit("FD #1", 100000.0, LocalDate.of(2024, 2, 3), bank, terms()),
            now
        )
        repo.saveEvent(
            FdEvent(
                instrumentId = id,
                date = LocalDate.of(2024, 3, 31),
                kind = FdEventKind.INTEREST,
                amount = 1000.0
            )
        )
        val tds = repo.saveEvent(
            FdEvent(
                instrumentId = id,
                date = LocalDate.of(2024, 3, 31),
                kind = FdEventKind.TDS,
                amount = 100.0,
                notes = "FY 23-24"
            )
        )
        repo.close(
            id,
            LocalDate.of(2025, 2, 3),
            105298.0,
            bank,
            FdStatus.CLOSED_AT_MATURITY,
            tdsReported = 1000.0,
            now = now
        )
        val view = FixedDeposits.build(
            db.instrumentDao().getAll(),
            db.instrumentDao().getAllFixedDeposits(),
            db.transactionDao().getAllWithInstrument(),
            emptyList(),
            LocalDate.of(2025, 3, 1),
            db.instrumentDao().getAllFdEvents()
        ).single()
        assertEquals(2, view.events.size)
        val ledger = FixedDeposits.ledger(listOf(view))
        assertEquals(
            listOf("open", "interest event", "tds event", "interest", "tax", "payout"),
            ledger.map {
                when (it) {
                    is FdLedgerEntry.Row -> if (it.row.kind == TransactionKind.INVEST) "open" else "payout"
                    is FdLedgerEntry.Event -> if (it.tax) "tds event" else "interest event"
                    is FdLedgerEntry.Derived -> if (it.tax) "tax" else "interest"
                    is FdLedgerEntry.Renewal -> "renewal"
                    is FdLedgerEntry.SweepIn -> "sweep-in"
                }
            }
        )
        // Gross interest = 5,298 realised + 1,000 TDS at closing + 100 TDS recorded; 1,000 of it was already recorded.
        assertEquals(
            5298.0 + 1000.0 + 100.0 - 1000.0,
            ledger.filterIsInstance<FdLedgerEntry.Derived>().first { !it.tax }.amount,
            0.001
        )
        // Account rows are untouched by events.
        assertEquals(2, db.transactionDao().count())
        repo.deleteEvent(tds)
        assertEquals(1, db.instrumentDao().getAllFdEvents().size)
    }

    @Test
    fun tdsIsEstimatedFromTheHoldersInterestWithTheBankPerYear() = runTest {
        // One deposit of 10 lakh at 7% from 3 Feb 2026: 10,739 of interest in FY 2025-26 (under 50,000), the rest in
        // FY 2026-27 (over), so TDS applies to the second year's interest only.
        val big = repo.create(
            NewFixedDeposit(
                "FD #1",
                1_000_000.0,
                LocalDate.of(2026, 2, 3),
                bank,
                terms(rate = 7.0).copy(holderName = "Self")
            ),
            now
        )
        var views = FixedDeposits.build(
            db.instrumentDao().getAll(), db.instrumentDao().getAllFixedDeposits(),
            db.transactionDao().getAllWithInstrument(), emptyList(), LocalDate.of(2026, 3, 1)
        )
        val v = views.single()
        val fy1 = FdTax.interestInYear(
            1_000_000.0,
            7.0,
            3,
            v.depositDate!!,
            v.maturityDate!!,
            LocalDate.of(2025, 4, 1)
        )
        val fy2 = FdTax.interestInYear(
            1_000_000.0,
            7.0,
            3,
            v.depositDate!!,
            v.maturityDate!!,
            LocalDate.of(2026, 4, 1)
        )
        assertEquals(10739.7, fy1, 1.0)
        assertEquals(v.maturityAmount!! - 1_000_000.0, fy1 + fy2, 0.01)
        assertTrue(fy1 < 50_000 && fy2 > 50_000)
        assertEquals(fy2 * 0.10, v.tdsAtMaturity, 0.01)

        // A second deposit of the same holder with the same bank pushes the first year over the threshold too.
        repo.create(
            NewFixedDeposit(
                "FD #2",
                5_000_000.0,
                LocalDate.of(2026, 2, 3),
                bank,
                terms(rate = 7.0).copy(holderName = "self ")
            ),
            now
        )
        views = FixedDeposits.build(
            db.instrumentDao().getAll(), db.instrumentDao().getAllFixedDeposits(),
            db.transactionDao().getAllWithInstrument(), emptyList(), LocalDate.of(2026, 3, 1)
        )
        assertEquals(
            (fy1 + fy2) * 0.10,
            views.first { it.instrument.id == big }.tdsAtMaturity,
            0.01
        )
        // With a different bank the deposits are counted apart.
        val other = views.first { it.instrument.id != big }
        db.instrumentDao().upsertFixedDeposit(other.terms!!.copy(bankName = "HDFC"))
        views = FixedDeposits.build(
            db.instrumentDao().getAll(), db.instrumentDao().getAllFixedDeposits(),
            db.transactionDao().getAllWithInstrument(), emptyList(), LocalDate.of(2026, 3, 1)
        )
        assertEquals(fy2 * 0.10, views.first { it.instrument.id == big }.tdsAtMaturity, 0.01)
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
                now = now
            )
        }
        assertTrue(failed.isFailure)
        assertEquals(1, db.transactionDao().count())
    }

    @Test
    fun deletingADepositRemovesItsRowsAndPutsTheBalanceBack() = runTest {
        val id = repo.create(
            NewFixedDeposit("FD #1", 100000.0, LocalDate.of(2026, 2, 3), bank, terms()),
            now
        )
        repo.saveEvent(
            FdEvent(
                instrumentId = id,
                date = LocalDate.of(2026, 3, 31),
                kind = FdEventKind.INTEREST,
                amount = 900.0
            )
        )
        assertEquals(400000.0, db.accountDao().getById(bank)!!.balance, 0.001)

        repo.delete(id, now)

        assertNull(db.instrumentDao().getById(id))
        assertNull(db.instrumentDao().getFixedDeposit(id))
        assertEquals(0, db.transactionDao().count())
        assertTrue(db.instrumentDao().getAllFdEvents().isEmpty())
        assertEquals(500000.0, db.accountDao().getById(bank)!!.balance, 0.001)
    }

    @Test
    fun deletingARenewedDepositReopensTheOneItCameFrom() = runTest {
        val old = repo.create(
            NewFixedDeposit("FD #1", 100000.0, LocalDate.of(2025, 2, 3), bank, terms()),
            now
        )
        val new = repo.renew(
            old,
            LocalDate.of(2026, 2, 3),
            106398.0,
            100000.0,
            bank,
            "FD #2",
            terms(),
            tdsReported = 500.0,
            now = now
        )
        assertEquals(406398.0, db.accountDao().getById(bank)!!.balance, 0.001)

        // The old deposit cannot go while the new one points back at it.
        assertTrue(runCatching { repo.delete(old, now) }.isFailure)
        assertEquals(4, db.transactionDao().count())

        repo.delete(new, now)

        assertNull(db.instrumentDao().getById(new))
        val oldTerms = db.instrumentDao().getFixedDeposit(old)!!
        assertEquals(FdStatus.ACTIVE, oldTerms.status)
        assertNull(oldTerms.nextFdId)
        assertNull(oldTerms.tdsReported)
        assertTrue(db.instrumentDao().getById(old)!!.active)
        val rows = db.transactionDao().getForInstrument(old)
        assertEquals(listOf(TransactionKind.INVEST), rows.map { it.kind })
        assertEquals(1, db.transactionDao().count())
        assertEquals(400000.0, db.accountDao().getById(bank)!!.balance, 0.001)
    }

    @Test
    fun templateNamesAreUniqueIgnoringCase() = runTest {
        val id = repo.saveTemplate(FdTemplate(name = "SBI 1 year", rate = 7.1))
        assertTrue(runCatching { repo.saveTemplate(FdTemplate(name = " sbi 1 YEAR ")) }.isFailure)
        val other = repo.saveTemplate(FdTemplate(name = "HDFC 1 year"))
        assertTrue(runCatching {
            repo.saveTemplate(
                FdTemplate(
                    id = other,
                    name = "SBI 1 year"
                )
            )
        }.isFailure)
        // Renaming a template to its own name (with other spacing) is fine.
        repo.saveTemplate(FdTemplate(id = id, name = "SBI 1 year ", rate = 7.15))
        val names = db.instrumentDao().getAllFdTemplates().map { it.name }
        assertEquals(listOf("SBI 1 year", "HDFC 1 year"), names)
        assertEquals(7.15, db.instrumentDao().getAllFdTemplates().first { it.id == id }.rate!!, 0.0)
    }

    @Test
    fun aSweepInDepositShowsItsLinkingDayInTheLedger() = runTest {
        val id = repo.create(
            NewFixedDeposit(
                "FD #1",
                100000.0,
                LocalDate.of(2024, 11, 22),
                bank,
                terms(months = 55, rate = 7.4)
            ), now
        )
        val instrument = db.instrumentDao().getById(id)!!
        repo.saveTerms(
            instrument,
            terms(months = 55, rate = 7.4).copy(
                depositType = FdType.SWEEP_IN,
                sweepInDate = LocalDate.of(2024, 12, 1)
            ),
            LocalDate.of(2024, 11, 22),
            now
        )
        // The bank credits the interest of each partial liquidation on its own, and charges TDS on it.
        db.transactionDao().insert(
            com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity(
                createdTime = now,
                modifiedTime = now,
                date = LocalDate.of(2024, 12, 1),
                kind = TransactionKind.INCOME,
                toAccountId = bank,
                instrumentId = id,
                amount = 9.0,
                particulars = "Sweep In Fixed Deposit Interest Credited",
            )
        )
        db.transactionDao().insert(
            com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity(
                createdTime = now,
                modifiedTime = now,
                date = LocalDate.of(2024, 12, 1),
                kind = TransactionKind.EXPENSE,
                fromAccountId = bank,
                instrumentId = id,
                amount = 0.9,
                particulars = "TDS On Interest On Sweep In Fixed Deposit",
            )
        )
        repo.close(
            id,
            LocalDate.of(2025, 8, 4),
            100000.0,
            bank,
            FdStatus.CLOSED_PREMATURELY,
            now = now
        )
        val view = FixedDeposits.build(
            db.instrumentDao().getAll(),
            db.instrumentDao().getAllFixedDeposits(),
            db.transactionDao().getAllWithInstrument(),
            emptyList(),
            LocalDate.of(2026, 11, 5)
        ).first()
        assertEquals(FdType.SWEEP_IN, view.terms!!.depositType)
        assertEquals(9.0 - 0.9, view.interestEarned, 0.001)
        val ledger = FixedDeposits.ledger(listOf(view)).map {
            when (it) {
                is FdLedgerEntry.Row -> when (it.row.kind) {
                    TransactionKind.INVEST -> "open"
                    TransactionKind.INCOME -> "interest row"
                    TransactionKind.EXPENSE -> "tds row"
                    else -> "payout"
                }

                is FdLedgerEntry.SweepIn -> "sweep-in ${it.date}"
                is FdLedgerEntry.Derived -> "derived interest"
                else -> "other"
            }
        }
        // No derived interest line: the interest is visible as its own rows.
        assertEquals(
            listOf("open", "sweep-in 2024-12-01", "interest row", "tds row", "payout"),
            ledger
        )
    }
}
