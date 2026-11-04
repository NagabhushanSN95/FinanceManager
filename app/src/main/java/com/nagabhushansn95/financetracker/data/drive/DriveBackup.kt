package com.nagabhushansn95.financetracker.data.drive

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Uploads database backups to the user's Google Drive with the Drive REST API and the `drive.file` scope (the app
 * sees only files it created). Files go into a visible folder, so they can be downloaded from drive.google.com and
 * restored like any other backup. Authorization comes from Google Play services' AuthorizationClient: the first
 * time the user consents on screen, afterwards tokens are issued silently (also inside the background worker).
 *
 * One-time set-up: a Google Cloud project with an OAuth client of type Android for this package name and the
 * signing certificate's SHA-1 (debug and release). No client id is needed in the code.
 */
class DriveBackup(private val context: Context) {

    sealed interface Auth {
        data class Token(val accessToken: String) : Auth

        /** The user has to consent first: launch this and call [authorizeFromResult] with the result. */
        data class NeedsConsent(val pendingIntent: PendingIntent) : Auth
    }

    /** A silent token when already granted, else the consent intent. */
    suspend fun authorize(): Auth = withContext(Dispatchers.IO) {
        val request =
            AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(SCOPE))).build()
        val result = Tasks.await(Identity.getAuthorizationClient(context).authorize(request))
        if (result.hasResolution()) Auth.NeedsConsent(result.pendingIntent!!)
        else Auth.Token(result.accessToken ?: throw IOException("Google returned no access token"))
    }

    /** The token after the consent screen returned, or a failure with Google's reason. */
    fun authorizeFromResult(data: Intent?): Result<String> = runCatching {
        Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data).accessToken
            ?: throw IOException("Google returned no access token")
    }.recoverCatching { e ->
        android.util.Log.w("DriveBackup", "Authorization failed", e)
        throw IOException(describe(e), e)
    }

    /** Google's status codes in words; 10 means the OAuth client for this package + signing key is missing. */
    private fun describe(e: Throwable): String = when (e) {
        is com.google.android.gms.common.api.ApiException -> when (e.statusCode) {
            10 -> "Google rejected the app (DEVELOPER_ERROR): no OAuth client for package ${context.packageName} and this signing key in the Google Cloud project"
            16 -> "Sign-in was cancelled"
            7 -> "No network"
            else -> "Google error ${e.statusCode}: ${
                e.message ?: com.google.android.gms.common.api.CommonStatusCodes.getStatusCodeString(
                    e.statusCode
                )
            }"
        }

        else -> e.message ?: e.javaClass.simpleName
    }

    /**
     * Uploads [bytes] as [fileName] into [folderPath] (levels separated by "/", created under My Drive when
     * missing) and keeps the newest [keep] files with [prunePrefix] there.
     */
    suspend fun upload(
        token: String,
        folderPath: String,
        fileName: String,
        mime: String,
        bytes: ByteArray,
        prunePrefix: String? = null,
        keep: Int = 7
    ): String =
        withContext(Dispatchers.IO) {
            val folderId = folderId(token, folderPath)
            val existing = list(
                token,
                "'$folderId' in parents and name = '${
                    fileName.replace(
                        "'",
                        "\\'"
                    )
                }' and trashed = false"
            )
            val id = if (existing.isNotEmpty()) {
                // Same name on the same day: replace the content instead of adding a copy.
                patchContent(token, existing.first().first, mime, bytes)
            } else {
                create(token, fileName, mime, bytes, folderId)
            }
            if (prunePrefix != null) {
                list(
                    token,
                    "'$folderId' in parents and name contains '$prunePrefix' and trashed = false"
                )
                    .sortedByDescending { it.second }.drop(keep)
                    .forEach { (oldId, _) -> delete(token, oldId) }
            }
            id
        }

    /**
     * Walks [folderPath] level by level from My Drive, creating what is missing. With the drive.file scope the app
     * sees only folders it created itself, so a level the user made by hand is not found and a same-named folder
     * appears next to it; moving the app's folder elsewhere in Drive is fine, it is found by name under its parent.
     */
    private fun folderId(token: String, folderPath: String): String {
        var parent = "root"
        for (level in levels(folderPath)) {
            val found = list(
                token,
                "name = '${
                    level.replace(
                        "'",
                        "\\'"
                    )
                }' and mimeType = '$FOLDER_MIME' and '$parent' in parents and trashed = false"
            )
            parent = found.firstOrNull()?.first ?: run {
                val body = JSONObject().put("name", level).put("mimeType", FOLDER_MIME)
                    .put("parents", org.json.JSONArray().put(parent)).toString()
                JSONObject(
                    request(
                        token,
                        "POST",
                        "$API/files?fields=id",
                        "application/json; charset=UTF-8",
                        body.toByteArray()
                    )
                ).getString("id")
            }
        }
        return parent
    }

    /** (id, name) pairs matching a Drive query. */
    private fun list(token: String, query: String): List<Pair<String, String>> {
        val url = "$API/files?spaces=drive&fields=files(id,name)&pageSize=100&q=${
            URLEncoder.encode(
                query,
                "UTF-8"
            )
        }"
        val files = JSONObject(request(token, "GET", url, null, null)).getJSONArray("files")
        return (0 until files.length()).map { files.getJSONObject(it) }
            .map { it.getString("id") to it.getString("name") }
    }

    private fun create(
        token: String,
        fileName: String,
        mime: String,
        bytes: ByteArray,
        folderId: String
    ): String {
        val metadata =
            JSONObject().put("name", fileName).put("parents", org.json.JSONArray().put(folderId))
                .toString()
        val response = request(
            token,
            "POST",
            "$UPLOAD/files?uploadType=multipart&fields=id",
            "multipart/related; boundary=$BOUNDARY",
            multipartBody(metadata, mime, bytes)
        )
        return JSONObject(response).getString("id")
    }

    private fun patchContent(token: String, id: String, mime: String, bytes: ByteArray): String {
        request(token, "PATCH", "$UPLOAD/files/$id?uploadType=media", mime, bytes)
        return id
    }

    private fun delete(token: String, id: String) {
        request(token, "DELETE", "$API/files/$id", null, null)
    }

    private fun request(
        token: String,
        method: String,
        url: String,
        contentType: String?,
        body: ByteArray?
    ): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 30_000
            readTimeout = 120_000
            setRequestProperty("Authorization", "Bearer $token")
            if (contentType != null) setRequestProperty("Content-Type", contentType)
            if (body != null) {
                doOutput = true; setFixedLengthStreamingMode(body.size)
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body) }
            val code = connection.responseCode
            if (code !in 200..299) {
                val error =
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException(
                    "Drive $method ${url.substringBefore('?')} failed: HTTP $code ${
                        error.take(
                            300
                        )
                    }"
                )
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val SCOPE = "https://www.googleapis.com/auth/drive.file"

        /** "Backups / Apps / Finance Tracker" -> [Backups, Apps, Finance Tracker]; blank means My Drive itself. */
        fun levels(folderPath: String): List<String> =
            folderPath.split('/', '\\').map { it.trim() }.filter { it.isNotEmpty() }

        private const val FOLDER_MIME = "application/vnd.google-apps.folder"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private const val BOUNDARY = "finance_tracker_upload_boundary"

        /** A multipart/related body: JSON metadata part, then the file part. */
        fun multipartBody(metadataJson: String, mime: String, bytes: ByteArray): ByteArray {
            val out = ByteArrayOutputStream()
            out.write("--$BOUNDARY\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadataJson\r\n--$BOUNDARY\r\nContent-Type: $mime\r\n\r\n".toByteArray())
            out.write(bytes)
            out.write("\r\n--$BOUNDARY--".toByteArray())
            return out.toByteArray()
        }
    }
}
