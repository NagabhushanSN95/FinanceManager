package com.nagabhushansn95.financetracker.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.nagabhushansn95.financetracker.data.bills.BillPeriod
import com.nagabhushansn95.financetracker.data.bills.Bills
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Bill
import com.nagabhushansn95.financetracker.data.db.entity.BillPayment
import com.nagabhushansn95.financetracker.data.db.entity.CreditAgency
import com.nagabhushansn95.financetracker.data.db.entity.CreditScore
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Bills and their payments. A payment is a ledger row written through [TransactionRepository] (balances follow) and
 * a [BillPayment] linking it to the period it settles, both in one write. Credit agencies and scores live here too.
 */
class BillsRepository(private val db: AppDatabase) {
    private val transactions = TransactionRepository(db)
    private val dao get() = db.billDao()

    suspend fun add(bill: Bill, now: LocalDateTime = LocalDateTime.now()): Long {
        validate(bill)
        return dao.insert(bill.copy(name = bill.name.trim(), createdTime = now, modifiedTime = now))
    }

    suspend fun update(bill: Bill, now: LocalDateTime = LocalDateTime.now()) {
        validate(bill)
        val old =
            dao.getById(bill.id) ?: throw IllegalArgumentException("Bill ${bill.id} does not exist")
        dao.update(
            bill.copy(
                name = bill.name.trim(),
                createdTime = old.createdTime,
                modifiedTime = now
            )
        )
    }

    /** The bill stops on [on]; periods due after that are not owed. The history stays. */
    suspend fun end(id: Long, on: LocalDate, now: LocalDateTime = LocalDateTime.now()) {
        val bill = dao.getById(id) ?: throw IllegalArgumentException("Bill $id does not exist")
        dao.update(bill.copy(endedOn = on, modifiedTime = now))
    }

    suspend fun reopen(id: Long, now: LocalDateTime = LocalDateTime.now()) {
        val bill = dao.getById(id) ?: throw IllegalArgumentException("Bill $id does not exist")
        dao.update(bill.copy(endedOn = null, modifiedTime = now))
    }

    /** Only a bill with no history goes; one that was paid is ended instead, so the ledger keeps its meaning. */
    suspend fun delete(id: Long) {
        require(dao.paymentCount(id) == 0) { "This bill has payments. End it instead; the history stays." }
        dao.delete(id)
    }

    /** Writes the ledger row and marks [period] of [bill] settled by it, in one write. Returns the row's id. */
    suspend fun recordPayment(
        bill: Bill,
        period: BillPeriod,
        row: TransactionEntity,
        saveAsTemplate: Boolean = false,
        now: LocalDateTime = LocalDateTime.now(),
    ): Long = db.useWriterConnection { transactor ->
        transactor.immediateTransaction {
            val id = transactions.addAllInTransaction(listOf(row), now).single()
            if (saveAsTemplate) transactions.saveTemplate(row.copy(id = id))
            dao.insertPayment(
                BillPayment(
                    billId = bill.id,
                    periodStart = period.start,
                    periodEnd = period.end,
                    dueDate = period.due,
                    transactionId = id,
                )
            )
            id
        }
    }

    /** The period is settled without a ledger row: nothing was paid, by choice. */
    suspend fun skip(billId: Long, period: BillPeriod, note: String? = null): Long =
        dao.insertPayment(
            BillPayment(
                billId = billId,
                periodStart = period.start,
                periodEnd = period.end,
                dueDate = period.due,
                note = note?.trim()?.ifBlank { null },
            )
        )

