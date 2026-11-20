package com.nagabhushansn95.financetracker.data.prices

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** The one network primitive the price sources need; replaced by a fake in tests. */
interface HttpClient {
    /** GET [url] and return the body as text; throws on any failure. */
    suspend fun get(url: String): String
}

class UrlHttpClient(private val timeoutMs: Int = 20_000) : HttpClient {
    override suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        var target = url
        repeat(4) {
            val connection = (URL(target).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) FinanceTracker/5.0")
                setRequestProperty("Accept", "*/*")
            }
            try {
                val code = connection.responseCode
                // Redirects across http/https are not followed by HttpURLConnection; do it by hand (AMFI does this).
                if (code in 300..399) {
                    target = connection.getHeaderField("Location")
                        ?: throw IllegalStateException("Redirect without location from $target")
                    return@repeat
                }
                if (code != 200) throw IllegalStateException("HTTP $code from $target")
                return@withContext connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
        throw IllegalStateException("Too many redirects from $url")
    }
}
