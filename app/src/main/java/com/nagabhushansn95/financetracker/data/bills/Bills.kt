package com.nagabhushansn95.financetracker.data.bills

import com.nagabhushansn95.financetracker.data.db.entity.Account
import com.nagabhushansn95.financetracker.data.db.entity.AccountType
import com.nagabhushansn95.financetracker.data.db.entity.Bill
import com.nagabhushansn95.financetracker.data.db.entity.BillPayment
import com.nagabhushansn95.financetracker.data.db.entity.BillSchedule
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.investments.Alerts
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/** One stretch a bill payment covers, and the day it was (or is) due. */
data class BillPeriod(val start: LocalDate, val end: LocalDate, val due: LocalDate) {
    /** "October 2026" for one month, "Oct 2026 – Dec 2026" for a span of months, dates otherwise. */
    val label: String get() = Bills.periodLabel(start, end)
}

/** A payment row with the ledger row behind it (none when the period was skipped). */
data class BillPaymentView(val payment: BillPayment, val transaction: TransactionEntity?) {
    val period: BillPeriod
        get() = BillPeriod(
            payment.periodStart,
            payment.periodEnd,
            payment.dueDate
        )
    val skipped: Boolean get() = payment.skipped
}

/** A bill as the screens show it: its history, what falls due next, and what it will likely cost. */
data class BillView(
    val bill: Bill,
    /** The due day in force: a card's own due day from its account, else the first due date's day. */
    val dueDay: Int,
    /** Oldest period first. */
    val payments: List<BillPaymentView>,
    /** The first period not settled, or null once the bill has ended with nothing outstanding. */
    val next: BillPeriod?,
    /** Unsettled periods whose due date has passed. */
    val overdue: Int,
    /** A card: what the statement before [next] asked for, when that statement date has come. */
    val statementAmount: Double?,
    /** Every period not settled yet, soonest first: those already due and the next one ([next] is the first). */
    val unsettled: List<BillPeriod> = emptyList(),
) {
    val nextDue: LocalDate? get() = next?.due
    val lastPaid: BillPaymentView? = payments.lastOrNull { !it.skipped && it.transaction != null }
    val lastPaidOn: LocalDate? get() = lastPaid?.transaction?.date
    val lastAmount: Double? get() = lastPaid?.transaction?.amount

    /** What the next payment is likely to be: the card statement, else the bill's amount, else the last paid. */
    val expected: Double? get() = statementAmount ?: bill.amount ?: lastAmount

    fun daysLeft(today: LocalDate): Long? = next?.let { ChronoUnit.DAYS.between(today, it.due) }
}

/** The schedule maths of bills: which periods there are, which are settled, and what a payment covers. */
object Bills {
    const val PERIOD_TOKEN = "{period}"

    /** How far ahead the calendar is walked when counting unsettled periods: a bill left unrecorded for two years. */
    private const val MAX_PERIODS = 400

    /** A duration bill with no validity set holds this long. */
    const val DEFAULT_VALIDITY_DAYS = 30

    private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val SHORT_MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    private val PARSERS: List<DateTimeFormatter> =
        listOf("MMMM yyyy", "MMM yyyy", "d MMM yyyy").map {
            DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(it)
                .toFormatter(Locale.ENGLISH)
        }

    /** The [day] of [month], or the month's last day when it is shorter. */
    fun clamp(month: YearMonth, day: Int): LocalDate =
        month.atDay(day.coerceIn(1, month.lengthOfMonth()))

    fun periodLabel(start: LocalDate, end: LocalDate): String {
        val wholeMonths = start.dayOfMonth == 1 && end == YearMonth.from(end).atEndOfMonth()
        return when {
            wholeMonths && YearMonth.from(start) == YearMonth.from(end) -> start.format(MONTH_YEAR)
            wholeMonths -> "${start.format(SHORT_MONTH_YEAR)} – ${end.format(SHORT_MONTH_YEAR)}"
            else -> "${start.format(DAY)} – ${end.format(DAY)}"
        }
    }

