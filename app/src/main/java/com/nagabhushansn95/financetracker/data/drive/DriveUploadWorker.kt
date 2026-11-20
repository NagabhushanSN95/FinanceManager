package com.nagabhushansn95.financetracker.data.drive

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nagabhushansn95.financetracker.data.backup.AutoBackup
import com.nagabhushansn95.financetracker.data.backup.DatabaseBackup
import com.nagabhushansn95.financetracker.data.db.AppDatabase
import com.nagabhushansn95.financetracker.data.prefs.AppPreferences
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Takes a fresh snapshot of the database and uploads it to Google Drive as today's auto backup, keeping the
 * newest [AutoBackup.KEEP] there. Queued after the daily backup and by "Upload now"; waits for a network.
 */
class DriveUploadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = AppPreferences(applicationContext)
        val drive = DriveBackup(applicationContext)
        val token = when (val auth =
            runCatching { drive.authorize() }.getOrElse { return failed(prefs, it) }) {
            is DriveBackup.Auth.Token -> auth.accessToken
            is DriveBackup.Auth.NeedsConsent -> {
                prefs.lastDriveUpload = "Needs sign-in: open Settings › Google Drive"
                return Result.failure()
            }
        }
        return try {
            val bytes = ByteArrayOutputStream().also {
                DatabaseBackup(
                    applicationContext,
                    AppDatabase.getInstance(applicationContext)
                ).writeTo(it)
            }.toByteArray()
            val fileName = "${AutoBackup.PREFIX}${LocalDate.now()}.db"
            drive.upload(
                token,
                prefs.driveFolderPath,
                fileName,
                DatabaseBackup.MIME_TYPE,
                bytes,
                prunePrefix = AutoBackup.PREFIX,
                keep = AutoBackup.KEEP
            )
            prefs.lastDriveUpload =
                "${LocalDateTime.now().format(STAMP)} · $fileName (${bytes.size / 1024} KB)"
            Result.success()
        } catch (e: Exception) {
            failed(prefs, e)
        }
    }

    private fun failed(prefs: AppPreferences, e: Throwable): Result {
        Log.w("DriveUpload", "Upload failed", e)
        prefs.lastDriveUpload = "Failed ${
            LocalDateTime.now().format(STAMP)
        }: ${e.message?.take(120) ?: e.javaClass.simpleName}"
        return if (runAttemptCount < 3) Result.retry() else Result.failure()
    }

    companion object {
        private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm")
        private const val WORK = "drive_upload"

        /** One upload at a time; a new request replaces a queued one. */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<DriveUploadWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
