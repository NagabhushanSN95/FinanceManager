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

    @Upsert
    suspend fun setAll(settings: List<Setting>)

    @Query("SELECT * FROM settings WHERE `key` LIKE :prefix || '%'")
    suspend fun getWithPrefix(prefix: String): List<Setting>

    @Query("DELETE FROM settings WHERE `key` LIKE :prefix || '%'")
    suspend fun removeWithPrefix(prefix: String)
}

/** The keys of the settings table. */
object Settings {
    /** The corpus to keep for the goal (the sheet's "Amount needed for Goal"), in INR. */
    const val GOAL_AMOUNT = "data.goal_amount"

    /**
     * The preferences worth carrying across phones (alert run-ups, theme, price and backup options), mirrored into the
     * database just before each backup so a restore brings them back. Device and session state never goes in.
     */
    const val PREFS_PREFIX = "prefs."
}
