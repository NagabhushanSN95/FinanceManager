package com.nagabhushansn95.financetracker.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.db.dao.Settings
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import java.io.File
import java.io.IOException

/**
 * Replaces the app's database with a "Finance Tracker Database - ….db" file written by [DatabaseBackup], and
 * brings back the preferences the backup carries in its settings table.
 *
 * The file is checked before anything is touched: it must be an SQLite database carrying Room's schema marker for
 * this app and the expected schema version. On first run ([restoreFrom]) the file is swapped in directly, nothing
 * is observing the database yet. While the app is running ([stage]) the file is parked next to the database and
 * swapped in by [applyPending] on the next start, before Room opens anything.
 */
class DatabaseRestore(private val context: Context) {

    data class Result(val accounts: Int, val transactions: Int)

    suspend fun restoreFrom(uri: Uri): Result {
        val temp = File(context.cacheDir, "restore.db")
        try {
            copyAndValidate(uri, temp)
            AppDatabase.replaceDatabaseFile(context, temp)
            val db = AppDatabase.getInstance(context)
            applyPreferences(db)
            return Result(
                accounts = db.accountDao().getAll().size,
                transactions = db.transactionDao().count()
            )
        } finally {
            temp.delete()
        }
    }

    /**
     * Checks the picked file and parks it as the pending restore; the preferences it carries are applied right
     * away (they live outside the database). The caller restarts the app, which swaps the file in.
     */
    fun stage(uri: Uri) {
        val pending = pendingFile(context)
        copyAndValidate(uri, pending)
        try {
            SQLiteDatabase.openDatabase(pending.path, null, SQLiteDatabase.OPEN_READONLY)
                .use { db ->
                    // Backups from before schema 2 have no settings table: nothing to bring back, the data still restores.
                    val hasSettings = db.rawQuery(
                        "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'settings'",
                        null
                    ).use { it.moveToFirst() && it.getInt(0) == 1 }
                    if (!hasSettings) return
                    val values = HashMap<String, String>()
                    db.rawQuery(
                        "SELECT `key`, value FROM settings WHERE `key` LIKE ?",
                        arrayOf(Settings.PREFS_PREFIX + "%")
                    ).use { c ->
                        while (c.moveToNext()) values[c.getString(0)
                            .removePrefix(Settings.PREFS_PREFIX)] =
                            c.getString(1)
                    }
                    AppPreferences(context).import(values)
                }
        } catch (e: Exception) {
            // Nothing may be left staged after a failure: it would be swapped in on the next start.
            pending.delete(); throw e
        } finally {
            File(pending.path + "-journal").delete()
            File(pending.path + "-wal").delete()
            File(pending.path + "-shm").delete()
        }
    }

    private fun copyAndValidate(uri: Uri, target: File) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open the selected file")
        input.use { source -> target.outputStream().use { source.copyTo(it) } }
        try {
            validate(target)
        } catch (e: Exception) {
            target.delete(); throw e
        }
    }

    private suspend fun applyPreferences(db: AppDatabase) {
        val rows = db.settingsDao().getWithPrefix(Settings.PREFS_PREFIX)
        if (rows.isNotEmpty()) AppPreferences(context).import(rows.associate {
            it.key.removePrefix(Settings.PREFS_PREFIX) to it.value
        })
    }

    private fun validate(file: File) {
        if (!file.isSqliteFile()) {
            throw IOException("The selected file is not an SQLite database. Pick a 'Finance Tracker Database - ….db' backup.")
        }
        val sqlite = try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: Exception) {
            throw IOException("The selected file could not be opened as a database: ${e.message}")
        }
        sqlite.use { db ->
            val hasRoomTable = db.rawQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'room_master_table'",
                null
            ).use { it.moveToFirst() && it.getInt(0) == 1 }
            val hasTransactions = db.rawQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'transactions'",
                null
            ).use { it.moveToFirst() && it.getInt(0) == 1 }
            if (!hasRoomTable || !hasTransactions) {
                throw IOException("This is not a Finance Tracker database. A Finance Manager 4.x file must be imported with the last option instead.")
            }
            val version = db.rawQuery("PRAGMA user_version", null)
                .use { if (it.moveToFirst()) it.getInt(0) else 0 }
            if (version > AppDatabase.SCHEMA_VERSION) {
                throw IOException("This backup was made by a newer Finance Tracker (schema $version). Update the app first.")
            }
        }
    }

    companion object {
        private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        private const val PENDING_NAME = "finance_tracker.restore-pending.db"

        private fun pendingFile(context: Context) =
            File(
                context.applicationContext.getDatabasePath(AppDatabase.DATABASE_NAME).parentFile,
                PENDING_NAME
            )

        /** Swaps in a restore staged by [stage], if there is one. Called before the database is first opened. */
        fun applyPending(context: Context) {
            val pending = pendingFile(context)
            if (!pending.isFile) return
            val target = context.applicationContext.getDatabasePath(AppDatabase.DATABASE_NAME)
            File(target.path + "-wal").delete()
            File(target.path + "-shm").delete()
            File(target.path + "-journal").delete()
            if (!pending.renameTo(target)) {
                pending.copyTo(target, overwrite = true); pending.delete()
            }
        }

        fun File.isSqliteFile(): Boolean {
            if (length() < SQLITE_HEADER.size) return false
            val header = ByteArray(SQLITE_HEADER.size)
            inputStream().use { it.read(header) }
            return header.contentEquals(SQLITE_HEADER)
        }
    }
}
