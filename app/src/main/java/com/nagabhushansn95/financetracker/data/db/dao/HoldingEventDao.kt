package com.nagabhushansn95.financetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nagabhushansn95.financetracker.data.db.entity.HoldingEvent
import kotlinx.coroutines.flow.Flow

/** Bonus and split units, deposit interest credited and TDS deducted: what happened without cash moving. */
@Dao
interface HoldingEventDao {
    @Insert
    suspend fun insert(event: HoldingEvent): Long

    @Insert
    suspend fun insertAll(events: List<HoldingEvent>): List<Long>

    @Update
    suspend fun update(event: HoldingEvent)

    @Query("DELETE FROM holding_events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM holding_events WHERE id = :id")
    suspend fun getById(id: Long): HoldingEvent?

    @Query("SELECT * FROM holding_events ORDER BY date, id")
    suspend fun getAll(): List<HoldingEvent>

    @Query("SELECT * FROM holding_events ORDER BY date, id")
    fun observeAll(): Flow<List<HoldingEvent>>

    @Query("SELECT * FROM holding_events WHERE instrument_id = :instrumentId ORDER BY date, id")
    suspend fun getForInstrument(instrumentId: Long): List<HoldingEvent>

    @Query("UPDATE holding_events SET instrument_id = :into WHERE instrument_id = :from")
    suspend fun reassignInstrument(from: Long, into: Long)
}
