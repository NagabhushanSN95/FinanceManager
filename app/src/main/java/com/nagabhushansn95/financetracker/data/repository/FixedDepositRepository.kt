package com.nagabhushansn95.financetracker.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.FdStatus
import com.nagabhushansn95.financetracker.data.db.entity.FixedDeposit
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import com.nagabhushansn95.financetracker.data.investments.FdMath
import java.time.LocalDate
import java.time.LocalDateTime

/** What "New fixed deposit" collects: the instrument, its terms and the deposit itself. */
data class NewFixedDeposit(
    val name: String,
    val amount: Double,
    val date: LocalDate,
    /** Bank account the money left, or null when it did not come from an account of ours (family deposits). */
    val fromAccountId: Long?,
    val terms: FixedDeposit,
    val owner: String = Instrument.OWNER_SELF,
    val categoryId: Long? = null,
    val notes: String? = null,
)

/**
 * Fixed deposits: open, edit terms, mature or close (REDEEM to a bank), and renew (REDEEM with no account plus an
 * INVEST from no account, the two deposits linked through previous/next). Every action is one write transaction.
 */
class FixedDepositRepository(private val db: AppDatabase) {
    private val transactions = TransactionRepository(db)

    suspend fun create(new: NewFixedDeposit, now: LocalDateTime = LocalDateTime.now()): Long =
        write {
            val id = insertInstrument(new.name, new.owner, new.categoryId, new.notes, now)
            val terms =
                completeTerms(new.terms.copy(instrumentId = id, status = FdStatus.ACTIVE), new.date)
            db.instrumentDao().upsertFixedDeposit(terms)
            transactions.addAllInTransaction(
                listOf(
                    TransactionEntity(
                        createdTime = now,
                        modifiedTime = now,
                        date = new.date,
                        kind = TransactionKind.INVEST,
                        fromAccountId = new.fromAccountId,
                        instrumentId = id,
                        amount = new.amount,
                        particulars = openedParticulars(terms),
                    )
                ), now
            )
            id
        }

    /** Edits the instrument and its terms together; the maturity date is derived from the period when left empty. */
    suspend fun saveTerms(
        instrument: Instrument,
        terms: FixedDeposit,
        depositDate: LocalDate?,
        now: LocalDateTime = LocalDateTime.now()
    ) = write {
        db.instrumentDao().update(instrument.copy(modifiedTime = now))
        db.instrumentDao().upsertFixedDeposit(
            completeTerms(
                terms.copy(instrumentId = instrument.id),
                depositDate
            )
        )
    }

    /**
     * The deposit was paid out: one REDEEM of [amountCredited] into [toAccountId] (null when nothing reached an
     * account of ours), status [status], instrument retired. The interest is the ledger's realised profit.
     */
    suspend fun close(
        instrumentId: Long,
        date: LocalDate,
        amountCredited: Double,
        toAccountId: Long?,
        status: FdStatus,
        tdsReported: Double? = null,
        now: LocalDateTime = LocalDateTime.now(),
    ) = write {
        require(status == FdStatus.CLOSED_AT_MATURITY || status == FdStatus.CLOSED_PREMATURELY) { "Use renew() for renewals" }
        val instrument =
            requireNotNull(db.instrumentDao().getById(instrumentId)) { "Deposit not found" }
        val terms = db.instrumentDao().getFixedDeposit(instrumentId)
            ?: FixedDeposit(instrumentId = instrumentId)
        transactions.addAllInTransaction(
            listOf(
                TransactionEntity(
                    createdTime = now,
                    modifiedTime = now,
                    date = date,
                    kind = TransactionKind.REDEEM,
                    toAccountId = toAccountId,
                    instrumentId = instrumentId,
                    amount = amountCredited,
                    particulars = if (status == FdStatus.CLOSED_AT_MATURITY) "Fixed deposit matured" else "Fixed deposit closed before maturity",
                )
            ), now
        )
        db.instrumentDao().upsertFixedDeposit(
            terms.copy(
                status = status,
                tdsReported = tdsReported ?: terms.tdsReported
            )
        )
        db.instrumentDao().update(instrument.copy(active = false, modifiedTime = now))
    }