    /**
     * The k-th period of a calendar bill (k from 0): due in the k-th due month on [dueDay], covering that month and
     * the [Bill.everyMonths] - 1 after it, or the months before it when the bill covers the previous month.
     */
    fun calendarPeriod(bill: Bill, k: Int, dueDay: Int = bill.firstDue.dayOfMonth): BillPeriod {
        val n = bill.everyMonths.coerceAtLeast(1).toLong()
        val dueMonth = YearMonth.from(bill.firstDue).plusMonths(k * n)
        val firstMonth = if (bill.coversPrevious) dueMonth.minusMonths(n) else dueMonth
        return BillPeriod(
            firstMonth.atDay(1),
            firstMonth.plusMonths(n - 1).atEndOfMonth(),
            clamp(dueMonth, dueDay)
        )
    }

    /**
     * What a duration payment made on [paidOn] covers: [days] from that day, or from the end of the current validity
     * when it is paid before that runs out (a recharge queued behind the running pack). It was due the day after the
     * previous validity ended, or on the day itself when there was none.
     */
    fun durationPeriod(previousEnd: LocalDate?, paidOn: LocalDate, days: Int): BillPeriod {
        val start =
            if (previousEnd != null && !paidOn.isAfter(previousEnd)) previousEnd.plusDays(1) else paidOn
        return BillPeriod(
            start,
            start.plusDays(days.toLong() - 1),
            previousEnd?.plusDays(1) ?: paidOn
        )
    }

    /**
     * The periods not settled yet, soonest first: every one already due, and the first still to come. A calendar bill
     * that has ended owes nothing due after its end date; a duration bill that has ended owes nothing at all.
     */
    fun unsettled(
        bill: Bill,
        payments: List<BillPayment>,
        today: LocalDate,
        dueDay: Int = bill.firstDue.dayOfMonth,
    ): List<BillPeriod> = when (bill.schedule) {
        BillSchedule.CALENDAR -> {
            val settled = payments.map { it.periodStart }.toHashSet()
            val out = ArrayList<BillPeriod>()
            for (k in 0 until MAX_PERIODS) {
                val p = calendarPeriod(bill, k, dueDay)
                if (bill.endedOn != null && p.due.isAfter(bill.endedOn)) break
                if (p.start in settled) continue
                out += p
                if (p.due.isAfter(today)) break
            }
            out
        }

        BillSchedule.DURATION -> {
            if (bill.endedOn != null) emptyList()
            else {
                val last = payments.maxByOrNull { it.periodEnd }
                val start = last?.periodEnd?.plusDays(1) ?: bill.firstDue
                val days = (bill.validityDays ?: DEFAULT_VALIDITY_DAYS).coerceAtLeast(1)
                listOf(BillPeriod(start, start.plusDays(days.toLong() - 1), start))
            }
        }
    }

    /** The bill's particulars pattern: what was typed, else "Name - {period}". */
    fun pattern(bill: Bill): String =
        bill.particulars?.trim()?.takeIf { it.isNotEmpty() } ?: "${bill.name} - $PERIOD_TOKEN"

    /** A regex for particulars written from the bill's pattern, the period text captured; null when the pattern has no token. */
    private fun periodRegex(bill: Bill): Regex? {
        val pattern = pattern(bill)
        if (!pattern.contains(PERIOD_TOKEN)) return null
        val fixed = pattern.split(PERIOD_TOKEN).map { Regex.escape(it.trim()) }
        return Regex(
            "^\\s*" + fixed.joinToString("\\s*(.+?)\\s*") + "\\s*$",
            RegexOption.IGNORE_CASE
        )
    }

    /** Whether a row's particulars were written from the bill's pattern (with or without a period in it). */
    fun matchesPattern(bill: Bill, row: TransactionEntity): Boolean {
        val re = periodRegex(bill) ?: return row.particulars.trim()
            .equals(pattern(bill), ignoreCase = true)
        return re.matches(row.particulars)
    }

