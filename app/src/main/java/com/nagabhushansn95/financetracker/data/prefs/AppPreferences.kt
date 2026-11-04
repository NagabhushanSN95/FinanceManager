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
    /** Report of the last Zerodha statement import, shown under Extras. */
    var lastZerodhaImportSummary: String?
        get() = prefs.getString(KEY_LAST_ZERODHA_IMPORT, null)
        set(value) = prefs.edit().putString(KEY_LAST_ZERODHA_IMPORT, value).apply()

    /** Accounts chosen for the last Zerodha import; -1 = none chosen yet. */
    var zerodhaDematAccountId: Long
        get() = prefs.getLong(KEY_ZERODHA_DEMAT, -1L)
        set(value) = prefs.edit().putLong(KEY_ZERODHA_DEMAT, value).apply()
    var zerodhaBankAccountId: Long
        get() = prefs.getLong(KEY_ZERODHA_BANK, -1L)
        set(value) = prefs.edit().putLong(KEY_ZERODHA_BANK, value).apply()

    /** URI of the last Zerodha statement picked, so the file picker opens in the same folder next time. */
    var lastZerodhaStatementUri: String?
        get() = prefs.getString(KEY_ZERODHA_STATEMENT_URI, null)
        set(value) = prefs.edit().putString(KEY_ZERODHA_STATEMENT_URI, value).apply()

    /** Upload the daily backup to Google Drive. */
    var driveUploadEnabled: Boolean
        get() = prefs.getBoolean(KEY_DRIVE_UPLOAD, false)
        set(value) = prefs.edit().putBoolean(KEY_DRIVE_UPLOAD, value).apply()

    /** Folder path inside My Drive, levels separated by "/". */
    var driveFolderPath: String
        get() = prefs.getString(KEY_DRIVE_FOLDER, DEFAULT_DRIVE_FOLDER) ?: DEFAULT_DRIVE_FOLDER
        set(value) = prefs.edit().putString(KEY_DRIVE_FOLDER, value).apply()
    var lastDriveUpload: String?
        get() = prefs.getString(KEY_LAST_DRIVE_UPLOAD, null)
        set(value) = prefs.edit().putString(KEY_LAST_DRIVE_UPLOAD, value).apply()

    var lastExportDescription: String?
        get() = prefs.getString(KEY_LAST_EXPORT, null)
        set(value) = prefs.edit().putString(KEY_LAST_EXPORT, value).apply()

    /** Fetch prices when Investments opens, at most every [priceRefreshInterval]. */
    var autoPrices: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PRICES, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_PRICES, value).apply()

    /** [PRICES_DAILY] (default) or [PRICES_HOURLY]. */
    var priceRefreshInterval: String
        get() = prefs.getString(KEY_PRICE_INTERVAL, PRICES_DAILY) ?: PRICES_DAILY
        set(value) = prefs.edit().putString(KEY_PRICE_INTERVAL, value).apply()
    val priceRefreshIntervalMillis: Long
        get() = if (priceRefreshInterval == PRICES_HOURLY) 60L * 60 * 1000 else 24L * 60 * 60 * 1000
    var lastPriceRefreshMillis: Long
        get() = prefs.getLong(KEY_LAST_PRICE_REFRESH, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_PRICE_REFRESH, value).apply()
    var lastPriceRefreshSummary: String?
        get() = prefs.getString(KEY_LAST_PRICE_SUMMARY, null)
        set(value) = prefs.edit().putString(KEY_LAST_PRICE_SUMMARY, value).apply()

    fun recentIds(picker: String): List<Long> =
        prefs.getString(KEY_RECENT_PREFIX + picker, null)?.split(',')
            ?.mapNotNull { it.toLongOrNull() } ?: emptyList()

    fun pushRecentId(picker: String, id: Long, keep: Int = 5) {
        val updated = (listOf(id) + recentIds(picker).filter { it != id }).take(keep)
        prefs.edit().putString(KEY_RECENT_PREFIX + picker, updated.joinToString(",")).apply()
    }

    companion object {
        private const val KEY_RECENT_PREFIX = "recent_"
        private const val KEY_LAST_ZERODHA_IMPORT = "last_zerodha_import"
        private const val KEY_ZERODHA_DEMAT = "zerodha_demat_account"
        private const val KEY_ZERODHA_BANK = "zerodha_bank_account"
        private const val KEY_ZERODHA_STATEMENT_URI = "zerodha_statement_uri"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_AUTO_PRICES = "auto_prices"
        private const val KEY_PRICE_INTERVAL = "price_refresh_interval"
        const val PRICES_HOURLY = "HOURLY"
        const val PRICES_DAILY = "DAILY"
        private const val KEY_LAST_EXPORT = "last_export"
        private const val KEY_DRIVE_UPLOAD = "drive_upload_enabled"
        private const val KEY_DRIVE_FOLDER = "drive_folder_path"
        const val DEFAULT_DRIVE_FOLDER = "Backups/Apps/Finance Tracker"
        private const val KEY_LAST_DRIVE_UPLOAD = "last_drive_upload"
        private const val KEY_LAST_PRICE_REFRESH = "last_price_refresh"
        private const val KEY_LAST_PRICE_SUMMARY = "last_price_summary"
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
