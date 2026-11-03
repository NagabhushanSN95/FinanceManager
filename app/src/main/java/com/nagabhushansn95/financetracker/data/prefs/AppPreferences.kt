package com.nagabhushansn95.financetracker.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * The v5 app's own preferences file. Nothing here is shared with the 4.x app's "AllPreferences" file.
 */
class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** True once the user has imported a 4.x database or chosen to start fresh. */
    var setupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETE, value).apply()

    /** Human-readable report of the last 4.x import, kept so it can be shown again from Extras. */
    var lastImportSummary: String?
        get() = prefs.getString(KEY_LAST_IMPORT_SUMMARY, null)
        set(value) = prefs.edit().putString(KEY_LAST_IMPORT_SUMMARY, value).apply()

    /** Where the last database-file backup was written, e.g. "Documents/Finance Tracker/Backups/…db". */
    var lastBackupDescription: String?
        get() = prefs.getString(KEY_LAST_BACKUP, null)
        set(value) = prefs.edit().putString(KEY_LAST_BACKUP, value).apply()

    fun isSummarySectionCollapsed(section: String): Boolean =
        prefs.getBoolean(KEY_SUMMARY_COLLAPSED_PREFIX + section, false)

    fun setSummarySectionCollapsed(section: String, collapsed: Boolean) =
        prefs.edit().putBoolean(KEY_SUMMARY_COLLAPSED_PREFIX + section, collapsed).apply()

    /** SYSTEM, LIGHT or DARK. */
    var themeMode: String
        get() = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    /** Emits the theme mode now and whenever it changes. */
    val themeModeFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_THEME) trySend(themeMode)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(themeMode)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    var dailyBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_BACKUP, true)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_BACKUP, value).apply()

    /** ISO date of the last automatic backup, e.g. "2026-11-04". */
    var lastAutoBackupDate: String?
        get() = prefs.getString(KEY_LAST_AUTO_BACKUP, null)
        set(value) = prefs.edit().putString(KEY_LAST_AUTO_BACKUP, value).apply()

    /** Most recently used ids for a picker (newest first), e.g. "account_from", "category", "instrument". */
    fun recentIds(picker: String): List<Long> =
        prefs.getString(KEY_RECENT_PREFIX + picker, null)?.split(',')
            ?.mapNotNull { it.toLongOrNull() } ?: emptyList()

    fun pushRecentId(picker: String, id: Long, keep: Int = 5) {
        val updated = (listOf(id) + recentIds(picker).filter { it != id }).take(keep)
        prefs.edit().putString(KEY_RECENT_PREFIX + picker, updated.joinToString(",")).apply()
    }

    companion object {
        private const val KEY_RECENT_PREFIX = "recent_"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_DAILY_BACKUP = "daily_backup_enabled"
        private const val KEY_LAST_AUTO_BACKUP = "last_auto_backup_date"
        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
        const val FILE_NAME = "finance_tracker_prefs"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_LAST_IMPORT_SUMMARY = "last_import_summary"
        private const val KEY_LAST_BACKUP = "last_backup"
        private const val KEY_SUMMARY_COLLAPSED_PREFIX = "summary_collapsed_"
    }
}