    /**
     * The stretch a period label names: "October 2026", "Oct 2026 – Dec 2026" (any dash) or "1 Jan 2026 – 30 Mar 2026".
     * Null for anything else.
     */
    fun parsePeriodLabel(text: String): Pair<LocalDate, LocalDate>? {
        val parts = text.trim().split(Regex("\\s+[–—-]\\s+|\\s*[–—]\\s*")).map { it.trim() }
        fun month(s: String): YearMonth? = PARSERS.take(2)
            .firstNotNullOfOrNull { f -> runCatching { YearMonth.parse(s, f) }.getOrNull() }

        fun day(s: String): LocalDate? = runCatching { LocalDate.parse(s, PARSERS[2]) }.getOrNull()
        return when (parts.size) {
            1 -> month(parts[0])?.let { it.atDay(1) to it.atEndOfMonth() }
            2 -> {
                val a = month(parts[0])
                val b = month(parts[1])
                if (a != null && b != null) a.atDay(1) to b.atEndOfMonth()
                else {
                    val d1 = day(parts[0])
                    val d2 = day(parts[1])
                    if (d1 != null && d2 != null) d1 to d2 else null
                }
            }

            else -> null
        }
    }

    private const val MONTH_WORD = "(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*"
    private val LABEL_ANYWHERE = Regex(
        "(?:\\d{1,2}\\s+)?$MONTH_WORD\\s+\\d{4}(?:\\s*[–—-]\\s*(?:\\d{1,2}\\s+)?$MONTH_WORD\\s+\\d{4})?",
        RegexOption.IGNORE_CASE,
    )

    /** The first period label found anywhere in [text], whatever surrounds it: "Stipend - November 2021" names November 2021. */
    fun periodLabelIn(text: String): Pair<LocalDate, LocalDate>? =
        LABEL_ANYWHERE.findAll(text).firstNotNullOfOrNull { parsePeriodLabel(it.value) }

    /** The index k of the calendar period starting on [start] (negative before the first), or null when none starts there. */
    fun calendarIndexOf(bill: Bill, start: LocalDate): Int? {
        if (start.dayOfMonth != 1) return null
        val n = bill.everyMonths.coerceAtLeast(1).toLong()
        val months = ChronoUnit.MONTHS.between(
            YearMonth.from(calendarPeriod(bill, 0).start),
            YearMonth.from(start)
        )
        return if (months % n == 0L) (months / n).toInt() else null
    }

    /** The calendar period whose due date is nearest [date]. */
    fun nearestPeriod(
        bill: Bill,
        date: LocalDate,
        dueDay: Int = bill.firstDue.dayOfMonth
    ): BillPeriod {
        val n = bill.everyMonths.coerceAtLeast(1).toLong()
        val k = Math.floorDiv(
            ChronoUnit.MONTHS.between(
                YearMonth.from(bill.firstDue),
                YearMonth.from(date)
            ), n
        ).toInt()
        return (k - 1..k + 1).map { calendarPeriod(bill, it, dueDay) }
            .minBy { abs(ChronoUnit.DAYS.between(date, it.due)) }
    }

    /**
     * The calendar period a ledger row settles: the one its particulars name (through the bill's pattern, or a month
     * and year written anywhere in them) when that is a period of this bill, else the one whose due date is nearest
     * the row's date. Null for a duration bill, whose periods follow the payments themselves: see [assignPeriods].
     */
    fun periodOf(
        bill: Bill,
        row: TransactionEntity,
        dueDay: Int = bill.firstDue.dayOfMonth
    ): BillPeriod? {
        if (bill.schedule != BillSchedule.CALENDAR) return null
        val named = periodRegex(bill)?.find(row.particulars)?.groupValues?.getOrNull(1)
            ?.let(::parsePeriodLabel)
            ?: periodLabelIn(row.particulars)
        if (named != null) {
            val k = calendarIndexOf(bill, named.first)
            if (k != null) {
                val p = calendarPeriod(bill, k, dueDay)
                if (p.end == named.second) return p
            }
        }
        return nearestPeriod(bill, row.date, dueDay)
    }

