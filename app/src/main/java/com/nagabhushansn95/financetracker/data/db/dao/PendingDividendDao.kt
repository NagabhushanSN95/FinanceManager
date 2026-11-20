package com.nagabhushansn95.financetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nagabhushansn95.financetracker.data.db.entity.PendingDividend
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingDividendDao {
    @Insert
    suspend fun insertAll(rows: List<PendingDividend>): List<Long>

    @Query("SELECT * FROM pending_dividends ORDER BY ex_date, id")
    fun observeAll(): Flow<List<PendingDividend>>

    @Query("SELECT * FROM pending_dividends ORDER BY ex_date, id")
    suspend fun getAll(): List<PendingDividend>

    @Query("SELECT * FROM pending_dividends WHERE id = :id")
    suspend fun getById(id: Long): PendingDividend?

    @Query("SELECT EXISTS(SELECT 1 FROM pending_dividends WHERE broker_ref = :brokerRef)")
    suspend fun existsByBrokerRef(brokerRef: String): Boolean

    @Query("DELETE FROM pending_dividends WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE pending_dividends SET instrument_id = :into WHERE instrument_id = :from")
    suspend fun reassignInstrument(from: Long, into: Long)
}
