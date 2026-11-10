package com.nagabhushansn95.financetracker.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.entity.Instrument
import com.nagabhushansn95.financetracker.data.db.entity.InstrumentType
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import com.nagabhushansn95.financetracker.data.investments.InvestmentCategories
import com.nagabhushansn95.financetracker.data.investments.OtherEntryKind
import com.nagabhushansn95.financetracker.data.investments.OtherView
import com.nagabhushansn95.financetracker.data.investments.Others
import java.time.LocalDate
import java.time.LocalDateTime

/** Loans and money parked elsewhere (the Others group): the instruments and their money-out / money-in entries. */
class OthersRepository(private val db: AppDatabase) {
    private val transactions = TransactionRepository(db)

    /** A new loan or parked account, filed under the Others category when there is one. */
    suspend fun add(name: String, notes: String?, now: LocalDateTime = LocalDateTime.now()): Long {
        require(name.isNotBlank()) { "A name is needed" }
        val category = db.instrumentDao().getCategoryByName(InvestmentCategories.OTHERS)
        return EditDataRepository(db).addInstrument(
            Instrument(
                name = name.trim(),
                instrumentType = InstrumentType.OTHER,
                categoryId = category?.id,
                displayOrder = 0,
                notes = notes?.trim()?.ifBlank { null },
                createdTime = now,
                modifiedTime = now,
            ), now
        )
    }

    suspend fun update(instrument: Instrument, now: LocalDateTime = LocalDateTime.now()) =
        EditDataRepository(db).updateInstrument(instrument, now)

    /** Records an entry, or rewrites [existing] as the sheet now describes it; the bank balance follows. */
    suspend fun saveEntry(
        view: OtherView,
        kind: OtherEntryKind,
        date: LocalDate,
        amount: Double,
        particulars: String,
        bankAccountId: Long,
        existing: TransactionEntity? = null,
        now: LocalDateTime = LocalDateTime.now(),
    ): Long {
        require(amount > 0) { "The amount must be more than zero" }
        val row = Others.toTransaction(
            view,
            kind,
            date,
            amount,
            particulars,
            bankAccountId,
            existing,
            now
        )
        return if (existing == null) transactions.add(row, now = now)
        else {
            transactions.update(row, now); row.id
        }
    }

    suspend fun deleteEntry(id: Long, now: LocalDateTime = LocalDateTime.now()) =
        transactions.delete(id, now)

    /** Removes the instrument with every entry on it (balances put back) and its valuations, in one write. */
    suspend fun delete(instrumentId: Long, now: LocalDateTime = LocalDateTime.now()) =
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                requireNotNull(db.instrumentDao().getById(instrumentId)) { "Not found" }
                transactions.deleteAllInTransaction(
                    db.transactionDao().getForInstrument(instrumentId), now
                )
                db.instrumentDao().delete(instrumentId)
            }
        }
}
