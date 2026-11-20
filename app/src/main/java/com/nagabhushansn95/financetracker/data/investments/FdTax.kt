package com.nagabhushansn95.financetracker.data.investments

import java.time.LocalDate

/**
 * Tax deducted at source on deposit interest, estimated the way banks apply it: 10 % of the interest once the
 * holder's interest across all deposits with that bank crosses the year's threshold (₹40,000 up to FY 2024-25,
 * ₹50,000 from FY 2025-26 for depositors below 60). No TDS rate is asked for anywhere; this estimate feeds the
 * "TDS" figures and the amount prefilled on Matured / Renew, which can always be corrected there.
 */
object FdTax {
    const val RATE = 0.10

    /** Threshold for the financial year that starts on 1 April of [fyStartYear]. */
    fun threshold(fyStartYear: Int): Double = if (fyStartYear >= 2025) 50_000.0 else 40_000.0

    /** 1 April of the financial year [date] falls in. */
    fun financialYearStart(date: LocalDate): LocalDate =
        if (date.monthValue >= 4) LocalDate.of(date.year, 4, 1) else LocalDate.of(
            date.year - 1,
            4,
            1
        )

    /** Interest a deposit earns inside the financial year starting [fyStart]; zero outside the deposit's life. */
    fun interestInYear(
        principal: Double,
        ratePct: Double,
        compoundingMonths: Int,
        depositDate: LocalDate,
        maturity: LocalDate,
        fyStart: LocalDate,
    ): Double {
        val from = maxOf(depositDate, fyStart.minusDays(1))
        val to = minOf(maturity, fyStart.plusYears(1).minusDays(1))
        if (!to.isAfter(from)) return 0.0
        return FdMath.value(principal, ratePct, compoundingMonths, depositDate, to) -
                FdMath.value(principal, ratePct, compoundingMonths, depositDate, from)
    }

    /** Holder (or owner, when no holder is recorded) and bank, normalised; null when the bank is unknown. */
    private fun holderAndBank(d: FdView): Pair<String, String>? {
        val bank = d.terms?.bankName?.trim()?.lowercase()?.ifEmpty { null } ?: return null
        val holder = d.terms?.holderName?.trim()?.lowercase()?.ifEmpty { null }
            ?: d.instrument.owner.lowercase()
        return holder to bank
    }

    private fun principalOf(d: FdView) = d.principal.takeIf { it > 0 } ?: d.position.totalInvested

    private fun complete(d: FdView) =
        d.terms?.rate != null && d.depositDate != null && d.maturityDate != null && principalOf(d) > 0

    /**
     * Estimated TDS over the life of [deposit]: for every financial year it touches, 10 % of its interest in that year
     * when the holder's interest in that year across [all] deposits with the same bank exceeds the threshold.
     */
    fun estimate(deposit: FdView, all: List<FdView>): Double {
        if (!complete(deposit)) return 0.0
        val key = holderAndBank(deposit)
        val peers = if (key == null) emptyList() else all.filter {
            it.instrument.id != deposit.instrument.id && complete(it) && holderAndBank(it) == key
        }

        fun FdView.interestIn(fy: LocalDate) = interestInYear(
            principalOf(this),
            terms!!.rate!!,
            terms.compoundingMonths ?: FdMath.DEFAULT_COMPOUNDING_MONTHS,
            depositDate!!,
            maturityDate!!,
            fy
        )

        var tds = 0.0
        var fy = financialYearStart(deposit.depositDate!!)
        while (!fy.isAfter(deposit.maturityDate!!)) {
            val own = deposit.interestIn(fy)
            if (own > 0.0 && own + peers.sumOf { it.interestIn(fy) } > threshold(fy.year)) tds += own * RATE
            fy = fy.plusYears(1)
        }
        return tds
    }
}
