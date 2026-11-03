package com.nagabhushansn95.financetracker.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import java.io.File
import java.io.IOException

/**
 * Replaces the app's database with a "Finance Tracker Database - ….db" file written by [DatabaseBackup].
 *
 * The file is checked before anything is touched: it must be an SQLite database carrying Room's schema marker for
 * this app and the expected schema version. Only used on first run for now, where there is nothing to lose.
 */
class DatabaseRestore(private val context: Context) {

    data class Result(val accounts: Int, val transactions: Int)

    suspend fun restoreFrom(uri: Uri): Result {
        val temp = File(context.cacheDir, "restore.db")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Could not open the selected file")
            input.use { source -> temp.outputStream().use { source.copyTo(it) } }
            validate(temp)
            AppDatabase.replaceDatabaseFile(context, temp)
            val db = AppDatabase.getInstance(context)
            return Result(
                accounts = db.accountDao().getAll().size,
                transactions = db.transactionDao().count()
            )
        } finally {
            temp.delete()
        }
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

        fun File.isSqliteFile(): Boolean {
            if (length() < SQLITE_HEADER.size) return false
            val header = ByteArray(SQLITE_HEADER.size)
            inputStream().use { it.read(header) }
            return header.contentEquals(SQLITE_HEADER)
        }
    }
}
