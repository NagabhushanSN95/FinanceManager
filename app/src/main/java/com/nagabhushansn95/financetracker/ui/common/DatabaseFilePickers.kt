package com.nagabhushansn95.financetracker.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import com.nagabhushansn95.financetracker.data.backup.DatabaseBackup

/**
 * System file pickers tuned for database files.
 *
 * The Storage Access Framework filters by MIME type only, and Android reports ".db" files as the generic
 * "application/octet-stream" (the same as the old ".snb" backups), so an extension filter is not possible.
 * The pickers therefore accept SQLite and generic binary types, hide everything else, and open in the folder
 * where the wanted files normally are. Whatever is picked is validated by the caller.
 */
object DatabaseFilePickers {
    /** Finance Manager 4.x wrote its data and database backups here. */
    const val LEGACY_BACKUP_FOLDER = "Documents/Finance Manager/Backups"
    const val BACKUP_FOLDER = "Documents/${DatabaseBackup.BACKUP_FOLDER}"

    val DATABASE_MIME_TYPES =
        arrayOf("application/vnd.sqlite3", "application/x-sqlite3", "application/octet-stream")

    /** Document URI of a folder on the primary shared storage, for [DocumentsContract.EXTRA_INITIAL_URI]. */
    fun sharedFolderUri(relativePath: String): Uri =
        DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:$relativePath"
        )

    /**
     * ACTION_OPEN_DOCUMENT limited to database-like files. Opens where the previous file was picked when
     * [lastPicked] returns its URI, otherwise in [initialFolder].
     */
    class Open(
        private val initialFolder: String,
        private val lastPicked: () -> Uri? = { null }
    ) : ActivityResultContracts.OpenDocument() {
        override fun createIntent(context: Context, input: Array<String>): Intent =
            super.createIntent(context, DATABASE_MIME_TYPES)
                .putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    lastPicked() ?: sharedFolderUri(initialFolder)
                )
    }

    /**
     * ACTION_OPEN_DOCUMENT for any file (statements come as CSV with assorted MIME types).
     *
     * Opens where the previous file was picked when [lastPicked] returns its URI (the picker shows the folder
     * that contains a file URI, for cloud providers as well), otherwise in [initialFolder].
     */
    class OpenAny(
        private val initialFolder: String,
        private val lastPicked: () -> Uri? = { null }
    ) :
        ActivityResultContracts.OpenDocument() {
        override fun createIntent(context: Context, input: Array<String>): Intent =
            super.createIntent(context, input)
                .putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    lastPicked() ?: sharedFolderUri(initialFolder)
                )
    }
}
