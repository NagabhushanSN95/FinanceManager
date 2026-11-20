package com.nagabhushansn95.financetracker.data.investments

import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

/** The two things that happen on a loan or money parked elsewhere: it goes out of a bank account, or comes back. */
enum class OtherEntryKind(
    val title: String,
    val bankLabel: String,
    val transactionKind: TransactionKind
) {
    OUT("Money out", "Paid from", TransactionKind.INVEST),
    IN("Money in", "Paid into", TransactionKind.REDEEM),
}

/**
 * One "other" instrument with its ledger summed up: what went out, what came back, and what is still outstanding.
 * A loan reads "loaned" and "paid back"; anything else (an account kept for someone, money parked) "put in" and
 * "taken back". [settled] when everything has come back (or the instrument was retired by hand).
 */
data class OtherView(
    val instrument: Instrument,
    val rows: List<TransactionEntity>,
) {
    val putIn: Double = rows.filter { it.kind == TransactionKind.INVEST }.sumOf { it.amount }
    val takenBack: Double = rows.filter { it.kind == TransactionKind.REDEEM }.sumOf { it.amount }
    val outstanding: Double get() = putIn - takenBack
    val firstDate: LocalDate? get() = rows.minOfOrNull { it.date }
    val lastDate: LocalDate? get() = rows.maxOfOrNull { it.date }
    val isLoan: Boolean get() = Others.isLoan(instrument.name)
    val settled: Boolean get() = !instrument.active || (rows.isNotEmpty() && abs(outstanding) < Others.EPSILON)
    val own: Boolean get() = instrument.owner == Instrument.OWNER_SELF

    /** "Loaned" / "Put in". */
    val outLabel: String get() = if (isLoan) "Loaned" else "Put in"

    /** "Paid back" / "Taken back". */
    val inLabel: String get() = if (isLoan) "Paid back" else "Taken back"

    fun rowsOf(kind: OtherEntryKind): List<TransactionEntity> =
        rows.filter { it.kind == kind.transactionKind }
            .sortedWith(compareBy({ it.date }, { it.id }))
}

object Others {
    const val EPSILON = 0.005
    private const val LOAN_PREFIX = "Loan To "

    /** "Loan To Mahadeva Anna" is a loan; the rest is money parked somewhere. */
    fun isLoan(name: String): Boolean = name.trim().startsWith(LOAN_PREFIX, ignoreCase = true)

    /** The person a loan went to, from its name. */
    fun borrower(name: String): String? =
        name.trim().takeIf { isLoan(it) }?.drop(LOAN_PREFIX.length)?.trim()?.ifBlank { null }

    fun build(instruments: List<Instrument>, ledger: List<TransactionEntity>): List<OtherView> {
        val byInstrument = ledger.filter { it.instrumentId != null }.groupBy { it.instrumentId!! }
        return instruments.filter { it.instrumentType == InstrumentType.OTHER }
            .sortedWith(compareBy<Instrument, String>(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.id })
            .map {
                OtherView(
                    it,
                    byInstrument[it.id].orEmpty()
                        .sortedWith(compareBy({ r -> r.date }, { r -> r.id }))
                )
            }
    }

    /** The entry a ledger row is, or null for a row the sheet cannot describe (an income tagged with it, say). */
    fun entryKindOf(row: TransactionEntity): OtherEntryKind? = when (row.kind) {
        TransactionKind.INVEST -> OtherEntryKind.OUT
        TransactionKind.REDEEM -> OtherEntryKind.IN
        else -> null
    }

    /** The entry most likely next: the first is always money out; on an open loan, money coming back. */
    fun likelyNext(view: OtherView): OtherEntryKind =
        if (view.rows.isNotEmpty() && view.outstanding > EPSILON) OtherEntryKind.IN else OtherEntryKind.OUT

    /** 4.x wording: "Loaned To Mahadeva Anna" / "Mahadeva Anna Paid Back"; "Transferred To X" / "Withdrawn From X". */
    fun defaultParticulars(kind: OtherEntryKind, view: OtherView): String {
        val name = view.instrument.name.trim()
        val borrower = borrower(name)
        return when (kind) {
            OtherEntryKind.OUT -> if (borrower != null) "Loaned To $borrower" else "Transferred To $name"
            OtherEntryKind.IN -> if (borrower != null) "$borrower Paid Back" else "Withdrawn From $name"
        }
    }

    /** The house style continues the last entry of the kind on this instrument, else the default. */
    fun suggestedParticulars(kind: OtherEntryKind, view: OtherView): String =
        view.rowsOf(kind).lastOrNull()?.particulars?.takeIf { it.isNotBlank() }
            ?: defaultParticulars(kind, view)

    /** The ledger row for an entry: an investment from the bank, or a redemption into it; never units or a rate. */
    fun toTransaction(
        view: OtherView,
        kind: OtherEntryKind,
        date: LocalDate,
        amount: Double,
        particulars: String,
        bankAccountId: Long,
        existing: TransactionEntity? = null,
        now: LocalDateTime = LocalDateTime.now(),
    ): TransactionEntity = (existing ?: TransactionEntity(
        createdTime = now,
        modifiedTime = now,
        date = date,
        kind = kind.transactionKind,
        particulars = particulars,
        amount = amount,
    )).copy(
        date = date,
        kind = kind.transactionKind,
        fromAccountId = bankAccountId.takeIf { kind == OtherEntryKind.OUT },
        toAccountId = bankAccountId.takeIf { kind == OtherEntryKind.IN },
        categoryId = null,
        instrumentId = view.instrument.id,
        particulars = particulars.trim().ifBlank { defaultParticulars(kind, view) },
        quantity = null,
        rate = null,
        amount = amount,
        modifiedTime = now,
    )
}
