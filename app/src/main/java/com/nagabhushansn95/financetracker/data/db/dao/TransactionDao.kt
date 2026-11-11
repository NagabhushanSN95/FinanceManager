package com.nagabhushansn95.financetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nagabhushansn95.financetracker.data.db.entity.Template
import com.nagabhushansn95.financetracker.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("SELECT * FROM transactions ORDER BY date, id")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    suspend fun getBetween(from: String, to: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE instrument_id = :instrumentId ORDER BY date, id")
    suspend fun getForInstrument(instrumentId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE instrument_id IS NOT NULL ORDER BY date, id")
    suspend fun getAllWithInstrument(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE instrument_id IS NOT NULL ORDER BY date, id")
    fun observeWithInstrument(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE from_account_id = :accountId OR to_account_id = :accountId ORDER BY date DESC, id DESC")
    suspend fun getForAccount(accountId: Long): List<TransactionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE broker_ref = :brokerRef)")
    suspend fun existsByBrokerRef(brokerRef: String): Boolean

    @Query("UPDATE transactions SET broker_ref = :brokerRef WHERE id = :id")
    suspend fun setBrokerRef(id: Long, brokerRef: String)

    @Query("UPDATE transactions SET instrument_id = :into, modified_time = :modifiedTime WHERE instrument_id = :from")
    suspend fun reassignInstrument(from: Long, into: Long, modifiedTime: String)

    @Query("UPDATE templates SET instrument_id = :into WHERE instrument_id = :from")
    suspend fun reassignTemplateInstrument(from: Long, into: Long)

    // --- templates
    @Insert
    suspend fun insertTemplate(template: Template): Long

    @Update
    suspend fun updateTemplate(template: Template)

    @Delete
    suspend fun deleteTemplate(template: Template)

    @Query("DELETE FROM templates WHERE kind IN ('INVEST', 'REDEEM')")
    suspend fun deleteInvestmentTemplates(): Int

    @Query("SELECT * FROM templates WHERE hidden = 0 ORDER BY particulars")
    suspend fun getVisibleTemplates(): List<Template>

    @Query("SELECT * FROM templates ORDER BY particulars")
    suspend fun getAllTemplates(): List<Template>

    @Query("SELECT * FROM templates ORDER BY kind, particulars")
    fun observeTemplates(): Flow<List<Template>>
}
