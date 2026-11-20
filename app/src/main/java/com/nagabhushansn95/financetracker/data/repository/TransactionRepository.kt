package com.nagabhushansn95.financetracker.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.Converters
import com.nagabhushansn95.financetracker.data.db.entity.Template
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.db.entity.TransactionKind
import java.time.LocalDateTime

/**
 * Writes to the ledger together with their effect on account balances (plan 3.3):
 * INCOME adds to `to`; EXPENSE subtracts from `from`; TRANSFER both; INVEST subtracts from `from`; REDEEM adds to
 * `to`. A NULL account side changes no balance. Every write runs in one database transaction.
 */
class TransactionRepository(private val db: AppDatabase) {

    suspend fun add(
        transaction: TransactionEntity,
        saveAsTemplate: Boolean = false,
        now: LocalDateTime = LocalDateTime.now()
    ): Long =
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                validate(transaction)
                val stamped = transaction.copy(createdTime = now, modifiedTime = now)
                val id = db.transactionDao().insert(stamped)
                applyBalances(stamped, sign = +1, stamp = now)
                reactivateIfNeeded(stamped, now)
                if (saveAsTemplate) saveTemplate(stamped)
                id
            }
        }

    /** Several rows in one transaction (imports). */
    suspend fun addAll(
        transactions: List<TransactionEntity>,
        now: LocalDateTime = LocalDateTime.now()
    ): List<Long> =
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                addAllInTransaction(
                    transactions,
                    now
                )
            }
        }

    /** For callers that already hold a write transaction: inserts with balance effects, no transaction of its own. */
    suspend fun addAllInTransaction(
        transactions: List<TransactionEntity>,
        now: LocalDateTime
    ): List<Long> =
        transactions.map { transaction ->
            validate(transaction)
            val stamped = transaction.copy(createdTime = now, modifiedTime = now)
            val id = db.transactionDao().insert(stamped)
            applyBalances(stamped, sign = +1, stamp = now)
            reactivateIfNeeded(stamped, now)
            id
        }

    /** For callers that already hold a write transaction: deletes with balance effects, no transaction of its own. */
    suspend fun deleteAllInTransaction(transactions: List<TransactionEntity>, now: LocalDateTime) {
        transactions.forEach {
            applyBalances(it, sign = -1, stamp = now)
            db.transactionDao().delete(it)
        }
    }

    /** Reverses the stored row's balance effect, then applies the new one; optionally saves the row as a template too. */
    suspend fun update(
        transaction: TransactionEntity,
        saveAsTemplate: Boolean = false,
        now: LocalDateTime = LocalDateTime.now()
    ) =
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                updateInTransaction(transaction, now)
                if (saveAsTemplate) saveTemplate(transaction)
            }
        }

    /** For callers that already hold a write transaction: [update] without a transaction of its own. */
    suspend fun updateInTransaction(transaction: TransactionEntity, now: LocalDateTime) {
        validate(transaction)
        val old = db.transactionDao().getById(transaction.id)
            ?: throw IllegalArgumentException("Transaction ${transaction.id} does not exist")
        applyBalances(old, sign = -1, stamp = now)
        val stamped = transaction.copy(createdTime = old.createdTime, modifiedTime = now)
        db.transactionDao().update(stamped)
        applyBalances(stamped, sign = +1, stamp = now)
    }

    suspend fun delete(id: Long, now: LocalDateTime = LocalDateTime.now()) =
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val old = db.transactionDao().getById(id) ?: return@immediateTransaction
                applyBalances(old, sign = -1, stamp = now)
                db.transactionDao().delete(old)
            }
        }

    /** An instrument retired automatically (sold out) comes back when something is bought or credited into it again. */
    private suspend fun reactivateIfNeeded(t: TransactionEntity, now: LocalDateTime) {
        val id = t.instrumentId ?: return
        if (t.kind != TransactionKind.INVEST && t.kind != TransactionKind.INCOME) return
        val instrument = db.instrumentDao().getById(id) ?: return
        if (!instrument.active) db.instrumentDao()
            .setActive(id, true, Converters().localDateTimeToString(now)!!)
    }

    private fun validate(t: TransactionEntity) {
        require(t.amount >= 0) { "Amount cannot be negative" }
        require(t.kind == TransactionKind.TRANSFER || t.toAmount == null) { "Only a transfer has a second amount" }
        when (t.kind) {
            TransactionKind.INCOME -> require(t.fromAccountId == null) { "Income has no source account" }
            TransactionKind.EXPENSE -> {
                require(t.toAccountId == null) { "Expense has no destination account" }
                require(t.fromAccountId != null) { "Expense needs an account" }
                require(t.categoryId != null) { "Expense needs a category" }
            }

            TransactionKind.TRANSFER -> {
                require(t.fromAccountId != null && t.toAccountId != null) { "Transfer needs both accounts" }
                require(t.fromAccountId != t.toAccountId) { "Transfer needs two different accounts" }
                require(t.toAmount == null || t.toAmount >= 0) { "The amount arriving cannot be negative" }
            }

            TransactionKind.INVEST -> {
                require(t.instrumentId != null) { "Investment needs an instrument" }
                require(t.toAccountId == null) { "Investment has no destination account" }
            }

            TransactionKind.REDEEM -> {
                require(t.instrumentId != null) { "Redemption needs an instrument" }
                require(t.fromAccountId == null) { "Redemption has no source account" }
            }
        }
    }

    private suspend fun applyBalances(t: TransactionEntity, sign: Int, stamp: LocalDateTime) {
        val dao = db.accountDao()
        val stampText = Converters().localDateTimeToString(stamp)!!
        // Balances are kept in each account's own currency: what left the from-account, what arrived in the to-account.
        t.fromAccountId?.let { dao.adjustBalance(it, -t.amount * sign, stampText) }
        t.toAccountId?.let { dao.adjustBalance(it, +t.arriving * sign, stampText) }
    }

    /**
     * The row as a template. The template of the same kind and particulars (ignoring case) is replaced, keeping its
     * place and whether it is hidden, so that picking a template, changing its figures and saving with the box
     * ticked updates it rather than adding a twin.
     */
    private suspend fun saveTemplate(t: TransactionEntity) {
        val dao = db.transactionDao()
        val fresh = t.toTemplate()
        val existing = dao.findTemplate(t.kind, t.particulars)
        if (existing == null) dao.insertTemplate(fresh)
        else dao.updateTemplate(fresh.copy(id = existing.id, hidden = existing.hidden))
    }

    /** A rate alone when the row is one unit: a template never forces a quantity of one over what is typed. */
    private fun TransactionEntity.toTemplate() = Template(
        particulars = particulars,
        kind = kind,
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        rate = rate ?: amount,
        quantity = quantity?.takeIf { it != 1.0 },
    )
}
