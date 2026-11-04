package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.FdStatus
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentValuation
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.LocalDate

/**
 * One fixed deposit as the screens show it: the instrument, its terms (may be missing for deposits created from
 * the transaction editor), the deposit row's amount and date, and everything computed from them.
 */
data class FdView(
    val instrument: Instrument,
    val terms: FixedDeposit?,
    val position: Position,
    /** Amount deposited (net of partial withdrawals, as the ledger says). */
    val principal: Double,
    val depositDate: LocalDate?,
    val maturityDate: LocalDate?,
    /** Value today: a manual valuation if one exists, else the formula, else the principal. */
    val estimated: Double,
    val maturityAmount: Double?,
    val tdsAtMaturity: Double,
    val daysLeft: Long?,
    val rows: List<TransactionEntity>,
) {
    val status: FdStatus
        get() = terms?.status
            ?: if (position.costHeld > Position.UNIT_EPSILON) FdStatus.ACTIVE else FdStatus.CLOSED_AT_MATURITY
    val isOpen: Boolean get() = status == FdStatus.ACTIVE
    val interestSoFar: Double get() = estimated - principal
    val maturityAfterTds: Double? get() = maturityAmount?.let { it - tdsAtMaturity }
    val bank: String? get() = terms?.bankName
    val rate: Double? get() = terms?.rate
    val matured: Boolean get() = isOpen && maturityDate != null && daysLeft != null && daysLeft <= 0
    val periodLabel: String?
        get() = terms?.let {
            FdMath.periodLabel(
                it.periodMonths,
                it.periodDays
            )
        }
}

object FixedDeposits {
    private val NUMBERED = Regex("""FD #(\d+)""")

    /** "FD #n" with the next free number among the existing deposits. */
    fun nextName(existing: List<Instrument>): String {
        val max = existing.mapNotNull { NUMBERED.find(it.name)?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull() ?: 0
        return "FD #${max + 1}"
    }

    /** Builds the views for every FIXED_DEPOSIT instrument, including closed ones (they keep their history). */
    fun build(
        instruments: List<Instrument>,
        terms: List<FixedDeposit>,
        ledgerWithInstrument: List<TransactionEntity>,
        valuations: List<InstrumentValuation>,
        asOf: LocalDate,
    ): List<FdView> {
        val termsById = terms.associateBy { it.instrumentId }
        val rowsById = ledgerWithInstrument.groupBy { it.instrumentId }
        val latest = valuations.groupBy { it.instrumentId }
            .mapValues { (_, v) -> v.maxWith(compareBy({ it.date }, { it.id })) }
        return instruments.filter { it.instrumentType == InstrumentType.FIXED_DEPOSIT }
            .map { instrument ->
                val rows =
                    rowsById[instrument.id].orEmpty().sortedWith(compareBy({ it.date }, { it.id }))
                val position = Holdings.position(instrument.id, rows, unitBased = false)
                val fd = termsById[instrument.id]
                val depositDate = rows.firstOrNull { it.kind == TransactionKind.INVEST }?.date
                    ?: position.firstDate
                val principal = position.costHeld
                val maturity = fd?.maturityDate ?: depositDate?.let { d ->
                    fd?.let {
                        FdMath.maturityDate(
                            d,
                            it.periodMonths,
                            it.periodDays
                        )
                    }
                }
                val valuation = latest[instrument.id]
                val formula =
                    if (fd != null && depositDate != null && principal > 0) FdMath.estimate(
                        fd,
                        principal,
                        depositDate,
                        asOf
                    ) else null
                val estimated = when {
                    principal <= Position.UNIT_EPSILON -> 0.0
                    valuation?.totalValue != null && (formula == null || !valuation.date.isBefore(
                        asOf.minusDays(7)
                    )) -> valuation.totalValue

                    formula != null -> formula
                    else -> principal
                }
                val maturityAmount = if (fd != null && depositDate != null) FdMath.maturityAmount(
                    fd,
                    principal.takeIf { it > 0 } ?: position.totalInvested,
                    depositDate) else null
                FdView(
                    instrument = instrument,
                    terms = fd,
                    position = position,
                    principal = principal,
                    depositDate = depositDate,
                    maturityDate = maturity,
                    estimated = estimated,
                    maturityAmount = maturityAmount,
                    tdsAtMaturity = maturityAmount?.let {
                        FdMath.tds(it - (principal.takeIf { p -> p > 0 } ?: position.totalInvested),
                            fd?.tdsRate)
                    } ?: 0.0,
                    daysLeft = FdMath.daysLeft(maturity, asOf),
                    rows = rows,
                )
            }
    }
}
