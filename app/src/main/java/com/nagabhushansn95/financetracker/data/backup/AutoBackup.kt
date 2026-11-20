package com.nagabhushansn95.financetracker.data.backup

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import java.io.File
import java.time.LocalDate

/**
 * Replaces the 4.x DailyBackupService: at most one database snapshot per calendar day into
 * Documents/Finance Tracker/Auto Backups, keeping the newest [KEEP] files. Runs on launch, but only does work on a
 * day that has no backup yet, so the usual launch costs one preference read.
 */
class AutoBackup(
    private val context: Context,
    private val db: AppDatabase,
    private val prefs: AppPreferences
) {

    suspend fun runIfDue(today: LocalDate = LocalDate.now()): Boolean {
        if (!prefs.dailyBackupEnabled) return false
        if (prefs.lastAutoBackupDate == today.toString()) return false
        return try {
            write(today)
            prune()
            prefs.lastAutoBackupDate = today.toString()
            true
        } catch (e: Exception) {
            Log.e("AutoBackup", "Daily backup failed", e)
            false
        }
    }

    private suspend fun write(today: LocalDate) {
        val backup = DatabaseBackup(context, db)
        val fileName = "Finance Tracker Auto Backup - $today.db"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, DatabaseBackup.MIME_TYPE)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOCUMENTS}/$FOLDER"
                )
            }
            val resolver = context.contentResolver
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            // A file with today's name may already exist (the preference was cleared); reuse it rather than make a copy.
            val existing = query().firstOrNull { it.second == fileName }?.first
            val uri = existing ?: resolver.insert(collection, values)
            ?: throw IllegalStateException("MediaStore refused $fileName")
            resolver.openOutputStream(uri, "wt")!!.use { backup.writeTo(it) }
        } else {
            @Suppress("DEPRECATION")
            val folder = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                FOLDER
            )
            if (!folder.exists() && !folder.mkdirs()) throw IllegalStateException("Could not create ${folder.path}")
            File(folder, fileName).outputStream().use { backup.writeTo(it) }
        }
    }

    /** Deletes the oldest auto backups beyond [KEEP]. Only this app's own files are visible to the query. */
    private fun prune() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            query().sortedByDescending { it.second }.drop(KEEP)
                .forEach { (uri, _) -> context.contentResolver.delete(uri, null, null) }
        } else {
            @Suppress("DEPRECATION")
            val folder = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                FOLDER
            )
            folder.listFiles { f -> f.name.startsWith(PREFIX) }?.sortedByDescending { it.name }
                ?.drop(KEEP)?.forEach { it.delete() }
        }
    }

    private fun query(): List<Pair<android.net.Uri, String>> {
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
        val selection =
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("${Environment.DIRECTORY_DOCUMENTS}/$FOLDER/", "$PREFIX%")
        val out = ArrayList<Pair<android.net.Uri, String>>()
        context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
            while (c.moveToNext()) out += ContentUris.withAppendedId(
                collection,
                c.getLong(0)
            ) to c.getString(1)
        }
        return out
    }

    companion object {
        const val FOLDER = "Finance Tracker/Auto Backups"
        const val PREFIX = "Finance Tracker Auto Backup - "
        const val KEEP = 7
    }
}
