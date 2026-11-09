package com.nagabhushansn95.financetracker.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InsurancePolicy
import com.nagabhushansn95.financetracker.data.db.entity.InsuranceType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.ValidityKind
import com.nagabhushansn95.financetracker.data.db.entity.ValidityPeriod
import com.nagabhushansn95.financetracker.data.investments.Insurance
import com.nagabhushansn95.financetracker.data.investments.InsuranceEntry
import com.nagabhushansn95.financetracker.data.investments.InsuranceEntryKind
import com.nagabhushansn95.financetracker.data.investments.PolicyView
import java.time.LocalDate
import java.time.LocalDateTime

/** Writes for policies and vehicles: the policy with its instrument, and entries with the validity they buy. */
class InsuranceRepository(private val db: AppDatabase) {
    private val transactions = TransactionRepository(db)

    /**
     * Adds or updates a policy with its instrument in one write and sets the validity the sheet entered by hand
     * ([manual], by kind: null clears a hand-set period; a value equal to the one in force changes nothing).
     * Returns the instrument id.
     */
    suspend fun savePolicy(
        instrument: Instrument,
        policy: InsurancePolicy,
        manual: Map<ValidityKind, LocalDate?> = emptyMap(),
        now: LocalDateTime = LocalDateTime.now(),
    ): Long = db.useWriterConnection { transactor ->
        transactor.immediateTransaction {
            val id = if (instrument.id == 0L) db.instrumentDao().insert(
                instrument.copy(
                    displayOrder = db.instrumentDao().maxDisplayOrder() + 1,
                    createdTime = now,
                    modifiedTime = now
                )
            ) else {
                db.instrumentDao().update(instrument.copy(modifiedTime = now)); instrument.id
            }
            db.instrumentDao().upsertInsurancePolicy(policy.copy(instrumentId = id))
            val periods = db.instrumentDao().getValidityPeriods(id)
            for ((kind, end) in manual) {
                val ofKind = periods.filter { it.kind == kind }
                val inForce = ofKind.maxWithOrNull(compareBy({ it.endDate }, { it.id }))?.endDate
                val byHand = ofKind.filter { it.transactionId == null }
                when {
                    end == null -> byHand.forEach { db.instrumentDao().deleteValidityPeriod(it.id) }
                    end == inForce -> Unit
                    byHand.isNotEmpty() -> db.instrumentDao()
                        .updateValidityPeriod(byHand.last().copy(endDate = end))

                    else -> db.instrumentDao().insertValidityPeriod(
                        ValidityPeriod(
                            instrumentId = id,
                            kind = kind,
                            startDate = policy.startDate,
                            endDate = end
                        )
                    )
                }
            }
            id
        }
    }

    /**
     * Records an entry (or rewrites [existing]) with the validity it buys, in one write: the ledger row with its
     * balance effect, then the validity rows tied to it (the old ones replaced). Returns the transaction id.
     */
    suspend fun saveEntry(
        view: PolicyView,
        entry: InsuranceEntry,
        categoryId: Long?,
        existing: TransactionEntity? = null,
        now: LocalDateTime = LocalDateTime.now(),
    ): Long = db.useWriterConnection { transactor ->
        transactor.immediateTransaction {
            val row = Insurance.toTransaction(view, entry, categoryId, existing, now)
            val id =
                if (existing == null) transactions.addAllInTransaction(listOf(row), now).single()
                else {
                    transactions.updateInTransaction(row, now); row.id
                }
            db.instrumentDao().deleteValidityPeriodsOf(id)
            Insurance.toPeriods(view, entry, id)
                .forEach { db.instrumentDao().insertValidityPeriod(it) }
            if (view.type == InsuranceType.HEALTH && entry.kind == InsuranceEntryKind.PREMIUM) refreshHealthPolicy(
                view.instrument.id
            )
            id
        }
    }

    /**
     * A health policy's row shows the terms in force: after a premium is recorded or changed, its sum insured, policy
     * number and premium amount follow the cover period with the furthest end (when that period says them).
     */
    private suspend fun refreshHealthPolicy(instrumentId: Long) {
        val policy = db.instrumentDao().getInsurancePolicy(instrumentId) ?: return
        val periods = db.instrumentDao().getValidityPeriods(instrumentId)
            .filter { it.kind == ValidityKind.COVER }
        val current = periods.maxWithOrNull(compareBy({ it.endDate }, { it.id })) ?: return
        val premium = current.transactionId?.let { db.transactionDao().getById(it) }
        val updated = policy.copy(
            sumAssured = current.sumInsured ?: policy.sumAssured,
            policyNumber = current.reference ?: policy.policyNumber,
            provider = current.provider ?: policy.provider,
            premiumAmount = premium?.amount ?: policy.premiumAmount,
        )
        if (updated != policy) db.instrumentDao().upsertInsurancePolicy(updated)
    }

    /** Deletes an entry; the validity it bought goes with it (foreign key). */
    suspend fun deleteEntry(id: Long, now: LocalDateTime = LocalDateTime.now()) =
        transactions.delete(id, now)

    /**
     * Removes a policy or vehicle added by mistake: every entry on it (with the account balances put back), its
     * validity rows and terms, and the instrument, in one write.
     */
    suspend fun deletePolicy(instrumentId: Long, now: LocalDateTime = LocalDateTime.now()) =
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                requireNotNull(db.instrumentDao().getById(instrumentId)) { "Policy not found" }
                transactions.deleteAllInTransaction(
                    db.transactionDao().getForInstrument(instrumentId), now
                )
                db.instrumentDao().getValidityPeriods(instrumentId)
                    .forEach { db.instrumentDao().deleteValidityPeriod(it.id) }
                db.instrumentDao().delete(instrumentId)
            }
        }
}