    /**
     * The periods [rows] settle when linked together, oldest row first. A calendar bill's come from [periodOf]; a
     * duration bill's are chained, each holding the bill's validity from the row's date (or from the end of the
     * validity still running then).
     */
    fun assignPeriods(
        bill: Bill,
        rows: List<TransactionEntity>,
        payments: List<BillPayment>,
        dueDay: Int = bill.firstDue.dayOfMonth,
    ): List<Pair<TransactionEntity, BillPeriod>> {
        val ordered = rows.sortedWith(compareBy({ it.date }, { it.id }))
        return when (bill.schedule) {
            BillSchedule.CALENDAR -> ordered.map { it to periodOf(bill, it, dueDay)!! }
            BillSchedule.DURATION -> {
                val days = (bill.validityDays ?: DEFAULT_VALIDITY_DAYS).coerceAtLeast(1)
                var previousEnd: LocalDate? = null
                ordered.map { row ->
                    val running = (payments.map { it.periodEnd } + listOfNotNull(previousEnd))
                        .filter { it.isBefore(row.date.plusDays(days.toLong())) }.maxOrNull()
                    val p = durationPeriod(running, row.date, days)
                    previousEnd = p.end
                    row to p
                }
            }
        }
    }

    /** [bill] with its history starting at [period] when that lies before its first period; otherwise unchanged. */
    fun startingFrom(bill: Bill, period: BillPeriod): Bill = when (bill.schedule) {
        BillSchedule.DURATION -> if (period.start.isBefore(bill.firstDue)) bill.copy(firstDue = period.start) else bill
        BillSchedule.CALENDAR -> {
            val k = calendarIndexOf(bill, period.start)
            if (k == null || k >= 0) bill
            else {
                val month = YearMonth.from(bill.firstDue)
                    .plusMonths(k.toLong() * bill.everyMonths.coerceAtLeast(1))
                bill.copy(firstDue = clamp(month, bill.firstDue.dayOfMonth))
            }
        }
    }

    /**
     * The first due date that keeps the bill's history where it starts but moves its due day (and, for a bill every
     * several months, its anchor month) so that the next due date reads [newNext] rather than [oldNext].
     */
    fun firstDueFor(bill: Bill, oldNext: LocalDate, newNext: LocalDate): LocalDate {
        val n = bill.everyMonths.coerceAtLeast(1).toLong()
        val shift = Math.floorMod(
            ChronoUnit.MONTHS.between(
                YearMonth.from(oldNext),
                YearMonth.from(newNext)
            ), n
        )
        return clamp(YearMonth.from(bill.firstDue).plusMonths(shift), newNext.dayOfMonth)
    }

    /** The coming due date of a bill paid on [dueDay] of every month: this month's when it has not passed, else next month's. */
    fun comingDue(today: LocalDate, dueDay: Int): LocalDate {
        val thisMonth = clamp(YearMonth.from(today), dueDay)
        return if (thisMonth.isBefore(today)) clamp(
            YearMonth.from(today).plusMonths(1),
            dueDay
        ) else thisMonth
    }

    /**
     * The periods a payment may be recorded for: every unsettled one, then [ahead] more beyond the last of them for
     * paying early. A calendar bill only; a duration bill has no periods to choose from.
     */
    fun periodChoices(
        bill: Bill,
        payments: List<BillPayment>,
        today: LocalDate,
        dueDay: Int = bill.firstDue.dayOfMonth,
        ahead: Int = 3,
    ): List<BillPeriod> {
        if (bill.schedule != BillSchedule.CALENDAR) return emptyList()
        val open = unsettled(bill, payments, today, dueDay)
        val last = open.lastOrNull() ?: return open
        val k = calendarIndexOf(bill, last.start) ?: return open
        val settled = payments.map { it.periodStart }.toHashSet()
        val more = ArrayList<BillPeriod>()
        var i = k + 1
        while (more.size < ahead && i < k + ahead * 4) {
            val p = calendarPeriod(bill, i++, dueDay)
            if (bill.endedOn != null && p.due.isAfter(bill.endedOn)) break
            if (p.start !in settled) more += p
        }
        return open + more
    }

    /** The particulars of a payment for [period]: the bill's pattern with the period filled in, else "Name - period". */
    fun particularsFor(bill: Bill, period: BillPeriod): String {
        return pattern(bill).replace(PERIOD_TOKEN, period.label)
    }

