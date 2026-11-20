package com.nagabhushansn95.financetracker.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.InsurancePolicy
import com.nagabhushansn95.financetracker.data.db.entity.InsuranceType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.ValidityKind
import com.nagabhushansn95.financetracker.data.db.entity.ValidityPeriod
import com.nagabhushansn95.financetracker.data.db.entity.Vehicle
import com.nagabhushansn95.financetracker.data.investments.Insurance
import com.nagabhushansn95.financetracker.data.investments.InsuranceEntry
import com.nagabhushansn95.financetracker.data.investments.InsuranceEntryKind
import com.nagabhushansn95.financetracker.data.investments.PolicyView
import com.nagabhushansn95.financetracker.data.investments.Validity
import java.time.LocalDate
import java.time.LocalDateTime

/** Writes for policies and vehicles: the policy with its instrument, and entries with the validity they buy. */
class InsuranceRepository(private val db: AppDatabase) {
    private val transactions = TransactionRepository(db)

    /**
     * Adds or updates a policy (or a vehicle) with its instrument in one write and sets the validity the sheet
     * entered by hand ([manual], by kind: null clears a hand-set period; a value equal to the one in force changes
     * nothing). A policy comes with its [policy] terms, a vehicle with its [vehicle] row. Returns the instrument id.
     */
    suspend fun savePolicy(
        instrument: Instrument,
        policy: InsurancePolicy?,
        manual: Map<ValidityKind, LocalDate?> = emptyMap(),
        now: LocalDateTime = LocalDateTime.now(),
        vehicle: Vehicle? = null,
    ): Long = db.useWriterConnection { transactor ->
        transactor.immediateTransaction {
            require((vehicle != null) == (instrument.instrumentType == InstrumentType.VEHICLE)) { "A vehicle needs its vehicle row" }
            require(vehicle != null || policy != null) { "A policy needs its terms" }
            val id = if (instrument.id == 0L) db.instrumentDao().insert(
                instrument.copy(createdTime = now, modifiedTime = now)
            ) else {
                db.instrumentDao().update(instrument.copy(modifiedTime = now)); instrument.id
            }
            if (vehicle != null) db.vehicleDao().upsertVehicle(vehicle.copy(instrumentId = id))
            else db.instrumentDao().upsertInsurancePolicy(policy!!.copy(instrumentId = id))
            val periods = Validity.getFor(db, id)
            for ((kind, end) in manual) {
                val ofKind = periods.filter { it.kind == kind }
                val inForce = ofKind.maxWithOrNull(compareBy({ it.endDate }, { it.id }))?.endDate
                val byHand = ofKind.filter { it.transactionId == null }
                when {
                    end == null -> byHand.forEach { Validity.delete(db, it) }
                    end == inForce -> Unit
                    byHand.isNotEmpty() -> Validity.update(db, byHand.last().copy(endDate = end))
                    else -> Validity.insert(
                        db,
                        ValidityPeriod(
                            instrumentId = id,
                            kind = kind,
                            startDate = policy?.startDate,
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
            Validity.deleteOf(db, id)
            Insurance.toPeriods(view, entry, id).forEach { Validity.insert(db, it) }
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
        val periods = db.vehicleDao().getCovers(instrumentId).map { it.toPeriod() }
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
                Validity.getFor(db, instrumentId).forEach { Validity.delete(db, it) }
                db.instrumentDao().delete(instrumentId)
            }
        }
}
