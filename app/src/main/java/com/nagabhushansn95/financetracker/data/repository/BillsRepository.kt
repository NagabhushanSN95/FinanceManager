package com.nagabhushansn95.financetracker.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.nagabhushansn95.financetracker.data.bills.BillPeriod
import com.nagabhushansn95.financetracker.data.bills.Bills
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Bill
import com.nagabhushansn95.financetracker.data.db.entity.BillPayment
import com.nagabhushansn95.financetracker.data.db.entity.BillSchedule
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
        val order = if (bill.displayOrder > 0) bill.displayOrder else dao.maxOrder() + 1
        return dao.insert(
            bill.copy(
                name = bill.name.trim(),
                displayOrder = order,
                createdTime = now,
                modifiedTime = now
            )
        )
    }

    /** The Custom sort: [ids] in the order they should list, first to last. */
    suspend fun saveOrder(ids: List<Long>) {
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                ids.forEachIndexed { i, id ->
                    dao.setOrder(
                        id,
                        i + 1
                    )
                }
            }
        }
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
                // One row may pay several bills (rent and maintenance paid together), but a bill only once.
                val mine = dao.getPayments(bill.id).mapNotNull { it.transactionId }.toHashSet()
                for ((period, transactionId) in links) {
                    requireNotNull(
                        db.transactionDao().getById(transactionId)
                    ) { "That transaction no longer exists" }
                    require(transactionId !in mine) { "That transaction already pays this bill" }
                    mine += transactionId
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

    /** The payment (or skip) now settles [period] instead: a row that was filed under the wrong month. */
    suspend fun changePeriod(paymentId: Long, period: BillPeriod) {
        val payment = dao.getPayment(paymentId)
            ?: throw IllegalArgumentException("That payment no longer exists")
        dao.updatePayment(
            payment.copy(
                periodStart = period.start,
                periodEnd = period.end,
                dueDate = period.due
            )
        )
    }

    /**
     * Validity bill: chains every payment made after this one again, each from its own row's date with the bill's
     * usual validity, queued behind the one before (see [Bills.rechained]). Returns how many moved.
     */
    suspend fun rechainAfter(paymentId: Long): Int {
        val anchor = dao.getPayment(paymentId)
            ?: throw IllegalArgumentException("That payment no longer exists")
        val bill = dao.getById(anchor.billId)
            ?: throw IllegalArgumentException("That bill no longer exists")
        require(bill.schedule == BillSchedule.DURATION) { "Only a validity bill is chained" }
        val rows = dao.getPayments(bill.id).mapNotNull { p ->
            p.transactionId?.let { db.transactionDao().getById(it) }?.let { p to it.date }
        }
        val moved = Bills.rechained(bill, anchor, rows)
        if (moved.isNotEmpty()) db.useWriterConnection { transactor ->
            transactor.immediateTransaction { moved.forEach { dao.updatePayment(it) } }
        }
        return moved.size
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
        require(bill.kind != TransactionKind.INVEST && bill.kind != TransactionKind.REDEEM) { "A bill is an expense, a transfer or an income" }
        require(bill.everyMonths >= 1) { "The months between due dates must be at least one" }
        require(bill.validity == null || bill.validity > 0) { "The validity must be at least one" }
        require(bill.amount == null || bill.amount >= 0) { "The amount cannot be negative" }
        require(bill.kind != TransactionKind.TRANSFER || bill.fromAccountId == null || bill.fromAccountId != bill.toAccountId) { "A transfer needs two different accounts" }
    }
}
