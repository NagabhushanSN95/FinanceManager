package com.nagabhushansn95.financetracker.data.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import java.io.OutputStream

/** Writes a file into the phone's shared Documents folder (MediaStore on Android 10+, plain files before). */
object SharedDocuments {
    const val EXPORT_FOLDER = "Finance Tracker/Exports"
    const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    /** Returns the path shown to the user, e.g. "Documents/Finance Tracker/Exports/x.xlsx". */
    suspend fun write(
        context: Context,
        folder: String,
        fileName: String,
        mime: String,
        writer: suspend (OutputStream) -> Unit
    ): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOCUMENTS}/$folder"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                values
            )
                ?: throw IOException("MediaStore refused to create $fileName")
            try {
                (resolver.openOutputStream(uri, "wt")
                    ?: throw IOException("Could not open $uri")).use { writer(it) }
                values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null); throw e
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                folder
            )
            if (!dir.exists() && !dir.mkdirs()) throw IOException("Could not create ${dir.path}")
            File(dir, fileName).outputStream().use { writer(it) }
        }
        return "${Environment.DIRECTORY_DOCUMENTS}/$folder/$fileName"
    }

    suspend fun writeTo(
        context: Context,
        uri: Uri,
        writer: suspend (OutputStream) -> Unit
    ): String {
        (context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Could not open $uri for writing")).use { writer(it) }
        return uri.lastPathSegment ?: uri.toString()
    }
}