    /**
     * Renews [oldId] into a new deposit on [date]: the matured amount leaves the old deposit, [renewedAmount] of it
     * goes straight into the new one (no account touched) and any remainder is credited to [remainderToAccountId].
     * Returns the new instrument id. The new terms inherit the bank details that are left blank.
     */
    suspend fun renew(
        oldId: Long,
        date: LocalDate,
        maturedAmount: Double,
        renewedAmount: Double,
        remainderToAccountId: Long?,
        newName: String,
        newTerms: FixedDeposit,
        now: LocalDateTime = LocalDateTime.now(),
    ): Long = write {
        require(renewedAmount > 0) { "Renewed amount must be positive" }
        require(renewedAmount <= maturedAmount + 0.005) { "Cannot renew more than the matured amount" }
        val old = requireNotNull(db.instrumentDao().getById(oldId)) { "Deposit not found" }
        val oldTerms =
            db.instrumentDao().getFixedDeposit(oldId) ?: FixedDeposit(instrumentId = oldId)
        val newId = insertInstrument(newName, old.owner, old.categoryId, null, now)
        val terms = completeTerms(
            newTerms.copy(
                instrumentId = newId,
                status = FdStatus.ACTIVE,
                previousFdId = oldId,
                nextFdId = null,
                holderName = newTerms.holderName ?: oldTerms.holderName,
                bankName = newTerms.bankName ?: oldTerms.bankName,
                branch = newTerms.branch ?: oldTerms.branch,
                bankAccountNo = newTerms.bankAccountNo ?: oldTerms.bankAccountNo,
                custId = newTerms.custId ?: oldTerms.custId,
                tdsRate = newTerms.tdsRate ?: oldTerms.tdsRate,
            ), date
        )
        db.instrumentDao().upsertFixedDeposit(terms)
        val remainder = maturedAmount - renewedAmount
        val rows = ArrayList<TransactionEntity>()
        rows += TransactionEntity(
            createdTime = now, modifiedTime = now, date = date, kind = TransactionKind.REDEEM,
            instrumentId = oldId, amount = renewedAmount, particulars = "Renewed into $newName",
        )
        if (remainder > 0.005) rows += TransactionEntity(
            createdTime = now, modifiedTime = now, date = date, kind = TransactionKind.REDEEM,
            toAccountId = remainderToAccountId, instrumentId = oldId, amount = remainder,
            particulars = "Fixed deposit matured, balance paid out",
        )
        rows += TransactionEntity(
            createdTime = now,
            modifiedTime = now,
            date = date,
            kind = TransactionKind.INVEST,
            instrumentId = newId,
            amount = renewedAmount,
            particulars = "Renewed from ${old.name} · " + openedParticulars(terms).removePrefix("Fixed deposit opened · "),
        )
        transactions.addAllInTransaction(rows, now)
        db.instrumentDao()
            .upsertFixedDeposit(oldTerms.copy(status = FdStatus.RENEWED, nextFdId = newId))
        db.instrumentDao().update(old.copy(active = false, modifiedTime = now))
        newId
    }

    private suspend fun insertInstrument(
        name: String,
        owner: String,
        categoryId: Long?,
        notes: String?,
        now: LocalDateTime
    ): Long =
        db.instrumentDao().insert(
            Instrument(
                name = name.trim(),
                instrumentType = InstrumentType.FIXED_DEPOSIT,
                categoryId = categoryId,
                owner = owner,
                displayOrder = db.instrumentDao().maxDisplayOrder() + 1,
                notes = notes?.takeIf { it.isNotBlank() },
                createdTime = now,
                modifiedTime = now,
            )
        )

    private fun completeTerms(terms: FixedDeposit, depositDate: LocalDate?): FixedDeposit =
        terms.copy(
            compoundingMonths = terms.compoundingMonths ?: FdMath.DEFAULT_COMPOUNDING_MONTHS,
            maturityDate = terms.maturityDate ?: depositDate?.let {
                FdMath.maturityDate(
                    it,
                    terms.periodMonths,
                    terms.periodDays
                )
            },
        )

    private fun openedParticulars(terms: FixedDeposit): String = listOfNotNull(
        terms.bankName,
        FdMath.periodLabel(terms.periodMonths, terms.periodDays),
        terms.rate?.let { "@ ${FdMath.plain(it)}%" },
    ).joinToString(" ")
        .let { if (it.isEmpty()) "Fixed deposit opened" else "Fixed deposit opened · $it" }

    private suspend fun <T> write(block: suspend () -> T): T =
        db.useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
}