    /** Several periods settled with nothing paid, in one write. */
    suspend fun skipAll(billId: Long, periods: List<BillPeriod>, note: String? = null) {
        if (periods.isEmpty()) return
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction { periods.forEach { skip(billId, it, note) } }
        }
    }

    /** A row already in the ledger settles the period. */
    suspend fun link(
        billId: Long,
        period: BillPeriod,
        transactionId: Long,
        now: LocalDateTime = LocalDateTime.now()
    ) {
        val bill =
            dao.getById(billId) ?: throw IllegalArgumentException("Bill $billId does not exist")
        linkAll(bill, listOf(period to transactionId), now)
    }

    /**
     * Rows already in the ledger settle their periods, all in one write. A period before the bill's first moves the
     * start of its history back, so a backfill needs no guess at when the bill began.
     */
    suspend fun linkAll(
        bill: Bill,
        links: List<Pair<BillPeriod, Long>>,
        now: LocalDateTime = LocalDateTime.now()
    ) {
        if (links.isEmpty()) return
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                var moved = bill
                for ((period, transactionId) in links) {
                    requireNotNull(
                        db.transactionDao().getById(transactionId)
                    ) { "That transaction no longer exists" }
                    require(dao.getPaymentOf(transactionId) == null) { "That transaction already pays a bill" }
                    dao.insertPayment(
                        BillPayment(
                            billId = bill.id,
                            periodStart = period.start,
                            periodEnd = period.end,
                            dueDate = period.due,
                            transactionId = transactionId,
                        )
                    )
                    moved = Bills.startingFrom(moved, period)
                }
                if (moved.firstDue != bill.firstDue) dao.update(moved.copy(modifiedTime = now))
            }
        }
    }

    /** Removes the link (or the skip); a ledger row behind it stays as an ordinary transaction. */
    suspend fun unlink(paymentId: Long) = dao.deletePayment(paymentId)

    /** Removes several links (or skips) in one write; the ledger rows stay. */
    suspend fun unlinkAll(paymentIds: Collection<Long>) {
        if (paymentIds.isEmpty()) return
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction { paymentIds.forEach { dao.deletePayment(it) } }
        }
    }

    /** Removes the payment with its ledger row (balance put back); a skip just goes. */
    suspend fun deletePayment(paymentId: Long, now: LocalDateTime = LocalDateTime.now()) {
        val payment = dao.getPayment(paymentId) ?: return
        val transactionId = payment.transactionId
        if (transactionId != null) transactions.delete(
            transactionId,
            now
        ) // cascades to the payment row
        else dao.deletePayment(paymentId)
    }

    // --- credit scores

    suspend fun addAgency(name: String, notes: String? = null): Long {
        require(name.isNotBlank()) { "A name is needed" }
        return dao.insertAgency(
            CreditAgency(
                name = name.trim(),
                displayOrder = dao.maxAgencyOrder() + 1,
                notes = notes?.trim()?.ifBlank { null })
        )
    }

    suspend fun updateAgency(agency: CreditAgency) {
        require(agency.name.isNotBlank()) { "A name is needed" }
        dao.updateAgency(agency.copy(name = agency.name.trim()))
    }

    /** The agency goes with every score recorded under it. */
    suspend fun deleteAgency(id: Long) = dao.deleteAgency(id)

    suspend fun addScore(agencyId: Long, date: LocalDate, score: Int, notes: String? = null): Long {
        require(score in 1..2000) { "Enter the score" }
        return dao.insertScore(
            CreditScore(
                agencyId = agencyId,
                date = date,
                score = score,
                notes = notes?.trim()?.ifBlank { null })
        )
    }

    suspend fun updateScore(score: CreditScore) {
        require(score.score in 1..2000) { "Enter the score" }
        dao.updateScore(score.copy(notes = score.notes?.trim()?.ifBlank { null }))
    }

    suspend fun deleteScore(id: Long) = dao.deleteScore(id)

    private fun validate(bill: Bill) {
        require(bill.name.isNotBlank()) { "A name is needed" }
        require(bill.kind == TransactionKind.EXPENSE || bill.kind == TransactionKind.TRANSFER) { "A bill is paid as an expense or a transfer" }
        require(bill.everyMonths >= 1) { "The months between due dates must be at least one" }
        require(bill.validityDays == null || bill.validityDays > 0) { "The validity must be at least a day" }
        require(bill.amount == null || bill.amount >= 0) { "The amount cannot be negative" }
        require(bill.kind != TransactionKind.TRANSFER || bill.fromAccountId == null || bill.fromAccountId != bill.toAccountId) { "A transfer needs two different accounts" }
    }
}
