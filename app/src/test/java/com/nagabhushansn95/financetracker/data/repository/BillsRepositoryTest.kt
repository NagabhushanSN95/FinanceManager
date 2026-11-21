package com.nagabhushansn95.financetracker.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nagabhushansn95.financetracker.data.bills.BillPeriod
import com.nagabhushansn95.financetracker.data.bills.Bills
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.TestDb
import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Bill
import com.nagabhushansn95.financetracker.data.db.entity.BillSchedule
import com.nagabhushansn95.financetracker.data.db.entity.ValidityUnit
import com.nagabhushansn95.financetracker.data.db.entity.Category
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class BillsRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: BillsRepository
    private val now = LocalDateTime.of(2026, 11, 20, 9, 0)
    private val today = LocalDate.of(2026, 11, 20)
    private var bank = 0L
    private var home = 0L

    @Before
    fun setUp() = runTest {
        db = TestDb.inMemory(ApplicationProvider.getApplicationContext<Context>())
        repository = BillsRepository(db)
        bank = db.accountDao().insert(
            Account(
                name = "Bank",
                accountType = AccountType.BANK,
                balance = 50000.0,
                displayOrder = 1,
                createdTime = now,
                modifiedTime = now
            )
        )
        home = db.categoryDao().insert(Category(name = "Home", displayOrder = 1))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun rent(): Bill {
        val id = repository.add(
            Bill(
                name = "Home Rent",
                fromAccountId = bank,
                categoryId = home,
                amount = 11500.0,
                firstDue = LocalDate.of(2026, 11, 10),
                coversPrevious = true,
                createdTime = now,
                modifiedTime = now,
            ), now
        )
        return db.billDao().getById(id)!!
    }

    private fun row(particulars: String, amount: Double, date: LocalDate = today) =
        TransactionEntity(
            createdTime = now, modifiedTime = now, date = date, kind = TransactionKind.EXPENSE,
            fromAccountId = bank, categoryId = home, particulars = particulars, amount = amount,
        )

    @Test
    fun recordingAPaymentWritesTheRowAndSettlesThePeriod() = runTest {
        val bill = rent()
        val period = Bills.calendarPeriod(bill, 0)
        val id = repository.recordPayment(
            bill,
            period,
            row(Bills.particularsFor(bill, period), 11500.0),
            now = now
        )
        assertEquals(50000.0 - 11500.0, db.accountDao().getById(bank)!!.balance, 0.0)
        val payment = db.billDao().getPaymentOf(id)!!
        assertEquals(LocalDate.of(2026, 10, 1), payment.periodStart)
        assertEquals("Home Rent - October 2026", db.transactionDao().getById(id)!!.particulars)
        val view = Bills.build(
            listOf(bill),
            db.billDao().getPayments(bill.id),
            mapOf(id to db.transactionDao().getById(id)!!),
            emptyMap(),
            today
        ).single()
        assertEquals("November 2026", view.next?.label)
        assertEquals(LocalDate.of(2026, 12, 10), view.next?.due)
    }

    @Test
    fun deletingTheRowUnsettlesThePeriodAndPutsTheBalanceBack() = runTest {
        val bill = rent()
        val period = Bills.calendarPeriod(bill, 0)
        val id = repository.recordPayment(
            bill,
            period,
            row("Home Rent - October 2026", 11500.0),
            now = now
        )
        TransactionRepository(db).delete(id, now)
        assertNull(db.billDao().getPaymentOf(id))
        assertTrue(db.billDao().getPayments(bill.id).isEmpty())
        assertEquals(50000.0, db.accountDao().getById(bank)!!.balance, 0.0)
    }

    @Test
    fun skipLinkAndUnlink() = runTest {
        val bill = rent()
        val october = Bills.calendarPeriod(bill, 0)
        repository.skip(bill.id, october, "Stayed with family")
        val open = Bills.unsettled(bill, db.billDao().getPayments(bill.id), today)
        assertEquals("November 2026", open.first().label)

        // A row entered through the ledger pays November.
        val existing =
            TransactionRepository(db).add(row("Home Rent - November 2026", 11500.0), now = now)
        repository.link(bill.id, open.first(), existing)
        assertEquals(2, db.billDao().getPayments(bill.id).size)
        try {
            repository.link(bill.id, open.first(), existing); fail("linked twice")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("already pays"))
        }
        // Unlinking keeps the row.
        repository.unlink(db.billDao().getPaymentOf(existing)!!.id)
        assertNull(db.billDao().getPaymentOf(existing))
        assertEquals(
            "Home Rent - November 2026",
            db.transactionDao().getById(existing)!!.particulars
        )
    }

    @Test
    fun deletingAPaymentRemovesItsRowAndASkipJustGoes() = runTest {
        val bill = rent()
        val october = Bills.calendarPeriod(bill, 0)
        val id = repository.recordPayment(
            bill,
            october,
            row("Home Rent - October 2026", 11500.0),
            now = now
        )
        repository.deletePayment(db.billDao().getPaymentOf(id)!!.id, now)
        assertNull(db.transactionDao().getById(id))
        assertEquals(50000.0, db.accountDao().getById(bank)!!.balance, 0.0)
        val skip = repository.skip(bill.id, october)
        repository.deletePayment(skip, now)
        assertTrue(db.billDao().getPayments(bill.id).isEmpty())
    }

    @Test
    fun oneRowMayPayTwoBillsAndAPaymentMayMoveToAnotherPeriod() = runTest {
        val rent = rent()
        val maintenanceId = repository.add(
            Bill(
                name = "Maintenance",
                fromAccountId = bank,
                categoryId = home,
                amount = 1500.0,
                firstDue = LocalDate.of(2026, 11, 10),
                createdTime = now,
                modifiedTime = now
            ), now
        )
        val maintenance = db.billDao().getById(maintenanceId)!!
        val row = TransactionRepository(db).add(row("Rent and maintenance", 13000.0), now = now)
        repository.linkAll(rent, listOf(Bills.calendarPeriod(rent, 0) to row), now)
        repository.linkAll(maintenance, listOf(Bills.calendarPeriod(maintenance, 0) to row), now)
        assertEquals(1, db.billDao().getPayments(rent.id).size)
        assertEquals(1, db.billDao().getPayments(maintenance.id).size)
        try {
            repository.linkAll(
                rent,
                listOf(Bills.calendarPeriod(rent, 1) to row),
                now
            ); fail("linked twice to one bill")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("this bill"))
        }
        // Filed under the wrong month: move it.
        val payment = db.billDao().getPayments(rent.id).single()
        repository.changePeriod(payment.id, Bills.calendarPeriod(rent, 1))
        assertEquals(
            Bills.calendarPeriod(rent, 1).start,
            db.billDao().getPayment(payment.id)!!.periodStart
        )
    }

    @Test
    fun aValidityPaymentMovesByHandAndTheLaterOnesChainAgain() = runTest {
        val id = repository.add(
            Bill(
                name = "Airtel Wi-Fi",
                fromAccountId = bank,
                categoryId = home,
                schedule = BillSchedule.DURATION,
                validity = 6,
                validityUnit = ValidityUnit.MONTHS,
                firstDue = LocalDate.of(2024, 3, 13),
                createdTime = now,
                modifiedTime = now,
            ), now
        )
        val bill = db.billDao().getById(id)!!
        val ledger = TransactionRepository(db)
        val rows = listOf(
            LocalDate.of(2024, 3, 13),
            LocalDate.of(2024, 6, 30),
            LocalDate.of(2025, 1, 16)
        ).map {
            db.transactionDao()
                .getById(ledger.add(row("Airtel Home Internet", 3000.0, it), now = now))!!
        }
        repository.linkAll(
            bill,
            Bills.assignPeriods(bill, rows, emptyList()).map { it.second to it.first.id },
            now
        )
        val chained = db.billDao().getPayments(id).sortedBy { it.periodStart }
        // Linked with 6 months each: the second queues behind the first, the third behind the second.
        assertEquals(
            listOf(
                LocalDate.of(2024, 3, 13),
                LocalDate.of(2024, 9, 13),
                LocalDate.of(2025, 3, 13)
            ), chained.map { it.periodStart })
        // The first really covered 3 months; the later ones then run from their own dates.
        repository.changePeriod(
            chained[0].id,
            BillPeriod(
                LocalDate.of(2024, 3, 13),
                LocalDate.of(2024, 6, 12),
                LocalDate.of(2024, 3, 13)
            )
        )
        assertEquals(2, repository.rechainAfter(chained[0].id))
        val after = db.billDao().getPayments(id).sortedBy { it.periodStart }
        assertEquals(
            listOf(
                LocalDate.of(2024, 3, 13),
                LocalDate.of(2024, 6, 30),
                LocalDate.of(2025, 1, 16)
            ), after.map { it.periodStart })
        assertEquals(
            listOf(
                LocalDate.of(2024, 6, 12),
                LocalDate.of(2024, 12, 29),
                LocalDate.of(2025, 7, 15)
            ), after.map { it.periodEnd })
        assertEquals(0, repository.rechainAfter(after.last().id))
        try {
            repository.rechainAfter(
                db.billDao().getPayments(rent().id).firstOrNull()?.id ?: -1
            ); fail("re-chained a calendar bill")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("validity") || e.message!!.contains("no longer"))
        }
    }

    @Test
    fun severalPeriodsSkipInOneWrite() = runTest {
        val bill = rent()
        val open = Bills.unsettled(bill, emptyList(), today)
        repository.skipAll(bill.id, open.take(2), "Away")
        val payments = db.billDao().getPayments(bill.id)
        assertEquals(2, payments.size)
        assertTrue(payments.all { it.skipped && it.note == "Away" })
        // October and November are settled; the bill moves on to the one after.
        assertEquals(listOf("October 2026", "November 2026"), open.map { it.label })
        assertEquals(
            listOf("December 2026"),
            Bills.unsettled(bill, payments, today).map { it.label })
    }

    @Test
    fun aBillWithHistoryIsEndedNotDeleted() = runTest {
        val bill = rent()
        repository.skip(bill.id, Bills.calendarPeriod(bill, 0))
        try {
            repository.delete(bill.id); fail("deleted a paid bill")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("End it"))
        }
        repository.end(bill.id, today, now)
        assertEquals(today, db.billDao().getById(bill.id)!!.endedOn)
        repository.reopen(bill.id, now)
        assertNull(db.billDao().getById(bill.id)!!.endedOn)
        // Deleting the bill takes its payment rows with it (the ledger rows stay).
        val fresh = repository.add(
            Bill(
                name = "Netflix",
                firstDue = today,
                createdTime = now,
                modifiedTime = now
            ), now
        )
        repository.delete(fresh)
        assertNull(db.billDao().getById(fresh))
    }

    @Test
    fun creditScoresByAgency() = runTest {
        val cibil = repository.addAgency("CIBIL")
        repository.addScore(cibil, LocalDate.of(2026, 10, 1), 780)
        repository.addScore(cibil, LocalDate.of(2026, 11, 1), 792, "After the card closed")
        assertEquals(listOf(780, 792), db.billDao().getScores().map { it.score })
        try {
            repository.addScore(cibil, today, 0); fail("zero score")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("score"))
        }
        repository.deleteAgency(cibil)
        assertTrue(db.billDao().getScores().isEmpty())
    }

    @Test
    fun backfillLinksEveryRowAndMovesTheStartBack() = runTest {
        // Created with the coming due date, as the form does; the history is linked afterwards.
        val id = repository.add(
            Bill(
                name = "Home Rent",
                fromAccountId = bank,
                categoryId = home,
                amount = 11500.0,
                firstDue = LocalDate.of(2026, 12, 10),
                coversPrevious = true,
                createdTime = now,
                modifiedTime = now,
            ), now
        )
        val bill = db.billDao().getById(id)!!
        val ledger = TransactionRepository(db)
        val rows = listOf("May", "June", "July").mapIndexed { i, m ->
            ledger.add(
                row("Home Rent - $m 2026", 11500.0, LocalDate.of(2026, 6 + i, 10)),
                now = now
            )
        }.map { db.transactionDao().getById(it)!! }
        val links = Bills.assignPeriods(bill, rows, emptyList()).map { it.second to it.first.id }
        assertEquals(listOf("May 2026", "June 2026", "July 2026"), links.map { it.first.label })
        repository.linkAll(bill, links, now)
        val after = db.billDao().getById(id)!!
        assertEquals(LocalDate.of(2026, 6, 10), after.firstDue)
        val open = Bills.unsettled(after, db.billDao().getPayments(id), today)
        assertEquals(
            listOf("August 2026", "September 2026", "October 2026", "November 2026"),
            open.map { it.label })
        // Unlinking several at once keeps the ledger rows.
        val ids = db.billDao().getPayments(id).map { it.id }
        repository.unlinkAll(ids.take(2))
        assertEquals(1, db.billDao().getPayments(id).size)
        assertEquals(3, rows.count { db.transactionDao().getById(it.id) != null })
    }
}
