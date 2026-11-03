package com.nagabhushansn95.financetracker.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.room.useWriterConnection
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Copies the whole v5 database as the hand-over / backup format: one self-contained SQLite file that opens in any
 * SQLite tool.
 *
 * The copy is produced with `VACUUM INTO`, which writes a consistent snapshot regardless of the write-ahead log or
 * concurrent readers. The snapshot's header is then marked as an ordinary rollback-journal database, because a file
 * still flagged as WAL cannot be opened read-only on a desktop without its -wal/-shm siblings.
 */
class DatabaseBackup(private val context: Context, private val db: AppDatabase) {

    data class Result(val description: String)

    /** Suggested file name, e.g. "Finance Tracker Database - 2026-11-03 09-02-55.db". */
    fun suggestedFileName(now: LocalDateTime = LocalDateTime.now()): String =
        "Finance Tracker Database - ${now.format(FILE_NAME_FORMAT)}.db"

    /** Writes a snapshot of the database into [output]. The caller owns the stream. */
    suspend fun writeTo(output: OutputStream) {
        val snapshot = File.createTempFile("backup", ".db", context.cacheDir)
        try {
            snapshot.delete() // VACUUM INTO refuses to overwrite an existing file
            val escapedPath = snapshot.path.replace("'", "''")
            db.useWriterConnection { transactor ->
                transactor.usePrepared("VACUUM INTO '$escapedPath'") { it.step() }
            }
            snapshot.inputStream().use { input ->
                val header = ByteArray(HEADER_SIZE)
                if (input.read(header) < HEADER_SIZE) throw IOException("Snapshot is shorter than an SQLite header")
                header[WRITE_VERSION_OFFSET] = 1
                header[READ_VERSION_OFFSET] = 1
                output.write(header)
                input.copyTo(output)
            }
            output.flush()
        } finally {
            snapshot.delete()
        }
    }

    /** Writes the database to a document the user picked with ACTION_CREATE_DOCUMENT. */
    suspend fun saveTo(uri: Uri): Result {
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Could not open $uri for writing")
        stream.use { writeTo(it) }
        return Result(uri.lastPathSegment ?: uri.toString())
    }

    /**
     * Writes the database to the phone's shared Documents folder under [BACKUP_FOLDER], the same place the 4.x app
     * used for its "Database Backup" files. On Android 10 and later this goes through MediaStore, which needs no
     * storage permission; earlier versions need WRITE_EXTERNAL_STORAGE granted beforehand.
     */
    suspend fun saveToDocuments(now: LocalDateTime = LocalDateTime.now()): Result {
        val fileName = suggestedFileName(now)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOCUMENTS}/$BACKUP_FOLDER"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values)
                ?: throw IOException("MediaStore refused to create $fileName")
            try {
                val stream =
                    resolver.openOutputStream(uri, "wt") ?: throw IOException("Could not open $uri")
                stream.use { writeTo(it) }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            @Suppress("DEPRECATION")
            val folder = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                BACKUP_FOLDER
            )
            if (!folder.exists() && !folder.mkdirs()) throw IOException("Could not create ${folder.path}")
            File(folder, fileName).outputStream().use { writeTo(it) }
        }
        return Result("${Environment.DIRECTORY_DOCUMENTS}/$BACKUP_FOLDER/$fileName")
    }

    companion object {
        const val BACKUP_FOLDER = "Finance Tracker/Backups"
        const val MIME_TYPE = "application/vnd.sqlite3"
        private const val HEADER_SIZE = 100
        private const val WRITE_VERSION_OFFSET = 18
        private const val READ_VERSION_OFFSET = 19
        private val FILE_NAME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss")
    }
}
