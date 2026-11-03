package com.nagabhushansn95.financetracker.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nagabhushansn95.financetracker.data.db.entity.Setting
import kotlinx.coroutines.flow.Flow

/** Key-value settings stored in the database (so backups carry them). Keys live in [Settings]. */
@Dao
interface SettingsDao {
    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM settings WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Upsert
    suspend fun set(setting: Setting)

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun remove(key: String)
}

/** The keys of the settings table. */
object Settings {
    /** The corpus to keep for the goal (the sheet's "Amount needed for Goal"), in INR. */
    const val GOAL_AMOUNT = "investments.goal_amount"
}
