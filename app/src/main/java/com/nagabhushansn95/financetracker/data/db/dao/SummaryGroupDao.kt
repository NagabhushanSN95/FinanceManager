package com.nagabhushansn95.financetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nagabhushansn95.financetracker.data.db.entity.SummaryGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryGroupDao {
    @Insert
    suspend fun insert(group: SummaryGroup): Long

    @Update
    suspend fun update(group: SummaryGroup)

    @Query("DELETE FROM summary_groups WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM summary_groups ORDER BY display_order, id")
    suspend fun getAll(): List<SummaryGroup>

    @Query("SELECT * FROM summary_groups ORDER BY display_order, id")
    fun observeAll(): Flow<List<SummaryGroup>>

    @Query("SELECT COUNT(*) FROM summary_groups")
    suspend fun count(): Int

    @Query("UPDATE summary_groups SET display_order = :order WHERE id = :id")
    suspend fun setDisplayOrder(id: Long, order: Int)
}
