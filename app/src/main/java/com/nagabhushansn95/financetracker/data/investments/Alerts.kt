package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InsuranceType
import com.nagabhushansn95.financetracker.data.db.entity.ValidityKind
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * How many days before something falls due Home starts showing it, by how often it recurs: a monthly premium gets
 * a short run-up, a yearly one longer, a multi-year health cover longer still.
 */
data class AlertThresholds(
    val monthly: Int = DEFAULT_MONTHLY,
    val withinYear: Int = DEFAULT_WITHIN_YEAR,
    val beyondYear: Int = DEFAULT_BEYOND_YEAR,
) {
    fun daysFor(periodMonths: Long): Int = when {
        periodMonths <= 1 -> monthly
        periodMonths <= 12 -> withinYear
        else -> beyondYear
    }

    companion object {
        const val DEFAULT_MONTHLY = 7
        const val DEFAULT_WITHIN_YEAR = 15
        const val DEFAULT_BEYOND_YEAR = 30
    }
}

enum class AlertKind { DEPOSIT_MATURED, PREMIUM_DUE, COVER_ENDS, SERVICE_DUE, EMISSION_EXPIRES }

/** One line of Home's Alerts card. [daysLeft] is negative once the date has passed. */
data class Alert(
    val kind: AlertKind,
    val instrumentType: InstrumentType,
    val instrumentId: Long,
    val title: String,
    val detail: String,
    val date: LocalDate,
    val daysLeft: Long,
)

object Alerts {
    /** "due today", "due in 4 days", "overdue by 3 days". */
    fun phrase(daysLeft: Long, due: String = "due", passed: String = "overdue"): String = when {
        daysLeft < 0 -> "$passed by ${-daysLeft} day${if (daysLeft == -1L) "" else "s"}"
        daysLeft == 0L -> "$due today"
        else -> "$due in $daysLeft day${if (daysLeft == 1L) "" else "s"}"
    }

    /**
     * Deposits past maturity and still open; life and term premiums within their run-up or overdue; health cover,
     * vehicle insurance, service and emission tests ending within theirs or ended. Soonest first.
     */
    fun build(
        deposits: List<FdView>,
        policies: List<PolicyView>,
        today: LocalDate,
        thresholds: AlertThresholds = AlertThresholds(),
    ): List<Alert> {
        val alerts = mutableListOf<Alert>()
        for (d in deposits) {
            if (!d.matured || d.instrument.owner != com.nagabhushansn95.financetracker.data.db.entity.Instrument.OWNER_SELF) continue
            val date = d.maturityDate ?: continue
            val days = ChronoUnit.DAYS.between(today, date)
            alerts += Alert(
                AlertKind.DEPOSIT_MATURED,
                InstrumentType.FIXED_DEPOSIT,
                d.instrument.id,
                listOfNotNull(d.instrument.name, d.bank).joinToString(" · "),
                "Matured ${if (days == 0L) "today" else "${-days} day${if (days == -1L) "" else "s"} ago"}, still open",
                date,
                days,
            )
        }
        for (p in policies) {
            if (!p.active || p.instrument.owner != com.nagabhushansn95.financetracker.data.db.entity.Instrument.OWNER_SELF) continue
            if (p.type.scheduled) {
                val due = p.nextDue ?: continue
                val days = ChronoUnit.DAYS.between(today, due)
                val months = (p.policy?.premiumFrequencyMonths ?: 12).toLong()
                if (days <= thresholds.daysFor(months)) alerts += Alert(
                    AlertKind.PREMIUM_DUE,
                    p.instrument.instrumentType,
                    p.instrument.id,
                    p.title,
                    "Premium ${phrase(days)}",
                    due,
                    days,
                )
                continue
            }
            val kinds = if (p.type == InsuranceType.HEALTH) listOf(ValidityKind.COVER) else listOf(
                ValidityKind.THIRD_PARTY,
                ValidityKind.OWN_DAMAGE,
                ValidityKind.SERVICE,
                ValidityKind.EMISSION_TEST
            )
            for (kind in kinds) {
                val period = p.current(kind) ?: continue
                val days = ChronoUnit.DAYS.between(today, period.endDate)
                if (days > thresholds.daysFor(Insurance.periodMonths(period))) continue
                val (alertKind, detail) = when (kind) {
                    ValidityKind.COVER -> AlertKind.COVER_ENDS to "Cover ${
                        phrase(
                            days,
                            "ends",
                            "ended"
                        )
                    }"

                    ValidityKind.THIRD_PARTY -> AlertKind.COVER_ENDS to "Third-party cover ${
                        phrase(
                            days,
                            "ends",
                            "ended"
                        )
                    }"

                    ValidityKind.OWN_DAMAGE -> AlertKind.COVER_ENDS to "Own-damage cover ${
                        phrase(
                            days,
                            "ends",
                            "ended"
                        )
                    }"

                    ValidityKind.SERVICE -> AlertKind.SERVICE_DUE to "Service ${phrase(days)}"
                    ValidityKind.EMISSION_TEST -> AlertKind.EMISSION_EXPIRES to "Emission test ${
                        phrase(
                            days,
                            "expires",
                            "expired"
                        )
                    }"
                }
                alerts += Alert(
                    alertKind,
                    p.instrument.instrumentType,
                    p.instrument.id,
                    listOfNotNull(p.title, p.reference).joinToString(" · "),
                    detail,
                    period.endDate,
                    days,
                )
            }
        }
        return alerts.sortedWith(compareBy({ it.daysLeft }, { it.title }))
    }
}
