package com.nagabhushansn95.financetracker.data.prefs

import android.content.Context
import android.content.SharedPreferences

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

    companion object {
        const val FILE_NAME = "finance_tracker_prefs"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_LAST_IMPORT_SUMMARY = "last_import_summary"
        private const val KEY_LAST_BACKUP = "last_backup"
        private const val KEY_SUMMARY_COLLAPSED_PREFIX = "summary_collapsed_"
    }
}