    /**
     * What a card's statement before [due] asked for: the account's balance wound back to the statement date by
     * undoing every later row (the card's balance is negative while money is owed). Null before that statement date.
     */
    fun statementAmount(
        account: Account,
        rows: List<TransactionEntity>,
        due: LocalDate,
        today: LocalDate
    ): Double? {
        val statementDay = account.statementDay ?: return null
        val dueMonth = YearMonth.from(due)
        val statementMonth =
            if (statementDay > due.dayOfMonth) dueMonth.minusMonths(1) else dueMonth
        val statementDate = clamp(statementMonth, statementDay)
        if (statementDate.isAfter(today)) return null
        var balance = account.balance
        for (t in rows) {
            if (!t.date.isAfter(statementDate)) continue
            if (t.toAccountId == account.id) balance -= t.arriving
            if (t.fromAccountId == account.id) balance += t.amount
        }
        return (-balance).takeIf { it > 0.005 }
    }

    /** The due day a bill follows: its card's own due day when it pays a card that has one. */
    fun dueDayOf(bill: Bill, accounts: Map<Long, Account>): Int {
        val card = bill.toAccountId?.let { accounts[it] }
            ?.takeIf { it.accountType == AccountType.CREDIT_CARD }
        return card?.dueDay ?: bill.firstDue.dayOfMonth
    }

    fun build(
        bills: List<Bill>,
        payments: List<BillPayment>,
        transactions: Map<Long, TransactionEntity>,
        accounts: Map<Long, Account>,
        today: LocalDate,
        /** The whole ledger, for the card statements; may be left out where no statement is wanted. */
        ledger: List<TransactionEntity> = emptyList(),
    ): List<BillView> {
        val byBill = payments.groupBy { it.billId }
        val byAccount = HashMap<Long, List<TransactionEntity>>()
        return bills.map { bill ->
            val own = byBill[bill.id].orEmpty().sortedWith(compareBy({ it.periodStart }, { it.id }))
            val dueDay = dueDayOf(bill, accounts)
            val open = unsettled(bill, own, today, dueDay)
            val next = open.firstOrNull()
            val card = bill.toAccountId?.let { accounts[it] }
                ?.takeIf { it.accountType == AccountType.CREDIT_CARD && bill.kind == com.nagabhushansn95.financetracker.data.db.entity.TransactionKind.TRANSFER }
            val statement = if (card != null && next != null) statementAmount(
                card,
                byAccount.getOrPut(card.id) { ledger.filter { it.fromAccountId == card.id || it.toAccountId == card.id } },
                next.due,
                today
            ) else null
            BillView(
                bill = bill,
                dueDay = dueDay,
                payments = own.map {
                    BillPaymentView(
                        it,
                        it.transactionId?.let { id -> transactions[id] })
                },
                next = next,
                overdue = open.count { it.due.isBefore(today) },
                statementAmount = statement,
                unsettled = open,
            )
        }
    }

    /** "Due in 4 days · for October 2026", "Overdue by 3 days · for October 2026 (2 unpaid)", "Ended". */
    fun status(view: BillView, today: LocalDate): String {
        val next = view.next ?: return "Ended"
        val days = ChronoUnit.DAYS.between(today, next.due)
        val more = if (view.overdue > 1) " (${view.overdue} unpaid)" else ""
        return Alerts.phrase(days)
            .replaceFirstChar { it.uppercase() } + " · for ${next.label}" + more
    }

    /** How often the bill recurs, in words: "Monthly on the 5th", "Every 3 months", "Yearly in April", "Every 90 days". */
    fun scheduleText(bill: Bill, dueDay: Int = bill.firstDue.dayOfMonth): String =
        when (bill.schedule) {
            BillSchedule.DURATION -> "Every ${bill.validityDays ?: DEFAULT_VALIDITY_DAYS} days from the last payment"
            BillSchedule.CALENDAR -> when (bill.everyMonths) {
                1 -> "Monthly on the ${ordinal(dueDay)}"
                12 -> "Yearly in ${
                    bill.firstDue.month.getDisplayName(
                        java.time.format.TextStyle.FULL,
                        java.util.Locale.ENGLISH
                    )
                } on the ${ordinal(dueDay)}"

                else -> "Every ${bill.everyMonths} months on the ${ordinal(dueDay)}"
            } + if (bill.coversPrevious) (if (bill.everyMonths == 1) ", for the month before" else ", for the period before") else ""
        }

    fun ordinal(n: Int): String {
        val suffix = when {
            n % 100 in 11..13 -> "th"
            n % 10 == 1 -> "st"
            n % 10 == 2 -> "nd"
            n % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }
}
