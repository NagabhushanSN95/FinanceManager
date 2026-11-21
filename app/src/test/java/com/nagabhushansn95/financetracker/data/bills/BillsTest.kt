package com.nagabhushansn95.financetracker.data.bills

import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Bill
import com.nagabhushansn95.financetracker.data.db.entity.BillPayment
import com.nagabhushansn95.financetracker.data.db.entity.BillSchedule
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.investments.Alerts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BillsTest {
    private val now = LocalDateTime.of(2026, 11, 20, 9, 0)
    private val today = LocalDate.of(2026, 11, 20)

    private fun bill(
        name: String = "Home Rent",
        firstDue: LocalDate = LocalDate.of(2026, 10, 10),
        everyMonths: Int = 1,
        coversPrevious: Boolean = false,
        schedule: BillSchedule = BillSchedule.CALENDAR,
        validityDays: Int? = null,
        endedOn: LocalDate? = null,
        particulars: String? = null,
        toAccountId: Long? = null,
        kind: TransactionKind = TransactionKind.EXPENSE,
    ) = Bill(
        id = 1,
        name = name,
        kind = kind,
        toAccountId = toAccountId,
        particulars = particulars,
        schedule = schedule,
        everyMonths = everyMonths,
        firstDue = firstDue,
        coversPrevious = coversPrevious,
        validityDays = validityDays,
        endedOn = endedOn,
        createdTime = now,
        modifiedTime = now,
    )

    private fun paid(period: BillPeriod, transactionId: Long? = 1) =
        BillPayment(
            billId = 1,
            periodStart = period.start,
            periodEnd = period.end,
            dueDate = period.due,
            transactionId = transactionId
        )

    @Test
    fun monthlyPeriodsFollowTheFirstDueDateAndClampShortMonths() {
        val b = bill(firstDue = LocalDate.of(2026, 1, 31))
        assertEquals(LocalDate.of(2026, 1, 31), Bills.calendarPeriod(b, 0).due)
        assertEquals(LocalDate.of(2026, 2, 28), Bills.calendarPeriod(b, 1).due)
        assertEquals(LocalDate.of(2026, 3, 31), Bills.calendarPeriod(b, 2).due)
        assertEquals("January 2026", Bills.calendarPeriod(b, 0).label)
    }

    @Test
    fun rentPaidInNovemberCoversOctober() {
        val b = bill(firstDue = LocalDate.of(2026, 11, 10), coversPrevious = true)
        val p = Bills.calendarPeriod(b, 0)
        assertEquals(LocalDate.of(2026, 10, 1), p.start)
        assertEquals(LocalDate.of(2026, 10, 31), p.end)
        assertEquals(LocalDate.of(2026, 11, 10), p.due)
        assertEquals("Home Rent - October 2026", Bills.particularsFor(b, p))
    }

    @Test
    fun quarterlyAndYearlyPeriodsSpanTheirMonths() {
        val q = bill(firstDue = LocalDate.of(2026, 10, 15), everyMonths = 3)
        assertEquals("Oct 2026 – Dec 2026", Bills.calendarPeriod(q, 0).label)
        assertEquals(LocalDate.of(2027, 1, 15), Bills.calendarPeriod(q, 1).due)
        val y = bill(firstDue = LocalDate.of(2026, 4, 30), everyMonths = 12)
        assertEquals("Apr 2026 – Mar 2027", Bills.calendarPeriod(y, 0).label)
        assertEquals(LocalDate.of(2027, 4, 30), Bills.calendarPeriod(y, 1).due)
    }

    @Test
    fun unsettledPeriodsAreThoseDueSoFarPlusTheNextOne() {
        val b = bill(firstDue = LocalDate.of(2026, 9, 10))
        val open = Bills.unsettled(b, emptyList(), today)
        assertEquals(
            listOf("September 2026", "October 2026", "November 2026", "December 2026"),
            open.map { it.label })
        val settled = listOf(paid(open[0]), paid(open[1]), paid(open[2]))
        val next = Bills.unsettled(b, settled, today)
        assertEquals(listOf("December 2026"), next.map { it.label })
    }

    @Test
    fun aSkippedPeriodCountsAsSettled() {
        val b = bill(firstDue = LocalDate.of(2026, 10, 10))
        val october = Bills.calendarPeriod(b, 0)
        val open = Bills.unsettled(b, listOf(paid(october, transactionId = null)), today)
        assertEquals("November 2026", open.first().label)
    }

    @Test
    fun anEndedBillOwesNothingAfterItsEndDate() {
        val b = bill(firstDue = LocalDate.of(2026, 10, 10), endedOn = LocalDate.of(2026, 10, 31))
        val open = Bills.unsettled(b, emptyList(), today)
        assertEquals(listOf("October 2026"), open.map { it.label })
        val done = Bills.unsettled(b, listOf(paid(open.single())), today)
        assertTrue(done.isEmpty())
    }

    @Test
    fun aDurationBillFallsDueWhenTheValidityRunsOut() {
        val b = bill(
            "Jio Recharge",
            firstDue = LocalDate.of(2026, 9, 22),
            schedule = BillSchedule.DURATION,
            validityDays = 90
        )
        // Nothing paid yet: due on the first due date.
        assertEquals(LocalDate.of(2026, 9, 22), Bills.unsettled(b, emptyList(), today).single().due)
        // Paid on the day: holds 90 days, so the next is due the day after.
        val first = Bills.durationPeriod(null, LocalDate.of(2026, 9, 22), 90)
        assertEquals(LocalDate.of(2026, 12, 20), first.end)
        assertEquals(
            LocalDate.of(2026, 12, 21),
            Bills.unsettled(b, listOf(paid(first)), today).single().due
        )
        // Recharged early: the new pack queues behind the running one.
        val early = Bills.durationPeriod(first.end, LocalDate.of(2026, 12, 15), 90)
        assertEquals(LocalDate.of(2026, 12, 21), early.start)
        assertEquals(LocalDate.of(2027, 3, 20), early.end)
        // Recharged late: it runs from the day it was done, and it was due the day after the old one ended.
        val late = Bills.durationPeriod(first.end, LocalDate.of(2026, 12, 25), 28)
        assertEquals(LocalDate.of(2026, 12, 25), late.start)
        assertEquals(LocalDate.of(2026, 12, 21), late.due)
    }

    @Test
    fun theViewCountsOverduePeriodsAndReadsTheLastPayment() {
        val b = bill(firstDue = LocalDate.of(2026, 9, 10))
        val september = Bills.calendarPeriod(b, 0)
        val t = TransactionEntity(
            id = 7,
            createdTime = now,
            modifiedTime = now,
            date = LocalDate.of(2026, 9, 11),
            kind = TransactionKind.EXPENSE,
            fromAccountId = 1,
            categoryId = 1,
            particulars = "Home Rent - September 2026",
            amount = 11500.0,
        )
        val view =
            Bills.build(listOf(b), listOf(paid(september, 7)), mapOf(7L to t), emptyMap(), today)
                .single()
        assertEquals("October 2026", view.next?.label)
        assertEquals(2, view.overdue) // October and November are past due on 20 November
        assertEquals(LocalDate.of(2026, 9, 11), view.lastPaidOn)
        assertEquals(11500.0, view.expected!!, 0.0)
        assertEquals("Overdue by 41 days · for October 2026 (2 unpaid)", Bills.status(view, today))
    }

    @Test
    fun aCardFollowsItsAccountsDueDayAndShowsTheStatementBalance() {
        val card = Account(
            id = 38, name = "Card", accountType = AccountType.CREDIT_CARD, balance = -30000.0,
            statementDay = 17, dueDay = 5, displayOrder = 1, createdTime = now, modifiedTime = now,
        )
        val b = bill(
            "Card bill",
            firstDue = LocalDate.of(2026, 12, 1),
            coversPrevious = true,
            toAccountId = 38,
            kind = TransactionKind.TRANSFER
        )
        // Spent 5,000 after the 17 November statement: the statement asked for 25,000.
        val later = TransactionEntity(
            id = 9,
            createdTime = now,
            modifiedTime = now,
            date = LocalDate.of(2026, 11, 19),
            kind = TransactionKind.EXPENSE,
            fromAccountId = 38,
            categoryId = 1,
            particulars = "Groceries",
            amount = 5000.0,
        )
        val view = Bills.build(
            listOf(b),
            emptyList(),
            emptyMap(),
            mapOf(38L to card),
            today,
            listOf(later)
        ).single()
        assertEquals(5, view.dueDay)
        assertEquals(LocalDate.of(2026, 12, 5), view.next?.due)
        assertEquals("November 2026", view.next?.label)
        assertEquals(25000.0, view.statementAmount!!, 0.001)
        // Before the statement date nothing is known.
        assertNull(Bills.statementAmount(card, emptyList(), LocalDate.of(2027, 1, 5), today))
    }

    @Test
    fun scheduleWordsAndOrdinals() {
        assertEquals(
            "Monthly on the 1st",
            Bills.scheduleText(bill(firstDue = LocalDate.of(2026, 11, 1)))
        )
        assertEquals(
            "Monthly on the 22nd, for the month before",
            Bills.scheduleText(bill(firstDue = LocalDate.of(2026, 11, 22), coversPrevious = true))
        )
        assertEquals(
            "Every 3 months on the 15th",
            Bills.scheduleText(bill(firstDue = LocalDate.of(2026, 10, 15), everyMonths = 3))
        )
        assertEquals(
            "Yearly in April on the 30th",
            Bills.scheduleText(bill(firstDue = LocalDate.of(2026, 4, 30), everyMonths = 12))
        )
        assertEquals(
            "Every 90 days from the last payment",
            Bills.scheduleText(bill(schedule = BillSchedule.DURATION, validityDays = 90))
        )
        assertEquals("11th", Bills.ordinal(11))
        assertEquals("23rd", Bills.ordinal(23))
    }

    private fun row(id: Long, date: LocalDate, particulars: String) = TransactionEntity(
        id = id, createdTime = now, modifiedTime = now, date = date, kind = TransactionKind.EXPENSE,
        fromAccountId = 1, categoryId = 1, particulars = particulars, amount = 100.0,
    )

    @Test
    fun periodLabelsParseBackToTheirMonths() {
        assertEquals(
            LocalDate.of(2026, 10, 1) to LocalDate.of(2026, 10, 31),
            Bills.parsePeriodLabel("October 2026")
        )
        assertEquals(
            LocalDate.of(2026, 10, 1) to LocalDate.of(2026, 12, 31),
            Bills.parsePeriodLabel("Oct 2026 – Dec 2026")
        )
        assertEquals(
            LocalDate.of(2026, 10, 1) to LocalDate.of(2026, 12, 31),
            Bills.parsePeriodLabel("Oct 2026 - Dec 2026")
        )
        assertEquals(
            LocalDate.of(2026, 9, 22) to LocalDate.of(2026, 12, 20),
            Bills.parsePeriodLabel("22 Sep 2026 – 20 Dec 2026")
        )
        assertEquals(
            LocalDate.of(2026, 5, 1) to LocalDate.of(2026, 5, 31),
            Bills.parsePeriodLabel("may 2026")
        )
        assertNull(Bills.parsePeriodLabel("Groceries"))
    }

    @Test
    fun aRowIsFiledUnderThePeriodItsParticularsName() {
        val b = bill(
            "Mudupu",
            firstDue = LocalDate.of(2026, 12, 1),
            coversPrevious = true,
            particulars = "Mudupu To Venkateshwara (Salary - {period})"
        )
        val named =
            row(1, LocalDate.of(2026, 11, 1), "Mudupu To Venkateshwara (Salary - October 2026)")
        assertTrue(Bills.matchesPattern(b, named))
        val p = Bills.periodOf(b, named)!!
        assertEquals("October 2026", p.label)
        assertEquals(LocalDate.of(2026, 11, 1), p.due)
        // Paid late and labelled: the label wins over the date.
        val late =
            row(2, LocalDate.of(2026, 10, 25), "mudupu to venkateshwara (salary - august 2026)")
        assertEquals("August 2026", Bills.periodOf(b, late)!!.label)
        // No period in the particulars: the nearest due date decides.
        val plain = row(3, LocalDate.of(2026, 9, 3), "Mudupu")
        assertTrue(!Bills.matchesPattern(b, plain))
        assertEquals("August 2026", Bills.periodOf(b, plain)!!.label)
        assertEquals(
            "September 2026",
            Bills.periodOf(b, row(4, LocalDate.of(2026, 9, 28), "Mudupu"))!!.label
        )
    }

    @Test
    fun aQuarterlyLabelThatDoesNotFitTheBillFallsBackToTheDate() {
        val monthly = bill(firstDue = LocalDate.of(2026, 10, 10))
        val r = row(1, LocalDate.of(2026, 10, 12), "Home Rent - Oct 2026 – Dec 2026")
        assertEquals("October 2026", Bills.periodOf(monthly, r)!!.label)
    }

    @Test
    fun durationRowsChainBehindEachOther() {
        val b = bill(
            "Jio",
            firstDue = LocalDate.of(2026, 12, 1),
            schedule = BillSchedule.DURATION,
            validityDays = 28
        )
        val rows = listOf(
            row(2, LocalDate.of(2026, 9, 29), "Jio Recharge"),
            row(1, LocalDate.of(2026, 9, 1), "Jio Recharge"),
            row(3, LocalDate.of(2026, 11, 2), "Jio Recharge"),
        )
        val assigned = Bills.assignPeriods(b, rows, emptyList())
        assertEquals(listOf(1L, 2L, 3L), assigned.map { it.first.id })
        assertEquals(
            LocalDate.of(2026, 9, 1) to LocalDate.of(2026, 9, 28),
            assigned[0].second.start to assigned[0].second.end
        )
        assertEquals(LocalDate.of(2026, 9, 29), assigned[1].second.start)
        assertEquals(LocalDate.of(2026, 10, 26), assigned[1].second.end)
        // Recharged a week late: the pack runs from the day it was done.
        assertEquals(LocalDate.of(2026, 11, 2), assigned[2].second.start)
        assertEquals(LocalDate.of(2026, 10, 27), assigned[2].second.due)
    }

    @Test
    fun linkingBeforeTheFirstPeriodMovesTheStartBack() {
        val b = bill(firstDue = LocalDate.of(2026, 12, 31), coversPrevious = true, everyMonths = 1)
        val may = Bills.calendarPeriod(b, -6)
        assertEquals("May 2026", may.label)
        assertEquals(-6, Bills.calendarIndexOf(b, may.start))
        val moved = Bills.startingFrom(b, may)
        assertEquals(LocalDate.of(2026, 6, 30), moved.firstDue) // the 31st, clamped to June's end
        assertEquals("May 2026", Bills.calendarPeriod(moved, 0).label)
        // Later periods leave it alone, as does one that is not a period of this bill.
        assertEquals(b, Bills.startingFrom(b, Bills.calendarPeriod(b, 2)))
        assertNull(Bills.calendarIndexOf(bill(everyMonths = 3), LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun changingTheNextDueDateMovesTheDayNotTheStart() {
        val monthly = bill(firstDue = LocalDate.of(2026, 6, 10))
        assertEquals(
            LocalDate.of(2026, 6, 5),
            Bills.firstDueFor(monthly, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 5))
        )
        assertEquals(
            LocalDate.of(2026, 6, 5),
            Bills.firstDueFor(monthly, LocalDate.of(2026, 12, 10), LocalDate.of(2027, 1, 5))
        )
        val quarterly = bill(firstDue = LocalDate.of(2026, 1, 15), everyMonths = 3)
        // Jan/Apr/Jul/Oct → Feb/May/Aug/Nov
        assertEquals(
            LocalDate.of(2026, 2, 15),
            Bills.firstDueFor(quarterly, LocalDate.of(2027, 1, 15), LocalDate.of(2027, 2, 15))
        )
        assertEquals(LocalDate.of(2026, 11, 20), Bills.comingDue(LocalDate.of(2026, 11, 20), 20))
        assertEquals(LocalDate.of(2026, 12, 5), Bills.comingDue(LocalDate.of(2026, 11, 20), 5))
    }

    @Test
    fun aMonthWrittenAnywhereInTheParticularsNamesThePeriod() {
        val b = bill(
            "Mudupu",
            firstDue = LocalDate.of(2026, 12, 1),
            coversPrevious = true,
            particulars = "Mudupu To Venkateshwara (Salary - {period})"
        )
        // An older wording: the pattern does not match, but the month does, and it wins over the date.
        val stipend =
            row(1, LocalDate.of(2022, 1, 1), "Mudupu To Venkateshwara (Stipend - November 2021)")
        assertTrue(!Bills.matchesPattern(b, stipend))
        assertEquals("November 2021", Bills.periodOf(b, stipend)!!.label)
        assertEquals(
            "February 2023",
            Bills.periodOf(
                b,
                row(
                    2,
                    LocalDate.of(2023, 4, 1),
                    "Mudupu To Venkateshwara (Stipend - February 2023)"
                )
            )!!.label
        )
        assertEquals(
            LocalDate.of(2022, 9, 1) to LocalDate.of(2022, 9, 30),
            Bills.periodLabelIn("Stipend - Sep 2022")
        )
        assertNull(Bills.periodLabelIn("Mudupu To Venkateshwara (Salary -"))
        // A truncated label falls back to the date.
        assertEquals(
            "June 2024",
            Bills.periodOf(
                b,
                row(3, LocalDate.of(2024, 7, 1), "Mudupu To Venkateshwara (Salary -")
            )!!.label
        )
    }

    @Test
    fun periodChoicesRunFromTheOverdueOnesToAFewAhead() {
        val b = bill(firstDue = LocalDate.of(2026, 9, 10))
        val choices = Bills.periodChoices(b, emptyList(), today)
        assertEquals(
            listOf(
                "September 2026",
                "October 2026",
                "November 2026",
                "December 2026",
                "January 2027",
                "February 2027",
                "March 2027"
            ),
            choices.map { it.label },
        )
        // A settled month ahead is left out; a duration bill offers nothing.
        val settled = listOf(paid(Bills.calendarPeriod(b, 4)))
        assertTrue(Bills.periodChoices(b, settled, today).none { it.label == "January 2027" })
        assertTrue(
            Bills.periodChoices(
                bill(schedule = BillSchedule.DURATION, validityDays = 30),
                emptyList(),
                today
            ).isEmpty()
        )
    }

    @Test
    fun aBillThatCanWaitAlertsOnlyOnceOverdue() {
        val soon = bill("Netflix", firstDue = LocalDate.of(2026, 11, 23)) // due in 2 days
        val late = bill("Maintenance", firstDue = LocalDate.of(2026, 11, 15)) // overdue
        fun alerts(b: Bill) = Alerts.forBills(
            Bills.build(listOf(b), emptyList(), emptyMap(), emptyMap(), today),
            today
        )
        assertEquals(1, alerts(soon).size)
        assertTrue(alerts(soon.copy(alertWhenOverdue = true)).isEmpty())
        assertEquals(1, alerts(late.copy(alertWhenOverdue = true)).size)
        assertTrue(
            alerts(
                bill(
                    firstDue = LocalDate.of(
                        2026,
                        11,
                        21
                    )
                ).copy(alertWhenOverdue = true)
            ).isEmpty()
        ) // due today: not yet
    }
}
