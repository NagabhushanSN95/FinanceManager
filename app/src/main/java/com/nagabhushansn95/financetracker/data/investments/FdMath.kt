package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow

/**
 * Bank-style fixed-deposit arithmetic: interest compounds every [FixedDeposit.compoundingMonths] (quarterly by
 * default) for the whole periods that fit between two dates, and the remaining days earn simple interest.
 * Checks against the sheet: 1,00,000 at 5.1% for 12 months quarterly = 1,05,198; at 6.25% for 366 days = 1,06,416.
 */
object FdMath {
    const val DEFAULT_COMPOUNDING_MONTHS = 3

    /** Maturity date from the deposit date and the period, as banks count it (months first, then days). */
    fun maturityDate(depositDate: LocalDate, periodMonths: Int?, periodDays: Int?): LocalDate? {
        if ((periodMonths ?: 0) <= 0 && (periodDays ?: 0) <= 0) return null
        return depositDate.plusMonths((periodMonths ?: 0).toLong())
            .plusDays((periodDays ?: 0).toLong())
    }

    /** Value of [principal] at [ratePct] per year from [from] to [to]; [from] or earlier gives the principal back. */
    fun value(
        principal: Double,
        ratePct: Double,
        compoundingMonths: Int,
        from: LocalDate,
        to: LocalDate
    ): Double {
        if (!to.isAfter(from) || principal <= 0.0 || ratePct <= 0.0) return principal
        val months = compoundingMonths.coerceAtLeast(1)
        val periodsPerYear = 12.0 / months
        // Each period end is counted from the deposit date (not from the previous end), so a 30 Aug deposit
        // compounds on 30 Nov, 28 Feb, 30 May and 30 Aug rather than drifting to the 28th.
        var periods = 0L
        var cursor = from
        while (!from.plusMonths((periods + 1) * months).isAfter(to)) {
            periods++
            cursor = from.plusMonths(periods * months)
        }
        val compounded = principal * (1 + ratePct / 100.0 / periodsPerYear).pow(periods.toDouble())
        val remainingDays = ChronoUnit.DAYS.between(cursor, to)
        return compounded * (1 + ratePct / 100.0 * remainingDays / 365.0)
    }

    /** Amount at maturity before tax, or null when the terms are incomplete. */
    fun maturityAmount(fd: FixedDeposit, principal: Double, depositDate: LocalDate): Double? {
        val rate = fd.rate ?: return null
        val maturity = fd.maturityDate ?: maturityDate(depositDate, fd.periodMonths, fd.periodDays)
        ?: return null
        return value(
            principal,
            rate,
            fd.compoundingMonths ?: DEFAULT_COMPOUNDING_MONTHS,
            depositDate,
            maturity
        )
    }

    /** Estimated value today: accrued up to [asOf], capped at maturity. Null when the rate is unknown. */
    fun estimate(
        fd: FixedDeposit,
        principal: Double,
        depositDate: LocalDate,
        asOf: LocalDate
    ): Double? {
        val rate = fd.rate ?: return null
        val maturity = fd.maturityDate ?: maturityDate(depositDate, fd.periodMonths, fd.periodDays)
        val to = if (maturity != null && maturity.isBefore(asOf)) maturity else asOf
        return value(
            principal,
            rate,
            fd.compoundingMonths ?: DEFAULT_COMPOUNDING_MONTHS,
            depositDate,
            to
        )
    }

    /** Tax deducted at source on the interest, at the deposit's TDS rate (a fraction such as 0.1, or 10 for 10%). */
    fun tds(interest: Double, tdsRate: Double?): Double {
        val rate = tdsRate ?: return 0.0
        val fraction = if (rate > 1.0) rate / 100.0 else rate
        return (interest * fraction).coerceAtLeast(0.0)
    }

    /** "6.25" rather than "6.25000" for rates and amounts in text. */
    fun plain(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString()
        else java.math.BigDecimal(value).setScale(4, java.math.RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString()

    fun daysLeft(maturity: LocalDate?, asOf: LocalDate): Long? =
        maturity?.let { ChronoUnit.DAYS.between(asOf, it) }

    /** "12 months", "366 days", "1 year 6 months", or null. */
    fun periodLabel(periodMonths: Int?, periodDays: Int?): String? {
        val parts = ArrayList<String>()
        val m = periodMonths ?: 0
        val d = periodDays ?: 0
        if (m >= 12 && m % 12 == 0 && d == 0) return if (m == 12) "1 year" else "${m / 12} years"
        if (m > 0) parts += "$m month${if (m == 1) "" else "s"}"
        if (d > 0) parts += "$d day${if (d == 1) "" else "s"}"
        return parts.joinToString(" ").ifEmpty { null }
    }
}
